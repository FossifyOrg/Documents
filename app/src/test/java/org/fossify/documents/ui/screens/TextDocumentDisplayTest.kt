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
}
