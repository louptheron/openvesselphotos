package org.openvesselphotos.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI
import java.net.URLDecoder
import javax.sql.DataSource

/**
 * Builds the datasource from a `postgres://user:password@host:port/database?sslmode=...` URL,
 * the format Scalingo exposes in SCALINGO_POSTGRESQL_URL / DATABASE_URL. A `jdbc:` URL is also accepted.
 */
@Configuration
class DatabaseConfig(
    @param:Value("\${openvesselphotos.database.url}")
    private val databaseUrl: String,
    @param:Value("\${openvesselphotos.database.hikari-max-pool-size}")
    private val poolSize: Int,
) {
    @Bean
    fun dataSource(): DataSource {
        val config = toHikariConfig(databaseUrl)
        config.maximumPoolSize = poolSize

        return HikariDataSource(config)
    }

    companion object {
        fun toHikariConfig(url: String): HikariConfig {
            val config = HikariConfig()
            if (url.startsWith("jdbc:")) {
                config.jdbcUrl = url

                return config
            }

            val uri = URI(url)
            require(
                uri.scheme == "postgres" || uri.scheme == "postgresql",
            ) { "Unsupported database URL scheme: ${uri.scheme}" }
            val port = if (uri.port == -1) 5432 else uri.port
            val query = uri.rawQuery?.let { "?$it" } ?: ""
            config.jdbcUrl = "jdbc:postgresql://${uri.host}:$port${uri.rawPath}$query"
            uri.rawUserInfo?.split(":", limit = 2)?.let { userInfo ->
                config.username = URLDecoder.decode(userInfo[0], Charsets.UTF_8)
                userInfo.getOrNull(1)?.let { config.password = URLDecoder.decode(it, Charsets.UTF_8) }
            }

            return config
        }
    }
}
