package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.gzip
import okio.source
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class BackupProtoReader(
    private val context: Context,
) {
    suspend fun read(uri: Uri, onField: suspend (fieldNumber: Int, data: ByteArray) -> Unit) {
        withContext(Dispatchers.IO) {
            (context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException()).use { inputStream ->
                readFields(
                    inputStream,
                    invalidFormat = { isJson ->
                        val message = if (isJson) {
                            MR.strings.invalid_backup_file_json
                        } else {
                            MR.strings.invalid_backup_file_unknown
                        }
                        IOException(context.stringResource(message))
                    },
                    onField,
                )
            }
        }
    }

    companion object {
        private const val WIRE_TYPE_VARINT = 0
        private const val WIRE_TYPE_64BIT = 1
        private const val WIRE_TYPE_LENGTH_DELIMITED = 2
        private const val WIRE_TYPE_32BIT = 5
        private const val GZIP_MAGIC = 0x1f8b
        private const val MAGIC_JSON_SIGNATURE1 = 0x7b7d // "{}"
        private const val MAGIC_JSON_SIGNATURE2 = 0x7b22 // "{\""
        private const val MAGIC_JSON_SIGNATURE3 = 0x7b0a // "{\n"

        internal suspend fun readFields(
            inputStream: InputStream,
            invalidFormat: (isJson: Boolean) -> IOException = { IOException("Invalid backup format") },
            onField: suspend (Int, ByteArray) -> Unit,
        ) {
            val source = inputStream.source().buffer()
            val signature = source.peek().readShort().toInt()
            val payloadSource = when (signature) {
                GZIP_MAGIC -> source.gzip().buffer()
                MAGIC_JSON_SIGNATURE1, MAGIC_JSON_SIGNATURE2, MAGIC_JSON_SIGNATURE3 -> throw invalidFormat(true)
                else -> source
            }
            payloadSource.use {
                val invalidProto = { invalidFormat(false) }
                while (true) {
                    val tag = readVarint(payloadSource, invalidProto) ?: break
                    val wireType = (tag and 0x07).toInt()
                    val fieldNumber = (tag ushr 3).toInt()
                    if (fieldNumber <= 0 || tag ushr 32 != 0L) throw invalidProto()
                    when (wireType) {
                        WIRE_TYPE_LENGTH_DELIMITED -> {
                            val length = readVarint(payloadSource, invalidProto) ?: throw invalidProto()
                            if (length !in 0..Int.MAX_VALUE.toLong()) throw invalidProto()
                            onField(fieldNumber, payloadSource.readByteArray(length))
                        }
                        WIRE_TYPE_VARINT -> readVarint(payloadSource, invalidProto) ?: throw invalidProto()
                        WIRE_TYPE_64BIT -> payloadSource.skip(8)
                        WIRE_TYPE_32BIT -> payloadSource.skip(4)
                        else -> throw invalidProto()
                    }
                }
            }
        }

        private fun readVarint(source: okio.BufferedSource, invalidProto: () -> IOException): Long? {
            if (source.exhausted()) return null
            var shift = 0
            var result = 0L
            while (shift < 64) {
                val byte = source.readByte().toInt() and 0xff
                result = result or ((byte and 0x7f).toLong() shl shift)
                if (byte and 0x80 == 0) return result
                shift += 7
            }
            throw invalidProto()
        }
    }
}
