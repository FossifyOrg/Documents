@file:OptIn(ExperimentalFoundationApi::class)
@file:Suppress("FunctionNaming")

package org.fossify.documents.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.documents.models.DocumentFolder
import org.fossify.documents.models.DocumentViewMode
import org.fossify.documents.viewmodels.DocumentsUiState

internal fun LazyListScope.folderChildren(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
) {
    if (uiState.selectedViewMode == DocumentViewMode.GRID) {
        folderGridRows(
            keyPrefix = "folder_child_grid",
            folders = uiState.visibleFolders,
            uiState = uiState,
            selectable = false,
            actions = actions,
        )
    } else {
        folderRows(
            keyPrefix = "folder_child",
            folders = uiState.visibleFolders,
            uiState = uiState,
            selectable = false,
            actions = actions,
        )
    }
    if (uiState.visibleDocuments.isNotEmpty()) {
        item(key = "folder_child_divider") {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = SimpleTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            )
        }
    }
}

internal fun LazyListScope.folderRows(
    keyPrefix: String,
    folders: List<DocumentFolder>,
    uiState: DocumentsUiState,
    selectable: Boolean = true,
    actions: DocumentsMainActions,
) {
    items(
        items = folders,
        key = { "${keyPrefix}_${it.uri}" },
    ) { folder ->
        FolderRow(
            folder = folder,
            selected = selectable && uiState.selectedFolders.any { it.uri == folder.uri },
            modifier = Modifier.animateItem(),
            onClick = {
                if (selectable && uiState.hasSelection) {
                    actions.onToggleFolderSelection(folder)
                } else {
                    actions.onFolderClick(folder)
                }
            },
            onLongClick = if (selectable) {
                { actions.onToggleFolderSelection(folder) }
            } else {
                null
            },
        )
    }
}

internal fun LazyListScope.folderGridRows(
    keyPrefix: String,
    folders: List<DocumentFolder>,
    uiState: DocumentsUiState,
    selectable: Boolean = true,
    actions: DocumentsMainActions,
) {
    itemsIndexed(
        items = folders.chunked(GRID_COLUMNS),
        key = { index, row -> "${keyPrefix}_${index}_${row.joinToString { it.uri }}" },
    ) { _, rowFolders ->
        Row(
            modifier = Modifier
                .animateItem()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            rowFolders.forEach { folder ->
                FolderGridCard(
                    folder = folder,
                    selected = selectable && uiState.selectedFolders.any { it.uri == folder.uri },
                    onClick = {
                        if (selectable && uiState.hasSelection) {
                            actions.onToggleFolderSelection(folder)
                        } else {
                            actions.onFolderClick(folder)
                        }
                    },
                    onLongClick = if (selectable) {
                        { actions.onToggleFolderSelection(folder) }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                )
            }
            repeat(GRID_COLUMNS - rowFolders.size) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                )
            }
        }
    }
}

internal const val GRID_COLUMNS = 2
