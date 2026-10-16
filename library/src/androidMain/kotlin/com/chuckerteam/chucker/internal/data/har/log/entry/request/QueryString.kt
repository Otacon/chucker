package com.chuckerteam.chucker.internal.data.har.log.entry.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl

// https://github.com/ahmadnassri/har-spec/blob/master/versions/1.2.md#querystring
// http://www.softwareishard.com/blog/har-12-spec/#queryString
@Serializable
internal data class QueryString(
    @SerialName("name") val name: String,
    @SerialName("value") val value: String,
    @SerialName("comment") val comment: String? = null,
) {
    companion object {
        fun fromUrl(url: HttpUrl): List<QueryString> =
            List(url.querySize) { index ->
                QueryString(
                    name = url.queryParameterName(index),
                    value = url.queryParameterValue(index).orEmpty(),
                )
            }
    }
}
