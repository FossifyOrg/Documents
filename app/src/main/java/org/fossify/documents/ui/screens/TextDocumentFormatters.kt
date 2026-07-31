package org.fossify.documents.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import org.fossify.documents.R
import org.fossify.documents.viewmodels.TextDocumentUiState

@Composable
internal fun TextDocumentUiState.statusLabel(): String {
    return when {
        isReadOnly -> stringResource(id = R.string.read_only)
        isSaving -> stringResource(id = org.fossify.commons.R.string.saving)
        isDirty -> stringResource(id = R.string.unsaved_changes)
        else -> stringResource(id = R.string.saved)
    }
}

@Composable
internal fun String.documentStats(): String {
    val wordCount = trim()
        .split(Regex("\\s+"))
        .count { it.isNotBlank() }
        .takeIf { isNotBlank() }
        ?: 0

    return stringResource(
        id = R.string.document_text_stats,
        pluralStringResource(id = R.plurals.document_text_words, wordCount, wordCount),
        pluralStringResource(id = R.plurals.document_text_chars, length, length),
    )
}
