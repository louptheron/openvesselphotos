package org.openvesselphotos.domain.entities

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.openvesselphotos.domain.exceptions.BackendUsageException

class LicenceUTests {
    @Test
    fun `fromSpdx Should accept older versions and the public domain`() {
        assertThat(Licence.fromSpdx("CC-BY-SA-3.0")).isEqualTo(Licence.CC_BY_SA_3_0)
        assertThat(Licence.fromSpdx("cc-by-2.0")).isEqualTo(Licence.CC_BY_2_0)
        assertThat(Licence.fromSpdx("LicenseRef-PublicDomain")).isEqualTo(Licence.PUBLIC_DOMAIN)
        assertThat(Licence.PUBLIC_DOMAIN.requiresCredit).isFalse()
    }

    @Test
    fun `fromSpdx Should reject a non-free licence`() {
        assertThatThrownBy { Licence.fromSpdx("CC-BY-NC-4.0") }.isInstanceOf(BackendUsageException::class.java)
    }

    @Test
    fun `entries Should fit in the database column`() {
        assertThat(Licence.entries).allSatisfy { assertThat(it.name.length).isLessThanOrEqualTo(16) }
    }
}
