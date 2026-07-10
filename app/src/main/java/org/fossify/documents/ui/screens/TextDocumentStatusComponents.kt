@file:Suppress("FunctionNaming", "MagicNumber")

package org.fossify.documents.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.fossify.commons.compose.theme.SimpleTheme
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = uiState.text.documentStats(),
                style = SimpleTheme.typography.bodyMedium,
                maxLines = 1,
            )
            Text(
                text = uiState.statusLabel(),
                style = SimpleTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
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
internal fun StatusStrip(text: String, isError: Boolean) {
    Surface(
        color = if (isError) SimpleTheme.colorScheme.errorContainer else SimpleTheme.colorScheme.secondaryContainer,
        contentColor = if (isError) {
            SimpleTheme.colorScheme.onErrorContainer
        } else {
            SimpleTheme.colorScheme.onSecondaryContainer
        },
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            style = SimpleTheme.typography.bodyMedium,
        )
    }
}
