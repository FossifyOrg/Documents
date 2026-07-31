package org.fossify.documents.data

private val externalDocumentLinkSchemes = setOf("http", "https", "mailto", "tel")

internal fun isAllowedExternalDocumentLink(destination: String): Boolean {
    val scheme = destination.substringBefore(':', missingDelimiterValue = "").lowercase()
    return scheme in externalDocumentLinkSchemes
}
