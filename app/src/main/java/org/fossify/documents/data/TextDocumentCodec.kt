package org.fossify.documents.data

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

internal object TextDocumentCodec {
    fun decode(bytes: ByteArray): DecodedTextDocument {
        if (unsupportedByteOrderMarks.any(bytes::startsWith)) {
            return DecodedTextDocument(
                text = bytes.toString(Charsets.UTF_8),
                encoding = null,
            )
        }

        val encoding = TextDocumentEncoding.entries.firstOrNull {
            it.byteOrderMark.isNotEmpty() && bytes.startsWith(it.byteOrderMark)
        } ?: TextDocumentEncoding.UTF_8
        val decoded = if (encoding == TextDocumentEncoding.UTF_8 && bytes.any { it == 0.toByte() }) {
            null
        } else {
            bytes.decodeStrictly(encoding.charset, encoding.byteOrderMark.size)
        }

        return if (decoded != null) {
            DecodedTextDocument(
                text = decoded,
                encoding = encoding,
            )
        } else {
            DecodedTextDocument(
                text = bytes.toString(Charsets.UTF_8),
                encoding = null,
            )
        }
    }

    fun encode(text: String, encoding: TextDocumentEncoding): ByteArray {
        return encoding.byteOrderMark + text.toByteArray(encoding.charset)
    }
}

private val unsupportedByteOrderMarks = listOf(
    byteArrayOf(0x00, 0x00, 0xFE.toByte(), 0xFF.toByte()),
    byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00, 0x00),
)

internal data class DecodedTextDocument(
    val text: String,
    val encoding: TextDocumentEncoding?,
)

internal enum class TextDocumentEncoding(
    val charset: Charset,
    val byteOrderMark: ByteArray,
) {
    UTF_8(Charsets.UTF_8, byteArrayOf()),
    UTF_8_BOM(Charsets.UTF_8, byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())),
    UTF_16_LE(Charsets.UTF_16LE, byteArrayOf(0xFF.toByte(), 0xFE.toByte())),
    UTF_16_BE(Charsets.UTF_16BE, byteArrayOf(0xFE.toByte(), 0xFF.toByte())),
}

private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
    return size >= prefix.size && prefix.indices.all { index -> this[index] == prefix[index] }
}

private fun ByteArray.decodeStrictly(charset: Charset, offset: Int): String? {
    return try {
        charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(this, offset, size - offset))
            .toString()
    } catch (_: CharacterCodingException) {
        null
    }
}
