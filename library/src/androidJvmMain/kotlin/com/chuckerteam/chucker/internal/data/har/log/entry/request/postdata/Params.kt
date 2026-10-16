package com.chuckerteam.chucker.internal.data.har.log.entry.request.postdata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// https://github.com/ahmadnassri/har-spec/blob/master/versions/1.2.md#params
// http://www.softwareishard.com/blog/har-12-spec/#params
@Serializable
internal data class Params(
    @SerialName("name") val name: String,
    @SerialName("value") val value: String? = null,
    @SerialName("fileName") val fileName: String? = null,
    @SerialName("contentType") val contentType: String? = null,
    @SerialName("comment") val comment: String? = null,
)
