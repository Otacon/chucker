package com.chuckerteam.chucker.internal.support

/** Formats an epoch timestamp using the date format currently used by HAR export. */
internal expect fun formatHarDateTime(epochMillis: Long): String
