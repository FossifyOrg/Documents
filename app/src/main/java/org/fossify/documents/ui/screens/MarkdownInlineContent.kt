package org.fossify.documents.ui.screens

import android.content.ActivityNotFoundException
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.HtmlInline
import org.commonmark.node.Image
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.commons.extensions.toast
import org.fossify.documents.data.isAllowedExternalDocumentLink
import org.commonmark.node.Text as MarkdownText

@Composable
internal fun Node.inlineContent(): AnnotatedString {
    val linkColor = SimpleTheme.colorScheme.primary
    val codeBackground = markdownNeutralSurfaceColor(alpha = 0.08f)
    val linkListener = rememberMarkdownLinkListener()

    return buildAnnotatedString {
        fun appendNode(node: Node) {
            when (node) {
                is MarkdownText -> append(node.literal)
                is Code -> withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackground,
                    )
                ) { append(node.literal) }

                is Emphasis -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    node.children().forEach(::appendNode)
                }

                is StrongEmphasis -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    node.children().forEach(::appendNode)
                }

                is Strikethrough -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    node.children().forEach(::appendNode)
                }

                is Link -> appendLink(node, linkColor, linkListener, ::appendNode)

                is Image -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    node.children().forEach(::appendNode)
                }

                is SoftLineBreak -> append(" ")
                is HardLineBreak -> append("\n")
                is HtmlInline -> append(node.literal)
                is TaskListItemMarker -> Unit
                else -> node.children().forEach(::appendNode)
            }
        }

        children().forEach(::appendNode)
    }
}

@Composable
private fun rememberMarkdownLinkListener(): LinkInteractionListener {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    return remember(context, uriHandler) {
        LinkInteractionListener { annotation ->
            val url = (annotation as? LinkAnnotation.Url)?.url ?: return@LinkInteractionListener
            try {
                uriHandler.openUri(url)
            } catch (_: ActivityNotFoundException) {
                context.toast(org.fossify.commons.R.string.no_app_found)
            } catch (_: IllegalArgumentException) {
                context.toast(org.fossify.commons.R.string.no_app_found)
            } catch (_: SecurityException) {
                context.toast(org.fossify.commons.R.string.no_app_found)
            }
        }
    }
}

private fun AnnotatedString.Builder.appendLink(
    node: Link,
    linkColor: Color,
    linkListener: LinkInteractionListener,
    appendNode: (Node) -> Unit,
) {
    if (isAllowedExternalDocumentLink(node.destination)) {
        withLink(
            LinkAnnotation.Url(
                url = node.destination,
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                    )
                ),
                linkInteractionListener = linkListener,
            )
        ) {
            node.children().forEach(appendNode)
        }
    } else {
        node.children().forEach(appendNode)
    }
}

@Composable
internal fun markdownNeutralSurfaceColor(alpha: Float): Color {
    return SimpleTheme.colorScheme.onSurface
        .copy(alpha = alpha)
        .compositeOver(SimpleTheme.colorScheme.surface)
}

internal fun Node.children(): List<Node> {
    return generateSequence(firstChild) { it.next }.toList()
}

internal fun Node.descendants(): List<Node> {
    return children().flatMap { child -> listOf(child) + child.descendants() }
}
