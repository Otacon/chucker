package com.chuckerteam.chucker.internal.support

import com.chuckerteam.chucker.internal.data.entity.HttpHeader

internal object HttpHeaderSerializer {
    fun toJson(headers: List<HttpHeader>): String = JsonConverter.toJson(headers)
}
