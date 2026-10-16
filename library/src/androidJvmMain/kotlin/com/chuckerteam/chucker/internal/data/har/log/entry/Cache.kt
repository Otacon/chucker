package com.chuckerteam.chucker.internal.data.har.log.entry

import com.chuckerteam.chucker.internal.data.har.log.entry.cache.SecondaryRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// https://github.com/ahmadnassri/har-spec/blob/master/versions/1.2.md#cache
// http://www.softwareishard.com/blog/har-12-spec/#cache
@Serializable
internal data class Cache(
    @SerialName("afterRequest") val afterRequest: SecondaryRequest? = null,
    @SerialName("beforeRequest") val beforeRequest: SecondaryRequest? = null,
    @SerialName("comment") val comment: String? = null,
)
