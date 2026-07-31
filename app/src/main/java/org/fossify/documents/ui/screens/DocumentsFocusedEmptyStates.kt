@file:Suppress("FunctionNaming")

package org.fossify.documents.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.documents.R
import org.fossify.documents.viewmodels.DocumentsHomeSection
import org.fossify.documents.viewmodels.DocumentsUiState

@Composable
internal fun FocusedDocumentsEmptyState(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
) {
    when {
        uiState.query.isNotBlank() || uiState.selectedFilters.isNotEmpty() -> NoResultsState()
        uiState.homeSection == DocumentsHomeSection.FAVORITES -> EmptyFavoritesState()
        uiState.homeSection == DocumentsHomeSection.RECENT -> EmptyRecentState()
        else -> EmptyDocumentsState(
            openDocument = actions.openDocument,
            openFolder = actions.openFolder,
        )
    }
}

@Composable
internal fun DocumentsLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = SimpleTheme.colorScheme.primary)
    }
}

@Composable
internal fun EmptyRecentState() {
    DocumentsEmptyState(
        icon = Icons.Filled.Description,
        title = stringResource(id = R.string.no_recent_documents),
        message = stringResource(id = R.string.no_recent_documents_hint),
    )
}

@Composable
internal fun FolderErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    DocumentsEmptyState(
        icon = Icons.Filled.Folder,
        title = stringResource(id = R.string.folder_unavailable),
        message = message,
        actions = {
            EmptyActionButton(
                text = stringResource(id = R.string.retry),
                onClick = onRetry,
            )
        },
    )
}
