package org.fossify.documents.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.fossify.commons.extensions.getFilenameFromUri
import org.fossify.documents.R
import org.fossify.documents.data.DocumentTooLargeException
import org.fossify.documents.data.DocumentsRepository
import org.fossify.documents.data.StructuredDocumentContent
import org.fossify.documents.data.StructuredDocumentLoader
import org.fossify.documents.models.DocumentKind
import java.io.IOException

internal class StructuredDocumentViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val loader = StructuredDocumentLoader(application.contentResolver)
    private val repository = DocumentsRepository(application)
    private val _uiState = MutableStateFlow(StructuredDocumentUiState())
    val uiState: StateFlow<StructuredDocumentUiState> = _uiState

    private var loadedUri: Uri? = null
    private val app: Application get() = getApplication()

    @Suppress("TooGenericExceptionCaught")
    fun load(uri: Uri, kind: DocumentKind, force: Boolean = false) {
        if (!force && uri == loadedUri) {
            return
        }

        loadedUri = uri
        val title = app.getFilenameFromUri(uri)
        _uiState.value = StructuredDocumentUiState(
            title = title,
            isLoading = true,
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = loader.load(uri, kind, title)
                _uiState.update {
                    it.copy(
                        content = result.content,
                        canEdit = kind == DocumentKind.CSV &&
                                result.canEditText &&
                                repository.isDocumentWritable(uri),
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (_: OutOfMemoryError) {
                showOpenError(app.getString(R.string.document_too_large_to_view))
            } catch (_: DocumentTooLargeException) {
                showOpenError(app.getString(R.string.document_too_large_to_view))
            } catch (_: IOException) {
                showOpenError()
            } catch (_: SecurityException) {
                showOpenError(app.getString(R.string.document_no_longer_available))
            } catch (_: RuntimeException) {
                showOpenError()
            }
        }
    }

    private fun showOpenError(message: String = app.getString(R.string.could_not_open_document)) {
        _uiState.update {
            it.copy(
                isLoading = false,
                error = message,
            )
        }
    }
}

internal data class StructuredDocumentUiState(
    val title: String = "",
    val content: StructuredDocumentContent? = null,
    val canEdit: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)
