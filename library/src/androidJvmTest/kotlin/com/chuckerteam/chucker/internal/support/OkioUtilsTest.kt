package com.chuckerteam.chucker.internal.support

import com.google.common.truth.Truth.assertThat
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import org.junit.jupiter.api.Test

internal class OkioUtilsTest {
    @Test
    fun `no content is probably plain text`() {
        val buffer = Buffer()

        assertThat(buffer.isProbablyPlainText).isTrue()
    }

    @Test
    fun `blank content is probably plain text`() {
        val buffer = Buffer()
        buffer.writeUtf8(" ")

        assertThat(buffer.isProbablyPlainText).isTrue()
    }

    @Test
    fun `plain text content is probably plain text`() {
        val buffer = Buffer()
        buffer.writeUtf8("just a string")

        assertThat(buffer.isProbablyPlainText).isTrue()
    }

    @Test
    fun `non plain text content is probably not plain text`() {
        val buffer = Buffer()
        buffer.writeByte(0x11000000)

        assertThat(buffer.isProbablyPlainText).isFalse()
    }

    @Test
    fun `non-ASCII content is probably plain text`() {
        val buffer = Buffer()
        buffer.writeUtf8("ą")

        assertThat(buffer.isProbablyPlainText).isTrue()
    }

    @Test
    fun `truncated UTF-8 content is probably not plain text`() {
        val bytes = "ą".encodeUtf8().let { it.substring(0, it.size - 1) }
        val buffer = Buffer().write(bytes)

        assertThat(buffer.isProbablyPlainText).isFalse()
    }
}
