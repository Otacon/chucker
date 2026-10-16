package com.chuckerteam.chucker.internal.data.har.log.entry

import com.chuckerteam.chucker.internal.data.entity.HttpHeader
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// https://github.com/ahmadnassri/har-spec/blob/master/versions/1.2.md#headers
// http://www.softwareishard.com/blog/har-12-spec/#headers
@Serializable
internal data class Header(
    @SerialName("name") val name: String,
    @SerialName("value") val value: String,
    @SerialName("comment") val comment: String? = null,
) {
    constructor(header: HttpHeader) : this(
        name = header.name,
        value = header.value,
    )
}
