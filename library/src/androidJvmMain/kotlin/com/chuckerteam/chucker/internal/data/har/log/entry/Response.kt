package com.chuckerteam.chucker.internal.data.har.log.entry

import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.data.har.log.entry.response.Content
import com.chuckerteam.chucker.internal.data.har.log.entry.response.Content.Companion.EMPTY
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// https://github.com/ahmadnassri/har-spec/blob/master/versions/1.2.md#response
// http://www.softwareishard.com/blog/har-12-spec/#response
@Serializable
internal data class Response(
    @SerialName("status") val status: Int,
    @SerialName("statusText") val statusText: String,
    @SerialName("httpVersion") val httpVersion: String,
    @SerialName("cookies") val cookies: List<kotlinx.serialization.json.JsonElement> = emptyList(),
    @SerialName("headers") val headers: List<Header>,
    @SerialName("content") val content: Content? = null,
    @SerialName("redirectURL") val redirectUrl: String = "",
    @SerialName("headersSize") val headersSize: Long,
    @SerialName("bodySize") val bodySize: Long,
    @SerialName("totalSize") val totalSize: Long,
    @SerialName("comment") val comment: String? = null,
) {
    constructor(transaction: HttpTransaction) : this(
        status = transaction.responseCode ?: 0,
        statusText = transaction.responseMessage ?: "",
        httpVersion = transaction.protocol ?: "",
        headers = transaction.getParsedResponseHeaders()?.map { Header(it) } ?: emptyList(),
        content = transaction.responsePayloadSize?.run { Content(transaction) } ?: EMPTY,
        headersSize = transaction.responseHeadersSize ?: 0,
        bodySize = transaction.getHarResponseBodySize(),
        totalSize = transaction.getResponseTotalSize(),
    )
}
