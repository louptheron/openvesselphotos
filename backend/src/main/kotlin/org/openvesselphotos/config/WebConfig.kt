package org.openvesselphotos.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Duration

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

    /** Vite fingerprints the file names of its assets: they never change and can be cached forever. */
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry
            .addResourceHandler("/assets/**")
            .addResourceLocations("classpath:/static/assets/")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
    }
}
