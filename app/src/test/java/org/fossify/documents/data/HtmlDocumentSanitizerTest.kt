package org.fossify.documents.data

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlDocumentSanitizerTest {
    @Test
    fun `removes active content and unsafe resource loading`() {
        val sanitized = HtmlDocumentSanitizer.sanitize(
            """
            <html><head><script>alert('no')</script><link rel="stylesheet" href="https://example.com/x.css"></head>
            <body onload="run()">
                <a id="unsafe" href="javascript:run()">Unsafe</a>
                <a id="safe" href="https://fossify.org">Safe</a>
                <img id="remote" src="https://example.com/image.png" onerror="run()">
                <img id="embedded" src="data:image/png;base64,AA==">
                <iframe src="https://example.com"></iframe>
            </body></html>
            """.trimIndent(),
        )
        val document = Jsoup.parse(sanitized)

        assertTrue(document.select("script, link, iframe").isEmpty())
        assertFalse(document.body().hasAttr("onload"))
        assertFalse(document.getElementById("unsafe")!!.hasAttr("href"))
        assertEquals("https://fossify.org", document.getElementById("safe")!!.attr("href"))
        assertFalse(document.getElementById("remote")!!.hasAttr("src"))
        assertFalse(document.getElementById("remote")!!.hasAttr("onerror"))
        assertTrue(document.getElementById("embedded")!!.attr("src").startsWith("data:image/"))
    }

    @Test
    fun `preserves document formatting`() {
        val sanitized = HtmlDocumentSanitizer.sanitize(
            "<h1>Title</h1><p><strong>Bold</strong></p><table><tr><th>A</th></tr><tr><td>B</td></tr></table>",
            isFragment = true,
        )
        val document = Jsoup.parse(sanitized)

        assertEquals("Title", document.selectFirst("h1")!!.text())
        assertEquals("Bold", document.selectFirst("strong")!!.text())
        assertEquals("B", document.selectFirst("td")!!.text())
    }
}
