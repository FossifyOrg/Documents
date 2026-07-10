package org.fossify.documents.data

import org.junit.Assert.assertTrue
import org.junit.Test
import org.zwobble.mammoth.DocumentConverter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocxConversionTest {
    @Test
    fun `converts docx paragraphs and bold text to semantic html`() {
        val docx = minimalDocx()
        validateDocxXml(ByteArrayInputStream(docx))
        val html = DocumentConverter().convertToHtml(ByteArrayInputStream(docx)).value

        assertTrue(html.contains("Hello "))
        assertTrue(html.contains("<strong>world</strong>"))
    }

    @Test(expected = IOException::class)
    fun `rejects doctype declarations before conversion`() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.addXml("word/document.xml", "x".repeat(8_190) + "<!DOCTYPE document><document/>")
        }

        validateDocxXml(ByteArrayInputStream(output.toByteArray()))
    }

    @Test(expected = IOException::class)
    fun `rejects utf16 doctype declarations before conversion`() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.addBytes(
                "word/document.xml",
                "<!DOCTYPE document><document/>".toByteArray(Charsets.UTF_16LE),
            )
        }

        validateDocxXml(ByteArrayInputStream(output.toByteArray()))
    }

    @Test(expected = DocumentTooLargeException::class)
    fun `rejects docx archives with too many entries`() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.addXml("first.xml", "<document/>")
            zip.addXml("second.xml", "<document/>")
        }

        validateDocxXml(
            input = ByteArrayInputStream(output.toByteArray()),
            maxEntries = 1,
            maxUncompressedBytes = 1_024,
        )
    }

    @Test(expected = DocumentTooLargeException::class)
    fun `rejects docx archives that expand past the size limit`() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.addXml("word/document.xml", "<document>${"x".repeat(128)}</document>")
        }

        validateDocxXml(
            input = ByteArrayInputStream(output.toByteArray()),
            maxEntries = 10,
            maxUncompressedBytes = 64,
        )
    }

    private fun minimalDocx(): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.addXml(
                "[Content_Types].xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                    <Default Extension="xml" ContentType="application/xml"/>
                    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                </Types>
                """.trimIndent(),
            )
            zip.addXml(
                "_rels/.rels",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                </Relationships>
                """.trimIndent(),
            )
            zip.addXml(
                "word/document.xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                    <w:body>
                        <w:p>
                            <w:r><w:t xml:space="preserve">Hello </w:t></w:r>
                            <w:r><w:rPr><w:b/></w:rPr><w:t>world</w:t></w:r>
                        </w:p>
                    </w:body>
                </w:document>
                """.trimIndent(),
            )
        }
        return output.toByteArray()
    }

    private fun ZipOutputStream.addXml(path: String, xml: String) {
        addBytes(path, xml.toByteArray())
    }

    private fun ZipOutputStream.addBytes(path: String, content: ByteArray) {
        putNextEntry(ZipEntry(path))
        write(content)
        closeEntry()
    }
}
