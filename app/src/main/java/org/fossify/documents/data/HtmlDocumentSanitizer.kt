package org.fossify.documents.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

internal object HtmlDocumentSanitizer {
    private val allowedLinkSchemes = setOf("http", "https", "mailto", "tel")

    fun sanitize(source: String, isFragment: Boolean = false): String {
        val document = if (isFragment) {
            Jsoup.parseBodyFragment(source)
        } else {
            Jsoup.parse(source)
        }

        document.select("script, iframe, frame, object, embed, applet, base, link, meta").remove()
        document.select("form").unwrap()
        document.select("input, button, textarea, select, option").remove()

        document.allElements.forEach { element ->
            element.attributes().asList()
                .filter { attribute ->
                    attribute.key.startsWith("on", ignoreCase = true) ||
                            attribute.key.equals("srcdoc", ignoreCase = true)
                }
                .forEach { attribute -> element.removeAttr(attribute.key) }
        }

        document.select("a[href]").forEach { link ->
            val href = link.attr("href").trim()
            if (!isAllowedLink(href)) {
                link.removeAttr("href")
            }
        }
        document.select("img[src]").forEach { image ->
            if (!image.attr("src").trim().startsWith("data:image/", ignoreCase = true)) {
                image.removeAttr("src")
            }
            image.removeAttr("srcset")
        }
        document.outputSettings()
            .prettyPrint(false)
            .syntax(Document.OutputSettings.Syntax.html)

        return document.outerHtml()
    }

    private fun isAllowedLink(href: String): Boolean {
        if (href.startsWith('#')) {
            return true
        }

        val scheme = href.substringBefore(':', missingDelimiterValue = "").lowercase()
        return scheme in allowedLinkSchemes
    }
}
