package com.chuckerteam.chucker.internal.support

import com.chuckerteam.chucker.api.KtorBodyDecoder
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.charset
import okio.ByteString
import java.util.Locale
import kotlin.text.Charsets.UTF_8

internal object KtorPlainTextDecoder : KtorBodyDecoder {
    override fun decodeRequest(
        request: HttpRequestBuilder,
        body: ByteString,
    ) = body.tryDecodeAsPlainText(request.headers.build())

    override fun decodeResponse(
        response: HttpResponse,
        body: ByteString,
    ) = body.tryDecodeAsPlainText(response.headers)

    private fun ByteString.tryDecodeAsPlainText(headers: Headers) =
        if (headers.hasSupportedContentEncoding && isProbablyPlainText) {
            string(headers.contentType()?.charset() ?: UTF_8)
        } else {
            null
        }

    private val Headers.hasSupportedContentEncoding: Boolean
        get() =
            get(HttpHeaders.ContentEncoding)
                ?.takeIf { it.isNotEmpty() }
                ?.let { it.lowercase(Locale.ROOT) in supportedEncodings }
                ?: true

    private val supportedEncodings = listOf("identity", "gzip", "br")

    private fun Headers.contentType(): ContentType? =
        get(HttpHeaders.ContentType)?.let { ContentType.parse(it) }
}
