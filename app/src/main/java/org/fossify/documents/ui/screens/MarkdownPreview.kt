@file:Suppress("FunctionNaming", "MagicNumber")

package org.fossify.documents.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import org.commonmark.Extension
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.LinkReferenceDefinition
import org.commonmark.node.ListBlock
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser
import org.fossify.commons.compose.theme.SimpleTheme
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

private val markdownExtensions: List<Extension> = listOf(
    AutolinkExtension.create(),
    StrikethroughExtension.create(),
    TablesExtension.create(),
    TaskListItemsExtension.create(),
)

private val markdownParser: Parser = Parser.builder()
    .extensions(markdownExtensions)
    .build()

@Composable
internal fun MarkdownPreview(
    markdown: String,
    textZoom: Float,
    onTextZoomChange: (Float) -> Unit,
) {
    val document = remember(markdown) { markdownParser.parse(markdown) }
    val scrollState = rememberScrollState()
    var pendingScrollPosition by remember { mutableStateOf<PendingScrollPosition?>(null) }

    LaunchedEffect(textZoom) {
        val pendingPosition = pendingScrollPosition ?: return@LaunchedEffect
        val newMaxValue = snapshotFlow { scrollState.maxValue }
            .first { it > 0 && it != pendingPosition.maxValue }
        scrollState.scrollTo((newMaxValue * pendingPosition.fraction).roundToInt())
        pendingScrollPosition = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .documentTextZoomGesture(
                textZoom = textZoom,
                onTextZoomChange = { requestedZoom ->
                    if (requestedZoom != textZoom) {
                        pendingScrollPosition = PendingScrollPosition(
                            fraction = if (scrollState.maxValue > 0) {
                                scrollState.value.toFloat() / scrollState.maxValue
                            } else {
                                0f
                            },
                            maxValue = scrollState.maxValue,
                        )
                    }
                    onTextZoomChange(requestedZoom)
                },
            )
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        document.children().forEach { node ->
            MarkdownBlock(node, textZoom)
        }
    }
}

@Composable
private fun MarkdownBlock(
    node: Node,
    textZoom: Float,
) {
    when (node) {
        is Heading -> MarkdownHeading(node, textZoom)
        is Paragraph -> MarkdownParagraph(node, textZoom)
        is BulletList -> MarkdownList(node, textZoom)
        is OrderedList -> MarkdownList(node, textZoom)
        is BlockQuote -> MarkdownQuote(node, textZoom)
        is FencedCodeBlock -> MarkdownCode(node.literal, textZoom)
        is IndentedCodeBlock -> MarkdownCode(node.literal, textZoom)
        is TableBlock -> MarkdownTable(node, textZoom)
        is ThematicBreak -> HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        is HtmlBlock -> MarkdownCode(node.literal, textZoom)
        is LinkReferenceDefinition -> Unit
        else -> Column {
            node.children().forEach { child -> MarkdownBlock(child, textZoom) }
        }
    }
}

@Composable
private fun MarkdownHeading(
    node: Heading,
    textZoom: Float,
) {
    val style = when (node.level) {
        1 -> SimpleTheme.typography.headlineLarge
        2 -> SimpleTheme.typography.headlineMedium
        else -> SimpleTheme.typography.titleLarge
    }
    Text(
        text = node.inlineContent(),
        style = style.documentTextZoomed(textZoom).copy(fontWeight = FontWeight.SemiBold),
        color = SimpleTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
    )
}

@Composable
private fun MarkdownParagraph(
    node: Paragraph,
    textZoom: Float,
    inList: Boolean = false,
) {
    Text(
        text = node.inlineContent(),
        style = SimpleTheme.typography.bodyLarge.copy(lineHeight = 24.sp).documentTextZoomed(textZoom),
        color = SimpleTheme.colorScheme.onSurface,
        modifier = if (inList) Modifier else Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun MarkdownList(
    node: ListBlock,
    textZoom: Float,
) {
    val start = (node as? OrderedList)?.markerStartNumber ?: 1
    val markerStyle = SimpleTheme.typography.bodyLarge
        .copy(lineHeight = 24.sp)
        .documentTextZoomed(textZoom)
    val markerWidth = LIST_MARKER_WIDTH * textZoom
    val markerGap = LIST_MARKER_GAP * textZoom
    val markerIconSize = LIST_MARKER_ICON_SIZE * textZoom
    val markerLineHeight = with(LocalDensity.current) {
        markerStyle.lineHeight.toDp()
    }

    Column(modifier = Modifier.padding(vertical = LIST_VERTICAL_PADDING * textZoom)) {
        node.children().filterIsInstance<ListItem>().forEachIndexed { index, item ->
            val task = item.children().filterIsInstance<TaskListItemMarker>().firstOrNull()
            Row(
                modifier = Modifier.padding(vertical = LIST_ITEM_VERTICAL_PADDING * textZoom),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .width(markerWidth)
                        .height(markerLineHeight),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    if (task != null) {
                        Icon(
                            imageVector = if (task.isChecked) {
                                Icons.Filled.CheckBox
                            } else {
                                Icons.Filled.CheckBoxOutlineBlank
                            },
                            contentDescription = null,
                            modifier = Modifier.size(markerIconSize),
                            tint = SimpleTheme.colorScheme.primary,
                        )
                    } else {
                        Text(
                            text = if (node is OrderedList) "${start + index}." else "•",
                            style = markerStyle,
                            color = SimpleTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .padding(start = markerGap)
                        .weight(1f),
                ) {
                    item.children()
                        .filterNot { it is TaskListItemMarker }
                        .forEach { child ->
                            if (child is Paragraph) {
                                MarkdownParagraph(child, textZoom, inList = true)
                            } else {
                                MarkdownBlock(child, textZoom)
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun MarkdownQuote(
    node: BlockQuote,
    textZoom: Float,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .background(SimpleTheme.colorScheme.outline),
        )
        Column(modifier = Modifier.padding(start = 12.dp, end = 4.dp)) {
            node.children().forEach { child -> MarkdownBlock(child, textZoom) }
        }
    }
}

@Composable
private fun MarkdownCode(
    code: String,
    textZoom: Float,
) {
    Surface(
        color = markdownNeutralSurfaceColor(CONTAINER_TINT_ALPHA),
        contentColor = SimpleTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Text(
                text = code.trimEnd(),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ).documentTextZoomed(textZoom),
                softWrap = false,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
private fun MarkdownTable(
    node: TableBlock,
    textZoom: Float,
) {
    val rows = node.descendants().filterIsInstance<TableRow>()
    val borderColor = SimpleTheme.colorScheme.outlineVariant
    val columnCount = rows.maxOfOrNull { row ->
        row.children().count { it is TableCell }
    } ?: 1

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        val tableWidth = maxWidth.coerceAtLeast(TABLE_COLUMN_MIN_WIDTH * columnCount)
        val columnWidth = tableWidth / columnCount

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            Column(modifier = Modifier.width(tableWidth)) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .width(tableWidth)
                            .height(IntrinsicSize.Min),
                    ) {
                        row.children().filterIsInstance<TableCell>().forEach { cell ->
                            Surface(
                                modifier = Modifier
                                    .width(columnWidth)
                                    .fillMaxHeight(),
                                color = if (cell.isHeader) {
                                    markdownNeutralSurfaceColor(TABLE_HEADER_TINT_ALPHA)
                                } else {
                                    SimpleTheme.colorScheme.surface
                                },
                                border = BorderStroke(0.5.dp, borderColor),
                            ) {
                                Text(
                                    text = cell.inlineContent(),
                                    style = SimpleTheme.typography.bodyMedium.documentTextZoomed(textZoom).copy(
                                        fontWeight = if (cell.isHeader) FontWeight.SemiBold else FontWeight.Normal,
                                    ),
                                    color = SimpleTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun TextStyle.documentTextZoomed(zoom: Float): TextStyle {
    return copy(
        fontSize = if (fontSize.isSpecified) fontSize * zoom else fontSize,
        lineHeight = if (lineHeight.isSpecified) lineHeight * zoom else lineHeight,
    )
}

private const val CONTAINER_TINT_ALPHA = 0.06f
private const val TABLE_HEADER_TINT_ALPHA = 0.1f
private data class PendingScrollPosition(
    val fraction: Float,
    val maxValue: Int,
)

private val LIST_MARKER_WIDTH = 24.dp
private val LIST_MARKER_GAP = 8.dp
private val LIST_MARKER_ICON_SIZE = 20.dp
private val LIST_VERTICAL_PADDING = 3.dp
private val LIST_ITEM_VERTICAL_PADDING = 2.dp
private val TABLE_COLUMN_MIN_WIDTH = 144.dp
