@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("FunctionNaming", "LongMethod", "LongParameterList", "MagicNumber")

package org.fossify.documents.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.Print
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.infomaniak.lib.pdfview.PDFView
import com.infomaniak.lib.pdfview.scroll.DefaultScrollHandle
import com.infomaniak.lib.pdfview.util.FitPolicy
import com.shockwave.pdfium.PdfPasswordException
import kotlinx.coroutines.delay
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.documents.R

@Composable
internal fun PdfDocumentScreen(
    uri: Uri,
    title: String,
    startPage: Int,
    onBack: () -> Unit,
    onPageChange: (page: Int, pageCount: Int) -> Unit,
    onLoad: (pageCount: Int) -> Unit,
    onPrint: () -> Unit,
    onOpenWith: () -> Unit,
    onFullscreenChange: (Boolean) -> Unit,
) {
    var pdfView by remember(uri) { mutableStateOf<PDFView?>(null) }
    var currentPage by rememberSaveable(uri) { mutableIntStateOf(startPage) }
    var pageCount by rememberSaveable(uri) { mutableIntStateOf(0) }
    var controlsVisible by rememberSaveable(uri) { mutableStateOf(true) }
    var initialAutoHidePending by rememberSaveable(uri) { mutableStateOf(true) }
    var hasLoaded by remember(uri) { mutableStateOf(false) }
    var isLoading by remember(uri) { mutableStateOf(true) }
    var error by remember(uri) { mutableStateOf<String?>(null) }
    var password by rememberSaveable(uri) { mutableStateOf<String?>(null) }
    var passwordInput by rememberSaveable(uri) { mutableStateOf("") }
    var passwordRequested by rememberSaveable(uri) { mutableStateOf(false) }
    var invalidPassword by rememberSaveable(uri) { mutableStateOf(false) }
    var loadAttempt by rememberSaveable(uri) { mutableIntStateOf(0) }
    val backgroundColor = SimpleTheme.colorScheme.surfaceVariant.toArgb()
    val scrollHandleTextColor = SimpleTheme.colorScheme.onPrimary.toArgb()
    val currentOnFullscreenChange by rememberUpdatedState(onFullscreenChange)

    BackHandler(enabled = !controlsVisible) {
        controlsVisible = true
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
            pdfView?.recycle()
            currentOnFullscreenChange(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SimpleTheme.colorScheme.surfaceVariant)
            .cancelInitialAutoHideOnTouch(uri) {
                initialAutoHidePending = false
            },
    ) {
        key(uri, loadAttempt) {
            AndroidView(
                factory = { context ->
                    PDFView(context, null).apply {
                        pdfView = this
                        setBackgroundColor(backgroundColor)
                        minZoom = 1f
                        midZoom = 1.75f
                        maxZoom = 5f
                        useBestQuality(true)

                        fromUri(uri)
                            .password(password)
                            .defaultPage(currentPage.coerceAtLeast(0))
                            .enableSwipe(true)
                            .swipeHorizontal(false)
                            .enableDoubletap(true)
                            .enableAnnotationRendering(true)
                            .enableAntialiasing(true)
                            .pageFitPolicy(FitPolicy.WIDTH)
                            .fitEachPage(false)
                            .autoSpacing(false)
                            .pageSnap(false)
                            .pageFling(false)
                            .pageSeparatorSpacing(8)
                            .scrollHandle(
                                DefaultScrollHandle(context).apply {
                                    setTextColor(scrollHandleTextColor)
                                }
                            )
                            .onTap {
                                controlsVisible = !controlsVisible
                                false
                            }
                            .onPageChange { page, count ->
                                currentPage = page
                                pageCount = count
                                onPageChange(page, count)
                            }
                            .onLoad { count ->
                                hasLoaded = true
                                isLoading = false
                                error = null
                                pageCount = count
                                passwordRequested = false
                                invalidPassword = false
                                onLoad(count)
                            }
                            .onError { throwable ->
                                isLoading = false
                                if (throwable is PdfPasswordException) {
                                    invalidPassword = password != null
                                    passwordRequested = true
                                } else {
                                    error = context.getString(R.string.could_not_open_document)
                                }
                            }
                            .onPageError { _, _ ->
                                error = context.getString(R.string.could_not_open_document)
                            }
                            .load()
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (error != null) {
            Text(
                text = error.orEmpty(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                color = SimpleTheme.colorScheme.error,
                style = SimpleTheme.typography.bodyLarge,
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            PdfTopBar(
                title = title,
                onBack = onBack,
                onPrint = onPrint,
                onOpenWith = onOpenWith,
            )
        }

        AnimatedVisibility(
            visible = controlsVisible && pageCount > 0,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            PdfControls(
                currentPage = currentPage,
                pageCount = pageCount,
                onPreviousPage = {
                    pdfView?.jumpTo((currentPage - 1).coerceAtLeast(0), true)
                },
                onNextPage = {
                    pdfView?.jumpTo((currentPage + 1).coerceAtMost(pageCount - 1), true)
                },
                onZoomOut = {
                    pdfView?.let { view ->
                        view.zoomWithAnimation((view.zoom - 0.5f).coerceAtLeast(view.minZoom))
                    }
                },
                onZoomIn = {
                    pdfView?.let { view ->
                        view.zoomWithAnimation((view.zoom + 0.5f).coerceAtMost(view.maxZoom))
                    }
                },
            )
        }
    }

    if (passwordRequested) {
        PasswordDialog(
            password = passwordInput,
            invalidPassword = invalidPassword,
            onPasswordChange = { passwordInput = it },
            onConfirm = {
                if (passwordInput.isNotBlank()) {
                    password = passwordInput
                    passwordRequested = false
                    isLoading = true
                    error = null
                    loadAttempt++
                }
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
            delay(INITIAL_CONTROLS_HIDE_DELAY_MS)
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

@Composable
private fun PdfTopBar(
    title: String,
    onBack: () -> Unit,
    onPrint: () -> Unit,
    onOpenWith: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = SimpleTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = org.fossify.commons.R.string.back),
                )
            }
        },
        actions = {
            IconButton(onClick = onPrint) {
                Icon(
                    imageVector = Icons.Filled.Print,
                    contentDescription = stringResource(id = org.fossify.commons.R.string.print),
                )
            }
            IconButton(onClick = onOpenWith) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = stringResource(id = org.fossify.commons.R.string.open_with),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SimpleTheme.colorScheme.surface,
        ),
    )
}

@Composable
private fun PdfControls(
    currentPage: Int,
    pageCount: Int,
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
            Text(
                text = stringResource(id = R.string.page_count_value, currentPage + 1, pageCount),
                style = SimpleTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
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
private fun PasswordDialog(
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
