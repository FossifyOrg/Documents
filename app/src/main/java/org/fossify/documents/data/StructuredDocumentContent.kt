package org.fossify.documents.data

internal sealed interface StructuredDocumentContent {
    data class Web(
        val html: String,
    ) : StructuredDocumentContent

    data class Table(
        val rows: List<List<String>>,
        val columnCount: Int,
    ) : StructuredDocumentContent
}
