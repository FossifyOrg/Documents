package org.fossify.documents.viewmodels

import org.fossify.documents.models.DocumentEntry
import org.fossify.documents.models.DocumentFolder
import org.fossify.documents.models.DocumentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentsSelectionTest {
    @Test
    fun `select all on home stays within selected recent section`() {
        val recentDocuments = (1..5).map(::document)
        val favoriteDocument = recentDocuments.first().copy(isFavorite = true)
        val state = DocumentsUiState(
            documents = recentDocuments,
            recentDocuments = recentDocuments,
            favoriteDocuments = listOf(favoriteDocument),
        )

        val selected = state.documentsForSelectAll(
            mapOf(
                favoriteDocument.uri to SelectedDocument(
                    favoriteDocument,
                    HOME_RECENT_SELECTION_PREFIX,
                )
            )
        )

        assertEquals(HOME_RECENT_LIMIT, selected.size)
        assertEquals(recentDocuments.take(HOME_RECENT_LIMIT).map { it.uri }, selected.keys.toList())
        assertTrue(selected.values.all { it.selectionPrefix == HOME_RECENT_SELECTION_PREFIX })
    }

    @Test
    fun `select all uses currently filtered documents on collection screens`() {
        val visibleDocuments = listOf(document(2), document(4))
        val state = DocumentsUiState(
            visibleDocuments = visibleDocuments,
            homeSection = DocumentsHomeSection.RECENT,
        )

        val selected = state.documentsForSelectAll(
            mapOf(
                visibleDocuments.first().uri to SelectedDocument(
                    visibleDocuments.first(),
                    "document_RECENT_",
                )
            )
        )

        assertEquals(
            visibleDocuments.map { it.uri }.toSet(),
            selected.keys,
        )
    }

    @Test
    fun `select all on home selects only rendered folders`() {
        val folders = (1..5).map(::folder)
        val state = DocumentsUiState(folders = folders)

        val selected = state.foldersForSelectAll()

        assertEquals(HOME_FOLDER_LIMIT, selected.size)
        assertEquals(folders.take(HOME_FOLDER_LIMIT).map { it.uri }, selected.keys.toList())
    }

    @Test
    fun `select all on folders screen uses filtered folders`() {
        val visibleFolders = listOf(folder(2), folder(4))
        val state = DocumentsUiState(
            folders = (1..5).map(::folder),
            visibleFolders = visibleFolders,
            homeSection = DocumentsHomeSection.FOLDERS,
        )

        val selected = state.foldersForSelectAll()

        assertEquals(visibleFolders.map { it.uri }, selected.keys.toList())
    }

    private fun document(index: Int) = DocumentEntry(
        uri = "content://documents/$index",
        name = "Document $index.pdf",
        mimeType = "application/pdf",
        kind = DocumentKind.PDF,
        location = "Documents",
        size = index.toLong(),
        lastModified = index.toLong(),
        lastOpened = index.toLong(),
    )

    private fun folder(index: Int) = DocumentFolder(
        uri = "content://folders/$index",
        name = "Folder $index",
    )
}
