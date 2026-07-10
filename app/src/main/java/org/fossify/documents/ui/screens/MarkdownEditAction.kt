@file:Suppress("MagicNumber")

package org.fossify.documents.ui.screens

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

internal enum class MarkdownEditAction {
    Heading,
    Bold,
    Italic,
    Bullet,
    Quote,
    Code,
}

internal fun TextFieldValue.applyMarkdownAction(action: MarkdownEditAction): TextFieldValue {
    return when (action) {
        MarkdownEditAction.Heading -> prefixSelectedLines("# ") { line ->
            line.replace(headingPrefixRegex, "")
        }

        MarkdownEditAction.Bold -> wrapSelection("**", "**", "bold")
        MarkdownEditAction.Italic -> wrapSelection("_", "_", "italic")
        MarkdownEditAction.Bullet -> prefixSelectedLines("- ")
        MarkdownEditAction.Quote -> prefixSelectedLines("> ")
        MarkdownEditAction.Code -> codeSelection()
    }
}

internal fun TextFieldState.applyMarkdownAction(action: MarkdownEditAction) {
    val updated = TextFieldValue(
        text = text.toString(),
        selection = selection,
    ).applyMarkdownAction(action)

    edit {
        replace(0, length, updated.text)
        selection = updated.selection
    }
}

private fun TextFieldValue.wrapSelection(
    prefix: String,
    suffix: String,
    placeholder: String,
): TextFieldValue {
    val range = selection.normalizedBounds()
    val selectedText = text.substring(range.start, range.end)
    val content = selectedText.ifEmpty { placeholder }
    val replacement = prefix + content + suffix
    val newText = text.replaceRange(range.start, range.end, replacement)
    val newSelection = if (selectedText.isEmpty()) {
        TextRange(range.start + prefix.length, range.start + prefix.length + placeholder.length)
    } else {
        TextRange(range.start, range.start + replacement.length)
    }

    return TextFieldValue(newText, selection = newSelection)
}

private fun TextFieldValue.prefixSelectedLines(
    prefix: String,
    transformLine: (String) -> String = { it },
): TextFieldValue {
    val range = selection.normalizedBounds()
    val lineStart = if (range.start == 0) {
        0
    } else {
        text.lastIndexOf('\n', range.start - 1).let { index ->
            if (index == -1) 0 else index + 1
        }
    }
    val lineEnd = text.indexOf('\n', range.end).let { index ->
        if (index == -1) text.length else index
    }
    val selectedLines = text.substring(lineStart, lineEnd)
    val replacement = selectedLines
        .split('\n')
        .joinToString("\n") { line ->
            val transformed = transformLine(line)
            if (transformed.startsWith(prefix)) {
                transformed.removePrefix(prefix)
            } else {
                prefix + transformed
            }
        }
    val newText = text.replaceRange(lineStart, lineEnd, replacement)
    val delta = replacement.length - selectedLines.length

    return TextFieldValue(
        text = newText,
        selection = TextRange((range.end + delta).coerceIn(0, newText.length)),
    )
}

private fun TextFieldValue.codeSelection(): TextFieldValue {
    val range = selection.normalizedBounds()
    val selectedText = text.substring(range.start, range.end)
    return if ('\n' in selectedText) {
        wrapSelection("```\n", "\n```", selectedText)
    } else {
        wrapSelection("`", "`", "code")
    }
}

private fun TextRange.normalizedBounds(): SelectionBounds {
    return SelectionBounds(
        start = minOf(start, end),
        end = maxOf(start, end),
    )
}

private data class SelectionBounds(
    val start: Int,
    val end: Int,
)

private val headingPrefixRegex = Regex("^#{1,6}\\s*")
