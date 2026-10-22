package org.openvesselphotos.infrastructure.web

import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.util.HtmlUtils
import tools.jackson.databind.json.JsonMapper

/** What search engines and link previews read from a page, before the React app takes over. */
data class HtmlPage(
    val title: String,
    val description: String,
    /** Path of the canonical URL; null for pages that must not be indexed. */
    val canonicalPath: String?,
    val language: Language = Language.EN,
    /**
     * Path of the page without its language prefix, for pages in every language (e.g. "/vessels"): search engines link
     * each language's version to the others with it (hreflang).
     */
    val pathInEveryLanguage: String? = null,
    val isIndexable: Boolean = true,
    val imageUrl: String? = null,
    val structuredData: List<Map<String, Any?>> = emptyList(),
    /** Server-rendered content, replaced by the React app once loaded. Must be escaped HTML. */
    val body: String = "",
    val status: HttpStatus = HttpStatus.OK,
)

fun escapeHtml(value: String?): String = HtmlUtils.htmlEscape(value ?: "", "UTF-8")

/**
 * Renders the React app's index.html with the page's metadata, between the markers of frontend/index.html.
 */
@Component
class HtmlPageRenderer(
    private val jsonMapper: JsonMapper,
    private val publicUrl: PublicUrl,
) {
    private val logger = LoggerFactory.getLogger(HtmlPageRenderer::class.java)

    private val template: String by lazy {
        val resource = ClassPathResource("static/index.html")
        if (resource.exists()) {
            resource.inputStream.use { String(it.readAllBytes(), Charsets.UTF_8) }
        } else {
            logger.warn("static/index.html not found: the frontend was not built, serving a bare page.")
            FALLBACK_TEMPLATE
        }
    }

    fun render(page: HtmlPage): ResponseEntity<String> {
        val html =
            template
                .replace(
                    Regex("$HEAD_START.*?$HEAD_END", RegexOption.DOT_MATCHES_ALL),
                    Regex.escapeReplacement(renderHead(page)),
                ).replace(HTML_LANG, """<html lang="${page.language.code}">""")
                .replace(BODY_MARKER, page.body)

        return ResponseEntity
            .status(page.status)
            .contentType(MediaType("text", "html", Charsets.UTF_8))
            .cacheControl(CacheControl.noCache())
            .body(html)
    }

    private fun renderHead(page: HtmlPage): String {
        val baseUrl = publicUrl.get()
        val canonicalUrl = page.canonicalPath?.let { baseUrl + it }
        val title = escapeHtml(page.title)
        val description = escapeHtml(page.description)

        return buildString {
            appendLine("<title>$title</title>")
            appendLine("""    <meta name="description" content="$description" />""")
            if (!page.isIndexable) {
                appendLine("""    <meta name="robots" content="noindex" />""")
            }
            canonicalUrl?.let {
                appendLine("""    <link rel="canonical" href="${escapeHtml(it)}" />""")
                appendLine("""    <meta property="og:url" content="${escapeHtml(it)}" />""")
            }
            page.pathInEveryLanguage?.let { path ->
                Language.entries.forEach {
                    appendLine(
                        """    <link rel="alternate" hreflang="${it.code}" href="${escapeHtml(
                            baseUrl + it.path(path),
                        )}" />""",
                    )
                }
                appendLine("""    <link rel="alternate" hreflang="x-default" href="${escapeHtml(baseUrl + path)}" />""")
            }
            appendLine("""    <meta property="og:site_name" content="$SITE_NAME" />""")
            appendLine("""    <meta property="og:locale" content="${page.language.ogLocale}" />""")
            appendLine("""    <meta property="og:type" content="website" />""")
            appendLine("""    <meta property="og:title" content="$title" />""")
            appendLine("""    <meta property="og:description" content="$description" />""")
            if (page.imageUrl != null) {
                appendLine("""    <meta property="og:image" content="${escapeHtml(page.imageUrl)}" />""")
                appendLine("""    <meta name="twitter:card" content="summary_large_image" />""")
            } else {
                appendLine("""    <meta name="twitter:card" content="summary" />""")
            }
            page.structuredData.forEach {
                // "<" is escaped so that user content cannot close the <script> element
                val json = jsonMapper.writeValueAsString(it).replace("<", "\\u003c")
                appendLine("""    <script type="application/ld+json">$json</script>""")
            }
            append("    ")
        }
    }

    companion object {
        const val SITE_NAME = "Open Vessel Photos"
        private const val HEAD_START = "<!-- seo:head -->"
        private const val HEAD_END = "<!-- /seo:head -->"
        private const val BODY_MARKER = "<!-- seo:body -->"
        private const val HTML_LANG = """<html lang="en">"""
        private const val FALLBACK_TEMPLATE =
            "<!doctype html><html lang=\"en\"><head>$HEAD_START$HEAD_END</head><body><div id=\"root\">$BODY_MARKER</div></body></html>"
    }
}
