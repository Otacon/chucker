package com.chuckerteam.chucker.internal.support

import com.chuckerteam.chucker.internal.data.entity.HttpHeader
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Test

internal class HttpHeaderSerializerTest {
    private val sampleHeaders =
        listOf(
            HttpHeader("Content-Type", "application/json"),
            HttpHeader("X-Trace", "abc\"123\n"),
        )

    @Test
    fun `empty list serializes to an empty JSON array`() {
        val json = HttpHeaderSerializer.toJson(emptyList())

        val parsed = JsonConverter.instance.decodeFromString<List<HttpHeader>>(json)
        assertThat(parsed).isEmpty()
    }

    @Test
    fun `round-trips headers`() {
        val json = HttpHeaderSerializer.toJson(sampleHeaders)

        val parsed = JsonConverter.instance.decodeFromString<List<HttpHeader>>(json)
        assertThat(parsed).containsExactlyElementsIn(sampleHeaders).inOrder()
    }

    @Test
    fun `serialization escapes control characters and quotes`() {
        val awkward =
            listOf(
                HttpHeader("k\"ey", "line1\nline2\tend"),
            )

        val fallbackJson = HttpHeaderSerializer.toJson(awkward)

        val parsed = JsonConverter.instance.decodeFromString<List<HttpHeader>>(fallbackJson)
        assertThat(parsed).containsExactlyElementsIn(awkward).inOrder()
    }

    @Test
    fun `deserialization accepts missing or null input like Gson`() {
        assertThat(JsonConverter.fromJson<List<HttpHeader>>(null)).isNull()
        assertThat(JsonConverter.fromJson<List<HttpHeader>>("  ")).isNull()
        assertThat(JsonConverter.fromJson<List<HttpHeader>>("null")).isNull()
    }

    @Test
    fun `deserialization ignores unknown fields like Gson`() {
        val parsed = JsonConverter.fromJson<List<HttpHeader>>(
            """[{"name":"x","value":"y","extra":true}]""",
        )

        assertThat(parsed).containsExactly(HttpHeader("x", "y"))
    }

}
