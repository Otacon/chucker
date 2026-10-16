package com.chuckerteam.chucker.internal.support

import okhttp3.Headers

private val supportedEncodings = listOf("identity", "gzip", "br")

internal val Headers.hasSupportedContentEncoding: Boolean
    get() =
        get("Content-Encoding")
            ?.takeIf { it.isNotEmpty() }
            ?.let { encoding -> supportedEncodings.any { it.equals(encoding, ignoreCase = true) } }
            ?: true
