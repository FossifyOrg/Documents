@file:OptIn(ExperimentalFoundationApi::class)
@file:Suppress("FunctionNaming")

package org.fossify.documents.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
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
internal fun DocumentGridCard(
    document: DocumentEntry,
    showLocation: Boolean,
    selected: Boolean,
    actions: DocumentRowActions,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            SimpleTheme.colorScheme.primary.copy(alpha = primaryTintAlpha())
        } else {
            SimpleTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 180),
        label = "DocumentGridSelectionColor",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            SimpleTheme.colorScheme.primary
        } else {
            SimpleTheme.colorScheme.onSurface.copy(alpha = 0.14f)
        },
        animationSpec = tween(durationMillis = 180),
        label = "DocumentGridSelectionBorder",
    )
    val shape = RoundedCornerShape(8.dp)

    OutlinedCard(
        modifier = modifier
            .combinedClickable(
                onClick = actions.onClick,
                onLongClick = actions.onLongClick,
            )
            .semantics { this.selected = selected },
        shape = shape,
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(width = 1.dp, color = borderColor),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DocumentKindIcon(kind = document.kind)
                DocumentGridCardText(document = document, showLocation = showLocation)
            }
            if (selected) {
                SelectionBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                )
            }
        }
    }
}

@Composable
internal fun FolderGridCard(
    folder: DocumentFolder,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            SimpleTheme.colorScheme.primary.copy(alpha = primaryTintAlpha())
        } else {
            SimpleTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 180),
        label = "FolderGridSelectionColor",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            SimpleTheme.colorScheme.primary
        } else {
            SimpleTheme.colorScheme.onSurface.copy(alpha = 0.14f)
        },
        animationSpec = tween(durationMillis = 180),
        label = "FolderGridSelectionBorder",
    )

    OutlinedCard(
        modifier = modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics { this.selected = selected },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(width = 1.dp, color = borderColor),
    ) {
        FolderGridCardContent(folder = folder, selected = selected)
    }
}

@Composable
private fun FolderGridCardContent(
    folder: DocumentFolder,
    selected: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box {
            FolderIcon()
            if (selected) {
                SelectionBadge(modifier = Modifier.align(Alignment.TopEnd))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = SimpleTheme.colorScheme.onSurface,
                style = SimpleTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            )
            Text(
                text = folder.itemCount?.let { itemCount ->
                    pluralStringResource(
                        id = R.plurals.folder_item_count,
                        count = itemCount,
                        itemCount,
                    )
                }.orEmpty().ifBlank { " " },
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = SimpleTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                style = SimpleTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SelectionBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(24.dp),
        shape = CircleShape,
        color = SimpleTheme.colorScheme.primary,
        contentColor = SimpleTheme.colorScheme.onPrimary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun DocumentGridCardText(
    document: DocumentEntry,
    showLocation: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = document.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = SimpleTheme.colorScheme.onSurface,
            style = SimpleTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
        )
        Text(
            text = document.metaLine(showOpenedFallback = true),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = SimpleTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            style = SimpleTheme.typography.bodyMedium,
        )
        if (showLocation) {
            Text(
                text = document.location.ifBlank { " " },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = SimpleTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                style = SimpleTheme.typography.bodySmall,
            )
        }
    }
}
