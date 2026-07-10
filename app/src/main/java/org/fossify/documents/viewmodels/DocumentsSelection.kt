package org.fossify.documents.viewmodels

import org.fossify.documents.models.DocumentEntry
import org.fossify.documents.models.DocumentFolder

internal const val HOME_RECENT_LIMIT = 4
internal const val HOME_FOLDER_LIMIT = 3
internal const val HOME_FAVORITES_LIMIT = 3

private const val HOME_RECENT_SELECTION_PREFIX = "recent_"
private const val HOME_FAVORITE_SELECTION_PREFIX = "favorite_"

internal fun DocumentsUiState.documentsForSelectAll(
    currentSelection: Map<String, DocumentEntry>,
): Map<String, DocumentEntry> {
    val selectedPrefixes = currentSelection.mapNotNullTo(linkedSetOf()) { (key, document) ->
        key.removeSuffix(document.uri).takeIf { key.endsWith(document.uri) }
    }
    if (selectedPrefixes.isEmpty()) {
        return emptyMap()
    }

    if (homeSection == DocumentsHomeSection.HOME && query.isBlank() && selectedFilters.isEmpty()) {
        val candidates = buildMap {
            if (HOME_RECENT_SELECTION_PREFIX in selectedPrefixes) {
                recentDocuments.take(HOME_RECENT_LIMIT).forEach { document ->
                    put("$HOME_RECENT_SELECTION_PREFIX${document.uri}", document)
                }
            }
            if (HOME_FAVORITE_SELECTION_PREFIX in selectedPrefixes) {
                favoriteDocuments.take(HOME_FAVORITES_LIMIT).forEach { document ->
                    put("$HOME_FAVORITE_SELECTION_PREFIX${document.uri}", document)
                }
            }
        }
        return candidates.ifEmpty { currentSelection }
    }

    val selectionPrefix = selectedPrefixes.first()
    val candidates = visibleDocuments.associateByTo(linkedMapOf()) { document ->
        "$selectionPrefix${document.uri}"
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
