package org.openvesselphotos.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DatabaseConfigUTests {
    @Test
    fun `toHikariConfig Should convert a Scalingo URL`() {
        val config =
            DatabaseConfig.toHikariConfig(
                "postgres://my_app:p%40ss@my-app.postgresql.a.osc-fr1.scalingo-dbs.com:30885/my_app?sslmode=prefer",
            )

        assertThat(
            config.jdbcUrl,
        ).isEqualTo("jdbc:postgresql://my-app.postgresql.a.osc-fr1.scalingo-dbs.com:30885/my_app?sslmode=prefer")
        assertThat(config.username).isEqualTo("my_app")
        assertThat(config.password).isEqualTo("p@ss")
    }

    @Test
    fun `toHikariConfig Should keep a JDBC URL`() {
        assertThat(
            DatabaseConfig.toHikariConfig("jdbc:postgresql://localhost/db").jdbcUrl,
        ).isEqualTo("jdbc:postgresql://localhost/db")
    }
}
