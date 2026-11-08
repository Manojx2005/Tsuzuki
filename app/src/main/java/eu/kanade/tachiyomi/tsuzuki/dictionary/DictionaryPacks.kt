package eu.kanade.tachiyomi.tsuzuki.dictionary

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.system.Os
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.HashingSink
import okio.sink
import org.json.JSONObject
import java.io.File
import java.io.IOException

internal data class DictionarySense(val partOfSpeech: String, val gloss: String)
internal data class DictionaryEntry(val headword: String, val reading: String, val senses: List<DictionarySense>)
internal data class InstalledDictionary(val version: String, val sizeBytes: Long, val license: String)
internal data class DictionaryPackManifest(val version: String, val sizeBytes: Long, val sha256: String)

internal class DictionaryPacks(context: Context, private val client: OkHttpClient) {
    private val directory = File(context.filesDir, "dictionaries")
    private val mutations = Mutex()

    suspend fun installed(language: DictionaryLanguage): InstalledDictionary? = withContext(Dispatchers.IO) {
        inspect(packFile(language), language)
    }

    suspend fun available(language: DictionaryLanguage): DictionaryPackManifest? = withContext(Dispatchers.IO) {
        client.newCall(request(language, "json")).await().use { response ->
            if (response.code == 404) return@withContext null
            if (!response.isSuccessful) throw IOException("Dictionary manifest HTTP ${response.code}")
            val body = response.body.source()
            if (body.request(MAX_MANIFEST_BYTES + 1)) throw IOException("Dictionary manifest is too large")
            val json = JSONObject(body.readUtf8())
            val version = json.getString("version")
            val size = json.getLong("sizeBytes")
            val sha256 = json.getString("sha256").lowercase()
            if (json.getInt("schema") != SCHEMA || json.getString("language") != language.code ||
                version.isBlank() || version.length > 80 || size !in 1..MAX_PACK_BYTES || !shaPattern.matches(sha256)
            ) {
                throw IOException("Invalid dictionary manifest")
            }
            DictionaryPackManifest(version, size, sha256)
        }
    }

    suspend fun install(language: DictionaryLanguage, manifest: DictionaryPackManifest): InstalledDictionary =
        mutations.withLock {
            withContext(Dispatchers.IO) {
                directory.mkdirs()
                directory.listFiles { file -> file.name.startsWith("dictionary-") && file.extension == "tmp" }
                    ?.forEach { it.delete() }
                val staged = File.createTempFile("dictionary-", ".tmp", directory)
                try {
                    client.newCall(request(language, "sqlite3")).awaitSuccess().use { response ->
                        val source = response.body.source()
                        val target = HashingSink.sha256(staged.sink())
                        val hash = target.use { sink ->
                            var copied = 0L
                            val buffer = Buffer()
                            while (true) {
                                ensureActive()
                                val count = source.read(buffer, 64 * 1024L)
                                if (count == -1L) break
                                copied += count
                                if (copied > manifest.sizeBytes) throw IOException("Dictionary size changed")
                                sink.write(buffer, count)
                            }
                            if (copied != manifest.sizeBytes) throw IOException("Dictionary download is incomplete")
                            sink.hash.hex()
                        }
                        if (hash != manifest.sha256) throw IOException("Dictionary checksum mismatch")
                    }
                    val installed = inspect(staged, language) ?: throw IOException("Invalid dictionary database")
                    if (installed.version != manifest.version) throw IOException("Dictionary version mismatch")
                    ensureActive()
                    Os.rename(staged.absolutePath, packFile(language).absolutePath)
                    installed
                } finally {
                    staged.delete()
                }
            }
        }

    suspend fun delete(language: DictionaryLanguage) = mutations.withLock {
        withContext(Dispatchers.IO) {
            val file = packFile(language)
            if (file.exists() && !file.delete()) throw IOException("Could not delete dictionary")
        }
    }

    suspend fun lookup(term: DictionaryTerm): List<DictionaryEntry> = withContext(Dispatchers.IO) {
        val file = packFile(term.language)
        if (!file.isFile) return@withContext emptyList()
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
        db.rawQuery("SELECT form FROM term LIMIT 1", null).use { if (!it.moveToFirst()) return@readPack null }
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
        } catch (_: android.database.sqlite.SQLiteException) {
            null
        }
    }

    private fun packFile(language: DictionaryLanguage) = File(directory, "dictionary-${language.code}.sqlite3")

    private fun request(language: DictionaryLanguage, extension: String) = Request.Builder()
        .url("$RELEASE/dictionary-${language.code}.$extension")
        .build()

    private companion object {
        const val SCHEMA = 1
        const val MAX_MANIFEST_BYTES = 16_384L
        const val MAX_PACK_BYTES = 256 * 1024 * 1024L
        const val RELEASE = "https://github.com/Manojx2005/Tsuzuki/releases/download/dictionaries-v1"
        val shaPattern = Regex("[0-9a-f]{64}")
    }
}
