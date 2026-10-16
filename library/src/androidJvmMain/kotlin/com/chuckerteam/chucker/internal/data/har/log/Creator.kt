package com.chuckerteam.chucker.internal.data.har.log

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// https://github.com/ahmadnassri/har-spec/blob/master/versions/1.2.md#creator
// http://www.softwareishard.com/blog/har-12-spec/#creator
@Serializable
internal data class Creator(
    @SerialName("name") val name: String,
    @SerialName("version") val version: String,
    @SerialName("comment") val comment: String? = null,
)
