@file:Suppress("LongMethod", "SwallowedException")

package org.fossify.documents.activities

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.fossify.commons.activities.BaseComposeActivity
import org.fossify.commons.compose.extensions.enableEdgeToEdgeSimple
import org.fossify.commons.extensions.getFilenameFromUri
import org.fossify.commons.extensions.getMimeTypeFromUri
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.documents.R
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent.data
        if (uri == null) {
            finish()
            return
        }

        val kind = intent.getStringExtra(EXTRA_DOCUMENT_KIND)
            ?.let { runCatching { DocumentKind.valueOf(it) }.getOrNull() }
            ?: DocumentKind.fromName(getFilenameFromUri(uri), getMimeTypeFromUri(uri))

        if (!intent.getBooleanExtra(EXTRA_DOCUMENT_PREPARED, false)) {
            lifecycleScope.launch(Dispatchers.IO) {
                DocumentsRepository(this@TextDocumentActivity).rememberDocument(uri, intent.flags)
            }
        }

        enableEdgeToEdgeSimple()
        setContent {
            DocumentsAppThemeSurface {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                var showDiscardDialog by remember { mutableStateOf(false) }
                var textZoom by remember {
                    mutableFloatStateOf(config.editorTextZoom.coerceDocumentTextZoom())
                }

                LaunchedEffect(uri, kind) {
                    viewModel.load(uri, kind)
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
                    onBack = requestClose,
                    onTextChange = viewModel::onTextChange,
                    onSave = { viewModel.save() },
                    onOpenWith = { openWith(uri) },
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

    private fun openWith(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, getMimeTypeFromUri(uri).ifBlank { "text/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(
                Intent.createChooser(intent, getString(org.fossify.commons.R.string.open_with)),
            )
        } catch (error: ActivityNotFoundException) {
            toast(org.fossify.commons.R.string.no_app_found)
        } catch (error: SecurityException) {
            showErrorToast(error)
        } catch (error: IllegalArgumentException) {
            showErrorToast(error)
        }
    }

    companion object {
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
