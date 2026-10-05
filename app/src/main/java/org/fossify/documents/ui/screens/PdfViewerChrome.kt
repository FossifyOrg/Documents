@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("FunctionNaming", "LongMethod", "LongParameterList", "MagicNumber")

package org.fossify.documents.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.infomaniak.lib.pdfview.PDFView
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.documents.R
import org.fossify.documents.data.PdfSearchResult

@Composable
internal fun PdfTopBar(
    title: String,
    onBack: () -> Unit,
    searchActive: Boolean,
    searchQuery: String,
    searchResult: PdfSearchResult,
    searchIndex: Int,
    onSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onChangeMatch: (Int) -> Unit,
    onOutline: () -> Unit,
    hasOutline: Boolean,
    onPrint: () -> Unit,
    menu: @Composable () -> Unit,
    isLoaded: Boolean,
) {
    TopAppBar(
        title = {
            if (searchActive) {
                TextDocumentSearchField(
                    query = searchQuery,
                    currentMatchNumber = if (searchIndex in searchResult.matches.indices) searchIndex + 1 else 0,
                    matchCount = searchResult.matches.size,
                    onQueryChange = onSearchQueryChange,
                    onNextMatch = { onChangeMatch(1) },
                )
            } else {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = SimpleTheme.typography.titleLarge,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(org.fossify.commons.R.string.back))
            }
        },
        actions = {
            if (searchActive) {
                IconButton(onClick = { onChangeMatch(-1) }, enabled = searchIndex in searchResult.matches.indices) {
                    Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.previous_match))
                }
                IconButton(onClick = { onChangeMatch(1) }, enabled = searchIndex in searchResult.matches.indices) {
                    Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.next_match))
                }
            } else {
                IconButton(onClick = onSearch, enabled = isLoaded) {
                    Icon(Icons.Filled.Search, stringResource(R.string.search_text_in_document))
                }
                if (hasOutline) {
                    IconButton(onClick = onOutline, enabled = isLoaded) {
                        Icon(Icons.AutoMirrored.Filled.List, stringResource(R.string.pdf_contents))
                    }
                } else {
                    IconButton(onClick = onPrint, enabled = isLoaded) {
                        Icon(Icons.Filled.Print, stringResource(org.fossify.commons.R.string.print))
                    }
                }
                menu()
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SimpleTheme.colorScheme.surface),
    )
}

@Composable
internal fun PdfControls(
    currentPage: Int,
    pageCount: Int,
    onGoToPage: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SimpleTheme.colorScheme.surface,
        contentColor = SimpleTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            IconButton(onClick = onPreviousPage, enabled = currentPage > 0) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(id = R.string.previous_page),
                )
            }
            val pageDescription = stringResource(R.string.pdf_page_navigation, currentPage + 1, pageCount)
            TextButton(onClick = onGoToPage) {
                Text(
                    modifier = Modifier.clearAndSetSemantics { contentDescription = pageDescription },
                    text = stringResource(id = R.string.page_count_value, currentPage + 1, pageCount),
                    style = SimpleTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                )
            }
            IconButton(onClick = onNextPage, enabled = currentPage < pageCount - 1) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(id = R.string.next_page),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onZoomOut) {
                Icon(
                    imageVector = Icons.Filled.ZoomOut,
                    contentDescription = stringResource(id = R.string.zoom_out),
                )
            }
            IconButton(onClick = onZoomIn) {
                Icon(
                    imageVector = Icons.Filled.ZoomIn,
                    contentDescription = stringResource(id = R.string.zoom_in),
                )
            }
        }
    }
}

@Composable
internal fun PasswordDialog(
    password: String,
    invalidPassword: Boolean,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = org.fossify.commons.R.string.enter_password))
        },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(text = stringResource(id = org.fossify.commons.R.string.password)) },
                supportingText = if (invalidPassword) {
                    { Text(text = stringResource(id = org.fossify.commons.R.string.invalid_password)) }
                } else {
                    null
                },
                isError = invalidPassword,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = password.isNotBlank()) {
                Text(text = stringResource(id = org.fossify.commons.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = org.fossify.commons.R.string.cancel))
            }
        },
    )
}

@Composable
internal fun PdfLoadingStatus(isLoading: Boolean, error: String?, modifier: Modifier = Modifier) {
    when {
        isLoading -> CircularProgressIndicator(modifier = modifier)
        error != null -> Text(
            text = error,
            modifier = modifier.padding(32.dp),
            color = SimpleTheme.colorScheme.error,
            style = SimpleTheme.typography.bodyLarge,
        )
    }
}

@Composable
internal fun PdfNavigationControls(view: PDFView?, currentPage: Int, pageCount: Int, onGoToPage: () -> Unit) {
    PdfControls(
        currentPage = currentPage,
        pageCount = pageCount,
        onGoToPage = onGoToPage,
        onPreviousPage = { view?.goToPage((currentPage - 1).coerceAtLeast(0), true) },
        onNextPage = { view?.goToPage((currentPage + 1).coerceAtMost(pageCount - 1), true) },
        onZoomOut = { view?.let { it.zoomWithAnimation((it.zoom - 0.5f).coerceAtLeast(it.minZoom)) } },
        onZoomIn = { view?.let { it.zoomWithAnimation((it.zoom + 0.5f).coerceAtMost(it.maxZoom)) } },
    )
}
