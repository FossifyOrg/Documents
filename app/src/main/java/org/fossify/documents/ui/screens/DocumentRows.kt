@file:OptIn(ExperimentalFoundationApi::class)
@file:Suppress("FunctionNaming")

package org.fossify.documents.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.documents.R
import org.fossify.documents.models.DocumentEntry
import org.fossify.documents.models.DocumentFolder

@Composable
internal fun DocumentRow(
    document: DocumentEntry,
    showLocation: Boolean,
    selected: Boolean,
    actions: DocumentRowActions,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            SimpleTheme.colorScheme.primary.copy(alpha = primaryTintAlpha())
        } else {
            SimpleTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 180),
        label = "DocumentSelectionColor",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .combinedClickable(
                onClick = actions.onClick,
                onLongClick = actions.onLongClick,
            )
            .semantics { this.selected = selected }
            .padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DocumentKindIcon(kind = document.kind)
        DocumentRowText(
            document = document,
            showLocation = showLocation,
            modifier = Modifier.weight(1f),
        )
    }
}

internal data class DocumentRowActions(
    val onClick: () -> Unit,
    val onLongClick: () -> Unit,
)

@Composable
internal fun FolderRow(
    folder: DocumentFolder,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            SimpleTheme.colorScheme.primary.copy(alpha = primaryTintAlpha())
        } else {
            SimpleTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 180),
        label = "FolderSelectionColor",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics { this.selected = selected }
            .padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FolderIcon()
        FolderRowText(
            folder = folder,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DocumentRowText(
    document: DocumentEntry,
    showLocation: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = document.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = SimpleTheme.colorScheme.onSurface,
            style = SimpleTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
        )
        Text(
            text = document.metaLine(showOpenedFallback = true),
            modifier = Modifier.padding(top = 3.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = SimpleTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            style = SimpleTheme.typography.bodyMedium,
        )
        if (showLocation && document.location.isNotBlank()) {
            Text(
                text = document.location,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = SimpleTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                style = SimpleTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun FolderRowText(
    folder: DocumentFolder,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = folder.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = SimpleTheme.colorScheme.onSurface,
            style = SimpleTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
        )
        folder.itemCount?.let { itemCount ->
            Text(
                text = pluralStringResource(
                    id = R.plurals.folder_item_count,
                    count = itemCount,
                    itemCount,
                ),
                modifier = Modifier.padding(top = 3.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = SimpleTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                style = SimpleTheme.typography.bodyMedium,
            )
        }
    }
}
