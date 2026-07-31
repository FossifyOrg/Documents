package org.fossify.documents.viewmodels

import org.fossify.documents.models.DocumentFolder

internal data class DocumentsNavigationDestination(
    val section: DocumentsHomeSection,
    val folderPath: List<DocumentFolder>,
)

internal fun folderPathAfterOpening(
    currentSection: DocumentsHomeSection,
    currentPath: List<DocumentFolder>,
    folder: DocumentFolder?,
): List<DocumentFolder> {
    return when {
        folder == null -> currentPath
        currentSection == DocumentsHomeSection.FOLDER -> currentPath + folder
        else -> listOf(folder)
    }
}

internal fun documentsBackDestination(
    currentSection: DocumentsHomeSection,
    currentPath: List<DocumentFolder>,
    folderOriginSection: DocumentsHomeSection,
): DocumentsNavigationDestination {
    return when (currentSection) {
        DocumentsHomeSection.FOLDER if currentPath.size > 1 -> {
            DocumentsNavigationDestination(
                section = DocumentsHomeSection.FOLDER,
                folderPath = currentPath.dropLast(1),
            )
        }
        DocumentsHomeSection.FOLDER -> {
            DocumentsNavigationDestination(
                section = folderOriginSection,
                folderPath = emptyList(),
            )
        }
        else -> {
            DocumentsNavigationDestination(
                section = DocumentsHomeSection.HOME,
                folderPath = emptyList(),
            )
        }
    }
}
