package org.fossify.documents.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.fossify.commons.extensions.getFilenameFromUri
import org.fossify.commons.extensions.getMimeTypeFromUri
import org.fossify.commons.extensions.toast
import org.fossify.documents.R
import org.fossify.documents.models.DocumentKind

class DocumentOpenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent.data
        if (uri == null) {
            finish()
            return
        }

        val destination = when (val kind = DocumentKind.fromName(getFilenameFromUri(uri), getMimeTypeFromUri(uri))) {
            DocumentKind.PDF -> Intent(this, PDFViewerActivity::class.java).apply { data = uri }
            DocumentKind.TEXT,
            DocumentKind.MARKDOWN -> TextDocumentActivity.newIntent(
                context = this,
                uri = uri,
                kind = kind,
                prepared = false,
            )

            DocumentKind.DOCX,
            DocumentKind.CSV,
            DocumentKind.HTML -> StructuredDocumentActivity.newIntent(
                context = this,
                uri = uri,
                kind = kind,
                prepared = false,
            )

            DocumentKind.OTHER -> null
        }

        if (destination == null) {
            toast(R.string.unsupported_document_type)
        } else {
            destination.addFlags(intent.flags and URI_PERMISSION_FLAGS)
            destination.clipData = intent.clipData
            startActivity(destination)
        }
        finish()
    }

    private companion object {
        const val URI_PERMISSION_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    }
}
