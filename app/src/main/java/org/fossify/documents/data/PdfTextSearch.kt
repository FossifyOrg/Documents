package org.fossify.documents.data

import android.content.Context
import android.graphics.RectF
import android.net.Uri
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
import kotlin.math.abs

internal data class PdfTextMatch(val page: Int, val bounds: List<RectF>)

internal data class PdfSearchResult(
    val matches: List<PdfTextMatch> = emptyList(),
    val hasText: Boolean = false,
)

internal class PdfTextSearch(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val core = PdfiumCore(context.applicationContext)

    fun search(uri: Uri, password: String?, query: String) = flow {
        val descriptor = resolver.openFileDescriptor(uri, "r") ?: throw IOException()
        descriptor.use {
            val document = core.newDocument(descriptor, password)
            try {
                val matches = mutableListOf<PdfTextMatch>()
                var hasText = false
                val count = core.getPageCount(document)
                for (page in 0 until count) {
                    currentCoroutineContext().ensureActive()
                    core.openPage(document, page)
                    val text = core.getPageText(document, page)
                    hasText = hasText || text.isNotBlank()
                    for (range in findPdfTextMatches(text, query)) {
                        currentCoroutineContext().ensureActive()
                        val start = core.getCharIndexFromTextIndex(document, page, range.first)
                        val end = core.getCharIndexFromTextIndex(document, page, range.last)
                        if (start >= 0 && end >= start) {
                            matches += PdfTextMatch(page, matchBounds(document, page, start..end))
                        }
                    }
                    core.closeTextPage(document, page)
                    emit(PdfSearchResult(matches.toList(), hasText))
                }
            } finally {
                core.closeDocument(document)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun matchBounds(document: PdfDocument, page: Int, range: IntRange): List<RectF> {
        val boxes = range.mapNotNull { index ->
            core.getCharBox(document, page, index)?.let { box ->
                core.mapRectToDevice(document, page, 0, 0, COORDINATE_SCALE, COORDINATE_SCALE, 0, box)
                    .apply {
                        sort()
                        set(
                            left / COORDINATE_SCALE, top / COORDINATE_SCALE,
                            right / COORDINATE_SCALE, bottom / COORDINATE_SCALE
                        )
                    }.takeUnless { it.isEmpty }
            }
        }
        return mergeMatchBounds(boxes)
    }
}

private fun mergeMatchBounds(boxes: List<RectF>): List<RectF> = buildList {
    boxes.forEach { box ->
        val line = lastOrNull()
        if (line != null && abs(line.centerY() - box.centerY()) <= maxOf(line.height(), box.height()) / 2f) {
            line.union(box)
        } else {
            add(box)
        }
    }
}

private fun findPdfTextMatches(text: String, query: String): List<IntRange> {
    if (query.isBlank()) return emptyList()
    return buildList {
        var start = text.indexOf(query, ignoreCase = true)
        while (start >= 0) {
            add(start until start + query.length)
            start = text.indexOf(query, start + query.length, ignoreCase = true)
        }
    }
}

private const val COORDINATE_SCALE = 10000
