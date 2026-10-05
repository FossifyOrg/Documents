package org.fossify.documents.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.DocumentsContract
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.fossify.commons.activities.BaseComposeActivity
import org.fossify.commons.compose.extensions.enableEdgeToEdgeSimple
import org.fossify.commons.extensions.getFilenameFromUri
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.documents.R
import org.fossify.documents.data.DocumentsRepository
import org.fossify.documents.extensions.config
import org.fossify.documents.helpers.PdfDocumentAdapter
import org.fossify.documents.ui.screens.PdfDocumentScreen
import org.fossify.documents.ui.theme.DocumentsAppThemeSurface
import org.fossify.documents.viewmodels.PdfDocumentViewModel

class PDFViewerActivity : BaseComposeActivity() {
    private val preferences by lazy { config }
    private val viewModel by lazy { ViewModelProvider(this)[PdfDocumentViewModel::class.java] }
    private val createCopy = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val destination = result.data?.data
        val source = intent.data
        if (result.resultCode == RESULT_OK && destination != null && source != null) {
            viewModel.saveCopy(source, destination, result.data?.flags ?: 0)
        }
    }
    private val pageUpdates = Channel<Int>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent.data
        if (uri == null) {
            finish()
            return
        }

        val repository = DocumentsRepository(this)
        val existingDocument = repository.getDocument(uri)
        val startPage = if (config.rememberPdfPage) existingDocument?.lastPage ?: 0 else 0
        val title = existingDocument?.name?.ifBlank { null }
            ?: getFilenameFromUri(uri).ifBlank { getString(R.string.document) }

        lifecycleScope.launch(Dispatchers.IO) {
            for (page in pageUpdates) {
                repository.updateLastPage(uri, page)
            }
        }

        lifecycleScope.launch {
            viewModel.copyMessages.collect { toast(it) }
        }

        enableEdgeToEdgeSimple()
        setContent {
            DocumentsAppThemeSurface {
                val nightMode by preferences.pdfDarkPagesFlow
                    .collectAsStateWithLifecycle(preferences.pdfDarkPages)
                val showPageIndicator by preferences.pdfPageIndicatorFlow
                    .collectAsStateWithLifecycle(preferences.pdfPageIndicator)
                val horizontalPaging by preferences.pdfHorizontalPagingFlow
                    .collectAsStateWithLifecycle(preferences.pdfHorizontalPaging)
                PdfDocumentScreen(
                    uri = uri,
                    title = title,
                    startPage = startPage,
                    onBack = ::finish,
                    onPageChange = { page, _ ->
                        pageUpdates.trySend(page)
                    },
                    onLoad = {
                        lifecycleScope.launch(Dispatchers.IO) {
                            repository.rememberDocument(uri, intent.flags)
                        }
                    },
                    onSaveCopy = { requestSaveCopy(uri, title) },
                    isCopying = viewModel.isCopying,
                    onPrint = { printPdf(uri, title) },
                    onOpenWith = { openWith(uri) },
                    onFullscreenChange = ::setFullscreen,
                    nightMode = nightMode,
                    showPageIndicator = showPageIndicator,
                    horizontalPaging = horizontalPaging,
                    onNightModeChange = { preferences.pdfDarkPages = it },
                    onSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                )
            }
        }
    }

    private fun requestSaveCopy(uri: Uri, title: String) {
        val request = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
        try {
            createCopy.launch(request)
        } catch (_: ActivityNotFoundException) {
            toast(org.fossify.commons.R.string.no_app_found)
        }
    }

    private fun setFullscreen(fullscreen: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (fullscreen) {
                hide(WindowInsetsCompat.Type.systemBars())
            } else {
                show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    private fun openWith(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
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

    private fun printPdf(uri: Uri, title: String) {
        val adapter = PdfDocumentAdapter(
            context = this,
            uri = uri,
            fileName = title,
        )

        try {
            (getSystemService(PRINT_SERVICE) as? PrintManager)
                ?.print(title, adapter, PrintAttributes.Builder().build())
        } catch (error: SecurityException) {
            showErrorToast(error)
        } catch (error: IllegalStateException) {
            showErrorToast(error)
        }
    }
}
