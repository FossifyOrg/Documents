package org.fossify.documents.data

import android.content.ContentResolver
import android.net.Uri
import org.fossify.documents.models.DocumentKind
import org.zwobble.mammoth.DocumentConverter
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

internal class StructuredDocumentLoader(
    private val resolver: ContentResolver,
) {
    fun load(uri: Uri, kind: DocumentKind, fileName: String): StructuredDocumentLoadResult {
        return when (kind) {
            DocumentKind.DOCX -> loadDocx(uri)
            DocumentKind.CSV -> loadCsv(uri, fileName)
            DocumentKind.HTML -> loadHtml(uri)
            else -> error("Unsupported structured document type: $kind")
        }
    }

    private fun loadDocx(uri: Uri): StructuredDocumentLoadResult {
        openInput(uri).use(::validateDocxXml)
        val html = openInput(uri).use { input ->
            DocumentConverter().convertToHtml(input).value
        }
        return StructuredDocumentLoadResult(
            content = StructuredDocumentContent.Web(
                html = HtmlDocumentSanitizer.sanitize(html, isFragment = true),
            ),
        )
    }

    private fun loadCsv(uri: Uri, fileName: String): StructuredDocumentLoadResult {
        val decoded = readText(uri)
        return StructuredDocumentLoadResult(
            content = CsvDocumentParser.parse(decoded.text, fileName),
            canEditText = decoded.encoding != null,
        )
    }

    private fun loadHtml(uri: Uri): StructuredDocumentLoadResult {
        val decoded = readText(uri)
        return StructuredDocumentLoadResult(
            content = StructuredDocumentContent.Web(
                html = HtmlDocumentSanitizer.sanitize(decoded.text),
            ),
        )
    }

    private fun readText(uri: Uri): DecodedTextDocument {
        return openInput(uri).use { input -> TextDocumentCodec.decode(input.readBytes()) }
    }

    private fun openInput(uri: Uri): InputStream {
        return resolver.openInputStream(uri) ?: throw IOException("Could not open this document.")
    }

}

internal data class StructuredDocumentLoadResult(
    val content: StructuredDocumentContent,
    val canEditText: Boolean = false,
)

internal fun validateDocxXml(input: InputStream) {
    validateDocxXml(input, MAX_DOCX_ENTRIES, MAX_DOCX_UNCOMPRESSED_BYTES)
}

internal fun validateDocxXml(
    input: InputStream,
    maxEntries: Int,
    maxUncompressedBytes: Long,
) {
    ZipInputStream(input.buffered()).use { zip ->
        var entryCount = 0
        var uncompressedBytes = 0L
        while (true) {
            val entry = zip.nextEntry ?: break
            entryCount += 1
            if (entryCount > maxEntries) {
                throw DocumentTooLargeException()
            }

            val entryName = entry.name.lowercase()
            val scanXml = entryName.endsWith(".xml") || entryName.endsWith(".rels")
            uncompressedBytes += zip.validateDocxEntry(
                scanXml = scanXml,
                remainingBytes = maxUncompressedBytes - uncompressedBytes,
            )
            zip.closeEntry()
        }
    }
}

private fun InputStream.validateDocxEntry(
    scanXml: Boolean,
    remainingBytes: Long,
): Long {
    val matched = IntArray(DOCTYPE_MARKERS.size)
    val buffer = ByteArray(XML_SCAN_BUFFER_SIZE)
    var bytesRead = 0L
    while (true) {
        val count = read(buffer)
        if (count == -1) {
            return bytesRead
        }

        bytesRead += count
        if (bytesRead > remainingBytes) {
            throw DocumentTooLargeException()
        }
        if (!scanXml) {
            continue
        }

        repeat(count) { index ->
            val value = buffer[index].toInt().and(UNSIGNED_BYTE_MASK).uppercaseAscii()
            DOCTYPE_MARKERS.forEachIndexed { markerIndex, marker ->
                matched[markerIndex] = when (value) {
                    marker[matched[markerIndex]].toInt().and(UNSIGNED_BYTE_MASK) -> {
                        matched[markerIndex] + MATCH_INCREMENT
                    }

                    marker.first().toInt().and(UNSIGNED_BYTE_MASK) -> MATCH_INCREMENT
                    else -> 0
                }
                if (matched[markerIndex] == marker.size) {
                    throw IOException("This document contains an unsupported XML declaration.")
                }
            }
        }
    }
}

internal class DocumentTooLargeException : IOException()

private fun Int.uppercaseAscii(): Int = if (this in 'a'.code..'z'.code) this - ASCII_CASE_OFFSET else this

private fun String.utf16Marker(littleEndian: Boolean): ByteArray {
    return ByteArray(length * 2) { index ->
        val character = this[index / 2].code.toByte()
        if ((index % 2 == 0) == littleEndian) character else 0
    }
}

private const val DOCTYPE_MARKER = "<!DOCTYPE"
private const val XML_SCAN_BUFFER_SIZE = 8_192
private const val ASCII_CASE_OFFSET = 32
private const val UNSIGNED_BYTE_MASK = 0xFF
private const val MATCH_INCREMENT = 1
private const val MAX_DOCX_ENTRIES = 4_096
private const val MAX_DOCX_UNCOMPRESSED_BYTES = 64L * 1024L * 1024L
private val DOCTYPE_MARKERS = listOf(
    DOCTYPE_MARKER.encodeToByteArray(),
    DOCTYPE_MARKER.utf16Marker(littleEndian = true),
    DOCTYPE_MARKER.utf16Marker(littleEndian = false),
)
