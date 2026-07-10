@file:Suppress("SwallowedException")

package org.fossify.documents.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
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

class PDFViewerActivity : BaseComposeActivity() {
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

        enableEdgeToEdgeSimple()
        setContent {
            DocumentsAppThemeSurface {
                PdfDocumentScreen(
                    uri = uri,
                    title = title,
                    startPage = startPage,
                    onBack = ::finish,
                    onPageChange = { page, _ ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            repository.updateLastPage(uri, page)
                        }
                    },
                    onLoad = {
                        lifecycleScope.launch(Dispatchers.IO) {
                            repository.rememberDocument(uri, intent.flags)
                        }
                    },
                    onPrint = { printPdf(uri, title) },
                    onOpenWith = { openWith(uri) },
                    onFullscreenChange = ::setFullscreen,
                )
            }
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
        } catch (error: ActivityNotFoundException) {
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
