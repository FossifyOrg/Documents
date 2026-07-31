package org.fossify.documents.viewmodels

import org.fossify.documents.models.DocumentFolder
import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentsNavigationTest {
    @Test
    fun `back returns from nested folders to folders collection`() {
        val root = folder(1)
        val child = folder(2)
        val nestedPath = folderPathAfterOpening(
            currentSection = DocumentsHomeSection.FOLDER,
            currentPath = folderPathAfterOpening(
                currentSection = DocumentsHomeSection.FOLDERS,
                currentPath = emptyList(),
                folder = root,
            ),
            folder = child,
        )

        val parent = documentsBackDestination(
            currentSection = DocumentsHomeSection.FOLDER,
            currentPath = nestedPath,
            folderOriginSection = DocumentsHomeSection.FOLDERS,
        )
        val collection = documentsBackDestination(
            currentSection = parent.section,
            currentPath = parent.folderPath,
            folderOriginSection = DocumentsHomeSection.FOLDERS,
        )

        assertEquals(
            DocumentsNavigationDestination(DocumentsHomeSection.FOLDER, listOf(root)),
            parent,
        )
        assertEquals(
            DocumentsNavigationDestination(DocumentsHomeSection.FOLDERS, emptyList()),
            collection,
        )
    }

    @Test
    fun `back from home folder returns to home`() {
        val root = folder(1)

        val destination = documentsBackDestination(
            currentSection = DocumentsHomeSection.FOLDER,
            currentPath = listOf(root),
            folderOriginSection = DocumentsHomeSection.HOME,
        )

        assertEquals(
            DocumentsNavigationDestination(DocumentsHomeSection.HOME, emptyList()),
            destination,
        )
    }

    @Test
    fun `opening siblings after backing does not retain removed folder`() {
        val root = folder(1)
        val firstChild = folder(2)
        val secondChild = folder(3)
        val firstPath = listOf(root, firstChild)
        val parent = documentsBackDestination(
            currentSection = DocumentsHomeSection.FOLDER,
            currentPath = firstPath,
            folderOriginSection = DocumentsHomeSection.FOLDERS,
        )

        val secondPath = folderPathAfterOpening(
            currentSection = parent.section,
            currentPath = parent.folderPath,
            folder = secondChild,
        )

        assertEquals(listOf(root, secondChild), secondPath)
    }

    private fun folder(index: Int) = DocumentFolder(
        uri = "content://folders/$index",
        name = "Folder $index",
    )
}
