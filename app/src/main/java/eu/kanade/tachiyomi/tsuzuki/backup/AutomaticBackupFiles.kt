package eu.kanade.tachiyomi.tsuzuki.backup

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.BuildConfig
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import okio.Buffer
import okio.ForwardingSink
import okio.Sink
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class AutomaticBackupFiles(private val directory: UniFile) {
    private val filename = buildString {
        append(backupFilePrefix(BuildConfig.APPLICATION_ID))
        append('_')
        append(SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.ENGLISH).format(Date()))
        append(".tachibk")
    }

    fun createStagingFile(): UniFile? = directory.createFile("$filename.part")

    fun publish(staged: UniFile): UniFile {
        if (staged.renameTo(filename)) return staged
        // SAF providers can support creating and reading files without supporting rename.
        val destination = directory.createFile(filename) ?: throw IOException()
        try {
            staged.openInputStream().use { input ->
                destination.openOutputStream().use { output -> input.copyTo(output) }
            }
        } catch (e: Exception) {
            delete(destination)
            throw e
        }
        delete(staged)
        return destination
    }

    fun prune(keep: Int, newest: UniFile) {
        val name = newest.name ?: return
        val regex = backupFileRegex(BuildConfig.APPLICATION_ID)
        val backups = directory.listFiles().orEmpty().filter { it.isFile }
        backupsToPrune(backups, regex, keep, name) { it.name }.forEach(::delete)
        backups.filter { regex.matches(it.name?.removeSuffix(".part").orEmpty()) && it.name?.endsWith(".part") == true }
            .forEach(::delete)
    }

    private fun delete(file: UniFile) {
        try {
            if (!file.delete()) logcat(LogPriority.WARN) { "Could not remove old backup ${file.name}" }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Could not remove old backup" }
        }
    }
}

internal class BackupSizeSink(delegate: Sink) : ForwardingSink(delegate) {
    var bytesWritten = 0L
        private set

    override fun write(source: Buffer, byteCount: Long) {
        super.write(source, byteCount)
        bytesWritten += byteCount
    }
}
