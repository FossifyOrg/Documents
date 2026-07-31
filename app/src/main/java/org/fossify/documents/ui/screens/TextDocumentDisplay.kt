package org.fossify.documents.ui.screens

import androidx.compose.ui.text.TextRange

internal const val DEFAULT_DOCUMENT_TEXT_ZOOM = 1f
internal const val MIN_DOCUMENT_TEXT_ZOOM = 0.75f
internal const val MAX_DOCUMENT_TEXT_ZOOM = 3f
internal const val DOCUMENT_TEXT_ZOOM_STEP = 0.15f
private const val MAX_SEARCH_HIGHLIGHT_COUNT = 5_000

internal fun Float.coerceDocumentTextZoom(): Float {
    return takeIf { it.isFinite() }
        ?.coerceIn(MIN_DOCUMENT_TEXT_ZOOM, MAX_DOCUMENT_TEXT_ZOOM)
        ?: DEFAULT_DOCUMENT_TEXT_ZOOM
}

internal fun findTextMatches(
    text: String,
    query: String,
): List<TextRange> {
    if (query.isEmpty() || text.isEmpty()) {
        return emptyList()
    }

    return buildList {
        var searchFrom = 0
        while (searchFrom <= text.length - query.length) {
            val matchStart = text.indexOf(query, startIndex = searchFrom, ignoreCase = true)
            if (matchStart < 0) {
                break
            }

            add(TextRange(matchStart, matchStart + query.length))
            searchFrom = matchStart + query.length
        }
    }
}

internal fun shouldHighlightTextMatches(matchCount: Int): Boolean {
    return matchCount in 1..MAX_SEARCH_HIGHLIGHT_COUNT
}
