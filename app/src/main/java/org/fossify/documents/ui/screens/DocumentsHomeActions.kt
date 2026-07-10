@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("FunctionNaming", "MagicNumber")

package org.fossify.documents.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.documents.R

@Composable
internal fun EmptyDocumentsState(
    openDocument: () -> Unit,
    openFolder: () -> Unit,
) {
    DocumentsEmptyState(
        icon = Icons.Filled.Description,
        title = stringResource(id = R.string.no_documents_found),
        message = stringResource(id = R.string.open_document_empty_hint),
        actions = {
            EmptyActionButton(
                text = stringResource(id = R.string.open_document),
                onClick = openDocument,
            )
            EmptyActionButton(
                text = stringResource(id = R.string.open_folder),
                onClick = openFolder,
            )
        },
    )
}

@Composable
internal fun EmptyFoldersState(openFolder: () -> Unit) {
    DocumentsEmptyState(
        icon = Icons.Filled.Folder,
        title = stringResource(id = R.string.no_folders_found),
        message = stringResource(id = R.string.open_folder_empty_hint),
        actions = {
            EmptyActionButton(
                text = stringResource(id = R.string.open_folder),
                onClick = openFolder,
            )
        },
    )
}

@Composable
internal fun EmptyFavoritesState() {
    DocumentsEmptyState(
        icon = Icons.Filled.Star,
        title = stringResource(id = R.string.no_favorites_found),
        message = stringResource(id = R.string.no_favorites_empty_hint),
    )
}

@Composable
internal fun NoResultsState() {
    DocumentsEmptyState(
        icon = Icons.Filled.Search,
        title = stringResource(id = R.string.no_results_found),
        message = stringResource(id = R.string.adjust_search_or_filters),
    )
}

@Composable
internal fun DocumentsEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(84.dp),
            shape = RoundedCornerShape(24.dp),
            color = SimpleTheme.colorScheme.primary.copy(alpha = primaryTintAlpha()),
            contentColor = SimpleTheme.colorScheme.primary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                )
            }
        }
        Text(
            text = title,
            modifier = Modifier.padding(top = 22.dp),
            style = SimpleTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = SimpleTheme.colorScheme.onSurface,
        )
        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            style = SimpleTheme.typography.bodyMedium,
            color = SimpleTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier.padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = actions,
        )
    }
}

@Composable
private fun EmptyActionButton(
    text: String,
    onClick: () -> Unit,
) {
    FilledTonalButton(onClick = onClick) {
        Text(
            text = text,
            style = SimpleTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
internal fun ImportAction(
    actions: DocumentsMainActions,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = DocumentsMenuMinWidth),
            offset = DocumentsEndMenuOffset,
        ) {
            ImportMenuItem(
                icon = Icons.AutoMirrored.Filled.NoteAdd,
                text = stringResource(id = R.string.new_text_file),
                onClick = {
                    expanded = false
                    actions.newTextFile()
                },
            )
            ImportMenuItem(
                icon = Icons.AutoMirrored.Filled.NoteAdd,
                text = stringResource(id = R.string.new_markdown_file),
                onClick = {
                    expanded = false
                    actions.newMarkdownFile()
                },
            )
            ImportMenuItem(
                icon = Icons.Filled.Description,
                text = stringResource(id = R.string.open_file),
                onClick = {
                    expanded = false
                    actions.openDocument()
                },
            )
            ImportMenuItem(
                icon = Icons.Filled.Folder,
                text = stringResource(id = R.string.open_folder),
                onClick = {
                    expanded = false
                    actions.openFolder()
                },
            )
        }
        FloatingActionButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(18.dp),
            containerColor = SimpleTheme.colorScheme.primary,
            contentColor = SimpleTheme.colorScheme.onPrimary,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(id = R.string.import_document),
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun ImportMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text = text) },
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SimpleTheme.colorScheme.primary,
            )
        },
    )
}

@Composable
internal fun primaryTintAlpha() = if (isDocumentsDarkTheme()) 0.25f else 0.14f

@Composable
internal fun isDocumentsDarkTheme() = SimpleTheme.colorScheme.surface.luminance() < 0.5f
