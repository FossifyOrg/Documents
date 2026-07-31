package org.fossify.documents.models

import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentKindTest {
    @Test
    fun `recognizes supported formats by extension`() {
        assertEquals(DocumentKind.PDF, DocumentKind.fromName("report.PDF", ""))
        assertEquals(DocumentKind.DOCX, DocumentKind.fromName("report.docx", "application/zip"))
        assertEquals(DocumentKind.TEXT, DocumentKind.fromName("notes.txt", ""))
        assertEquals(DocumentKind.TEXT, DocumentKind.fromName("calendar.ics", ""))
        assertEquals(DocumentKind.MARKDOWN, DocumentKind.fromName("guide.md", "text/plain"))
        assertEquals(DocumentKind.CSV, DocumentKind.fromName("people.csv", "text/plain"))
        assertEquals(DocumentKind.CSV, DocumentKind.fromName("people.tsv", "text/plain"))
        assertEquals(DocumentKind.HTML, DocumentKind.fromName("index.html", "text/plain"))
        assertEquals(DocumentKind.HTML, DocumentKind.fromName("page.xhtml", "text/plain"))
    }

    @Test
    fun `recognizes supported formats by mime type`() {
        assertEquals(
            DocumentKind.DOCX,
            DocumentKind.fromName(
                "document",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            ),
        )
        assertEquals(DocumentKind.CSV, DocumentKind.fromName("table", "text/csv; charset=utf-8"))
        assertEquals(DocumentKind.CSV, DocumentKind.fromName("table", "text/tab-separated-values"))
        assertEquals(DocumentKind.HTML, DocumentKind.fromName("page", "text/html"))
        assertEquals(DocumentKind.HTML, DocumentKind.fromName("page", "application/xhtml+xml"))
    }

    @Test
    fun `specific text formats win over generic text mime type`() {
        assertEquals(DocumentKind.MARKDOWN, DocumentKind.fromName("README.md", "text/plain"))
        assertEquals(DocumentKind.CSV, DocumentKind.fromName("budget.csv", "text/plain"))
        assertEquals(DocumentKind.HTML, DocumentKind.fromName("article.htm", "text/plain"))
    }

    @Test
    fun `does not treat legacy word or excel files as supported`() {
        assertEquals(DocumentKind.OTHER, DocumentKind.fromName("letter.doc", "application/msword"))
        assertEquals(
            DocumentKind.OTHER,
            DocumentKind.fromName("workbook.xls", "application/vnd.ms-excel"),
        )
    }
}
