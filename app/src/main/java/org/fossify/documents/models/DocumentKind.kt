package org.fossify.documents.models

import android.net.Uri

enum class DocumentKind {
    PDF,
    DOCX,
    TEXT,
    MARKDOWN,
    CSV,
    HTML,
    OTHER;

    companion object {
        fun from(uri: Uri, mimeType: String): DocumentKind {
            return fromName(uri.lastPathSegment.orEmpty().substringAfterLast('/'), mimeType)
        }

        fun fromName(fileName: String, mimeType: String): DocumentKind {
            val name = fileName.lowercase()
            val extension = name.substringAfterLast('.', missingDelimiterValue = "")
            val normalizedMimeType = mimeType.substringBefore(';').lowercase()
            return mimeKinds[normalizedMimeType]
                ?: extensionKinds[extension]
                ?: TEXT.takeIf {
                    normalizedMimeType.startsWith("text/") || normalizedMimeType in textApplicationMimeTypes
                }
                ?: OTHER
        }

        private val textExtensions = setOf(
            "txt",
            "log",
            "json",
            "xml",
            "yaml",
            "yml",
            "toml",
            "ini",
            "conf",
            "cfg",
            "properties",
            "ics",
            "vcf",
            "srt",
        )
        private val extensionKinds = mapOf(
            "pdf" to PDF,
            "docx" to DOCX,
            "md" to MARKDOWN,
            "markdown" to MARKDOWN,
            "mdown" to MARKDOWN,
            "mkd" to MARKDOWN,
            "csv" to CSV,
            "tsv" to CSV,
            "html" to HTML,
            "htm" to HTML,
            "xhtml" to HTML,
        ) + textExtensions.associateWith { TEXT }
        private val mimeKinds = mapOf(
            "application/pdf" to PDF,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to DOCX,
            "text/markdown" to MARKDOWN,
            "text/x-markdown" to MARKDOWN,
            "text/csv" to CSV,
            "text/comma-separated-values" to CSV,
            "text/tab-separated-values" to CSV,
            "application/csv" to CSV,
            "text/html" to HTML,
            "application/xhtml+xml" to HTML,
        )
        private val textApplicationMimeTypes = setOf(
            "application/json",
            "application/xml",
            "application/yaml",
            "application/x-yaml",
            "application/toml",
        )
    }
}
