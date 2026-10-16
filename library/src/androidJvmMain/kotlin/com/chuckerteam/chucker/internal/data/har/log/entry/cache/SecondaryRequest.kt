package com.chuckerteam.chucker.internal.data.har.log.entry.cache

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// https://github.com/ahmadnassri/har-spec/blob/master/versions/1.2.md#beforerequest--afterrequest
// http://www.softwareishard.com/blog/har-12-spec/#cache
@Serializable
internal data class SecondaryRequest(
    @SerialName("expires") val expires: String? = null,
    @SerialName("lastAccess") val lastAccess: String,
    @SerialName("eTag") val eTag: String,
    @SerialName("hitCount") val hitCount: Int,
    @SerialName("comment") val comment: String? = null,
)
