@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber")

package org.fossify.documents.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.fossify.commons.compose.lists.SimpleScaffold
import org.fossify.commons.compose.lists.simpleTopAppBarColors
import org.fossify.commons.compose.lists.topAppBarInsets
import org.fossify.commons.compose.lists.topAppBarPaddings
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.helpers.FONT_TYPE_CUSTOM
import org.fossify.commons.helpers.FONT_TYPE_MONOSPACE
import org.fossify.commons.helpers.FontHelper
import org.fossify.documents.R
import org.fossify.documents.data.StructuredDocumentContent
import org.fossify.documents.viewmodels.StructuredDocumentUiState
import java.io.File
import java.util.Locale
import android.graphics.Color as AndroidColor

@Composable
internal fun StructuredDocumentScreen(
    uiState: StructuredDocumentUiState,
    onBack: () -> Unit,
    onEdit: (() -> Unit)?,
    onOpenWith: () -> Unit,
    onOpenLink: (Uri) -> Unit,
) {
    SimpleScaffold(
        customTopBar = { scrolledColor: Color,
                         _,
                         scrollBehavior: TopAppBarScrollBehavior,
                         statusBarColor: Int,
                         colorTransitionFraction: Float,
                         contrastColor: Color ->
            StructuredDocumentTopBar(
                title = uiState.title,
                onBack = onBack,
                onEdit = onEdit,
                onOpenWith = onOpenWith,
                scrolledColor = scrolledColor,
                scrollBehavior = scrollBehavior,
                statusBarColor = statusBarColor,
                colorTransitionFraction = colorTransitionFraction,
                contrastColor = contrastColor,
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SimpleTheme.colorScheme.surface),
        ) {
            when {
                uiState.isLoading -> LoadingDocument()
                uiState.error != null -> StatusDocument(uiState.error, isError = true)
                uiState.content is StructuredDocumentContent.Web -> WebDocument(
                    content = uiState.content,
                    onOpenLink = onOpenLink,
                )

                uiState.content is StructuredDocumentContent.Table -> CsvTable(uiState.content)
                else -> StatusDocument(
                    text = stringResource(id = R.string.empty_document),
                    isError = false,
                )
            }
        }
    }
}

@Composable
private fun StructuredDocumentTopBar(
    title: String,
    onBack: () -> Unit,
    onEdit: (() -> Unit)?,
    onOpenWith: () -> Unit,
    scrolledColor: Color,
    scrollBehavior: TopAppBarScrollBehavior,
    statusBarColor: Int,
    colorTransitionFraction: Float,
    contrastColor: Color,
) {
    TopAppBar(
        title = {
            Text(
                text = title.ifBlank { stringResource(id = R.string.document) },
                color = scrolledColor,
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
                    tint = scrolledColor,
                )
            }
        },
        actions = {
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(id = R.string.edit_as_text),
                        tint = scrolledColor,
                    )
                }
            }
            IconButton(onClick = onOpenWith) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = stringResource(id = org.fossify.commons.R.string.open_with),
                    tint = scrolledColor,
                )
            }
        },
        scrollBehavior = scrollBehavior,
        colors = simpleTopAppBarColors(statusBarColor, colorTransitionFraction, contrastColor),
        modifier = Modifier.topAppBarPaddings(),
        windowInsets = topAppBarInsets(),
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebDocument(
    content: StructuredDocumentContent.Web,
    onOpenLink: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val currentOnOpenLink by rememberUpdatedState(onOpenLink)
    val backgroundColor = SimpleTheme.colorScheme.surface
    val textColor = SimpleTheme.colorScheme.onSurface
    val accentColor = SimpleTheme.colorScheme.primary
    val outlineColor = SimpleTheme.colorScheme.outlineVariant
    val codeColor = SimpleTheme.colorScheme.surfaceVariant
    val configuredFontType = context.baseConfig.fontType
    val configuredFontName = context.baseConfig.fontName
    val webFont = remember(context, configuredFontType, configuredFontName) {
        resolveWebDocumentFont(context, configuredFontType, configuredFontName)
    }
    val page = remember(
        content.html,
        backgroundColor,
        textColor,
        accentColor,
        outlineColor,
        codeColor,
        webFont,
    ) {
        buildWebPage(
            html = content.html,
            backgroundColor = backgroundColor,
            textColor = textColor,
            accentColor = accentColor,
            outlineColor = outlineColor,
            codeColor = codeColor,
            webFont = webFont,
        )
    }
    val webView = remember(context, webFont) {
        WebView(context).apply {
            setBackgroundColor(AndroidColor.TRANSPARENT)
            settings.javaScriptEnabled = false
            settings.domStorageEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.blockNetworkLoads = true
            settings.blockNetworkImage = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.safeBrowsingEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? {
                    if (request.url.toString() != WEB_FONT_URL) {
                        return super.shouldInterceptRequest(view, request)
                    }

                    return try {
                        webFont.file?.inputStream()?.let { input ->
                            WebResourceResponse(webFont.mimeType, null, input)
                        }
                    } catch (_: Exception) {
                        null
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val target = request.url
                    if (target.host == WEB_BASE_HOST && target.fragment != null) {
                        return false
                    }
                    currentOnOpenLink(target)
                    return true
                }
            }
        }
    }

    LaunchedEffect(webView, page) {
        webView.loadDataWithBaseURL(WEB_BASE_URL, page, "text/html", "UTF-8", null)
    }
    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }
    AndroidView(
        factory = { webView },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun CsvTable(content: StructuredDocumentContent.Table) {
    if (content.rows.isEmpty() || content.columnCount == 0) {
        StatusDocument(
            text = stringResource(id = R.string.empty_document),
            isError = false,
        )
        return
    }

    val columnWidths = remember(content.rows, content.columnCount) {
        calculateColumnWidths(content)
    }
    val totalWidth = columnWidths.fold(0.dp) { total, width -> total + width }
    val header = content.rows.first()
    val body = content.rows.drop(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
            .width(totalWidth),
    ) {
        CsvRow(
            values = header,
            columnWidths = columnWidths,
            header = true,
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .width(totalWidth),
        ) {
            itemsIndexed(body) { index, row ->
                CsvRow(
                    values = row,
                    columnWidths = columnWidths,
                    shaded = index % 2 == 1,
                )
            }
        }
    }
}

@Composable
private fun CsvRow(
    values: List<String>,
    columnWidths: List<Dp>,
    header: Boolean = false,
    shaded: Boolean = false,
) {
    val background = when {
        header -> SimpleTheme.colorScheme.primary.copy(alpha = primaryTintAlpha())
        shaded -> SimpleTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
        else -> SimpleTheme.colorScheme.surface
    }
    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .background(background),
    ) {
        columnWidths.forEachIndexed { index, width ->
            Surface(
                modifier = Modifier
                    .width(width)
                    .fillMaxHeight()
                    .heightIn(min = 48.dp)
                    .border(
                        border = BorderStroke(0.5.dp, SimpleTheme.colorScheme.outlineVariant),
                    ),
                color = Color.Transparent,
            ) {
                SelectionContainer {
                    Text(
                        text = values.getOrElse(index) { "" },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        color = SimpleTheme.colorScheme.onSurface,
                        style = SimpleTheme.typography.bodyMedium.copy(
                            fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                    )
                }
            }
        }
    }
}

private fun calculateColumnWidths(content: StructuredDocumentContent.Table): List<Dp> {
    return List(content.columnCount) { column ->
        val longestValue = content.rows.maxOfOrNull { row -> row.getOrElse(column) { "" }.length } ?: 0
        (longestValue.coerceIn(8, 28) * 8 + 24).dp
    }
}

private fun buildWebPage(
    html: String,
    backgroundColor: Color,
    textColor: Color,
    accentColor: Color,
    outlineColor: Color,
    codeColor: Color,
    webFont: WebDocumentFont,
): String {
    val customFontFace = webFont.file?.let {
        """
            @font-face {
                font-family: "FossifyReaderFont";
                src: url("$WEB_FONT_URL") format("${webFont.format}");
                font-display: swap;
            }
        """.trimIndent()
    }.orEmpty()
    val injectedHead = """
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
            $customFontFace
            :root { color-scheme: light dark; }
            html {
                background: ${backgroundColor.cssColor()};
                font-family: ${webFont.cssFamily};
            }
            body {
                box-sizing: border-box;
                max-width: 900px;
                min-height: 100vh;
                margin: 0 auto;
                padding: 20px;
                background: ${backgroundColor.cssColor()};
                color: ${textColor.cssColor()};
                font-size: 16px;
                line-height: 1.55;
                overflow-wrap: anywhere;
            }
            a { color: ${accentColor.cssColor()}; }
            img { max-width: 100%; height: auto; }
            table { display: block; max-width: 100%; overflow-x: auto; border-collapse: collapse; }
            th, td { padding: 8px 10px; border: 1px solid ${outlineColor.cssColor()}; text-align: start; }
            blockquote { margin-inline: 0; padding-inline-start: 14px; border-inline-start: 3px solid ${accentColor.cssColor()}; }
            pre { overflow-x: auto; padding: 12px; background: ${codeColor.cssColor()}; border-radius: 6px; }
            code { font-family: monospace; }
        </style>
    """.trimIndent()

    return html.replaceFirst("</head>", "$injectedHead</head>")
}

private fun resolveWebDocumentFont(
    context: Context,
    fontType: Int,
    fontName: String,
): WebDocumentFont {
    if (fontType == FONT_TYPE_MONOSPACE) {
        return WebDocumentFont(cssFamily = "monospace")
    }
    if (fontType != FONT_TYPE_CUSTOM || fontName.isBlank()) {
        return WebDocumentFont(cssFamily = "sans-serif")
    }

    val file = File(FontHelper.getFontsDir(context), fontName)
    if (!file.isFile) {
        return WebDocumentFont(cssFamily = "sans-serif")
    }

    val isOpenType = file.extension.equals("otf", ignoreCase = true)
    return WebDocumentFont(
        cssFamily = "\"FossifyReaderFont\", sans-serif",
        file = file,
        mimeType = if (isOpenType) "font/otf" else "font/ttf",
        format = if (isOpenType) "opentype" else "truetype",
    )
}

private fun Color.cssColor(): String {
    return String.format(Locale.ROOT, "#%06X", toArgb() and 0xFFFFFF)
}

private data class WebDocumentFont(
    val cssFamily: String,
    val file: File? = null,
    val mimeType: String = "",
    val format: String = "",
)

private const val WEB_BASE_HOST = "documents.fossify.local"
private const val WEB_BASE_URL = "https://$WEB_BASE_HOST/"
private const val WEB_FONT_URL = "${WEB_BASE_URL}reader-font"
