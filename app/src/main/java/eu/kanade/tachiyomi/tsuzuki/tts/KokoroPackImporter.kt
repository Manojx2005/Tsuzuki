package eu.kanade.tachiyomi.tsuzuki.tts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Debug-only install of the listening-test pack from a zip the tester picked.
 *
 * The zip is streamed, never extracted by entry name: only `kokoro-cmu-v1/<allowlisted file>` entries are accepted, each
 * written to a staging directory with its own byte count capped at the manifest size. The staged directory then goes
 * through [KokoroPack.verify] (sizes and SHA-256 of every file) before it replaces any existing install. Any failure
 * deletes the staging directory and leaves the previous install untouched. Replacing an install briefly needs about
 * twice the pack size free.
 */
internal class KokoroPackImporter(private val pack: KokoroPack, private val staging: File = stagingFor(pack)) {

    class RejectedPackException(message: String) : IOException(message)

    suspend fun import(zip: InputStream) = withContext(Dispatchers.IO) {
        val allowed = pack.expectedSizes()
        staging.deleteRecursively()
        check(staging.mkdirs()) { "Could not create the staging folder" }
        try {
            val seen = mutableSetOf<String>()
            ZipInputStream(zip.buffered()).use { input ->
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val entry = input.nextEntry ?: break
                    val name = acceptedName(entry.name, entry.isDirectory, allowed.keys)
                        ?: continue // the pack folder itself
                    if (!seen.add(name)) throw RejectedPackException("Duplicate entry: $name")
                    val limit = if (name == KokoroPack.MANIFEST) MANIFEST_LIMIT else allowed.getValue(name)
                    copyLimited(input, File(staging, name), limit)
                }
            }
            val missing = allowed.keys - seen
            if (missing.isNotEmpty()) throw RejectedPackException("Missing files: ${missing.sorted()}")
            try {
                pack.verify(staging)
            } catch (error: IllegalArgumentException) {
                throw RejectedPackException("Pack failed verification: ${error.message ?: "checksum mismatch"}")
            }
            replaceInstall()
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun acceptedName(raw: String, directory: Boolean, allowed: Set<String>): String? {
        if (raw.startsWith("/") || '\\' in raw || raw.split('/').any { it == ".." || it == "." }) {
            throw RejectedPackException("Unsafe entry: $raw")
        }
        if (directory) {
            if (raw.trimEnd('/') == KokoroPack.NAME) return null
            throw RejectedPackException("Unexpected folder: $raw")
        }
        val parts = raw.split('/')
        if (parts.size != 2 || parts[0] != KokoroPack.NAME) throw RejectedPackException("Unexpected entry: $raw")
        val name = parts[1]
        if (name != KokoroPack.MANIFEST && name !in allowed) throw RejectedPackException("Unexpected file: $name")
        return name
    }

    private suspend fun copyLimited(input: InputStream, target: File, limit: Long) {
        var total = 0L
        target.outputStream().buffered().use { output ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count == -1) break
                total += count
                if (total > limit) throw RejectedPackException("${target.name} is larger than expected")
                output.write(buffer, 0, count)
            }
        }
    }

    private fun replaceInstall() {
        val target = pack.directory
        val previous = File(target.parentFile, target.name + ".previous")
        previous.deleteRecursively()
        if (target.exists() && !target.renameTo(previous)) throw IOException("Could not replace the installed pack")
        if (!staging.renameTo(target)) {
            if (previous.exists()) previous.renameTo(target)
            throw IOException("Could not install the pack")
        }
        previous.deleteRecursively()
    }

    private companion object {
        const val MANIFEST_LIMIT = 64L * 1024

        fun stagingFor(pack: KokoroPack) = File(pack.directory.parentFile, pack.directory.name + ".importing")
    }
}
