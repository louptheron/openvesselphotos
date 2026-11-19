package org.openvesselphotos.infrastructure.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class IndexNowNotifierUTests {
    private val jsonMapper = JsonMapper.builder().build()

    @Test
    fun `IndexNow Should be enabled by a valid key with the public URL only`() {
        val key = "0f8fad5b-d9cb-469f-a165-70867728950e"

        assertThat(IndexNowNotifier(key, "https://openvesselphotos.org/", jsonMapper).key).isEqualTo(key)
        assertThat(IndexNowNotifier("", "https://openvesselphotos.org", jsonMapper).key).isNull()
        assertThat(IndexNowNotifier(key, "", jsonMapper).key).isNull()
        assertThat(IndexNowNotifier("short", "https://openvesselphotos.org", jsonMapper).key).isNull()
        assertThat(IndexNowNotifier("not a key!", "https://openvesselphotos.org", jsonMapper).key).isNull()
    }

    @Test
    fun `requestBody Should give the host, the key and its location, and the URLs`() {
        val notifier = IndexNowNotifier("0f8fad5b-d9cb", "https://openvesselphotos.org/", jsonMapper)

        assertThat(
            notifier.requestBody(
                "0f8fad5b-d9cb",
                listOf("https://openvesselphotos.org/vessels/cfr/FRA000539755"),
            ),
        ).isEqualTo(
            """{"host":"openvesselphotos.org","key":"0f8fad5b-d9cb",""" +
                """"keyLocation":"https://openvesselphotos.org/indexnow-key.txt",""" +
                """"urlList":["https://openvesselphotos.org/vessels/cfr/FRA000539755"]}""",
        )
    }
}
