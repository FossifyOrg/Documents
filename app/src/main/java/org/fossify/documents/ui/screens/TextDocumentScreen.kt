@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("FunctionNaming", "LongMethod", "LongParameterList", "MagicNumber")

package org.fossify.documents.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.drop
import org.fossify.commons.compose.lists.SimpleScaffold
import org.fossify.commons.compose.lists.simpleTopAppBarColors
import org.fossify.commons.compose.lists.topAppBarInsets
import org.fossify.commons.compose.lists.topAppBarPaddings
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.documents.R
import org.fossify.documents.viewmodels.TextDocumentUiState
import kotlin.math.roundToInt

@Composable
internal fun TextDocumentScreen(
    uiState: TextDocumentUiState,
    onBack: () -> Unit,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onOpenWith: () -> Unit,
    onPreviewChange: (Boolean) -> Unit,
    textZoom: Float,
    onTextZoomChange: (Float) -> Unit,
    onResetTextZoom: () -> Unit,
) {
    val editorState = rememberTextFieldState(
        initialText = uiState.text,
        initialSelection = TextRange.Zero,
    )

    LaunchedEffect(uiState.isLoaded, uiState.text) {
        if (uiState.isLoaded && uiState.text != editorState.text.toString()) {
            val currentSelection = editorState.selection
            editorState.edit {
                replace(0, length, uiState.text)
                selection = TextRange(
                    start = currentSelection.start.coerceIn(0, uiState.text.length),
                    end = currentSelection.end.coerceIn(0, uiState.text.length),
                )
            }
        }
    }

    LaunchedEffect(editorState) {
        snapshotFlow { editorState.text.toString() }
            .drop(1)
            .collect(onTextChange)
    }

    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var currentSearchIndex by rememberSaveable { mutableIntStateOf(0) }
    val editorText = editorState.text.toString()
    val searchMatches = remember(editorText, searchQuery) {
        findTextMatches(editorText, searchQuery)
    }
    val normalizedSearchIndex = currentSearchIndex.takeIf { searchMatches.isNotEmpty() }
        ?.mod(searchMatches.size)
        ?: 0
    val currentSearchMatch = searchMatches.getOrNull(normalizedSearchIndex)

    LaunchedEffect(searchQuery, editorText) {
        currentSearchIndex = 0
    }

    LaunchedEffect(currentSearchMatch) {
        currentSearchMatch?.let { match ->
            editorState.edit {
                selection = match
            }
        }
    }

    val closeSearch = {
        searchActive = false
        searchQuery = ""
        currentSearchIndex = 0
    }
    val openSearch = {
        if (uiState.previewEnabled) {
            onPreviewChange(false)
        }
        searchActive = true
    }
    val goToPreviousMatch = {
        if (searchMatches.isNotEmpty()) {
            currentSearchIndex = (normalizedSearchIndex - 1).mod(searchMatches.size)
        }
    }
    val goToNextMatch = {
        if (searchMatches.isNotEmpty()) {
            currentSearchIndex = (normalizedSearchIndex + 1).mod(searchMatches.size)
        }
    }

    BackHandler(enabled = searchActive, onBack = closeSearch)

    SimpleScaffold(
        customTopBar = { scrolledColor: Color,
                         _,
                         scrollBehavior: TopAppBarScrollBehavior,
                         statusBarColor: Int,
                         colorTransitionFraction: Float,
                         contrastColor: Color ->
            TextDocumentTopBar(
                uiState = uiState,
                onBack = if (searchActive) closeSearch else onBack,
                onSave = onSave,
                onOpenWith = onOpenWith,
                searchActive = searchActive,
                searchQuery = searchQuery,
                searchMatchCount = searchMatches.size,
                currentSearchMatchNumber = if (searchMatches.isEmpty()) 0 else normalizedSearchIndex + 1,
                onSearch = openSearch,
                onSearchQueryChange = {
                    searchQuery = it
                    currentSearchIndex = 0
                },
                onPreviousMatch = goToPreviousMatch,
                onNextMatch = goToNextMatch,
                textZoom = textZoom,
                onTextZoomChange = onTextZoomChange,
                onResetTextZoom = onResetTextZoom,
                scrolledColor = scrolledColor,
                scrollBehavior = scrollBehavior,
                statusBarColor = statusBarColor,
                colorTransitionFraction = colorTransitionFraction,
                contrastColor = contrastColor,
            )
        }
    ) {
        TextDocumentContent(
            uiState = uiState,
            editorState = editorState,
            onPreviewChange = onPreviewChange,
            searchMatches = searchMatches,
            currentSearchIndex = normalizedSearchIndex,
            textZoom = textZoom,
            onTextZoomChange = onTextZoomChange,
        )
    }
}

@Composable
private fun TextDocumentTopBar(
    uiState: TextDocumentUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onOpenWith: () -> Unit,
    searchActive: Boolean,
    searchQuery: String,
    searchMatchCount: Int,
    currentSearchMatchNumber: Int,
    onSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onPreviousMatch: () -> Unit,
    onNextMatch: () -> Unit,
    textZoom: Float,
    onTextZoomChange: (Float) -> Unit,
    onResetTextZoom: () -> Unit,
    scrolledColor: Color,
    scrollBehavior: TopAppBarScrollBehavior,
    statusBarColor: Int,
    colorTransitionFraction: Float,
    contrastColor: Color,
) {
    TopAppBar(
        title = {
            if (searchActive) {
                TextDocumentSearchField(
                    query = searchQuery,
                    currentMatchNumber = currentSearchMatchNumber,
                    matchCount = searchMatchCount,
                    onQueryChange = onSearchQueryChange,
                    onNextMatch = onNextMatch,
                )
            } else {
                Text(
                    text = uiState.title.ifBlank { stringResource(id = R.string.document) },
                    color = scrolledColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = SimpleTheme.typography.titleLarge,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = org.fossify.commons.R.string.back),
                    tint = scrolledColor,
                )
            }
        },
        actions = {
            if (searchActive) {
                IconButton(
                    onClick = onPreviousMatch,
                    enabled = searchMatchCount > 0,
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = stringResource(id = R.string.previous_match),
                    )
                }
                IconButton(
                    onClick = onNextMatch,
                    enabled = searchMatchCount > 0,
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(id = R.string.next_match),
                    )
                }
            } else {
                IconButton(onClick = onSearch) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(id = R.string.search_text_in_document),
                        tint = scrolledColor,
                    )
                }
            }
            if (!searchActive && !uiState.isReadOnly) {
                IconButton(
                    onClick = onSave,
                    enabled = uiState.isDirty && !uiState.isSaving && !uiState.isLoading,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = stringResource(id = org.fossify.commons.R.string.save),
                        tint = scrolledColor.copy(alpha = if (uiState.isDirty) 1f else 0.45f),
                    )
                }
            }
            if (!searchActive) {
                TextDocumentOverflowMenu(
                    textZoom = textZoom,
                    onTextZoomChange = onTextZoomChange,
                    onResetTextZoom = onResetTextZoom,
                    onOpenWith = onOpenWith,
                )
            }
        },
        scrollBehavior = scrollBehavior,
        colors = simpleTopAppBarColors(statusBarColor, colorTransitionFraction, contrastColor),
        modifier = Modifier.topAppBarPaddings(),
        windowInsets = topAppBarInsets(),
    )
}

@Composable
private fun TextDocumentContent(
    uiState: TextDocumentUiState,
    editorState: TextFieldState,
    onPreviewChange: (Boolean) -> Unit,
    searchMatches: List<TextRange>,
    currentSearchIndex: Int,
    textZoom: Float,
    onTextZoomChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
            .background(SimpleTheme.colorScheme.surface)
            .imePadding(),
    ) {
        when {
            uiState.isLoading -> LoadingDocument()
            !uiState.isLoaded && uiState.error != null -> StatusDocument(uiState.error, isError = true)
            else -> LoadedTextDocumentContent(
                uiState = uiState,
                editorState = editorState,
                onPreviewChange = onPreviewChange,
                searchMatches = searchMatches,
                currentSearchIndex = currentSearchIndex,
                textZoom = textZoom,
                onTextZoomChange = onTextZoomChange,
            )
        }
    }
}

@Composable
private fun ColumnScope.LoadedTextDocumentContent(
    uiState: TextDocumentUiState,
    editorState: TextFieldState,
    onPreviewChange: (Boolean) -> Unit,
    searchMatches: List<TextRange>,
    currentSearchIndex: Int,
    textZoom: Float,
    onTextZoomChange: (Float) -> Unit,
) {
    if (uiState.isMarkdown) {
        MarkdownModeRow(
            previewEnabled = uiState.previewEnabled,
            onPreviewChange = onPreviewChange,
        )
    }

    if (uiState.isReadOnly) {
        StatusStrip(
            text = uiState.readOnlyReason ?: stringResource(id = R.string.read_only),
            isError = false,
        )
    }
    uiState.error?.let {
        StatusStrip(text = it, isError = true)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    ) {
        if (uiState.previewEnabled && uiState.isMarkdown) {
            MarkdownPreview(
                markdown = uiState.text,
                textZoom = textZoom,
                onTextZoomChange = onTextZoomChange,
            )
        } else {
            TextEditor(
                state = editorState,
                readOnly = uiState.isReadOnly,
                searchMatches = searchMatches,
                currentSearchIndex = currentSearchIndex,
                textZoom = textZoom,
                onTextZoomChange = onTextZoomChange,
            )
        }
    }
    val showFormattingToolbar = uiState.isMarkdown && !uiState.previewEnabled && !uiState.isReadOnly
    EditorStatusBar(
        uiState = uiState,
        modifier = if (showFormattingToolbar) Modifier else Modifier.navigationBarsPadding(),
    )
    if (showFormattingToolbar) {
        MarkdownFormattingToolbar(
            onAction = { action ->
                editorState.applyMarkdownAction(action)
            },
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

@Composable
private fun MarkdownModeRow(
    previewEnabled: Boolean,
    onPreviewChange: (Boolean) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SegmentedButton(
            selected = !previewEnabled,
            onClick = { onPreviewChange(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = MARKDOWN_MODE_COUNT),
            label = {
                Text(
                    text = stringResource(id = org.fossify.commons.R.string.edit),
                    maxLines = 1,
                    style = SimpleTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            },
        )
        SegmentedButton(
            selected = previewEnabled,
            onClick = { onPreviewChange(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = MARKDOWN_MODE_COUNT),
            label = {
                Text(
                    text = stringResource(id = R.string.preview),
                    maxLines = 1,
                    style = SimpleTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            },
        )
    }
}

@Composable
private fun TextEditor(
    state: TextFieldState,
    readOnly: Boolean,
    searchMatches: List<TextRange>,
    currentSearchIndex: Int,
    textZoom: Float,
    onTextZoomChange: (Float) -> Unit,
) {
    val scrollState = rememberScrollState()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var viewportHeight by remember { mutableIntStateOf(0) }
    var zoomGestureActive by remember { mutableStateOf(false) }
    val cursorMargin = with(LocalDensity.current) { CURSOR_VISIBILITY_MARGIN.toPx() }
    val matchColor = SimpleTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
    val currentMatchColor = SimpleTheme.colorScheme.primaryContainer
    val searchTransformation = if (shouldHighlightTextMatches(searchMatches.size)) {
        remember(
            searchMatches,
            currentSearchIndex,
            matchColor,
            currentMatchColor,
        ) {
            OutputTransformation {
                searchMatches.forEachIndexed { index, match ->
                    if (match.min >= 0 && match.max <= length) {
                        addStyle(
                            spanStyle = SpanStyle(
                                background = if (index == currentSearchIndex) {
                                    currentMatchColor
                                } else {
                                    matchColor
                                },
                            ),
                            start = match.min,
                            end = match.max,
                        )
                    }
                }
            }
        }
    } else {
        null
    }
    val maxScroll = scrollState.maxValue

    LaunchedEffect(state.selection, viewportHeight, textLayoutResult, maxScroll, cursorMargin, zoomGestureActive) {
        if (zoomGestureActive) {
            return@LaunchedEffect
        }
        val layout = textLayoutResult ?: return@LaunchedEffect
        if (viewportHeight <= 0 || maxScroll <= 0) {
            return@LaunchedEffect
        }

        val cursor = layout.getCursorRect(state.selection.end)
        val viewportTop = scrollState.value.toFloat()
        val viewportBottom = viewportTop + viewportHeight
        val target = when {
            cursor.bottom > viewportBottom - cursorMargin -> {
                cursor.bottom - viewportHeight + cursorMargin
            }

            cursor.top < viewportTop + cursorMargin -> cursor.top - cursorMargin
            else -> return@LaunchedEffect
        }
        scrollState.scrollTo(target.roundToInt().coerceIn(0, maxScroll))
    }

    BasicTextField(
        state = state,
        readOnly = readOnly,
        modifier = Modifier
            .fillMaxSize()
            .documentTextZoomGesture(
                textZoom = textZoom,
                onTextZoomChange = onTextZoomChange,
                onZoomGestureChange = { zoomGestureActive = it },
            )
            .padding(16.dp)
            .onSizeChanged { viewportHeight = it.height },
        textStyle = SimpleTheme.typography.bodyLarge.copy(
            color = SimpleTheme.colorScheme.onSurface,
            fontSize = SimpleTheme.typography.bodyLarge.fontSize * textZoom,
            lineHeight = 24.sp * textZoom,
        ),
        cursorBrush = SolidColor(SimpleTheme.colorScheme.primary),
        outputTransformation = searchTransformation,
        scrollState = scrollState,
        onTextLayout = { getResult ->
            textLayoutResult = getResult()
        },
        decorator = { innerTextField ->
            if (state.text.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.empty_document),
                    color = SimpleTheme.colorScheme.onSurfaceVariant,
                    style = SimpleTheme.typography.bodyLarge,
                )
            }
            innerTextField()
        }
    )
}

private const val MARKDOWN_MODE_COUNT = 2
private val CURSOR_VISIBILITY_MARGIN = 24.dp
