package com.chuckerteam.chucker.internal.support

import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.KtorBodyDecoder
import com.chuckerteam.chucker.internal.data.entity.HttpHeader
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import okio.ByteString.Companion.toByteString
import okio.IOException

internal class KtorResponseProcessor(
    private val collector: ChuckerCollector,
    private val maxContentLength: Long,
    private val headersToRedact: Set<String>,
    private val bodyDecoders: List<KtorBodyDecoder>,
) {
    fun processMetadata(
        response: HttpResponse,
        transaction: HttpTransaction,
    ) {
        transaction.apply {
            responseDate = System.currentTimeMillis()
            protocol = response.version.toString()
            responseCode = response.status.value
            responseMessage = response.status.description
            responseContentType = response.contentType()?.toString()
            responseHeadersSize = response.headers.byteCount()
            responsePayloadSize = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
            setResponseHeaders(response.headers.redact(headersToRedact).toHttpHeaderList())
            tookMs = requestDate?.let { responseDate?.minus(it) }
        }
        collector.onResponseReceived(transaction)
    }

    suspend fun processPayload(
        response: HttpResponse,
        content: ByteReadChannel,
        transaction: HttpTransaction,
    ): ByteReadChannel {
        val bytes = content.readRemaining().readByteArray()
        val limitedBytes = bytes.copyOfRange(0, minOf(bytes.size, maxContentLength.toInt()))

        transaction.responsePayloadSize = bytes.size.toLong()
        if (limitedBytes.isNotEmpty()) {
            val decodedContent = decodePayload(response, limitedBytes.toByteString())
            transaction.responseBody = decodedContent
            transaction.isResponseBodyEncoded = decodedContent == null
        }
        collector.onResponseReceived(transaction)
        return ByteReadChannel(bytes)
    }

    fun processFailure(
        transaction: HttpTransaction,
        throwable: Throwable,
    ) {
        transaction.error = throwable.toString()
        collector.onResponseReceived(transaction)
    }

    private fun decodePayload(
        response: HttpResponse,
        body: okio.ByteString,
    ) = bodyDecoders
        .asSequence()
        .mapNotNull { decoder ->
            try {
                decoder.decodeResponse(response, body)
            } catch (e: IOException) {
                Logger.warn("Ktor decoder $decoder failed to process response payload", e)
                null
            }
        }.firstOrNull()

    private fun Headers.redact(names: Iterable<String>): Headers =
        Headers.build {
            entries().forEach { (name, values) ->
                val redacted = names.any { userHeader -> userHeader.equals(name, ignoreCase = true) }
                values.forEach { value -> append(name, if (redacted) "**" else value) }
            }
        }

    private fun Headers.toHttpHeaderList(): List<HttpHeader> =
        entries().flatMap { (name, values) -> values.map { value -> HttpHeader(name, value) } }

    private fun Headers.byteCount(): Long =
        entries().sumOf { (name, values) -> values.sumOf { value -> name.length + value.length + 4L } }
}
