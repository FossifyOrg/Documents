package org.fossify.documents.helpers

import android.content.Context
import androidx.core.content.edit
import org.fossify.commons.helpers.BaseConfig

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)

        private const val DOCUMENTS = "documents"
        private const val DOCUMENT_FOLDERS = "document_folders"
        private const val EDITOR_TEXT_ZOOM = "editor_text_zoom"
        private const val PDF_DARK_PAGES = "pdf_dark_pages"
        private const val PDF_HORIZONTAL_PAGING = "pdf_horizontal_paging"
        private const val PDF_PAGE_INDICATOR = "pdf_page_indicator"
        private const val REMEMBER_PDF_PAGE = "remember_pdf_page"
        private const val SHOW_FILE_LOCATIONS = "show_file_locations"
    }

    var documentsJson: String
        get() = prefs.getString(DOCUMENTS, "[]")!!
        set(value) = prefs.edit {
            putString(DOCUMENTS, value)
        }

    val documentsJsonFlow = ::documentsJson.asFlowNonNull()

    var documentFoldersJson: String
        get() = prefs.getString(DOCUMENT_FOLDERS, "[]")!!
        set(value) = prefs.edit {
            putString(DOCUMENT_FOLDERS, value)
        }

    val documentFoldersJsonFlow = ::documentFoldersJson.asFlowNonNull()

    var editorTextZoom: Float
        get() = prefs.getFloat(EDITOR_TEXT_ZOOM, 1f)
        set(value) = prefs.edit {
            putFloat(EDITOR_TEXT_ZOOM, value)
        }

    var pdfDarkPages: Boolean
        get() = prefs.getBoolean(PDF_DARK_PAGES, false)
        set(value) = prefs.edit { putBoolean(PDF_DARK_PAGES, value) }

    val pdfDarkPagesFlow = ::pdfDarkPages.asFlowNonNull(emitOnCollect = true)

    var pdfHorizontalPaging: Boolean
        get() = prefs.getBoolean(PDF_HORIZONTAL_PAGING, false)
        set(value) = prefs.edit { putBoolean(PDF_HORIZONTAL_PAGING, value) }

    val pdfHorizontalPagingFlow = ::pdfHorizontalPaging.asFlowNonNull(emitOnCollect = true)

    var pdfPageIndicator: Boolean
        get() = prefs.getBoolean(PDF_PAGE_INDICATOR, true)
        set(value) = prefs.edit { putBoolean(PDF_PAGE_INDICATOR, value) }

    val pdfPageIndicatorFlow = ::pdfPageIndicator.asFlowNonNull(emitOnCollect = true)

    var rememberPdfPage: Boolean
        get() = prefs.getBoolean(REMEMBER_PDF_PAGE, true)
        set(value) = prefs.edit {
            putBoolean(REMEMBER_PDF_PAGE, value)
        }

    val rememberPdfPageFlow = ::rememberPdfPage.asFlowNonNull()

    var showFileLocations: Boolean
        get() = prefs.getBoolean(SHOW_FILE_LOCATIONS, false)
        set(value) = prefs.edit {
            putBoolean(SHOW_FILE_LOCATIONS, value)
        }

    val showFileLocationsFlow = ::showFileLocations.asFlowNonNull()
}
