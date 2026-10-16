package com.chuckerteam.chucker.internal.data.har.log.page

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// https://github.com/ahmadnassri/har-spec/blob/master/versions/1.2.md#pagetimings
// http://www.softwareishard.com/blog/har-12-spec/#pageTimings
@Serializable
internal data class PageTimings(
    @SerialName("onContentLoad") val onContentLoad: Long? = null,
    @SerialName("onLoad") val onLoad: Long? = null,
    @SerialName("comment") val comment: String? = null,
)
