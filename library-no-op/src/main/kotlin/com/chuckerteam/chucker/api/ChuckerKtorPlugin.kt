package com.chuckerteam.chucker.api

import android.content.Context
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin

/**
 * No-op implementation.
 */
@Suppress("UnusedPrivateMember", "UNUSED_PARAMETER", "TooManyFunctions")
public class ChuckerKtorPluginBuilder {
    public fun context(context: Context): ChuckerKtorPluginBuilder = this

    public fun collector(collector: ChuckerCollector): ChuckerKtorPluginBuilder = this

    public fun maxContentLength(length: Long): ChuckerKtorPluginBuilder = this

    public fun redactHeaders(headerNames: Iterable<String>): ChuckerKtorPluginBuilder = this

    public fun redactHeaders(vararg headerNames: String): ChuckerKtorPluginBuilder = this

    public fun addBodyDecoder(decoder: KtorBodyDecoder): ChuckerKtorPluginBuilder = this

    public fun createShortcut(enable: Boolean): ChuckerKtorPluginBuilder = this

    public fun skipPaths(vararg paths: String): ChuckerKtorPluginBuilder = this

    public fun skipPaths(paths: Regex): ChuckerKtorPluginBuilder = this

    public fun skipDomains(vararg domains: String): ChuckerKtorPluginBuilder = this

    public fun skipDomains(domains: Regex): ChuckerKtorPluginBuilder = this
}

/**
 * No-op implementation.
 */
public val ChuckerKtorPlugin: ClientPlugin<ChuckerKtorPluginBuilder> =
    createClientPlugin("ChuckerKtor", ::ChuckerKtorPluginBuilder) {}
