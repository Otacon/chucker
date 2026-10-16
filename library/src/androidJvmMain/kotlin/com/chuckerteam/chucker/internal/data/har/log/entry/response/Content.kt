package com.chuckerteam.chucker.internal.data.har.log.entry.response

import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// https://github.com/ahmadnassri/har-spec/blob/master/versions/1.2.md#content
// http://www.softwareishard.com/blog/har-12-spec/#content
@Serializable
internal data class Content(
    @SerialName("size") val size: Long? = null,
    @SerialName("compression") val compression: Int? = null,
    @SerialName("mimeType") val mimeType: String,
    @SerialName("text") val text: String? = null,
    @SerialName("encoding") val encoding: String? = null,
    @SerialName("comment") val comment: String? = null,
) {
    companion object {
        internal val EMPTY =
            Content(
                size = 0L,
                compression = 0,
                mimeType = "text/plain",
                text = "",
            )
    }

    constructor(transaction: HttpTransaction) : this(
        size = transaction.responsePayloadSize,
        mimeType = transaction.responseContentType ?: "application/octet-stream",
        text = transaction.responseBody,
    )
}
