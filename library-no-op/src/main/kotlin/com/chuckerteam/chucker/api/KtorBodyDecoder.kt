package com.chuckerteam.chucker.api

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import okio.ByteString
import okio.IOException

/**
 * No-op declaration.
 */
public interface KtorBodyDecoder {
    @Throws(IOException::class)
    public fun decodeRequest(
        request: HttpRequestBuilder,
        body: ByteString,
    ): String?

    @Throws(IOException::class)
    public fun decodeResponse(
        response: HttpResponse,
        body: ByteString,
    ): String?
}
