package org.openvesselphotos.infrastructure.web

import jakarta.servlet.http.HttpServletRequest
import java.util.Locale

/** Language of a page: English at the root, French under /fr, so that search engines index both. */
enum class Language(
    val code: String,
    val locale: Locale,
    /** Open Graph locale, e.g. "fr_FR" */
    val ogLocale: String,
    private val pathPrefix: String,
) {
    EN("en", Locale.ENGLISH, "en_GB", ""),
    FR("fr", Locale.FRENCH, "fr_FR", "/fr"),
    ;

    /** e.g. "/fr/vessels?page=2" for "/vessels?page=2", and "/fr" for "/" in French */
    fun path(path: String): String =
        when {
            pathPrefix.isEmpty() -> path
            path == "/" -> pathPrefix
            path.startsWith("/?") -> pathPrefix + path.drop(1)
            else -> pathPrefix + path
        }

    companion object {
        fun of(request: HttpServletRequest): Language {
            val path = request.requestURI.removePrefix(request.contextPath)

            return if (path == "/fr" || path.startsWith("/fr/")) FR else EN
        }
    }
}
