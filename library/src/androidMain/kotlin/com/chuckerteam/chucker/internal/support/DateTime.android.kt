package com.chuckerteam.chucker.internal.support

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal actual fun formatHarDateTime(epochMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        .format(Date(epochMillis))
