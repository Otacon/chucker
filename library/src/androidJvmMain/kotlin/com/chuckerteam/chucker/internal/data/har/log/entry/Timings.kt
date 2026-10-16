package com.chuckerteam.chucker.internal.data.har.log.entry

import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// https://github.com/ahmadnassri/har-spec/blob/master/versions/1.2.md#timings
// http://www.softwareishard.com/blog/har-12-spec/#timings
@Serializable
internal data class Timings(
    @SerialName("blocked") val blocked: Long? = null,
    @SerialName("dns") val dns: Long? = null,
    @SerialName("ssl") val ssl: Long? = null,
    @SerialName("connect") val connect: Long = 0,
    @SerialName("send") val send: Long = 0,
    @SerialName("wait") val wait: Long,
    @SerialName("receive") val receive: Long = 0,
    @SerialName("comment") val comment: String = "The information described by this object is incomplete.",
) {
    constructor(transaction: HttpTransaction) : this(
        wait = transaction.tookMs ?: 0,
    )

    fun getTime(): Long = (blocked ?: 0) + (dns ?: 0) + (ssl ?: 0) + connect + send + wait + receive
}
