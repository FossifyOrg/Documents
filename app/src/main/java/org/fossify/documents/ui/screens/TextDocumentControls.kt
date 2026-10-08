package org.fossify.documents.ui.screens

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.documents.R

@Composable
internal fun TextDocumentSearchField(
    query: String,
    currentMatchNumber: Int,
    matchCount: Int,
    onQueryChange: (String) -> Unit,
    onNextMatch: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        singleLine = true,
        textStyle = SimpleTheme.typography.titleMedium.copy(
            color = SimpleTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(SimpleTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onNextMatch() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.find_in_document),
                            color = SimpleTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = SimpleTheme.typography.titleMedium,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            id = R.string.search_match_count,
                            currentMatchNumber,
                            matchCount,
                        ),
                        modifier = Modifier.padding(start = 8.dp),
                        color = SimpleTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        style = SimpleTheme.typography.labelLarge,
                    )
                }
            }
        },
    )
}

@Composable
internal fun TextDocumentOverflowMenu(
    textZoom: Float,
    onTextZoomChange: (Float) -> Unit,
    onResetTextZoom: () -> Unit,
    onOpenWith: () -> Unit,
    onSaveCopy: () -> Unit,
    canSaveCopy: Boolean,
    isReadOnly: Boolean,
    onOpenForEditing: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(id = org.fossify.commons.R.string.more_options),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = DocumentsMenuMinWidth),
            offset = DocumentsEndMenuOffset,
        ) {
            DropdownMenuItem(
                text = {
                    Text(stringResource(if (isReadOnly) R.string.edit_a_copy else org.fossify.commons.R.string.save_as))
                },
                enabled = canSaveCopy,
                onClick = {
                    expanded = false
                    onSaveCopy()
                },
            )
            if (isReadOnly && canSaveCopy) {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.reopen_to_edit)) },
                    onClick = {
                        expanded = false
                        onOpenForEditing()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(id = org.fossify.commons.R.string.open_with)) },
                onClick = {
                    expanded = false
                    onOpenWith()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.zoom_in)) },
                enabled = textZoom < MAX_DOCUMENT_TEXT_ZOOM,
                onClick = {
                    expanded = false
                    onTextZoomChange(textZoom + DOCUMENT_TEXT_ZOOM_STEP)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.zoom_out)) },
                enabled = textZoom > MIN_DOCUMENT_TEXT_ZOOM,
                onClick = {
                    expanded = false
                    onTextZoomChange(textZoom - DOCUMENT_TEXT_ZOOM_STEP)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.reset_zoom)) },
                enabled = textZoom != DEFAULT_DOCUMENT_TEXT_ZOOM,
                onClick = {
                    expanded = false
                    onResetTextZoom()
                },
            )
        }
    }
}

@Composable
internal fun Modifier.documentTextZoomGesture(
    textZoom: Float,
    onTextZoomChange: (Float) -> Unit,
    onZoomGestureChange: (Boolean) -> Unit = {},
): Modifier {
    val currentZoom by rememberUpdatedState(textZoom)
    val currentOnZoomChange by rememberUpdatedState(onTextZoomChange)
    val currentOnZoomGestureChange by rememberUpdatedState(onZoomGestureChange)

    return pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var zooming = false
            try {
                do {
                    val event = awaitPointerEvent()
                    if (event.changes.count { it.pressed } >= 2) {
                        if (!zooming) {
                            zooming = true
                            currentOnZoomGestureChange(true)
                        }
                        val zoomChange = event.calculateZoom()
                        if (zoomChange != 1f) {
                            currentOnZoomChange((currentZoom * zoomChange).coerceDocumentTextZoom())
                            event.changes.forEach { it.consume() }
                        }
                    }
                } while (event.changes.any { it.pressed })
            } finally {
                if (zooming) {
                    currentOnZoomGestureChange(false)
                }
            }
        }
    }
}
