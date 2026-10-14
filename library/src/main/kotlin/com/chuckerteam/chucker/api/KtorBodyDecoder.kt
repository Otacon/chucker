package com.chuckerteam.chucker.api

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import okio.ByteString
import okio.IOException

/**
 * Decodes Ktor HTTP request and response bodies to human-readable texts.
 */
public interface KtorBodyDecoder {
    /**
     * Returns a text representation of [body] that will be displayed in Chucker UI transaction,
     * or `null` if [request] cannot be handled by this decoder.
     */
    @Throws(IOException::class)
    public fun decodeRequest(
        request: HttpRequestBuilder,
        body: ByteString,
    ): String?

    /**
     * Returns a text representation of [body] that will be displayed in Chucker UI transaction,
     * or `null` if [response] cannot be handled by this decoder.
     */
    @Throws(IOException::class)
    public fun decodeResponse(
        response: HttpResponse,
        body: ByteString,
    ): String?
}
