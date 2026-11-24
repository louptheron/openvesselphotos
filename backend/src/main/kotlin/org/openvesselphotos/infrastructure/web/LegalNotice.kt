package org.openvesselphotos.infrastructure.web

/**
 * The legal notice of both sites (mentions légales), mandatory in France: who publishes them, how to reach them, and
 * who hosts them. The React app of Open Vessel Photos has the same texts (frontend/src/features/Legal/LegalPage.tsx).
 */
object LegalNotice {
    const val PUBLISHER = "Loup Theron"

    /** Forwarded to the publisher by Cloudflare Email Routing */
    const val CONTACT_EMAIL = "contact@lapechefrancaise.com"
    const val OPEN_VESSEL_PHOTOS_URL = "https://openvesselphotos.org"
    const val SOURCE_CODE_URL = "https://github.com/louptheron/openvesselphotos"

    /** As in Scalingo's own legal notice (https://scalingo.com/legal-notice), region osc-fr1 */
    const val HOST_FR =
        "Scalingo SAS, 13 rue Jacques Peirotes, 67000 Strasbourg, France (SIRET 808 665 483 00034), " +
            """<a href="https://scalingo.com">scalingo.com</a>. Les serveurs et les données sont en France, """ +
            "à Paris, chez 3DS Outscale."
    const val HOST_EN =
        "Scalingo SAS, 13 rue Jacques Peirotes, 67000 Strasbourg, France (SIRET 808 665 483 00034), " +
            """<a href="https://scalingo.com">scalingo.com</a>. Servers and data are in Paris, France, at """ +
            "3DS Outscale."
}
