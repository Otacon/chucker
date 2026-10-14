package com.chuckerteam.chucker.internal.support

import android.content.Context
import com.chuckerteam.chucker.R
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.KtorBodyDecoder
import com.chuckerteam.chucker.internal.data.entity.HttpHeader
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.charset
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okio.ByteString.Companion.toByteString
import okio.IOException

internal class KtorRequestProcessor(
    private val context: Context,
    private val collector: ChuckerCollector,
    private val maxContentLength: Long,
    private val headersToRedact: Set<String>,
    private val bodyDecoders: List<KtorBodyDecoder>,
) {
    fun process(
        request: HttpRequestBuilder,
        content: Any?,
        transaction: HttpTransaction,
    ) {
        processMetadata(request, content, transaction)
        processPayload(request, content, transaction)
        collector.onRequestSent(transaction)
    }

    private fun processMetadata(
        request: HttpRequestBuilder,
        content: Any?,
        transaction: HttpTransaction,
    ) {
        val headers = request.headers.build()
        transaction.apply {
            requestHeadersSize = headers.byteCount()
            setRequestHeaders(headers.redact(headersToRedact).toHttpHeaderList())
            graphQlOperationName = headers.findGraphQlOperationName()
            request.url.buildString().toHttpUrlOrNull()?.let { populateUrl(it) }
            graphQlDetected = graphQlOperationName != null || path?.contains("graphql", ignoreCase = true) == true

            requestDate = System.currentTimeMillis()
            method = request.method.value
            requestContentType = (content as? OutgoingContent)?.contentType?.toString()
            requestPayloadSize = (content as? OutgoingContent)?.contentLength
        }
    }

    private fun processPayload(
        request: HttpRequestBuilder,
        content: Any?,
        transaction: HttpTransaction,
    ) {
        val bytes = content.readReplayableBytes() ?: return
        val limitedBytes = bytes.copyOfRange(0, minOf(bytes.size, maxContentLength.toInt()))
        val decodedContent = decodePayload(request, limitedBytes.toByteString())
        transaction.requestBody = decodedContent
        transaction.isRequestBodyEncoded = decodedContent == null
        if (decodedContent != null && bytes.size > maxContentLength) {
            transaction.requestBody += context.getString(R.string.chucker_body_content_truncated)
        }
    }

    private fun decodePayload(
        request: HttpRequestBuilder,
        body: okio.ByteString,
    ) = bodyDecoders
        .asSequence()
        .mapNotNull { decoder ->
            try {
                decoder.decodeRequest(request, body)
            } catch (e: IOException) {
                Logger.warn("Ktor decoder $decoder failed to process request payload", e)
                null
            }
        }.firstOrNull()

    private fun Any?.readReplayableBytes(): ByteArray? =
        when (this) {
            null -> null
            is OutgoingContent.NoContent -> null
            is ByteArrayContent -> bytes()
            is TextContent -> text.toByteArray(charset = contentType.charset() ?: Charsets.UTF_8)
            else -> null
        }

    private fun Headers.redact(names: Iterable<String>): Headers =
        Headers.build {
            entries().forEach { (name, values) ->
                val redacted = names.any { userHeader -> userHeader.equals(name, ignoreCase = true) }
                values.forEach { value -> append(name, if (redacted) "**" else value) }
            }
        }

    private fun Headers.toHttpHeaderList(): List<HttpHeader> =
        entries().flatMap { (name, values) -> values.map { value -> HttpHeader(name, value) } }

    private fun Headers.findGraphQlOperationName(): String? =
        entries()
            .firstOrNull { (name, _) -> name.lowercase().contains("operation-name") }
            ?.value
            ?.firstOrNull()

    private fun Headers.byteCount(): Long =
        entries().sumOf { (name, values) -> values.sumOf { value -> name.length + value.length + 4L } }
}
