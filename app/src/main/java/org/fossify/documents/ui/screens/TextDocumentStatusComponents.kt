package org.fossify.documents.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.documents.R
import org.fossify.documents.viewmodels.TextDocumentUiState

@Composable
internal fun EditorStatusBar(
    uiState: TextDocumentUiState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SimpleTheme.colorScheme.primary.copy(alpha = primaryTintAlpha()),
        contentColor = SimpleTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = uiState.text.documentStats(),
                modifier = Modifier.padding(end = 12.dp),
                style = SimpleTheme.typography.bodyMedium,
            )
            Text(
                text = uiState.statusLabel(),
                style = SimpleTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
internal fun LoadingDocument() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun StatusDocument(text: String, isError: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (isError) SimpleTheme.colorScheme.error else SimpleTheme.colorScheme.onSurfaceVariant,
            style = SimpleTheme.typography.bodyLarge,
        )
    }
}

@Composable
internal fun StatusStrip(
    text: String,
    isError: Boolean,
    action: (@Composable () -> Unit)? = null,
) {
    Surface(
        color = if (isError) SimpleTheme.colorScheme.errorContainer else SimpleTheme.colorScheme.secondaryContainer,
        contentColor = if (isError) {
            SimpleTheme.colorScheme.onErrorContainer
        } else {
            SimpleTheme.colorScheme.onSecondaryContainer
        },
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = text,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 8.dp, top = 8.dp, bottom = 8.dp),
                style = SimpleTheme.typography.bodyMedium,
            )
            if (action != null) {
                Box(modifier = Modifier.align(Alignment.CenterVertically)) { action() }
            }
        }
    }
}

@Composable
internal fun TextDocumentNotices(uiState: TextDocumentUiState, onOpenForEditing: () -> Unit) {
    if (uiState.isReadOnly) {
        StatusStrip(
            text = uiState.readOnlyReason ?: stringResource(R.string.read_only),
            isError = false,
            action = if (uiState.canSaveCopy && !uiState.isSaving) {
                { TextButton(onClick = onOpenForEditing) { Text(stringResource(R.string.reopen_to_edit)) } }
            } else {
                null
            },
        )
    }
    uiState.error?.let { StatusStrip(text = it, isError = true) }
}
