package org.fossify.documents.viewmodels

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fossify.commons.extensions.getFilenameFromUri
import org.fossify.documents.R
import org.fossify.documents.data.DecodedTextDocument
import org.fossify.documents.data.DocumentsRepository
import org.fossify.documents.data.TextDocumentCodec
import org.fossify.documents.data.TextDocumentEncoding
import org.fossify.documents.models.DocumentKind
import java.io.IOException

@Suppress("TooManyFunctions")
class TextDocumentViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val resolver = application.contentResolver
    private val repository = DocumentsRepository(application)
    private val _uiState = MutableStateFlow(TextDocumentUiState())
    val uiState: StateFlow<TextDocumentUiState> = _uiState

    var editorState by mutableStateOf(TextFieldState())
        private set
    private var loadedUri: Uri? = null
    val documentUri: Uri? get() = loadedUri
    private var documentEncoding: TextDocumentEncoding? = null
    private var loadJob: Job? = null
    private var saveCopyPending = false

    private var originalText: String = ""
    private val app: Application get() = getApplication()

    fun load(
        uri: Uri,
        kind: DocumentKind,
        force: Boolean = false,
    ) {
        if (!force && loadedUri != null) {
            return
        }

        loadJob?.cancel()
        saveCopyPending = false
        loadedUri = uri
        documentEncoding = null
        _uiState.value = TextDocumentUiState(
            title = app.getFilenameFromUri(uri),
            kind = kind,
            isLoading = true,
            previewEnabled = kind == DocumentKind.MARKDOWN,
        )

        loadJob = viewModelScope.launch {
            try {
                val (decoded, writable) = withContext(Dispatchers.IO) {
                    val document = resolver.openInputStream(uri)?.use { input ->
                        TextDocumentCodec.decode(input.readBytes())
                    } ?: throw IOException()
                    document to repository.isDocumentWritable(uri)
                }
                showDocument(decoded, writable)
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

    private fun showDocument(decoded: DecodedTextDocument, writable: Boolean) {
        documentEncoding = decoded.encoding
        originalText = decoded.text
        editorState = TextFieldState(initialText = decoded.text, initialSelection = TextRange.Zero)
        _uiState.update {
            it.copy(
                text = decoded.text,
                isLoading = false,
                isLoaded = true,
                isReadOnly = decoded.encoding == null || !writable,
                canSaveCopy = decoded.encoding != null,
                readOnlyReason = app.getString(R.string.unsupported_text_encoding)
                    .takeIf { decoded.encoding == null },
                previewEnabled = it.previewEnabled && decoded.text.isNotEmpty(),
            )
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
        if (!_uiState.value.isReadOnly) {
            val uri = loadedUri ?: return
            viewModelScope.launch {
                saveTo(uri, onSaved = onSaved)
            }
        }
    }

    fun prepareSaveCopy(): Boolean {
        val state = _uiState.value
        if (!state.isLoaded || !state.canSaveCopy || state.isSaving) return false
        saveCopyPending = true
        return true
    }

    fun cancelSaveCopy() {
        saveCopyPending = false
    }

    fun saveCopy(uri: Uri, grantFlags: Int, onSaved: (() -> Unit)? = null) {
        if (!saveCopyPending) {
            _uiState.update { it.copy(error = app.getString(R.string.could_not_save_document)) }
            return
        }
        saveCopyPending = false
        viewModelScope.launch {
            saveTo(uri, grantFlags, onSaved)
        }
    }

    private suspend fun saveTo(uri: Uri, copyGrantFlags: Int? = null, onSaved: (() -> Unit)? = null) {
        val encoding = documentEncoding ?: return
        val currentState = _uiState.value
        if (currentState.isSaving || !currentState.isLoaded) {
            return
        }

        val text = currentState.text
        _uiState.update { it.copy(isSaving = true, error = null) }

        try {
            val (title, writable) = writeDocument(uri, text, encoding, copyGrantFlags)
            loadedUri = uri
            originalText = text
            _uiState.update {
                val hasNewerChanges = it.text != text
                it.copy(
                    title = title,
                    isSaving = false,
                    isDirty = hasNewerChanges,
                    isReadOnly = !writable,
                    readOnlyReason = null,
                    previewEnabled = !(copyGrantFlags != null && currentState.isReadOnly) && it.previewEnabled,
                )
            }
            if (_uiState.value.text == text) {
                onSaved?.invoke()
            }
        } catch (error: IOException) {
            showSaveError(error)
        } catch (error: SecurityException) {
            showSaveError(error)
        } catch (error: IllegalStateException) {
            showSaveError(error)
        }
    }

    private suspend fun writeDocument(
        uri: Uri,
        text: String,
        encoding: TextDocumentEncoding,
        copyGrantFlags: Int?,
    ): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        resolver.openOutputStream(uri, "wt").use { output ->
            output?.write(TextDocumentCodec.encode(text, encoding))
                ?: throw IOException()
        }
        if (copyGrantFlags != null) {
            repository.rememberDocument(uri, copyGrantFlags)
        } else {
            repository.refreshDocumentMetadata(uri)
        }
        app.getFilenameFromUri(uri) to repository.isDocumentWritable(uri)
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
    val canSaveCopy: Boolean = false,
    val readOnlyReason: String? = null,
    val isDirty: Boolean = false,
    val previewEnabled: Boolean = false,
    val error: String? = null,
) {
    val isMarkdown: Boolean get() = kind == DocumentKind.MARKDOWN
}
