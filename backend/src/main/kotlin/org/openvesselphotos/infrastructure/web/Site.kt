package org.openvesselphotos.infrastructure.web

/**
 * The websites served by this app, told apart by their host: Open Vessel Photos, and the map of the French fishing
 * vessels by home port on a domain of its own (OPENVESSELPHOTOS_PORTS_PUBLIC_URL). The /v1 API is shared.
 */
enum class Site(
    val siteName: String,
    /** The React app's entry, see frontend/vite.config.ts */
    val template: String,
) {
    OPENVESSELPHOTOS("Open Vessel Photos", "static/index.html"),
    PORTS("Ports de pêche", "static/ports.html"),
    ;

    companion object {
        /** Local development host of the ports site (browsers resolve *.localhost to the machine itself) */
        const val PORTS_LOCAL_HOST = "ports.localhost"
    }
}
