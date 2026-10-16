package com.chuckerteam.chucker.internal.data.har.log.entry

import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.data.har.log.entry.request.PostData
import com.chuckerteam.chucker.internal.data.har.log.entry.request.QueryString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl

// https://github.com/ahmadnassri/har-spec/blob/master/versions/1.2.md#request
// http://www.softwareishard.com/blog/har-12-spec/#request
@Serializable
internal data class Request(
    @SerialName("method") val method: String,
    @SerialName("url") val url: String,
    @SerialName("httpVersion") val httpVersion: String,
    @SerialName("cookies") val cookies: List<kotlinx.serialization.json.JsonElement> = emptyList(),
    @SerialName("headers") val headers: List<Header>,
    @SerialName("queryString") val queryString: List<QueryString>,
    @SerialName("postData") val postData: PostData? = null,
    @SerialName("headersSize") val headersSize: Long,
    @SerialName("bodySize") val bodySize: Long,
    @SerialName("totalSize") val totalSize: Long,
    @SerialName("comment") val comment: String? = null,
) {
    constructor(transaction: HttpTransaction) : this(
        method = transaction.method ?: "",
        url = transaction.url ?: "",
        httpVersion = transaction.protocol ?: "",
        headers = transaction.getParsedRequestHeaders()?.map { Header(it) } ?: emptyList(),
        queryString = transaction.url?.let { QueryString.fromUrl(it.toHttpUrl()) } ?: emptyList(),
        postData = transaction.requestPayloadSize?.run { PostData(transaction) },
        headersSize = transaction.requestHeadersSize ?: 0,
        bodySize = transaction.requestPayloadSize ?: 0,
        totalSize = transaction.getRequestTotalSize(),
    )
}
