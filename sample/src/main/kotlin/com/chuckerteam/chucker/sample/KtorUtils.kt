package com.chuckerteam.chucker.sample

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerKtorPlugin
import com.chuckerteam.chucker.api.RetentionManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

fun createKtorClient(context: Context): HttpClient {
    val appContext = context.applicationContext
    val collector =
        ChuckerCollector(
            context = appContext,
            showNotification = true,
            retentionPeriod = RetentionManager.Period.ONE_HOUR,
        )

    return HttpClient(CIO) {
        install(ChuckerKtorPlugin) {
            context(appContext)
            collector(collector)
            maxContentLength(250_000L)
            redactHeaders(emptySet())
            skipPaths("anything")
        }
    }
}
