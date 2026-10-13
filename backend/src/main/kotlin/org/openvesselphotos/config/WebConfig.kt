package org.openvesselphotos.config

import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {
    /** The public API can be called from any website, without credentials. */
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/v1/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST")
            .allowCredentials(false)
            .maxAge(86400)
    }
}

/** Serves the React app for its client-side routes. */
@Controller
class SpaController {
    @GetMapping("/", "/vessels/**", "/contribute", "/docs", "/admin")
    fun index() = "forward:/index.html"
}
