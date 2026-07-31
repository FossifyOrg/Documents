package org.fossify.documents.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentLinksTest {
    @Test
    fun `allows supported external links`() {
        assertTrue(isAllowedExternalDocumentLink("https://fossify.org"))
        assertTrue(isAllowedExternalDocumentLink("MAILTO:hello@fossify.org"))
        assertTrue(isAllowedExternalDocumentLink("tel:+123456789"))
    }

    @Test
    fun `rejects local and active content links`() {
        assertFalse(isAllowedExternalDocumentLink("guide.md"))
        assertFalse(isAllowedExternalDocumentLink("#section"))
        assertFalse(isAllowedExternalDocumentLink("javascript:alert('no')"))
        assertFalse(isAllowedExternalDocumentLink("file:///storage/emulated/0/private.txt"))
    }
}
