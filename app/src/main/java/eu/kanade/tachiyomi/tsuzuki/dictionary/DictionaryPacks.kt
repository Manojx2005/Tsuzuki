package eu.kanade.tachiyomi.tsuzuki.dictionary

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.OpenableColumns
import android.system.Os
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import logcat.LogPriority
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.BufferedSource
import okio.GzipSource
import okio.HashingSink
import okio.buffer
import okio.sink
import okio.source
import tachiyomi.core.common.util.system.logcat
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

internal data class DictionarySense(val partOfSpeech: String, val gloss: String)
internal data class DictionaryEntry(val headword: String, val reading: String, val senses: List<DictionarySense>)
internal data class InstalledDictionary(
    val version: String,
    val sizeBytes: Long,
    val license: String,
    val isBundled: Boolean = false,
)
internal data class DictionaryPackManifest(val version: String, val sizeBytes: Long, val sha256: String)

internal class DictionaryPacks(private val context: Context, private val client: OkHttpClient) {
    private val directory = File(context.filesDir, "dictionaries")
    private val mutations = Mutex()

    suspend fun installed(language: DictionaryLanguage): InstalledDictionary? = withContext(Dispatchers.IO) {
        inspect(packFile(language), language)
            ?: bundled(language)?.let { inspect(it, language)?.copy(isBundled = true) }
    }

    suspend fun available(language: DictionaryLanguage): DictionaryPackManifest? = withContext(Dispatchers.IO) {
        client.newCall(request(language, "json")).await().use { response ->
            if (response.code == 404) return@withContext null
            if (!response.isSuccessful) throw IOException("Dictionary manifest HTTP ${response.code}")
            val (packLanguage, manifest) = readManifest(response.body.source())
            if (packLanguage != language) throw IOException("Dictionary language mismatch")
            manifest
        }
    }

    suspend fun install(language: DictionaryLanguage, manifest: DictionaryPackManifest): InstalledDictionary =
        mutations.withLock {
            withContext(Dispatchers.IO) {
                client.newCall(request(language, "sqlite3")).awaitSuccess().use { response ->
                    installPack(language, manifest, response.body.source(), packFile(language))
                }
            }
        }

    suspend fun importFiles(files: List<Uri>): InstalledDictionary = mutations.withLock {
        withContext(Dispatchers.IO) {
            if (files.size != 2) throw IOException("Select a pack and its manifest")
            val documents = files.associateWith(::displayName)
            val manifestUri = documents.entries.singleOrNull { it.value.endsWith(".json", ignoreCase = true) }?.key
                ?: throw IOException("Select one dictionary manifest")
            val packUri = documents.keys.singleOrNull { it != manifestUri }
                ?: throw IOException("Select one dictionary pack")
            val (language, manifest) = openDocument(manifestUri).use(::readManifest)
            openDocument(packUri).use { source ->
                installPack(language, manifest, source, packFile(language))
            }
        }
    }

    private fun displayName(uri: Uri): String {
        if (uri.scheme != "content") return uri.lastPathSegment.orEmpty()
        return context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment.orEmpty()
    }

    private fun openDocument(uri: Uri): BufferedSource =
        context.contentResolver.openInputStream(uri)?.source()?.buffer()
            ?: throw IOException("Cannot open dictionary document")

    // Bundled packs verified (or extracted) in this process; a lookup then skips the manifest read and schema check.
    private val verifiedBundles = ConcurrentHashMap<DictionaryLanguage, File>()

    private suspend fun bundled(language: DictionaryLanguage): File? {
        verifiedBundles[language]?.takeIf(File::isFile)?.let { return it }
        return mutations.withLock {
            verifiedBundles[language]?.takeIf(File::isFile)
                ?: verifyBundled(language)?.also { verifiedBundles[language] = it }
        }
    }

    private suspend fun verifyBundled(language: DictionaryLanguage): File? {
        val prefix = "dictionaries/dictionary-${language.code}"
        return try {
            val (packLanguage, manifest) = context.assets.open("$prefix.json").source().buffer().use(::readManifest)
            if (packLanguage != language) throw IOException("Bundled dictionary language mismatch")
            val target = File(directory, "bundled/dictionary-${language.code}.sqlite3")
            if (target.length() == manifest.sizeBytes && inspect(target, language)?.version == manifest.version) {
                return target
            }
            context.assets.open("$prefix.pack").source().buffer().use { source ->
                installPack(language, manifest, source, target)
            }
            target
        } catch (error: android.system.ErrnoException) {
            logcat(LogPriority.WARN, error) { "Could not initialize bundled dictionary ${language.code}" }
            null
        } catch (error: IOException) {
            logcat(LogPriority.WARN, error) { "Could not extract bundled dictionary ${language.code}" }
            null
        }
    }

    private suspend fun installPack(
        language: DictionaryLanguage,
        manifest: DictionaryPackManifest,
        source: BufferedSource,
        destination: File,
    ): InstalledDictionary {
        val parent = checkNotNull(destination.parentFile)
        parent.mkdirs()
        parent.listFiles { file -> file.name.startsWith("dictionary-") && file.extension == "tmp" }
            ?.forEach(File::delete)
        val staged = File.createTempFile("dictionary-", ".tmp", parent)
        try {
            val input = if (source.request(2) && source.peek().readShort().toInt() == 0x1f8b) {
                GzipSource(source).buffer()
            } else {
                source
            }
            input.use {
                HashingSink.sha256(staged.sink()).use { sink ->
                    val buffer = Buffer()
                    var copied = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer, 64 * 1024L)
                        if (count == -1L) break
                        copied += count
                        if (copied > manifest.sizeBytes) throw IOException("Dictionary size changed")
                        sink.write(buffer, count)
                    }
                    if (copied != manifest.sizeBytes || sink.hash.hex() != manifest.sha256) {
                        throw IOException("Dictionary bytes do not match the manifest")
                    }
                }
            }
            val installed = inspect(staged, language) ?: throw IOException("Invalid dictionary database")
            if (installed.version != manifest.version) throw IOException("Dictionary version mismatch")
            currentCoroutineContext().ensureActive()
            Os.rename(staged.absolutePath, destination.absolutePath)
            return installed
        } finally {
            staged.delete()
        }
    }

    suspend fun delete(language: DictionaryLanguage) = mutations.withLock {
        withContext(Dispatchers.IO) {
            val file = packFile(language)
            if (file.exists() && !file.delete()) throw IOException("Could not delete dictionary")
        }
    }

    suspend fun lookup(term: DictionaryTerm): List<DictionaryEntry> = withContext(Dispatchers.IO) {
        val file = packFile(term.language).takeIf(File::isFile) ?: bundled(term.language)
            ?: return@withContext emptyList()
        readPack(file) { db ->
            val candidates = when (term.language) {
                DictionaryLanguage.English -> englishCandidates(db, term.value)
                DictionaryLanguage.Japanese -> japaneseDictionaryForms(term.value)
            }
            val exact = candidates.firstNotNullOfOrNull { form -> entriesFor(db, form).takeIf { it.isNotEmpty() } }
            exact ?: if (term.language == DictionaryLanguage.Japanese && term.value.length > 1) {
                // A platform selection may contain adjacent Japanese text as well as the word.
                (term.value.length - 1 downTo 1).firstNotNullOfOrNull { length ->
                    val forms = japaneseDictionaryForms(term.value.take(length))
                    forms.firstNotNullOfOrNull { form -> entriesFor(db, form).takeIf { it.isNotEmpty() } }
                }.orEmpty()
            } else {
                emptyList()
            }
        }.orEmpty()
    }

    private fun englishCandidates(db: SQLiteDatabase, word: String): List<String> {
        val direct = englishDictionaryForms(word)
        val exceptions = db.rawQuery("SELECT lemma FROM exception WHERE form = ?", arrayOf(word)).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        return (listOf(word) + exceptions + direct).distinct()
    }

    private fun entriesFor(db: SQLiteDatabase, form: String): List<DictionaryEntry> = db.rawQuery(
        """
        SELECT e.id, e.headword, e.reading, s.part, s.gloss
        FROM (
            SELECT entry FROM term WHERE form = ? ORDER BY entry LIMIT 8
        ) t JOIN entry e ON e.id = t.entry JOIN sense s ON s.entry = e.id
        WHERE s.ordinal < 12
        ORDER BY e.id, s.ordinal
        """.trimIndent(),
        arrayOf(form),
    ).use { cursor ->
        val entries = LinkedHashMap<Long, DictionaryEntry>()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val entry = entries[id] ?: DictionaryEntry(
                cursor.getString(1),
                cursor.getString(2),
                emptyList(),
            )
            entries[id] = entry.copy(senses = entry.senses + DictionarySense(cursor.getString(3), cursor.getString(4)))
        }
        entries.values.toList()
    }

    private fun inspect(file: File, language: DictionaryLanguage): InstalledDictionary? = readPack(file) { db ->
        val meta = db.rawQuery("SELECT key, value FROM metadata", null).use { cursor ->
            buildMap { while (cursor.moveToNext()) put(cursor.getString(0), cursor.getString(1)) }
        }
        if (db.version != SCHEMA || meta["language"] != language.code) return@readPack null
        val version = meta["version"]?.takeIf { it.isNotBlank() } ?: return@readPack null
        val license = meta["license"]?.takeIf { it.isNotBlank() } ?: return@readPack null
        // Every table and column lookup() reads must exist, and one term must resolve to an entry with a sense.
        // A missing table or column throws SQLiteException, which readPack reports as an invalid pack.
        db.rawQuery(PACK_LOOKUP_CHECK, null).use { if (!it.moveToFirst()) return@readPack null }
        if (language == DictionaryLanguage.English) db.rawQuery(PACK_EXCEPTION_CHECK, null).use { it.count }
        InstalledDictionary(version, file.length(), license)
    }

    private fun <T> readPack(file: File, read: (SQLiteDatabase) -> T): T? {
        if (!file.isFile) return null
        return try {
            SQLiteDatabase.openDatabase(
                file.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
            ).use(read)
        } catch (error: android.database.sqlite.SQLiteException) {
            logcat(LogPriority.WARN, error) { "Could not read dictionary pack" }
            null
        }
    }

    private fun packFile(language: DictionaryLanguage) = File(directory, "dictionary-${language.code}.sqlite3")

    private fun request(language: DictionaryLanguage, extension: String) = Request.Builder()
        .url("$RELEASE/dictionary-${language.code}.$extension")
        .build()

    private companion object {
        const val SCHEMA = 1
        const val PACK_LOOKUP_CHECK =
            "SELECT t.form, e.id, e.headword, e.reading, s.part, s.gloss, s.ordinal " +
                "FROM term t JOIN entry e ON e.id = t.entry JOIN sense s ON s.entry = e.id LIMIT 1"
        const val PACK_EXCEPTION_CHECK = "SELECT form, lemma FROM exception LIMIT 0"
        const val RELEASE = "https://github.com/Manojx2005/Tsuzuki/releases/download/dictionaries-v1"
    }
}
