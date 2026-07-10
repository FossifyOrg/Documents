@file:Suppress("FunctionNaming", "MagicNumber")

package org.fossify.documents.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.documents.R

@Composable
internal fun MarkdownFormattingToolbar(
    onAction: (MarkdownEditAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MarkdownToolButton(
            icon = Icons.Filled.Title,
            contentDescription = stringResource(id = R.string.format_heading),
            onClick = { onAction(MarkdownEditAction.Heading) },
        )
        MarkdownToolButton(
            icon = Icons.Filled.FormatBold,
            contentDescription = stringResource(id = R.string.format_bold),
            onClick = { onAction(MarkdownEditAction.Bold) },
        )
        MarkdownToolButton(
            icon = Icons.Filled.FormatItalic,
            contentDescription = stringResource(id = R.string.format_italic),
            onClick = { onAction(MarkdownEditAction.Italic) },
        )
        MarkdownToolButton(
            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
            contentDescription = stringResource(id = R.string.format_bullet_list),
            onClick = { onAction(MarkdownEditAction.Bullet) },
        )
        MarkdownToolButton(
            icon = Icons.Filled.FormatQuote,
            contentDescription = stringResource(id = R.string.format_quote),
            onClick = { onAction(MarkdownEditAction.Quote) },
        )
        MarkdownToolButton(
            icon = Icons.Filled.Code,
            contentDescription = stringResource(id = R.string.format_code),
            onClick = { onAction(MarkdownEditAction.Code) },
        )
    }
}

@Composable
private fun MarkdownToolButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = SimpleTheme.colorScheme.onSurface,
        )
    }
}
