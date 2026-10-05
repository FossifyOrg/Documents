package org.fossify.documents.ui.screens

import android.view.View
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue

internal class WebDocumentSearchState(
    initialQuery: String = "",
    initialMatchIndex: Int = 0,
) {
    var query by mutableStateOf(initialQuery)
        private set
    var activeMatchIndex by mutableIntStateOf(initialMatchIndex)
        private set
    var matchCount by mutableIntStateOf(0)
        private set

    val currentMatchNumber: Int
        get() = if (matchCount > 0) activeMatchIndex + 1 else 0

    private var webView: WebView? = null
    private var pageReady = false
    private var restoredMatchIndex: Int? = initialMatchIndex
    private var needsMatchReveal = false
    private val layoutListener =
        View.OnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                val view = webView ?: return@OnLayoutChangeListener
                view.postVisualStateCallback(0, object : WebView.VisualStateCallback() {
                    override fun onComplete(requestId: Long) {
                        if (webView === view) {
                            needsMatchReveal = query.isNotEmpty()
                            revealCurrentMatch()
                        }
                    }
                })
            }
        }

    fun attach(view: WebView) {
        webView = view
        view.setFindListener(::onFindResult)
        view.addOnLayoutChangeListener(layoutListener)
    }

    fun detach() {
        webView?.setFindListener(null)
        webView?.removeOnLayoutChangeListener(layoutListener)
        webView = null
        pageReady = false
        needsMatchReveal = false
    }

    fun pageStarted() {
        pageReady = false
        matchCount = 0
        restoredMatchIndex = activeMatchIndex
    }

    fun pageFinished() {
        if (!pageReady) {
            pageReady = true
            findMatches()
        }
    }

    fun updateQuery(value: String) {
        if (query == value) return
        query = value
        activeMatchIndex = 0
        matchCount = 0
        restoredMatchIndex = 0
        needsMatchReveal = false
        findMatches()
    }

    fun moveMatch(forward: Boolean) {
        if (matchCount > 0) {
            webView?.findNext(forward)
        }
    }

    private fun findMatches() {
        val view = webView ?: return
        if (!pageReady) return
        view.findAllAsync(query)
        if (query.isEmpty()) {
            view.clearMatches()
        }
    }

    private fun onFindResult(ordinal: Int, count: Int, doneCounting: Boolean) {
        if (!pageReady || query.isEmpty() || !doneCounting) return
        val target = restoredMatchIndex?.takeIf { count > 0 }?.coerceIn(0, count - 1)
        if (target != null && target != ordinal) {
            val forwardDistance = (target - ordinal).mod(count)
            val backwardDistance = (ordinal - target).mod(count)
            webView?.findNext(forwardDistance <= backwardDistance)
            return
        }
        restoredMatchIndex = null
        activeMatchIndex = if (count > 0) ordinal else 0
        matchCount = count
        revealCurrentMatch()
    }

    private fun revealCurrentMatch() {
        val view = webView ?: return
        if (!needsMatchReveal || matchCount == 0 || restoredMatchIndex != null) return
        needsMatchReveal = false
        restoredMatchIndex = activeMatchIndex
        view.findNext(false)
    }

    companion object {
        val saver = listSaver<WebDocumentSearchState, Any>(
            save = { listOf(it.query, it.activeMatchIndex) },
            restore = { WebDocumentSearchState(it[0] as String, it[1] as Int) },
        )
    }
}
