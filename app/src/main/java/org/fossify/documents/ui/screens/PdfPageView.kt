package org.fossify.documents.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.infomaniak.lib.pdfview.PDFView
import com.infomaniak.lib.pdfview.link.DefaultLinkHandler
import com.infomaniak.lib.pdfview.model.LinkTapEvent
import com.infomaniak.lib.pdfview.scroll.DefaultScrollHandle
import com.infomaniak.lib.pdfview.util.FitPolicy
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.commons.compose.theme.isSurfaceNotLitWell
import org.fossify.documents.data.PdfTextMatch

@Composable
internal fun PdfPageView(
    uri: Uri,
    title: String,
    currentPage: Int,
    password: String?,
    nightMode: Boolean,
    showPageIndicator: Boolean,
    horizontalPaging: Boolean,
    search: PdfSearchState,
    onReady: (PDFView) -> Unit,
    onSelectionChange: (Boolean) -> Unit,
    onTap: () -> Unit,
    onPageChange: (Int, Int) -> Unit,
    onLoad: (Int) -> Unit,
    onError: (Throwable) -> Unit,
    modifier: Modifier = Modifier,
) {
    var view by remember { mutableStateOf<PDFView?>(null) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var pageIndicator by remember { mutableStateOf<DefaultScrollHandle?>(null) }
    val currentShowPageIndicator by rememberUpdatedState(showPageIndicator)
    val currentSelectedMatch by rememberUpdatedState(search.selectedMatch)
    val matchesByPage by rememberUpdatedState(remember(search.result) { search.result.matches.groupBy { it.page } })
    val backgroundColor = SimpleTheme.colorScheme.surfaceVariant.toArgb()
    val scrollHandleTextColor = SimpleTheme.colorScheme.onSurfaceVariant.toArgb()
    val colors = SimpleTheme.colorScheme
    val highlightColor = pdfHighlightColor(nightMode)
    val selectionColor = highlightColor.toArgb()
    val selectionHighlight = highlightColor.copy(alpha = 0.3f).toArgb()
    val matchColor by rememberUpdatedState(highlightColor.copy(alpha = MATCH_HIGHLIGHT_ALPHA).toArgb())
    val currentMatchColor by rememberUpdatedState(highlightColor.copy(alpha = CURRENT_MATCH_HIGHLIGHT_ALPHA).toArgb())
    LaunchedEffect(nightMode, colors, view) {
        view?.apply {
            setNightMode(nightMode)
            setSelectionHandleColor(selectionColor)
            setSelectionHighlightColor(selectionHighlight)
            setSelectionPopupBackgroundColor(colors.surfaceContainerHigh.toArgb())
            setSelectionPopupTextColor(colors.onSurface.toArgb())
            invalidate()
        }
    }
    LaunchedEffect(showPageIndicator) {
        if (!showPageIndicator) pageIndicator?.hide()
    }
    LaunchedEffect(view) { view?.performPageSnap() }
    LaunchedEffect(search.selectedMatch, search.navigationRequest, view, viewSize) {
        search.selectedMatch?.let { view?.showSearchMatch(it) }
    }
    LaunchedEffect(search.result, search.index) { view?.invalidate() }
    AndroidView(
        factory = { context ->
            ResizablePdfView(context).apply {
                val matchPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                var maxPageHeight = 0f
                addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                    maxPageHeight = maxHorizontalPageHeight()
                }
                setBackgroundColor(backgroundColor)
                minZoom = 1f
                midZoom = PDF_MID_ZOOM
                maxZoom = PDF_MAX_ZOOM
                useBestQuality(true)

                fromUri(uri)
                    .password(password)
                    .defaultPage(currentPage.coerceAtLeast(0))
                    .enableSwipe(true)
                    .enableDoubletap(true)
                    .enableTextSelection(true)
                    .selectionHandleColor(selectionColor)
                    .selectionHighlightColor(selectionHighlight)
                    .selectionPopupBackgroundColor(colors.surfaceContainerHigh.toArgb())
                    .selectionPopupTextColor(colors.onSurface.toArgb())
                    .nightMode(nightMode)
                    .selectionPopupText(context.getString(android.R.string.copy))
                    .onSelectionChange(onSelectionChange)
                    .onSelectionAction { text ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(title, text))
                        clearTextSelection()
                    }
                    .onDrawAll { canvas, width, height, page ->
                        val left = if (isSwipeVertical) (this.width * zoom - width) / 2f else 0f
                        val top = if (isSwipeVertical) 0f else (maxPageHeight * zoom - height) / 2f
                        matchesByPage[page].orEmpty().forEach { match ->
                            matchPaint.color = if (match === currentSelectedMatch) currentMatchColor else matchColor
                            match.bounds.forEach { rect ->
                                canvas.drawRect(
                                    left + rect.left * width, top + rect.top * height,
                                    left + rect.right * width, top + rect.bottom * height, matchPaint,
                                )
                            }
                        }
                    }
                    .linkHandler { handleReadingLink(it) }
                    .enableAnnotationRendering(true)
                    .enableAntialiasing(true)
                    .readingMode(horizontalPaging)
                    .scrollHandle(
                        object : DefaultScrollHandle(context) {
                            override fun show() {
                                if (currentShowPageIndicator) super.show()
                            }
                        }.apply {
                            setPageHandleBackground(context.scrollHandleBackground(horizontalPaging, backgroundColor))
                            setTextColor(scrollHandleTextColor)
                            pageIndicator = this
                        }
                    )
                    .onTap { onTap(); false }
                    .onPageChange(onPageChange)
                    .onLoad { count ->
                        if (horizontalPaging) resetZoom()
                        maxPageHeight = maxHorizontalPageHeight()
                        view = this
                        onReady(this)
                        onLoad(count)
                    }
                    .onError(onError)
                    .onPageError { _, error -> onError(error) }
                    .load()
            }
        },
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it },
        onRelease = { view -> view.recycle() },
    )
}

private fun PDFView.maxHorizontalPageHeight(): Float {
    if (isSwipeVertical) return 0f
    return (0 until pageCount).maxOfOrNull { getPageSize(it).height } ?: 0f
}

private class ResizablePdfView(context: Context) : PDFView(context, null) {
    private var lastSize = IntSize.Zero

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // PDFView divides by its previous page size when restoring the scroll position.
        if (w == 0 || h == 0) {
            stopFling()
            return
        }
        super.onSizeChanged(w, h, lastSize.width, lastSize.height)
        lastSize = IntSize(w, h)
    }
}

@Composable
private fun pdfHighlightColor(nightMode: Boolean) = if (nightMode == isSurfaceNotLitWell()) {
    SimpleTheme.colorScheme.primary
} else {
    SimpleTheme.colorScheme.inversePrimary
}

private fun Context.scrollHandleBackground(horizontalPaging: Boolean, color: Int): Drawable? {
    val resource = if (horizontalPaging) {
        com.infomaniak.lib.pdfview.R.drawable.default_scroll_handle_bottom
    } else {
        com.infomaniak.lib.pdfview.R.drawable.default_scroll_handle_right
    }
    return ContextCompat.getDrawable(this, resource)?.mutate()?.apply { setTint(color) }
}

private fun PDFView.Configurator.readingMode(horizontalPaging: Boolean): PDFView.Configurator {
    return swipeHorizontal(horizontalPaging)
        .pageFitPolicy(if (horizontalPaging) FitPolicy.BOTH else FitPolicy.WIDTH)
        .fitEachPage(false)
        .autoSpacing(horizontalPaging)
        .pageSnap(horizontalPaging)
        .pageFling(horizontalPaging)
        .pageSeparatorSpacing(if (horizontalPaging) 0 else PDF_PAGE_SPACING_DP)
        .apply { if (horizontalPaging) zoom(1f, PDF_MID_ZOOM, PDF_MAX_ZOOM) }
}

private const val PDF_MID_ZOOM = 1.75f
private const val PDF_MAX_ZOOM = 5f
private const val PDF_PAGE_SPACING_DP = 8
private const val MATCH_HIGHLIGHT_ALPHA = 0.18f
private const val CURRENT_MATCH_HIGHLIGHT_ALPHA = 0.4f

private fun PDFView.showSearchMatch(match: PdfTextMatch) {
    if (width == 0 || height == 0) return
    stopFling()
    jumpTo(match.page)
    val bounds = match.bounds.firstOrNull() ?: return
    if (!isSwipeVertical && zoom == minZoom) {
        performPageSnap()
        return
    }
    val size = getPageSize(match.page)
    val matchX: Float
    val matchY: Float
    if (isSwipeVertical) {
        val pageTop = (0 until match.page).sumOf { getPageSize(it).height.toDouble() + pageSeparatorSpacing }.toFloat()
        matchY = (startSpacing + pageTop + bounds.centerY() * size.height) * zoom
        matchX = ((width - size.width) / 2f + bounds.centerX() * size.width) * zoom
    } else {
        val pageLeft = match.page * (width + pageSeparatorSpacing) + (width - size.width) / 2f
        val maxPageHeight = (0 until pageCount).maxOf { getPageSize(it).height }
        matchX = (pageLeft + bounds.centerX() * size.width) * zoom
        matchY = ((maxPageHeight - size.height) / 2f + bounds.centerY() * size.height) * zoom
    }
    moveTo(width / 2f - matchX, height / 2f - matchY)
    loadPages()
}

internal fun PDFView.goToPage(page: Int, withAnimation: Boolean = false) {
    stopFling()
    jumpTo(page, withAnimation && isSwipeVertical)
    if (!isSwipeVertical) performPageSnap()
}

private fun PDFView.handleReadingLink(event: LinkTapEvent) {
    DefaultLinkHandler(this).handleLinkEvent(event)
    if (!isSwipeVertical && event.link.uri.isNullOrEmpty() && event.link.destPageIdx != null) {
        performPageSnap()
    }
}
