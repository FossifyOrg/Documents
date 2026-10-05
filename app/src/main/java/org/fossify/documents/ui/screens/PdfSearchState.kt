package org.fossify.documents.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.fossify.documents.R
import org.fossify.documents.data.PdfSearchResult
import org.fossify.documents.data.PdfTextSearch
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

@Stable
internal class PdfSearchState(active: Boolean = false, query: String = "", index: Int = 0) {
    var active by mutableStateOf(active)
    var query by mutableStateOf(query)
        private set
    var result by mutableStateOf(PdfSearchResult())
        private set
    var index by mutableIntStateOf(index)
        private set
    var navigationRequest by mutableIntStateOf(0)
        private set
    var inProgress by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set
    var searchAttempt by mutableIntStateOf(0)
        private set
    val selectedMatch get() = result.matches.getOrNull(index)

    fun updateQuery(value: String) {
        query = value
        index = 0
    }

    fun retry() {
        searchAttempt++
    }

    fun close() {
        active = false
        updateQuery("")
    }

    fun back(onClose: () -> Unit) {
        if (active) close() else onClose()
    }

    fun changeMatch(direction: Int) {
        if (index in result.matches.indices) {
            index = (index + direction).mod(result.matches.size)
            navigationRequest++
        }
    }

    suspend fun search(context: Context, uri: Uri, password: String?) {
        result = PdfSearchResult()
        failed = false
        inProgress = active && query.isNotBlank()
        if (!inProgress) return
        delay(SEARCH_DELAY_MS.milliseconds)
        try {
            PdfTextSearch(context).search(uri, password, query).collect { result = it }
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            failed = true
        } catch (_: SecurityException) {
            failed = true
        } catch (_: IllegalStateException) {
            failed = true
        }
        inProgress = false
    }

    companion object {
        val Saver = listSaver<PdfSearchState, Any>(
            save = { listOf(it.active, it.query, it.index) },
            restore = { PdfSearchState(it[0] as Boolean, it[1] as String, it[2] as Int) },
        )
    }
}

@Composable
internal fun rememberPdfSearchState(uri: Uri, password: String?): PdfSearchState {
    val context = LocalContext.current
    val state = rememberSaveable(uri, saver = PdfSearchState.Saver) { PdfSearchState() }
    LaunchedEffect(uri, password, state.query, state.active, state.searchAttempt) {
        state.search(context, uri, password)
    }
    return state
}

@Composable
internal fun PdfSearchStatus(search: PdfSearchState) {
    if (search.inProgress) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    } else if (search.active && search.query.isNotBlank()) {
        val message = when {
            search.failed -> R.string.pdf_search_failed
            !search.result.hasText -> R.string.pdf_no_text
            search.result.matches.isEmpty() -> R.string.no_results_found
            else -> null
        }
        message?.let {
            StatusStrip(
                text = stringResource(it),
                isError = search.failed,
                action = if (search.failed) {
                    { TextButton(onClick = search::retry) { Text(stringResource(R.string.retry)) } }
                } else {
                    null
                },
            )
        }
    }
}

private const val SEARCH_DELAY_MS = 250L
