@file:Suppress("CyclomaticComplexMethod")

package org.fossify.documents.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.infomaniak.lib.pdfview.PDFView
import com.shockwave.pdfium.PdfPasswordException
import kotlinx.coroutines.delay
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.documents.R
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun PdfDocumentScreen(
    uri: Uri,
    title: String,
    startPage: Int,
    onBack: () -> Unit,
    onPageChange: (page: Int, pageCount: Int) -> Unit,
    onLoad: (pageCount: Int) -> Unit,
    onSaveCopy: () -> Unit,
    isCopying: Boolean,
    onPrint: () -> Unit,
    onOpenWith: () -> Unit,
    onFullscreenChange: (Boolean) -> Unit,
    nightMode: Boolean,
    showPageIndicator: Boolean,
    horizontalPaging: Boolean,
    onNightModeChange: (Boolean) -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var topBarHeight by remember { mutableStateOf(0.dp) }
    var navigationHeight by remember { mutableStateOf(0.dp) }
    var showOutline by rememberSaveable(uri) { mutableStateOf(false) }
    var showGoToPage by rememberSaveable(uri) { mutableStateOf(false) }
    var loadAttempt by rememberSaveable(uri) { mutableIntStateOf(0) }
    var hasSelection by remember(uri, horizontalPaging, loadAttempt) { mutableStateOf(false) }
    var pdfView by remember(uri, horizontalPaging, loadAttempt) { mutableStateOf<PDFView?>(null) }
    val bookmarks = remember(pdfView) { pdfView?.tableOfContents.orEmpty() }
    var currentPage by rememberSaveable(uri) { mutableIntStateOf(startPage) }
    var pageCount by rememberSaveable(uri) { mutableIntStateOf(0) }
    var controlsVisible by rememberSaveable(uri) { mutableStateOf(true) }
    var initialAutoHidePending by rememberSaveable(uri) { mutableStateOf(true) }
    var hasLoaded by remember(uri, horizontalPaging, loadAttempt) { mutableStateOf(false) }
    var isLoading by remember(uri, horizontalPaging, loadAttempt) { mutableStateOf(true) }
    var error by remember(uri, horizontalPaging, loadAttempt) { mutableStateOf<String?>(null) }
    var password by rememberSaveable(uri) { mutableStateOf<String?>(null) }
    var passwordInput by rememberSaveable(uri) { mutableStateOf("") }
    var passwordRequested by rememberSaveable(uri) { mutableStateOf(false) }
    var invalidPassword by rememberSaveable(uri) { mutableStateOf(false) }
    val currentOnFullscreenChange by rememberUpdatedState(onFullscreenChange)

    val search = rememberPdfSearchState(uri, password)
    val searchKeyboardVisible = search.active && WindowInsets.ime.getBottom(density) > 0
    BackHandler(enabled = !controlsVisible || search.active || hasSelection) {
        when {
            hasSelection -> pdfView?.clearTextSelection()
            search.active -> search.close()
            else -> controlsVisible = true
        }
    }

    LaunchedEffect(controlsVisible) {
        currentOnFullscreenChange(!controlsVisible)
    }

    AutoHideInitialPdfControls(
        hasLoaded = hasLoaded,
        pending = initialAutoHidePending,
    ) {
        controlsVisible = false
        initialAutoHidePending = false
    }

    DisposableEffect(uri) {
        onDispose {
            currentOnFullscreenChange(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (search.active) Modifier.imePadding() else Modifier)
            .background(SimpleTheme.colorScheme.surfaceVariant)
            .cancelInitialAutoHideOnTouch(uri) {
                initialAutoHidePending = false
            },
    ) {
        key(uri, loadAttempt, horizontalPaging) {
            PdfPageView(
                uri = uri,
                title = title,
                currentPage = currentPage,
                password = password,
                nightMode = nightMode,
                showPageIndicator = showPageIndicator,
                horizontalPaging = horizontalPaging,
                search = search,
                modifier = if (search.active) {
                    Modifier.padding(
                        top = topBarHeight,
                        bottom = if (searchKeyboardVisible) 0.dp else navigationHeight,
                    )
                } else {
                    Modifier
                },
                onReady = { pdfView = it },
                onSelectionChange = { hasSelection = it },
                onTap = { if (!search.active) controlsVisible = !controlsVisible },
                onPageChange = { page, count ->
                    currentPage = page
                    pageCount = count
                    onPageChange(page, count)
                },
                onLoad = { count ->
                    hasLoaded = true
                    isLoading = false
                    error = null
                    pageCount = count
                    passwordRequested = false
                    invalidPassword = false
                    onLoad(count)
                },
                onError = { throwable ->
                    isLoading = false
                    if (throwable is PdfPasswordException) {
                        invalidPassword = password != null
                        passwordRequested = true
                    } else {
                        error = context.getString(R.string.could_not_open_document)
                    }
                },
            )
        }

        PdfLoadingStatus(isLoading, error, Modifier.align(Alignment.Center))

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Column(Modifier.onSizeChanged { topBarHeight = with(density) { it.height.toDp() } }) {
                PdfTopBar(
                    title = title,
                    onBack = { search.back(onBack) },
                    searchActive = search.active,
                    searchQuery = search.query,
                    searchResult = search.result,
                    searchIndex = search.index,
                    onSearch = {
                        pdfView?.clearTextSelection()
                        search.active = true
                    },
                    onSearchQueryChange = search::updateQuery,
                    onChangeMatch = search::changeMatch,
                    onOutline = { showOutline = true },
                    hasOutline = bookmarks.isNotEmpty(),
                    onPrint = onPrint,
                    menu = {
                        PdfReadingMenu(
                            nightMode = nightMode,
                            onNightModeChange = onNightModeChange,
                            onSettings = onSettings,
                            onGoToPage = { showGoToPage = true },
                            onSaveCopy = onSaveCopy,
                            isCopying = isCopying,
                            onPrint = onPrint,
                            showPrint = bookmarks.isNotEmpty(),
                            onOpenWith = onOpenWith,
                            isLoaded = hasLoaded,
                        )
                    },
                    isLoaded = hasLoaded,
                )
                if (isCopying) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                PdfSearchStatus(search)
            }
        }

        AnimatedVisibility(
            visible = controlsVisible && pageCount > 0 && !searchKeyboardVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { navigationHeight = with(density) { it.height.toDp() } },
        ) {
            PdfNavigationControls(
                view = pdfView,
                currentPage = currentPage,
                pageCount = pageCount,
                onGoToPage = { showGoToPage = true },
            )
        }
    }

    if (showOutline && bookmarks.isNotEmpty()) {
        PdfOutlineSheet(
            bookmarks = bookmarks,
            onGo = { page -> pdfView?.goToPage(page); showOutline = false },
            onDismiss = { showOutline = false },
        )
    }
    if (showGoToPage) {
        PdfGoToPageDialog(
            currentPage = currentPage,
            pageCount = pageCount,
            onGo = { page -> pdfView?.goToPage(page); showGoToPage = false },
            onDismiss = { showGoToPage = false },
        )
    }
    if (passwordRequested) {
        PasswordDialog(
            password = passwordInput,
            invalidPassword = invalidPassword,
            onPasswordChange = { passwordInput = it },
            onConfirm = {
                password = passwordInput
                passwordRequested = false
                isLoading = true
                error = null
                loadAttempt++
            },
            onDismiss = onBack,
        )
    }
}

private const val INITIAL_CONTROLS_HIDE_DELAY_MS = 500L

@Composable
private fun AutoHideInitialPdfControls(
    hasLoaded: Boolean,
    pending: Boolean,
    onAutoHide: () -> Unit,
) {
    val currentOnAutoHide by rememberUpdatedState(onAutoHide)
    LaunchedEffect(hasLoaded, pending) {
        if (hasLoaded && pending) {
            delay(INITIAL_CONTROLS_HIDE_DELAY_MS.milliseconds)
            currentOnAutoHide()
        }
    }
}

private fun Modifier.cancelInitialAutoHideOnTouch(uri: Uri, onTouch: () -> Unit): Modifier {
    return pointerInput(uri) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.any { it.pressed }) {
                    onTouch()
                }
            }
        }
    }
}
