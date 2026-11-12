package eu.kanade.tachiyomi.tsuzuki.dictionary

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.sql.DriverManager
import java.sql.SQLException

class DictionaryPacksTest {
    @TempDir lateinit var root: File

    @Test
    fun rejectsMetadataAndTermWithoutLookupTables() = runTest {
        withDatabase(emptyList()) { packs ->
            assertNull(packs.installed(DictionaryLanguage.English))
        }
    }

    @Test
    fun rejectsEnglishPackMissingExceptionTable() = runTest {
        withDatabase(lookupTables) { packs ->
            assertNull(packs.installed(DictionaryLanguage.English))
        }
    }

    @Test
    fun rejectsSenseTableWithoutOrdinalColumn() = runTest {
        val tables = lookupTables.map {
            it.replace("ordinal INTEGER, ", "").replace("(1, 0, 'noun'", "(1, 'noun'")
        }
        withDatabase(tables + exceptionTable) { packs ->
            assertNull(packs.installed(DictionaryLanguage.English))
        }
    }

    @Test
    fun acceptsCompleteQueryableEnglishPack() = runTest {
        withDatabase(lookupTables + exceptionTable) { packs ->
            assertNotNull(packs.installed(DictionaryLanguage.English))
        }
    }

    // Run the production Android queries against real SQLite, adapting only the platform cursor API.
    private suspend fun withDatabase(tables: List<String>, block: suspend (DictionaryPacks) -> Unit) {
        val directory = File(root, "dictionaries").apply { mkdirs() }
        val file = File(directory, "dictionary-en.sqlite3")
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { connection ->
            connection.createStatement().use { statement ->
                listOf(
                    "PRAGMA user_version=1",
                    "CREATE TABLE metadata (key TEXT, value TEXT)",
                    "INSERT INTO metadata VALUES ('language', 'en'), ('version', '1'), ('license', 'WordNet')",
                    "CREATE TABLE term (form TEXT, entry INTEGER)",
                    "INSERT INTO term VALUES ('book', 1)",
                ).plus(tables).forEach { statement.execute(it) }
            }
            val database = mockk<SQLiteDatabase> {
                every { version } returns 1
                every { close() } returns Unit
                every { rawQuery(any(), any()) } answers {
                    try {
                        connection.createStatement().use { statement ->
                            statement.executeQuery(firstArg()).use { result ->
                                val rows = buildList {
                                    while (result.next()) {
                                        add((1..result.metaData.columnCount).map { result.getString(it) })
                                    }
                                }
                                var position = -1
                                mockk<Cursor> {
                                    every { moveToFirst() } answers {
                                        position = 0
                                        rows.isNotEmpty()
                                    }
                                    every { moveToNext() } answers { ++position < rows.size }
                                    every { getString(any()) } answers { rows[position][firstArg<Int>()] }
                                    every { count } returns rows.size
                                    every { close() } returns Unit
                                }
                            }
                        }
                    } catch (error: SQLException) {
                        throw SQLiteException(error.message)
                    }
                }
            }
            mockkStatic(SQLiteDatabase::class)
            try {
                every { SQLiteDatabase.openDatabase(any<String>(), any(), any<Int>()) } returns database
                val context = mockk<Context> { every { filesDir } returns root }
                block(DictionaryPacks(context, OkHttpClient()))
            } finally {
                unmockkStatic(SQLiteDatabase::class)
            }
        }
    }

    private val lookupTables = listOf(
        "CREATE TABLE entry (id INTEGER, headword TEXT, reading TEXT)",
        "INSERT INTO entry VALUES (1, 'book', '')",
        "CREATE TABLE sense (entry INTEGER, ordinal INTEGER, part TEXT, gloss TEXT)",
        "INSERT INTO sense VALUES (1, 0, 'noun', 'written work')",
    )
    private val exceptionTable = listOf("CREATE TABLE exception (form TEXT, lemma TEXT)")
}
