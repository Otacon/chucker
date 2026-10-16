package com.chuckerteam.chucker.internal.data.har.log.entry.request

import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.data.har.log.entry.request.postdata.Params
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// https://github.com/ahmadnassri/har-spec/blob/master/versions/1.2.md#postdata
// http://www.softwareishard.com/blog/har-12-spec/#postData
// text and params fields are mutually exclusive.
@Serializable
internal data class PostData(
    @SerialName("mimeType") val mimeType: String,
    @SerialName("params") val params: Params? = null,
    @SerialName("text") val text: String? = null,
    @SerialName("comment") val comment: String? = null,
) {
    constructor(transaction: HttpTransaction) : this(
        mimeType = transaction.requestContentType ?: "application/octet-stream",
        text = transaction.requestBody,
    )
}
