package com.chuckerteam.chucker.internal.support

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val harDateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .withZone(ZoneId.systemDefault())

internal actual fun formatHarDateTime(epochMillis: Long): String =
    harDateTimeFormatter.format(Instant.ofEpochMilli(epochMillis))
