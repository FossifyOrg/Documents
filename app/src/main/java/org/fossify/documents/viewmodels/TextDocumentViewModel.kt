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
import kotlinx.coroutines.withContext
import org.fossify.commons.extensions.getFilenameFromUri
import org.fossify.documents.R
import org.fossify.documents.data.DocumentsRepository
import org.fossify.documents.data.TextDocumentCodec
import org.fossify.documents.data.TextDocumentEncoding
import org.fossify.documents.models.DocumentKind
import java.io.IOException

class TextDocumentViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val resolver = application.contentResolver
    private val repository = DocumentsRepository(application)
    private val _uiState = MutableStateFlow(TextDocumentUiState())
    val uiState: StateFlow<TextDocumentUiState> = _uiState

    private var loadedUri: Uri? = null
    private var documentEncoding: TextDocumentEncoding? = null

    @Volatile
    private var originalText: String = ""
    private val app: Application get() = getApplication()

    fun load(uri: Uri, kind: DocumentKind) {
        if (loadedUri == uri) {
            return
        }

        loadedUri = uri
        _uiState.value = TextDocumentUiState(
            title = app.getFilenameFromUri(uri),
            kind = kind,
            isLoading = true,
            previewEnabled = kind == DocumentKind.MARKDOWN,
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val decoded = resolver.openInputStream(uri)?.use { input ->
                    TextDocumentCodec.decode(input.readBytes())
                } ?: throw IOException("Could not open this document.")
                val unsupportedEncoding = decoded.encoding == null
                val isReadOnly = unsupportedEncoding || !repository.isDocumentWritable(uri)
                documentEncoding = decoded.encoding
                originalText = decoded.text
                _uiState.update {
                    it.copy(
                        text = decoded.text,
                        isLoading = false,
                        isLoaded = true,
                        isReadOnly = isReadOnly,
                        readOnlyReason = app.getString(R.string.unsupported_text_encoding)
                            .takeIf { unsupportedEncoding },
                        isDirty = false,
                        error = null,
                    )
                }
            } catch (error: OutOfMemoryError) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.localizedMessage ?: app.getString(R.string.document_too_large),
                    )
                }
            } catch (error: IOException) {
                showOpenError(error)
            } catch (error: SecurityException) {
                showOpenError(error)
            }
        }
    }

    fun onTextChange(value: String) {
        _uiState.update {
            it.copy(
                text = value,
                isDirty = value != originalText,
            )
        }
    }

    fun setPreviewEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(previewEnabled = enabled)
        }
    }

    fun save(onSaved: (() -> Unit)? = null) {
        val uri = loadedUri ?: return
        val encoding = documentEncoding ?: return
        val currentState = _uiState.value
        if (currentState.isReadOnly || currentState.isSaving || !currentState.isLoaded) {
            return
        }

        val text = currentState.text
        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                resolver.openOutputStream(uri, "wt").use { output ->
                    output?.write(TextDocumentCodec.encode(text, encoding))
                        ?: error("Could not open this document for writing.")
                }
                repository.refreshDocumentMetadata(uri)
                originalText = text
                _uiState.update {
                    val hasNewerChanges = it.text != text
                    it.copy(
                        isSaving = false,
                        isDirty = hasNewerChanges,
                    )
                }
                withContext(Dispatchers.Main) {
                    if (_uiState.value.text == text) {
                        onSaved?.invoke()
                    }
                }
            } catch (error: IOException) {
                showSaveError(error)
            } catch (error: SecurityException) {
                showSaveError(error)
            } catch (error: IllegalStateException) {
                showSaveError(error)
            }
        }
    }

    private fun showOpenError(error: Throwable) {
        _uiState.update {
            it.copy(
                isLoading = false,
                error = error.localizedMessage ?: app.getString(R.string.could_not_open_document),
            )
        }
    }

    private fun showSaveError(error: Throwable) {
        _uiState.update {
            it.copy(
                isSaving = false,
                error = error.localizedMessage ?: app.getString(R.string.could_not_save_document),
            )
        }
    }
}

data class TextDocumentUiState(
    val title: String = "",
    val kind: DocumentKind = DocumentKind.TEXT,
    val text: String = "",
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val isReadOnly: Boolean = false,
    val readOnlyReason: String? = null,
    val isDirty: Boolean = false,
    val previewEnabled: Boolean = false,
    val error: String? = null,
) {
    val isMarkdown: Boolean get() = kind == DocumentKind.MARKDOWN
}
