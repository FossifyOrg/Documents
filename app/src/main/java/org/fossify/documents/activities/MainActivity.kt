@file:Suppress("SwallowedException")

package org.fossify.documents.activities

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.fossify.commons.activities.BaseComposeActivity
import org.fossify.commons.compose.extensions.enableEdgeToEdgeSimple
import org.fossify.commons.extensions.getFilenameFromUri
import org.fossify.commons.extensions.getMimeTypeFromUri
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.models.FAQItem
import org.fossify.documents.BuildConfig
import org.fossify.documents.R
import org.fossify.documents.extensions.startAboutActivity
import org.fossify.documents.models.DocumentEntry
import org.fossify.documents.models.DocumentKind
import org.fossify.documents.ui.screens.DocumentsMainActions
import org.fossify.documents.ui.screens.MainScreen
import org.fossify.documents.ui.theme.DocumentsAppThemeSurface
import org.fossify.documents.viewmodels.DocumentsHomeSection
import org.fossify.documents.viewmodels.DocumentsViewModel

class MainActivity : BaseComposeActivity() {
    private var hasResumed = false

    private val viewModel by lazy {
        ViewModelProvider(
            this,
            DocumentsViewModel.factory(application)
        )[DocumentsViewModel::class.java]
    }

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                return@registerForActivityResult
            }

            val data = result.data ?: return@registerForActivityResult
            val uri = data.data ?: return@registerForActivityResult
            viewModel.prepareDocument(uri, data.flags) { canRead ->
                if (canRead) {
                    launchDocument(uri)
                } else {
                    toast(R.string.document_no_longer_available)
                }
            }
        }

    private val openFolderLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                return@registerForActivityResult
            }

            val data = result.data ?: return@registerForActivityResult
            val uri = data.data ?: return@registerForActivityResult
            viewModel.rememberFolder(uri, data.flags)
        }

    private val createTextDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleCreatedDocument(result.resultCode, result.data, DocumentKind.TEXT)
        }

    private val createMarkdownDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleCreatedDocument(result.resultCode, result.data, DocumentKind.MARKDOWN)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeSimple()
        setContent {
            DocumentsAppThemeSurface {
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                MainScreen(
                    uiState = uiState,
                    actions = documentsMainActions(),
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasResumed) {
            viewModel.refreshVisibleFolder()
        } else {
            hasResumed = true
        }
    }

    private fun documentsMainActions() = DocumentsMainActions(
        openDocument = { openDocumentLauncher.launch(DocumentsViewModel.openDocumentIntent) },
        openFolder = { openFolderLauncher.launch(DocumentsViewModel.openFolderIntent) },
        newTextFile = {
            createTextDocumentLauncher.launch(
                DocumentsViewModel.createDocumentIntent(
                    mimeType = "text/plain",
                    title = getString(R.string.untitled_text_file),
                )
            )
        },
        newMarkdownFile = {
            createMarkdownDocumentLauncher.launch(
                DocumentsViewModel.createDocumentIntent(
                    mimeType = "text/markdown",
                    title = getString(R.string.untitled_markdown_file),
                )
            )
        },
        openSettings = ::launchSettings,
        openAbout = ::launchAbout,
        clearRecentDocuments = viewModel::clearRecentDocuments,
        onQueryChange = viewModel.onQueryChange,
        onFilterSelected = viewModel::onFilterSelected,
        onSortSelected = { viewModel.updateDisplayOptions(sort = it) },
        onViewModeSelected = { viewModel.updateDisplayOptions(viewMode = it) },
        onBack = viewModel::navigateBack,
        onShowHome = { viewModel.showSection(DocumentsHomeSection.HOME) },
        onShowRecent = { viewModel.showSection(DocumentsHomeSection.RECENT) },
        onShowFolders = { viewModel.showSection(DocumentsHomeSection.FOLDERS) },
        onShowFavorites = { viewModel.showSection(DocumentsHomeSection.FAVORITES) },
        onBreadcrumbClick = viewModel::navigateToBreadcrumb,
        onFolderClick = { viewModel.showSection(DocumentsHomeSection.FOLDER, it) },
        onDocumentClick = { document ->
            viewModel.prepareDocument(document.uri.toUri()) { canRead ->
                if (canRead) {
                    launchDocument(document.uri.toUri(), document.kind)
                } else {
                    toast(R.string.document_no_longer_available)
                }
            }
        },
        onToggleDocumentSelection = viewModel::toggleDocumentSelection,
        onToggleFolderSelection = viewModel::toggleFolderSelection,
        onSelectAll = viewModel::selectAllVisibleItems,
        clearSelection = viewModel::clearSelection,
        onOpenWith = ::openWith,
        onShareDocuments = ::shareDocuments,
        onRemoveSelection = viewModel::removeSelectedItems,
        onToggleSelectedFavorites = viewModel::toggleSelectedFavorites,
    )

    private fun handleCreatedDocument(resultCode: Int, data: Intent?, kind: DocumentKind) {
        val uri = data?.data
        if (resultCode == RESULT_OK && uri != null) {
            viewModel.prepareDocument(uri, data.flags) { canRead ->
                if (canRead) {
                    launchDocument(uri, kind)
                } else {
                    toast(R.string.document_no_longer_available)
                }
            }
        }
    }

    private fun launchDocument(uri: Uri) {
        val kind = DocumentKind.fromName(getFilenameFromUri(uri), getMimeTypeFromUri(uri))
        launchDocument(uri, kind)
    }

    private fun launchDocument(uri: Uri, kind: DocumentKind) {
        hideKeyboard()
        try {
            when (kind) {
                DocumentKind.PDF -> {
                    startActivity(
                        Intent(this, PDFViewerActivity::class.java).apply {
                            data = uri
                        }
                    )
                }

                DocumentKind.TEXT,
                DocumentKind.MARKDOWN -> {
                    startActivity(TextDocumentActivity.newIntent(this, uri, kind))
                }

                DocumentKind.DOCX,
                DocumentKind.CSV,
                DocumentKind.HTML -> {
                    startActivity(StructuredDocumentActivity.newIntent(this, uri, kind))
                }

                DocumentKind.OTHER -> toast(R.string.unsupported_document_type)
            }
        } catch (error: SecurityException) {
            toast(R.string.document_no_longer_available)
        }
    }

    private fun openWith(document: DocumentEntry) {
        hideKeyboard()
        viewModel.validateDocuments(listOf(document)) { readable, unavailableCount ->
            if (unavailableCount > 0 || readable.isEmpty()) {
                toast(R.string.document_no_longer_available)
                return@validateDocuments
            }

            val uri = document.uri.toUri()
            val mimeType = document.mimeType.ifBlank { "*/*" }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            launchExternalIntent(
                Intent.createChooser(intent, getString(org.fossify.commons.R.string.open_with)),
            )
        }
    }

    private fun shareDocuments(documents: List<DocumentEntry>) {
        hideKeyboard()
        viewModel.validateDocuments(documents) { readable, unavailableCount ->
            if (unavailableCount > 0) {
                toast(R.string.some_documents_no_longer_available)
            }
            if (readable.isEmpty()) {
                return@validateDocuments
            }

            val uris = ArrayList(readable.map { it.uri.toUri() })
            val mimeType = readable.map { it.mimeType }.filter { it.isNotBlank() }.distinct()
                .singleOrNull() ?: "*/*"
            val intent = Intent(
                if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE
            ).apply {
                type = mimeType
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newUri(contentResolver, readable.first().name, uris.first()).apply {
                    uris.drop(1).forEach { addItem(ClipData.Item(it)) }
                }
                if (uris.size == 1) {
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                } else {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                }
            }

            launchExternalIntent(Intent.createChooser(intent, getString(R.string.share_documents)))
        }
    }

    private fun launchSettings() {
        hideKeyboard()
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        val faqItems = ArrayList<FAQItem>()
        if (!resources.getBoolean(org.fossify.commons.R.bool.hide_google_relations)) {
            faqItems.add(
                FAQItem(
                    title = org.fossify.commons.R.string.faq_2_title_commons,
                    text = org.fossify.commons.R.string.faq_2_text_commons
                )
            )
            faqItems.add(
                FAQItem(
                    title = org.fossify.commons.R.string.faq_6_title_commons,
                    text = org.fossify.commons.R.string.faq_6_text_commons
                )
            )
        }

        startAboutActivity(
            appNameId = R.string.app_name,
            licenseMask = 0,
            versionName = BuildConfig.VERSION_NAME,
            packageName = packageName,
            repositoryName = "Documents",
            faqItems = faqItems,
            showFAQBeforeMail = false
        )
    }
}

private fun MainActivity.launchExternalIntent(intent: Intent) {
    try {
        startActivity(intent)
    } catch (error: ActivityNotFoundException) {
        toast(org.fossify.commons.R.string.no_app_found)
    } catch (error: SecurityException) {
        showErrorToast(error)
    } catch (error: IllegalArgumentException) {
        showErrorToast(error)
    }
}
