package com.chuckerteam.chucker.internal.support

import okio.Buffer
import okio.ByteString
import okio.EOFException
import kotlin.math.min

private const val MAX_PREFIX_SIZE = 64L
private const val CODE_POINT_SIZE = 16

/**
 * Returns true if the [Buffer] contains human readable text. Uses a small sample
 * of code points to detect unicode control characters commonly used in binary file signatures.
 */
internal val Buffer.isProbablyPlainText
    get() =
        try {
            val prefix = Buffer()
            val byteCount = min(size, MAX_PREFIX_SIZE)
            copyTo(prefix, 0, byteCount)
            sequence { while (!prefix.exhausted()) yield(prefix.readUtf8CodePoint()) }
                .take(CODE_POINT_SIZE)
                .all { codePoint -> codePoint.isPlainTextChar() }
        } catch (_: EOFException) {
            false // Truncated UTF-8 sequence
        }

internal val ByteString.isProbablyPlainText: Boolean
    get() {
        val byteCount = min(size, MAX_PREFIX_SIZE.toInt())
        return Buffer().write(this, offset = 0, byteCount).isProbablyPlainText
    }

private fun Int.isPlainTextChar(): Boolean =
    if (this > Char.MAX_VALUE.code) {
        true
    } else {
        toChar().isWhitespace() || !isControlCodePoint()
    }

private fun Int.isControlCodePoint() = this in 0x00..0x1F || this in 0x7F..0x9F
