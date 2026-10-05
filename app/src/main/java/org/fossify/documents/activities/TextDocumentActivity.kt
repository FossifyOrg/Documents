@file:Suppress("LongMethod")

package org.fossify.documents.activities

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fossify.commons.activities.BaseComposeActivity
import org.fossify.commons.compose.extensions.enableEdgeToEdgeSimple
import org.fossify.commons.extensions.getFilenameFromUri
import org.fossify.commons.extensions.getMimeTypeFromUri
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.documents.R
import org.fossify.documents.data.DocumentLocationResolver
import org.fossify.documents.data.DocumentsRepository
import org.fossify.documents.extensions.config
import org.fossify.documents.models.DocumentKind
import org.fossify.documents.ui.screens.DEFAULT_DOCUMENT_TEXT_ZOOM
import org.fossify.documents.ui.screens.TextDocumentScreen
import org.fossify.documents.ui.screens.coerceDocumentTextZoom
import org.fossify.documents.ui.theme.DocumentsAppThemeSurface
import org.fossify.documents.viewmodels.TextDocumentViewModel

class TextDocumentActivity : BaseComposeActivity() {
    private val viewModel by lazy {
        ViewModelProvider(this)[TextDocumentViewModel::class.java]
    }

    private val createCopy = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        val uri = data?.data
        if (result.resultCode == RESULT_OK && uri != null) {
            viewModel.saveCopy(uri, data.flags) { toast(R.string.copy_opened) }
        } else {
            viewModel.cancelSaveCopy()
        }
    }

    private val openForEditing = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        val uri = data?.data
        if (result.resultCode == RESULT_OK && uri != null) {
            val kind = DocumentKind.fromName(getFilenameFromUri(uri), getMimeTypeFromUri(uri))
            if (kind in setOf(DocumentKind.TEXT, DocumentKind.MARKDOWN, DocumentKind.CSV)) {
                intent.data = uri
                lifecycleScope.launch(Dispatchers.IO) {
                    DocumentsRepository(this@TextDocumentActivity).rememberDocument(uri, data.flags)
                }
                intent.putExtra(EXTRA_DOCUMENT_KIND, kind.name)
                viewModel.load(uri, kind, force = true)
            } else {
                toast(R.string.unsupported_document_type)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = savedInstanceState?.getString(STATE_DOCUMENT_URI)?.let(Uri::parse) ?: intent.data
        if (uri == null) {
            finish()
            return
        }

        val kind = (savedInstanceState?.getString(EXTRA_DOCUMENT_KIND) ?: intent.getStringExtra(EXTRA_DOCUMENT_KIND))
            ?.let { runCatching { DocumentKind.valueOf(it) }.getOrNull() }
            ?: DocumentKind.fromName(getFilenameFromUri(uri), getMimeTypeFromUri(uri))

        if (!intent.getBooleanExtra(EXTRA_DOCUMENT_PREPARED, false)) {
            lifecycleScope.launch(Dispatchers.IO) {
                DocumentsRepository(this@TextDocumentActivity).rememberDocument(uri, intent.flags)
            }
        }

        viewModel.load(uri, kind)
        enableEdgeToEdgeSimple()
        setContent {
            DocumentsAppThemeSurface {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                var showDiscardDialog by remember { mutableStateOf(false) }
                var showReopenDialog by rememberSaveable { mutableStateOf(false) }
                var textZoom by remember {
                    mutableFloatStateOf(config.editorTextZoom.coerceDocumentTextZoom())
                }

                val requestClose = {
                    if (uiState.isDirty) {
                        showDiscardDialog = true
                    } else {
                        finish()
                    }
                }

                BackHandler(enabled = !showDiscardDialog, onBack = requestClose)

                TextDocumentScreen(
                    uiState = uiState,
                    editorState = viewModel.editorState,
                    onBack = requestClose,
                    onTextChange = viewModel::onTextChange,
                    onSave = { viewModel.save() },
                    onOpenWith = { viewModel.documentUri?.let(::openWith) },
                    onSaveCopy = { saveCopy() },
                    onOpenForEditing = { showReopenDialog = true },
                    onPreviewChange = viewModel::setPreviewEnabled,
                    textZoom = textZoom,
                    onTextZoomChange = { requestedZoom ->
                        val zoom = requestedZoom.coerceDocumentTextZoom()
                        textZoom = zoom
                        config.editorTextZoom = zoom
                    },
                    onResetTextZoom = {
                        textZoom = DEFAULT_DOCUMENT_TEXT_ZOOM
                        config.editorTextZoom = DEFAULT_DOCUMENT_TEXT_ZOOM
                    },
                )

                if (showReopenDialog) {
                    AlertDialog(
                        onDismissRequest = { showReopenDialog = false },
                        title = { Text(getString(R.string.reopen_to_edit)) },
                        text = { Text(getString(R.string.reopen_to_edit_hint)) },
                        confirmButton = {
                            TextButton(onClick = { showReopenDialog = false; requestWriteAccess() }) {
                                Text(getString(R.string.open_file))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showReopenDialog = false }) {
                                Text(getString(org.fossify.commons.R.string.cancel))
                            }
                        },
                    )
                }

                if (showDiscardDialog) {
                    AlertDialog(
                        onDismissRequest = { showDiscardDialog = false },
                        title = { Text(text = getString(R.string.unsaved_changes)) },
                        text = {
                            Text(text = getString(org.fossify.commons.R.string.save_before_closing))
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDiscardDialog = false
                                    viewModel.save { finish() }
                                }
                            ) {
                                Text(text = getString(org.fossify.commons.R.string.save))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showDiscardDialog = false
                                    finish()
                                }
                            ) {
                                Text(text = getString(org.fossify.commons.R.string.discard))
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_DOCUMENT_URI, viewModel.documentUri?.toString())
        outState.putString(EXTRA_DOCUMENT_KIND, viewModel.uiState.value.kind.name)
        super.onSaveInstanceState(outState)
    }

    private fun saveCopy() {
        val state = viewModel.uiState.value
        val uri = viewModel.documentUri ?: return
        val extension = state.title.substringAfterLast('.', "").lowercase()
        val mimeType = if (extension.isEmpty()) {
            when (state.kind) {
                DocumentKind.MARKDOWN -> "text/markdown"
                DocumentKind.CSV -> "text/csv"
                else -> "text/plain"
            }
        } else {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
        }
        val request = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, state.title)
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
        if (!viewModel.prepareSaveCopy()) return
        try {
            createCopy.launch(request)
        } catch (_: ActivityNotFoundException) {
            viewModel.cancelSaveCopy()
            toast(org.fossify.commons.R.string.no_app_found)
        }
    }

    private fun requestWriteAccess() {
        val uri = viewModel.documentUri ?: return
        lifecycleScope.launch {
            val location = withContext(Dispatchers.IO) {
                DocumentLocationResolver(this@TextDocumentActivity).resolvePickerLocation(uri)
            }
            val request = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                location?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) }
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            }
            try {
                openForEditing.launch(request)
            } catch (_: ActivityNotFoundException) {
                toast(org.fossify.commons.R.string.no_app_found)
            }
        }
    }

    private fun openWith(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, getMimeTypeFromUri(uri).ifBlank { "text/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(
                Intent.createChooser(intent, getString(org.fossify.commons.R.string.open_with)),
            )
        } catch (_: ActivityNotFoundException) {
            toast(org.fossify.commons.R.string.no_app_found)
        } catch (error: SecurityException) {
            showErrorToast(error)
        } catch (error: IllegalArgumentException) {
            showErrorToast(error)
        }
    }

    companion object {
        private const val STATE_DOCUMENT_URI = "state_document_uri"
        private const val EXTRA_DOCUMENT_KIND = "extra_document_kind"
        private const val EXTRA_DOCUMENT_PREPARED = "extra_document_prepared"

        fun newIntent(
            context: Context,
            uri: Uri,
            kind: DocumentKind,
            prepared: Boolean = true,
        ): Intent {
            return Intent(context, TextDocumentActivity::class.java).apply {
                data = uri
                putExtra(EXTRA_DOCUMENT_KIND, kind.name)
                putExtra(EXTRA_DOCUMENT_PREPARED, prepared)
            }
        }
    }
}
