package com.chuckerteam.chucker.internal.data.har

import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.data.har.log.Creator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class Har(
    @SerialName("log") val log: Log,
) {
    constructor(transactions: List<HttpTransaction>, creator: Creator) : this(
        log = Log(transactions, creator),
    )
}
