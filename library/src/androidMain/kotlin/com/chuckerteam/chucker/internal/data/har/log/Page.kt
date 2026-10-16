package com.chuckerteam.chucker.internal.data.har.log

import com.chuckerteam.chucker.internal.data.har.log.page.PageTimings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// https://github.com/ahmadnassri/har-spec/blob/master/versions/1.2.md#pages
// http://www.softwareishard.com/blog/har-12-spec/#pages
@Serializable
internal data class Page(
    @SerialName("startedDateTime") val startedDateTime: String,
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("pageTimings") val pageTimings: PageTimings,
    @SerialName("comment") val comment: String? = null,
)
