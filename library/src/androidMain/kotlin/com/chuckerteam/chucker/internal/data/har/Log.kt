package com.chuckerteam.chucker.internal.data.har

import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.data.har.log.Browser
import com.chuckerteam.chucker.internal.data.har.log.Creator
import com.chuckerteam.chucker.internal.data.har.log.Entry
import com.chuckerteam.chucker.internal.data.har.log.Page
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// https://github.com/ahmadnassri/har-spec/blob/master/versions/1.2.md#log
// http://www.softwareishard.com/blog/har-12-spec/#log
@Serializable
internal data class Log(
    @SerialName("version") val version: String = "1.2",
    @SerialName("creator") val creator: Creator,
    @SerialName("browser") val browser: Browser? = null,
    @SerialName("pages") val pages: List<Page>? = null,
    @SerialName("entries") val entries: List<Entry>,
    @SerialName("comment") val comment: String? = null,
) {
    constructor(transactions: List<HttpTransaction>, creator: Creator) : this(
        creator = creator,
        entries = transactions.map { Entry(it) },
    )
}
