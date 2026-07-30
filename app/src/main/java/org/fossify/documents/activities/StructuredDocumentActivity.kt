package org.fossify.documents.activities

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
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
import org.fossify.documents.data.DocumentsRepository
import org.fossify.documents.models.DocumentKind
import org.fossify.documents.ui.screens.StructuredDocumentScreen
import org.fossify.documents.ui.theme.DocumentsAppThemeSurface
import org.fossify.documents.viewmodels.StructuredDocumentViewModel

class StructuredDocumentActivity : BaseComposeActivity() {
    private val viewModel by lazy {
        ViewModelProvider(this)[StructuredDocumentViewModel::class.java]
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
        if (kind !in supportedKinds) {
            finish()
            return
        }

        if (!intent.getBooleanExtra(EXTRA_DOCUMENT_PREPARED, false)) {
            lifecycleScope.launch(Dispatchers.IO) {
                DocumentsRepository(this@StructuredDocumentActivity).rememberDocument(uri, intent.flags)
            }
        }

        enableEdgeToEdgeSimple()
        setContent {
            DocumentsAppThemeSurface {
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                LaunchedEffect(uri, kind) {
                    viewModel.load(uri, kind)
                }
                val editDocumentLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
                ) {
                    viewModel.load(uri, kind, force = true)
                }
                StructuredDocumentScreen(
                    uiState = uiState,
                    onBack = ::finish,
                    onEdit = if (uiState.canEdit) {
                        {
                            editDocumentLauncher.launch(
                                TextDocumentActivity.newIntent(this, uri, DocumentKind.CSV)
                            )
                        }
                    } else {
                        null
                    },
                    onOpenWith = { openWith(uri) },
                    onOpenLink = ::openLink,
                )
            }
        }
    }

    private fun openWith(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, getMimeTypeFromUri(uri).ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        launchExternalIntent(
            Intent.createChooser(intent, getString(org.fossify.commons.R.string.open_with)),
        )
    }

    private fun openLink(uri: Uri) {
        if (uri.scheme !in externalLinkSchemes) {
            return
        }
        launchExternalIntent(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun launchExternalIntent(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
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
        private val supportedKinds = setOf(DocumentKind.DOCX, DocumentKind.CSV, DocumentKind.HTML)
        private val externalLinkSchemes = setOf("http", "https", "mailto", "tel")

        fun newIntent(
            context: Context,
            uri: Uri,
            kind: DocumentKind,
            prepared: Boolean = true,
        ): Intent {
            return Intent(context, StructuredDocumentActivity::class.java).apply {
                data = uri
                putExtra(EXTRA_DOCUMENT_KIND, kind.name)
                putExtra(EXTRA_DOCUMENT_PREPARED, prepared)
            }
        }
    }
}
