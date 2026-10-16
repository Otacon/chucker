package com.chuckerteam.chucker.internal.support

import android.content.Context
import android.text.SpannableStringBuilder
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction

internal fun HttpTransaction.getFormattedRequestBody(): String =
    requestBody?.let { formatBody(it, requestContentType) } ?: ""

internal fun HttpTransaction.getFormattedResponseBody(): String =
    responseBody?.let { formatBody(it, responseContentType) } ?: ""

internal fun HttpTransaction.getSpannedRequestBody(context: Context?): CharSequence =
    requestBody?.let { spanBody(it, requestContentType, context) }
        ?: SpannableStringBuilder.valueOf("")

internal fun HttpTransaction.getSpannedResponseBody(context: Context?): CharSequence =
    responseBody?.let { spanBody(it, responseContentType, context) }
        ?: SpannableStringBuilder.valueOf("")

private fun formatBody(
    body: String,
    contentType: String?,
): String =
    when {
        contentType.isNullOrBlank() -> body
        contentType.contains("json", ignoreCase = true) -> FormatUtils.formatJson(body)
        contentType.contains("xml", ignoreCase = true) -> FormatUtils.formatXml(body)
        contentType.contains("x-www-form-urlencoded", ignoreCase = true) ->
            FormatUtils.formatUrlEncodedForm(body)
        else -> body
    }

private fun spanBody(
    body: CharSequence,
    contentType: String?,
    context: Context?,
): CharSequence =
    when {
        contentType.isNullOrBlank() -> body
        contentType.contains("json", ignoreCase = true) && context != null -> {
            SpanTextUtil(context).spanJson(body)
        }
        else -> formatBody(body.toString(), contentType)
    }
