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
    NumberedList,
    Quote,
    Code,
}

internal fun TextFieldValue.applyMarkdownAction(action: MarkdownEditAction): TextFieldValue {
    return when (action) {
        MarkdownEditAction.Heading -> toggleHeading()
        MarkdownEditAction.Bold -> wrapSelection("**", "**", "bold")
        MarkdownEditAction.Italic -> wrapSelection("_", "_", "italic")
        MarkdownEditAction.Bullet -> prefixSelectedLines("- ")
        MarkdownEditAction.NumberedList -> toggleNumberedList()
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
    val selectionCanContainMarkers = selectedText.length >= prefix.length + suffix.length
    val selectionStartsWithPrefix = selectedText.startsWith(prefix)
    val selectionEndsWithSuffix = selectedText.endsWith(suffix)
    if (selectionCanContainMarkers && selectionStartsWithPrefix && selectionEndsWithSuffix) {
        val replacement = selectedText
            .removePrefix(prefix)
            .removeSuffix(suffix)
        return TextFieldValue(
            text = text.replaceRange(range.start, range.end, replacement),
            selection = TextRange(range.start, range.start + replacement.length),
        )
    }

    val outerStart = range.start - prefix.length
    val outerEnd = range.end + suffix.length
    val hasOuterPrefix = outerStart >= 0 && text.regionMatches(outerStart, prefix, 0, prefix.length)
    val hasOuterSuffix = outerEnd <= text.length && text.regionMatches(range.end, suffix, 0, suffix.length)
    if (hasOuterPrefix && hasOuterSuffix) {
        return TextFieldValue(
            text = text.replaceRange(outerStart, outerEnd, selectedText),
            selection = TextRange(outerStart, outerStart + selectedText.length),
        )
    }

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
    return transformSelectedLines { lines ->
        lines.map { line ->
            val transformed = transformLine(line)
            if (transformed.startsWith(prefix)) {
                transformed.removePrefix(prefix)
            } else {
                prefix + transformed
            }
        }
    }
}

private fun TextFieldValue.toggleHeading(): TextFieldValue {
    return transformSelectedLines { lines ->
        lines.map { line ->
            if (line.startsWith("# ")) {
                line.removePrefix("# ")
            } else {
                "# " + line.replace(headingPrefixRegex, "")
            }
        }
    }
}

private fun TextFieldValue.toggleNumberedList(): TextFieldValue {
    return transformSelectedLines { lines ->
        val removeNumbers = lines.all(numberedListPrefixRegex::containsMatchIn)
        lines.mapIndexed { index, line ->
            val content = line.replace(numberedListPrefixRegex, "")
            if (removeNumbers) content else "${index + 1}. $content"
        }
    }
}

private fun TextFieldValue.transformSelectedLines(
    transform: (List<String>) -> List<String>,
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
    val replacement = transform(selectedLines.split('\n')).joinToString("\n")
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
private val numberedListPrefixRegex = Regex("^\\d+[.)]\\s+")
