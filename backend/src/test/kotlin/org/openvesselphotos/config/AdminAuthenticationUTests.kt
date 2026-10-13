package org.openvesselphotos.config

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.springframework.mock.web.MockHttpServletRequest

class AdminAuthenticationUTests {
    private fun requestWith(cookieValue: String) =
        MockHttpServletRequest().apply { setCookies(Cookie(AdminAuthentication.COOKIE_NAME, cookieValue)) }

    @Test
    fun `login Should return a cookie that authenticates`() {
        val authentication = AdminAuthentication("secret")

        val cookie = authentication.login("secret", "1.2.3.4", secure = true)

        assertThat(cookie.isHttpOnly).isTrue()
        assertThat(cookie.sameSite).isEqualTo("Strict")
        assertThat(authentication.isAuthenticated(requestWith(cookie.value))).isTrue()
        assertThat(AdminAuthentication("other").isAuthenticated(requestWith(cookie.value))).isFalse()
        assertThat(authentication.isAuthenticated(requestWith("9999999999.forged"))).isFalse()
    }

    @Test
    fun `login Should lock an IP after too many failures`() {
        val authentication = AdminAuthentication("secret")
        repeat(AdminAuthentication.MAX_FAILED_ATTEMPTS) {
            assertThatThrownBy { authentication.login("wrong", "1.2.3.4", secure = true) }
                .hasFieldOrPropertyWithValue("code", BackendUsageErrorCode.UNAUTHORIZED)
        }

        assertThatThrownBy { authentication.login("secret", "1.2.3.4", secure = true) }
            .isInstanceOf(BackendUsageException::class.java)
            .hasFieldOrPropertyWithValue("code", BackendUsageErrorCode.TOO_MANY_REQUESTS)
        // Another IP is not affected
        authentication.login("secret", "5.6.7.8", secure = true)
    }

    @Test
    fun `login Should always fail When no password is configured`() {
        assertThatThrownBy { AdminAuthentication("").login("", "1.2.3.4", secure = true) }
            .hasFieldOrPropertyWithValue("code", BackendUsageErrorCode.UNAUTHORIZED)
    }
}
