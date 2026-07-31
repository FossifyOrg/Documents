package org.fossify.documents.viewmodels

import org.fossify.documents.models.DocumentEntry
import org.fossify.documents.models.DocumentFolder

internal const val HOME_RECENT_LIMIT = 4
internal const val HOME_FOLDER_LIMIT = 4
internal const val HOME_FAVORITES_LIMIT = 4

internal const val HOME_RECENT_SELECTION_PREFIX = "recent_"
internal const val HOME_FAVORITE_SELECTION_PREFIX = "favorite_"

internal data class SelectedDocument(
    val document: DocumentEntry,
    val selectionPrefix: String,
)

internal fun DocumentsUiState.documentsForSelectAll(
    currentSelection: Map<String, SelectedDocument>,
): Map<String, SelectedDocument> {
    val selectedPrefixes = currentSelection.values
        .mapTo(linkedSetOf(), SelectedDocument::selectionPrefix)
    if (selectedPrefixes.isEmpty()) {
        return emptyMap()
    }

    if (homeSection == DocumentsHomeSection.HOME && query.isBlank() && selectedFilters.isEmpty()) {
        val candidates = buildMap {
            if (HOME_RECENT_SELECTION_PREFIX in selectedPrefixes) {
                recentDocuments.take(HOME_RECENT_LIMIT).forEach { document ->
                    put(
                        document.uri,
                        SelectedDocument(document, HOME_RECENT_SELECTION_PREFIX),
                    )
                }
            }
            if (HOME_FAVORITE_SELECTION_PREFIX in selectedPrefixes) {
                favoriteDocuments.take(HOME_FAVORITES_LIMIT).forEach { document ->
                    put(
                        document.uri,
                        SelectedDocument(document, HOME_FAVORITE_SELECTION_PREFIX),
                    )
                }
            }
        }
        return candidates.ifEmpty { currentSelection }
    }

    val selectionPrefix = selectedPrefixes.first()
    val candidates = visibleDocuments.associateTo(linkedMapOf()) { document ->
        document.uri to SelectedDocument(document, selectionPrefix)
    }
    return candidates.ifEmpty { currentSelection }
}

internal fun DocumentsUiState.foldersForSelectAll(): Map<String, DocumentFolder> {
    val candidates = if (homeSection == DocumentsHomeSection.HOME) {
        folders.take(HOME_FOLDER_LIMIT)
    } else {
        visibleFolders
    }
    return candidates.associateByTo(linkedMapOf(), DocumentFolder::uri)
}
