package com.chuckerteam.chucker.internal.data.har.log

import androidx.annotation.VisibleForTesting
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.data.har.log.entry.Cache
import com.chuckerteam.chucker.internal.data.har.log.entry.Request
import com.chuckerteam.chucker.internal.data.har.log.entry.Response
import com.chuckerteam.chucker.internal.data.har.log.entry.Timings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// https://github.com/ahmadnassri/har-spec/blob/master/versions/1.2.md#entries
// http://www.softwareishard.com/blog/har-12-spec/#entries
@Serializable
internal data class Entry(
    @SerialName("pageref") val pageref: String? = null,
    @SerialName("startedDateTime") val startedDateTime: String,
    @SerialName("time") var time: Long,
    @SerialName("request") val request: Request,
    @SerialName("response") val response: Response,
    @SerialName("cache") val cache: Cache,
    @SerialName("timings") val timings: Timings,
    @SerialName("serverIPAddress") val serverIPAddress: String? = null,
    @SerialName("connection") val connection: String? = null,
    @SerialName("comment") val comment: String? = null,
) {
    constructor(transaction: HttpTransaction) : this(
        startedDateTime = transaction.requestDate?.harFormatted().orEmpty(),
        time = Timings(transaction).getTime(),
        request = Request(transaction),
        response = Response(transaction),
        cache = Cache(),
        timings = Timings(transaction),
    )

    @VisibleForTesting
    object DateFormat : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat = SimpleDateFormat(DATE_FORMAT, Locale.US)
    }

    companion object {
        private const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

        private fun Long.harFormatted(): String = DateFormat.get()!!.format(Date(this))
    }
}
