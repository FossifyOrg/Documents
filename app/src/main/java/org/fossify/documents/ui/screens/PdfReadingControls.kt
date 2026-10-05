@file:OptIn(ExperimentalMaterial3Api::class)

package org.fossify.documents.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.shockwave.pdfium.PdfDocument
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.documents.R

@Composable
internal fun PdfReadingMenu(
    nightMode: Boolean,
    onNightModeChange: (Boolean) -> Unit,
    onSettings: () -> Unit,
    onGoToPage: () -> Unit,
    onSaveCopy: () -> Unit,
    isCopying: Boolean,
    onPrint: () -> Unit,
    showPrint: Boolean,
    onOpenWith: () -> Unit,
    isLoaded: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, stringResource(org.fossify.commons.R.string.more_options))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = DocumentsMenuMinWidth),
            offset = DocumentsEndMenuOffset,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.go_to_page)) },
                enabled = isLoaded,
                onClick = { expanded = false; onGoToPage() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pdf_dark_pages)) },
                modifier = Modifier.semantics {
                    role = Role.Checkbox
                    toggleableState = ToggleableState(nightMode)
                },
                trailingIcon = { Checkbox(checked = nightMode, onCheckedChange = null) },
                onClick = { expanded = false; onNightModeChange(!nightMode) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.save_a_copy)) },
                enabled = isLoaded && !isCopying,
                onClick = { expanded = false; onSaveCopy() },
            )
            if (showPrint) {
                DropdownMenuItem(
                    text = { Text(stringResource(org.fossify.commons.R.string.print)) },
                    enabled = isLoaded,
                    onClick = { expanded = false; onPrint() },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(org.fossify.commons.R.string.open_with)) },
                onClick = { expanded = false; onOpenWith() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(org.fossify.commons.R.string.settings)) },
                onClick = { expanded = false; onSettings() },
            )
        }
    }
}

@Composable
internal fun PdfGoToPageDialog(currentPage: Int, pageCount: Int, onGo: (Int) -> Unit, onDismiss: () -> Unit) {
    var input by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val value = (currentPage + 1).toString()
        mutableStateOf(TextFieldValue(value, TextRange(0, value.length)))
    }
    val page = input.text.toIntOrNull()
    val valid = page != null && page in 1..pageCount
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.go_to_page)) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                label = { Text(stringResource(R.string.pdf_page_range, pageCount)) },
                isError = input.text.isNotEmpty() && !valid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { if (valid) onGo(page - 1) }),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (valid) onGo(page - 1) }, enabled = valid) {
                Text(stringResource(org.fossify.commons.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(org.fossify.commons.R.string.cancel)) }
        },
    )
}

@Composable
internal fun PdfOutlineSheet(bookmarks: List<PdfDocument.Bookmark>, onGo: (Int) -> Unit, onDismiss: () -> Unit) {
    val entries = remember(bookmarks) { flattenPdfOutline(bookmarks) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column {
            Text(
                stringResource(R.string.pdf_contents),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = SimpleTheme.typography.titleLarge,
            )
            LazyColumn {
                items(entries) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = entry.page >= 0) { onGo(entry.page) }
                            .padding(start = 24.dp + 16.dp * entry.depth, end = 24.dp, top = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(entry.title, modifier = Modifier.weight(1f), style = SimpleTheme.typography.bodyLarge)
                        if (entry.page >= 0) {
                            Text((entry.page + 1).toString(), color = SimpleTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private data class PdfOutlineEntry(val title: String, val page: Int, val depth: Int)

private fun flattenPdfOutline(
    bookmarks: List<PdfDocument.Bookmark>,
    depth: Int = 0,
): List<PdfOutlineEntry> = buildList {
    bookmarks.forEach { bookmark ->
        add(PdfOutlineEntry(bookmark.title.orEmpty(), bookmark.pageIdx.toInt(), depth))
        addAll(flattenPdfOutline(bookmark.children, depth + 1))
    }
}
