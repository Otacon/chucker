package com.chuckerteam.chucker.internal.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class HttpHeader(
    @SerialName("name") val name: String,
    @SerialName("value") val value: String,
)
