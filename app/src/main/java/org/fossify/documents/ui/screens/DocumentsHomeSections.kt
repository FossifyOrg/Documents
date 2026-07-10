@file:OptIn(ExperimentalFoundationApi::class)

package org.fossify.documents.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.fossify.documents.R
import org.fossify.documents.models.DocumentEntry
import org.fossify.documents.models.DocumentViewMode
import org.fossify.documents.viewmodels.DocumentsHomeSection
import org.fossify.documents.viewmodels.DocumentsUiState
import org.fossify.documents.viewmodels.HOME_FAVORITES_LIMIT
import org.fossify.documents.viewmodels.HOME_FOLDER_LIMIT
import org.fossify.documents.viewmodels.HOME_RECENT_LIMIT

internal fun LazyListScope.homeContent(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
) {
    if (uiState.query.isNotBlank() || uiState.selectedFilters.isNotEmpty()) {
        focusedDocuments(uiState, actions)
        return
    }

    if (uiState.documents.isEmpty() && uiState.folders.isEmpty()) {
        item(key = "empty") {
            EmptyDocumentsState(
                openDocument = actions.openDocument,
                openFolder = actions.openFolder,
            )
        }
        return
    }

    recentSection(uiState = uiState, actions = actions)
    favoritesSection(uiState = uiState, actions = actions)
    foldersSection(uiState = uiState, actions = actions)
}

internal fun LazyListScope.focusedContent(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
) {
    if (uiState.homeSection == DocumentsHomeSection.FOLDERS) {
        focusedFolders(uiState, actions)
    } else {
        focusedDocuments(uiState, actions)
    }
}

private fun LazyListScope.recentSection(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
) {
    if (uiState.recentDocuments.isEmpty()) {
        return
    }

    item(key = "recent_title") {
        SectionHeader(
            title = stringResource(id = R.string.recent),
            actionLabel = stringResource(id = R.string.view_all),
            onActionClick = actions.onShowRecent,
        )
    }
    documentRows(
        keyPrefix = "recent",
        documents = uiState.recentDocuments.take(HOME_RECENT_LIMIT),
        showLocation = false,
        uiState = uiState,
        actions = actions,
    )
    item(key = "recent_space") {
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun LazyListScope.foldersSection(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
) {
    if (uiState.folders.isEmpty()) {
        return
    }

    item(key = "folders_title") {
        SectionHeader(
            title = stringResource(id = R.string.folders),
            actionLabel = stringResource(id = R.string.view_all),
            onActionClick = actions.onShowFolders,
        )
    }
    folderRows(
        keyPrefix = "folder",
        folders = uiState.folders.take(HOME_FOLDER_LIMIT),
        uiState = uiState,
        actions = actions,
    )
    item(key = "folders_space") {
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun LazyListScope.favoritesSection(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
) {
    if (uiState.favoriteDocuments.isEmpty()) {
        return
    }

    item(key = "favorites_title") {
        SectionHeader(
            title = stringResource(id = org.fossify.commons.R.string.favorites),
            actionLabel = stringResource(id = R.string.view_all),
            onActionClick = actions.onShowFavorites,
        )
    }
    documentRows(
        keyPrefix = "favorite",
        documents = uiState.favoriteDocuments.take(HOME_FAVORITES_LIMIT),
        showLocation = false,
        uiState = uiState,
        actions = actions,
    )
    item(key = "favorites_space") {
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun LazyListScope.focusedFolders(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
) {
    if (uiState.visibleFolders.isEmpty()) {
        item(key = "no_folder_matches") {
            if (uiState.query.isBlank()) {
                EmptyFoldersState(openFolder = actions.openFolder)
            } else {
                NoResultsState()
            }
        }
    } else {
        if (uiState.selectedViewMode == DocumentViewMode.GRID) {
            folderGridRows(
                keyPrefix = "folder_all_grid",
                folders = uiState.visibleFolders,
                uiState = uiState,
                actions = actions,
            )
        } else {
            folderRows(
                keyPrefix = "folder_all",
                folders = uiState.visibleFolders,
                uiState = uiState,
                actions = actions,
            )
        }
    }
}

private fun LazyListScope.focusedDocuments(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
) {
    val showLocation = uiState.showFileLocations && uiState.homeSection != DocumentsHomeSection.FOLDER
    val showChildFolders = uiState.homeSection == DocumentsHomeSection.FOLDER &&
            uiState.selectedFilters.isEmpty() &&
            uiState.visibleFolders.isNotEmpty()

    when {
        uiState.isFolderLoading -> item(key = "folder_loading") {
            DocumentsLoadingState()
        }

        else -> {
            if (showChildFolders) {
                folderChildren(uiState = uiState, actions = actions)
            }
            when {
                uiState.visibleDocuments.isEmpty() && !showChildFolders -> item(key = "no_matches") {
                    FocusedDocumentsEmptyState(uiState = uiState, actions = actions)
                }

                uiState.visibleDocuments.isNotEmpty() && uiState.selectedViewMode == DocumentViewMode.GRID -> {
                    documentGridRows(
                        keyPrefix = "document_grid_${uiState.homeSection}",
                        documents = uiState.visibleDocuments,
                        showLocation = showLocation,
                        uiState = uiState,
                        actions = actions,
                    )
                }

                uiState.visibleDocuments.isNotEmpty() -> documentRows(
                    keyPrefix = "document_${uiState.homeSection}",
                    documents = uiState.visibleDocuments,
                    showLocation = showLocation,
                    uiState = uiState,
                    actions = actions,
                )
            }
        }
    }
}

private fun LazyListScope.documentRows(
    keyPrefix: String,
    documents: List<DocumentEntry>,
    showLocation: Boolean,
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
) {
    items(
        items = documents,
        key = { "${keyPrefix}_${it.uri}" },
    ) { document ->
        val selectionKey = "${keyPrefix}_${document.uri}"
        DocumentRow(
            document = document,
            showLocation = showLocation,
            selected = selectionKey in uiState.selectedDocumentKeys,
            modifier = Modifier.animateItem(),
            actions = DocumentRowActions(
                onClick = {
                    if (uiState.hasSelection) {
                        actions.onToggleDocumentSelection(selectionKey, document)
                    } else {
                        actions.onDocumentClick(document)
                    }
                },
                onLongClick = { actions.onToggleDocumentSelection(selectionKey, document) },
            ),
        )
    }
}

private fun LazyListScope.documentGridRows(
    keyPrefix: String,
    documents: List<DocumentEntry>,
    showLocation: Boolean,
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
) {
    itemsIndexed(
        items = documents.chunked(GRID_COLUMNS),
        key = { index, row -> "${keyPrefix}_${index}_${row.joinToString { it.uri }}" },
    ) { _, rowDocuments ->
        Row(
            modifier = Modifier
                .animateItem()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            rowDocuments.forEach { document ->
                val selectionKey = "${keyPrefix}_${document.uri}"
                DocumentGridCard(
                    document = document,
                    showLocation = showLocation,
                    selected = selectionKey in uiState.selectedDocumentKeys,
                    actions = DocumentRowActions(
                        onClick = {
                            if (uiState.hasSelection) {
                                actions.onToggleDocumentSelection(selectionKey, document)
                            } else {
                                actions.onDocumentClick(document)
                            }
                        },
                        onLongClick = { actions.onToggleDocumentSelection(selectionKey, document) },
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                )
            }
            repeat(GRID_COLUMNS - rowDocuments.size) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                )
            }
        }
    }
}
