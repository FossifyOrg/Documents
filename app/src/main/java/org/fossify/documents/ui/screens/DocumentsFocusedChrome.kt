@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@file:Suppress("FunctionNaming", "LongParameterList", "MagicNumber")

package org.fossify.documents.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.documents.R
import org.fossify.documents.viewmodels.DocumentsHomeSection
import org.fossify.documents.viewmodels.DocumentsUiState

@Composable
internal fun DocumentsFocusedTopBar(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
    searchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
) {
    val showSearchField = searchActive || uiState.query.isNotBlank()

    TopAppBar(
        title = {
            if (showSearchField) {
                FocusedSearchField(
                    query = uiState.query,
                    onQueryChange = actions.onQueryChange,
                )
            } else {
                Text(
                    text = uiState.screenTitle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = SimpleTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    if (showSearchField) {
                        actions.onQueryChange("")
                        onSearchActiveChange(false)
                    } else {
                        actions.onBack()
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = org.fossify.commons.R.string.back),
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    if (showSearchField) {
                        actions.onQueryChange("")
                        onSearchActiveChange(false)
                    } else {
                        onSearchActiveChange(true)
                    }
                },
            ) {
                Icon(
                    imageVector = if (showSearchField) Icons.Filled.Close else Icons.Filled.Search,
                    contentDescription = stringResource(id = R.string.search_documents),
                )
            }
            DocumentsOverflowMenu(
                hasDocuments = uiState.hasRecentDocuments,
                actions = actions,
            )
        },
    )
}

@Composable
private fun FocusedSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
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
        textStyle = SimpleTheme.typography.titleLarge.copy(
            color = SimpleTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        ),
        cursorBrush = SolidColor(SimpleTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            if (query.isBlank()) {
                Text(
                    text = stringResource(id = R.string.search_documents),
                    color = SimpleTheme.colorScheme.onSurface.copy(alpha = 0.56f),
                    style = SimpleTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            innerTextField()
        },
    )
}

@Composable
internal fun DocumentsBreadcrumb(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
) {
    if (uiState.homeSection != DocumentsHomeSection.FOLDERS && uiState.homeSection != DocumentsHomeSection.FOLDER) {
        return
    }

    val scrollState = rememberScrollState()
    val activeFolderUri = uiState.folderPath.lastOrNull()?.uri

    LaunchedEffect(activeFolderUri, scrollState.maxValue) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BreadcrumbRoot(uiState = uiState, actions = actions)
        BreadcrumbFolderPath(uiState = uiState, activeFolderUri = activeFolderUri, actions = actions)
    }
}

@Composable
private fun BreadcrumbRoot(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
) {
    Icon(
        imageVector = if (uiState.homeSection == DocumentsHomeSection.FOLDERS) {
            Icons.Filled.PhoneAndroid
        } else {
            Icons.Filled.Home
        },
        contentDescription = null,
        tint = SimpleTheme.colorScheme.primary,
        modifier = Modifier.size(22.dp),
    )
    BreadcrumbText(
        text = stringResource(id = org.fossify.commons.R.string.internal_storage),
        selected = uiState.homeSection == DocumentsHomeSection.FOLDERS,
        onClick = if (uiState.homeSection == DocumentsHomeSection.FOLDER) actions.onShowFolders else null,
    )
}

@Composable
private fun BreadcrumbFolderPath(
    uiState: DocumentsUiState,
    activeFolderUri: String?,
    actions: DocumentsMainActions,
) {
    if (uiState.homeSection != DocumentsHomeSection.FOLDER) {
        return
    }

    if (uiState.folderPath.isEmpty()) {
        BreadcrumbSeparator()
        BreadcrumbText(
            text = uiState.activeFolder?.name ?: stringResource(id = R.string.folders),
            selected = true,
        )
        return
    }

    uiState.folderPath.forEachIndexed { index, folder ->
        BreadcrumbSeparator()
        BreadcrumbText(
            text = folder.name,
            selected = folder.uri == activeFolderUri,
            onClick = if (index < uiState.folderPath.lastIndex) {
                { actions.onBreadcrumbClick(index) }
            } else {
                null
            },
        )
    }
}

@Composable
private fun BreadcrumbSeparator() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = SimpleTheme.colorScheme.onSurface.copy(alpha = 0.55f),
    )
}

@Composable
private fun BreadcrumbText(
    text: String,
    selected: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val textColor = if (selected) {
        SimpleTheme.colorScheme.primary
    } else {
        SimpleTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    }
    val textWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal

    if (onClick != null) {
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            BreadcrumbLabel(text = text, color = textColor, fontWeight = textWeight)
        }
    } else {
        BreadcrumbLabel(
            text = text,
            color = textColor,
            fontWeight = textWeight,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun BreadcrumbLabel(
    text: String,
    color: Color,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = SimpleTheme.typography.titleMedium.copy(fontWeight = fontWeight),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun DocumentsUiState.screenTitle(): String {
    return when (homeSection) {
        DocumentsHomeSection.HOME -> stringResource(id = R.string.all_documents)
        DocumentsHomeSection.FAVORITES -> stringResource(id = org.fossify.commons.R.string.favorites)
        DocumentsHomeSection.FOLDER -> activeFolder?.name ?: stringResource(id = R.string.folders)
        DocumentsHomeSection.FOLDERS -> stringResource(id = R.string.folders)
        DocumentsHomeSection.RECENT -> stringResource(id = R.string.recent)
    }
}
