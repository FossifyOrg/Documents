package org.fossify.documents.models

enum class DocumentFilter {
    PDF,
    DOCX,
    TEXT,
    MARKDOWN,
    CSV,
    HTML;

    fun accepts(kind: DocumentKind): Boolean {
        return when (this) {
            PDF -> kind == DocumentKind.PDF
            DOCX -> kind == DocumentKind.DOCX
            TEXT -> kind == DocumentKind.TEXT
            MARKDOWN -> kind == DocumentKind.MARKDOWN
            CSV -> kind == DocumentKind.CSV
            HTML -> kind == DocumentKind.HTML
        }
    }

    companion object {
        fun from(kind: DocumentKind): DocumentFilter? {
            return when (kind) {
                DocumentKind.PDF -> PDF
                DocumentKind.DOCX -> DOCX
                DocumentKind.TEXT -> TEXT
                DocumentKind.MARKDOWN -> MARKDOWN
                DocumentKind.CSV -> CSV
                DocumentKind.HTML -> HTML
                DocumentKind.OTHER -> null
            }
        }
    }
}
