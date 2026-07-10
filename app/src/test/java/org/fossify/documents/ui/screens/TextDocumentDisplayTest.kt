package org.fossify.documents.ui.screens

import androidx.compose.ui.text.TextRange
import org.junit.Assert.assertEquals
import org.junit.Test

class TextDocumentDisplayTest {
    @Test
    fun `finds case insensitive matches in document order`() {
        assertEquals(
            listOf(TextRange(0, 4), TextRange(10, 14)),
            findTextMatches("Text then TEXT", "text"),
        )
    }

    @Test
    fun `finds single character matches`() {
        assertEquals(
            listOf(TextRange(1, 2), TextRange(3, 4), TextRange(5, 6)),
            findTextMatches("banana", "a"),
        )
    }

    @Test
    fun `does not return overlapping matches`() {
        assertEquals(
            listOf(TextRange(0, 2), TextRange(2, 4)),
            findTextMatches("aaaa", "aa"),
        )
    }

    @Test
    fun `blank query has no matches`() {
        assertEquals(emptyList<TextRange>(), findTextMatches("document", ""))
    }

    @Test
    fun `coerces invalid and out of range zoom`() {
        assertEquals(MIN_DOCUMENT_TEXT_ZOOM, 0.1f.coerceDocumentTextZoom())
        assertEquals(MAX_DOCUMENT_TEXT_ZOOM, 10f.coerceDocumentTextZoom())
        assertEquals(DEFAULT_DOCUMENT_TEXT_ZOOM, Float.NaN.coerceDocumentTextZoom())
    }

    @Test
    fun `skips highlighting for excessive match counts`() {
        assertEquals(true, shouldHighlightTextMatches(matchCount = 120))
        assertEquals(false, shouldHighlightTextMatches(matchCount = 6_000))
        assertEquals(false, shouldHighlightTextMatches(matchCount = 0))
    }
}
