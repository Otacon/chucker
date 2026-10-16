package com.chuckerteam.chucker.util

import com.chuckerteam.chucker.internal.support.hasBody
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import okio.ByteString
import okio.buffer
import okio.source
import java.io.File

internal const val SEGMENT_SIZE = 8_192L

internal fun getResourceFile(file: String): Buffer =
    Buffer().apply {
        writeAll(File("./src/androidUnitTest/resources/$file").source().buffer())
    }

internal fun Response.readByteStringBody(length: Long? = null): ByteString? =
    if (hasBody()) {
        body?.source()?.use { source ->
            if (length == null) {
                source.readByteString()
            } else {
                source.readByteString(length)
            }
        }
    } else {
        null
    }

internal fun RequestBody.toServerRequest(serverUrl: HttpUrl) =
    Request
        .Builder()
        .url(serverUrl)
        .post(this)
        .build()
