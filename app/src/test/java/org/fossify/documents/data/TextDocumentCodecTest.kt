package org.fossify.documents.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextDocumentCodecTest {
    @Test
    fun `preserves utf8 without a byte order mark`() {
        val text = "Hello, \u4E16\u754C"
        val bytes = text.toByteArray(Charsets.UTF_8)
        val decoded = TextDocumentCodec.decode(bytes)

        assertEquals(text, decoded.text)
        assertEquals(TextDocumentEncoding.UTF_8, decoded.encoding)
        assertArrayEquals(bytes, TextDocumentCodec.encode(decoded.text, decoded.encoding!!))
    }

    @Test
    fun `preserves utf8 byte order mark`() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
                "Hello".toByteArray(Charsets.UTF_8)
        val decoded = TextDocumentCodec.decode(bytes)

        assertEquals("Hello", decoded.text)
        assertEquals(TextDocumentEncoding.UTF_8_BOM, decoded.encoding)
        assertArrayEquals(bytes, TextDocumentCodec.encode(decoded.text, decoded.encoding!!))
    }

    @Test
    fun `preserves utf16 byte order and mark`() {
        val littleEndian = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) +
                "Hello".toByteArray(Charsets.UTF_16LE)
        val bigEndian = byteArrayOf(0xFE.toByte(), 0xFF.toByte()) +
                "Hello".toByteArray(Charsets.UTF_16BE)

        listOf(
            littleEndian to TextDocumentEncoding.UTF_16_LE,
            bigEndian to TextDocumentEncoding.UTF_16_BE,
        ).forEach { (bytes, encoding) ->
            val decoded = TextDocumentCodec.decode(bytes)

            assertEquals("Hello", decoded.text)
            assertEquals(encoding, decoded.encoding)
            assertArrayEquals(bytes, TextDocumentCodec.encode(decoded.text, decoded.encoding!!))
        }
    }

    @Test
    fun `marks invalid utf8 as unsupported`() {
        val decoded = TextDocumentCodec.decode(byteArrayOf(0xC3.toByte(), 0x28))

        assertNull(decoded.encoding)
    }

    @Test
    fun `does not treat unmarked utf16 as editable utf8`() {
        val decoded = TextDocumentCodec.decode("Hello".toByteArray(Charsets.UTF_16LE))

        assertNull(decoded.encoding)
    }

    @Test
    fun `does not treat utf32 as editable utf16`() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00, 0x00, 0x48, 0x00, 0x00, 0x00)

        assertNull(TextDocumentCodec.decode(bytes).encoding)
    }
}
