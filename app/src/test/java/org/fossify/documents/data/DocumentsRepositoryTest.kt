package org.fossify.documents.data

import org.fossify.documents.models.DocumentEntry
import org.fossify.documents.models.DocumentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentsRepositoryTest {
    @Test
    fun clearRecentEntriesRemovesHistoryButPreservesFavorites() {
        val favorite = document(uri = "favorite", lastOpened = 200L, isFavorite = true)
        val recent = document(uri = "recent", lastOpened = 100L)

        val result = clearRecentEntries(listOf(favorite, recent))

        assertEquals(listOf("favorite"), result.retained.map { it.uri })
        assertEquals(0L, result.retained.single().lastOpened)
        assertTrue(result.retained.single().isFavorite)
        assertEquals(listOf("recent"), result.removed.map { it.uri })
    }

    @Test
    fun documentKindRecognizesSupportedFormats() {
        assertEquals(DocumentKind.PDF, DocumentKind.fromName("invoice.PDF", ""))
        assertEquals(DocumentKind.MARKDOWN, DocumentKind.fromName("notes.md", "text/plain"))
        assertEquals(DocumentKind.TEXT, DocumentKind.fromName("calendar.ics", ""))
        assertEquals(DocumentKind.DOCX, DocumentKind.fromName("letter.docx", ""))
    }

    @Test
    fun clearRecentEntriesDoesNotTurnRemovedDocumentsIntoFavorites() {
        val result = clearRecentEntries(listOf(document(uri = "recent", lastOpened = 100L)))

        assertTrue(result.retained.isEmpty())
        assertFalse(result.removed.single().isFavorite)
    }

    private fun document(
        uri: String,
        lastOpened: Long,
        isFavorite: Boolean = false,
    ) = DocumentEntry(
        uri = uri,
        name = "$uri.txt",
        mimeType = "text/plain",
        kind = DocumentKind.TEXT,
        location = "",
        size = 1L,
        lastModified = 1L,
        lastOpened = lastOpened,
        isFavorite = isFavorite,
    )
}
