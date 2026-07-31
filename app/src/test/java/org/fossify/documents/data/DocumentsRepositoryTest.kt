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

    @Test
    fun addingFavoriteDoesNotMakeDocumentRecent() {
        val folderDocument = document(uri = "folder", lastOpened = 0L)

        val result = updateFavoriteEntries(
            existing = emptyList(),
            selected = listOf(folderDocument),
            favorite = true,
        )

        assertTrue(result.removed.isEmpty())
        assertEquals(0L, result.retained.single().lastOpened)
        assertTrue(result.retained.single().isFavorite)
    }

    @Test
    fun removingFavoriteDropsDocumentWithNoRecentHistory() {
        val favorite = document(uri = "favorite", lastOpened = 0L, isFavorite = true)

        val result = updateFavoriteEntries(
            existing = listOf(favorite),
            selected = listOf(favorite),
            favorite = false,
        )

        assertTrue(result.retained.isEmpty())
        assertEquals(listOf(favorite), result.removed)
    }

    @Test
    fun removingFavoriteKeepsRecentDocument() {
        val favorite = document(uri = "favorite", lastOpened = 100L, isFavorite = true)

        val result = updateFavoriteEntries(
            existing = listOf(favorite),
            selected = listOf(favorite),
            favorite = false,
        )

        assertTrue(result.removed.isEmpty())
        assertFalse(result.retained.single().isFavorite)
        assertEquals(100L, result.retained.single().lastOpened)
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
