@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("FunctionNaming")

package org.fossify.documents.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.documents.R
import org.fossify.documents.viewmodels.DocumentsHomeSection
import org.fossify.documents.viewmodels.DocumentsUiState

@Composable
internal fun DocumentsSelectionTopBar(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
) {
    TopAppBar(
        title = {
            Text(
                text = pluralStringResource(
                    id = R.plurals.selected_count,
                    count = uiState.selectionCount,
                    uiState.selectionCount,
                ),
                style = SimpleTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = actions.clearSelection) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(id = R.string.clear_selection),
                    tint = SimpleTheme.colorScheme.primary,
                )
            }
        },
        actions = {
            DocumentsSelectionActions(uiState = uiState, actions = actions)
        },
    )
}

@Composable
private fun DocumentsSelectionActions(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (uiState.selectedDocuments.isNotEmpty()) {
            IconButton(onClick = actions.onToggleSelectedFavorites) {
                Icon(
                    imageVector = if (uiState.selectedDocumentsAreFavorites) {
                        Icons.Filled.Star
                    } else {
                        Icons.Filled.StarBorder
                    },
                    contentDescription = stringResource(id = org.fossify.commons.R.string.favorites),
                    tint = SimpleTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = { actions.onShareDocuments(uiState.selectedDocuments.distinctBy { it.uri }) }) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(id = R.string.share_documents),
                    tint = SimpleTheme.colorScheme.primary,
                )
            }
        }
        IconButton(onClick = actions.onSelectAll) {
            Icon(
                imageVector = Icons.Outlined.SelectAll,
                contentDescription = stringResource(id = org.fossify.commons.R.string.select_all),
                tint = SimpleTheme.colorScheme.primary,
            )
        }
        DocumentsSelectionOverflowMenu(uiState = uiState, actions = actions)
    }
}

@Composable
private fun DocumentsSelectionOverflowMenu(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
) {
    val selectedDocument = uiState.selectedDocuments.singleOrNull()
        ?.takeIf { uiState.selectionCount == 1 }
    val canRemove = uiState.homeSection != DocumentsHomeSection.FOLDER
    if (selectedDocument == null && !canRemove) {
        return
    }

    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(id = org.fossify.commons.R.string.more_options),
                tint = SimpleTheme.colorScheme.primary,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = DocumentsMenuMinWidth),
            offset = DocumentsEndMenuOffset,
        ) {
            if (selectedDocument != null) {
                DropdownMenuItem(
                    text = {
                        Text(text = stringResource(id = org.fossify.commons.R.string.open_with))
                    },
                    onClick = {
                        expanded = false
                        actions.onOpenWith(selectedDocument)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                        )
                    },
                )
            }
            if (canRemove) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.remove_from_documents)) },
                    onClick = {
                        expanded = false
                        actions.onRemoveSelection()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.RemoveCircleOutline,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}
