package com.chuckerteam.chucker.api

import android.content.Context
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.support.KtorPlainTextDecoder
import com.chuckerteam.chucker.internal.support.KtorRequestProcessor
import com.chuckerteam.chucker.internal.support.KtorResponseProcessor
import com.chuckerteam.chucker.internal.support.addNonBlankPathSegments
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.SendingRequest
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Url
import io.ktor.utils.io.ByteReadChannel
import io.ktor.util.AttributeKey
import okhttp3.HttpUrl

/**
 * Configuration for [ChuckerKtorPlugin].
 */
@Suppress("TooManyFunctions")
public class ChuckerKtorPluginBuilder {
    internal var context: Context? = null
    internal var collector: ChuckerCollector? = null
    internal var maxContentLength: Long = MAX_CONTENT_LENGTH
    internal var headersToRedact: Set<String> = emptySet()
    internal var decoders: List<KtorBodyDecoder> = emptyList()
    internal var createShortcut: Boolean = true
    internal val skipPaths: MutableSet<String> = mutableSetOf()
    internal val skipPathsRegex: MutableSet<Regex> = mutableSetOf()
    internal val skipDomains: MutableSet<String> = mutableSetOf()
    internal val skipDomainRegex: MutableSet<Regex> = mutableSetOf()

    /** Sets the Android [Context] used by Chucker. */
    public fun context(context: Context): ChuckerKtorPluginBuilder =
        apply {
            this.context = context
        }

    /** Sets the [ChuckerCollector] to customize data retention. */
    public fun collector(collector: ChuckerCollector): ChuckerKtorPluginBuilder =
        apply {
            this.collector = collector
        }

    /** Sets the maximum length for requests and responses content before their truncation. */
    public fun maxContentLength(length: Long): ChuckerKtorPluginBuilder =
        apply {
            this.maxContentLength = length
        }

    /** Sets headers that will be redacted if their names match. */
    public fun redactHeaders(headerNames: Iterable<String>): ChuckerKtorPluginBuilder =
        apply {
            this.headersToRedact = headerNames.toSet()
        }

    /** Sets headers that will be redacted if their names match. */
    public fun redactHeaders(vararg headerNames: String): ChuckerKtorPluginBuilder =
        apply {
            this.headersToRedact = headerNames.toSet()
        }

    /** Adds a [decoder] into Chucker's Ktor processing pipeline. */
    public fun addBodyDecoder(decoder: KtorBodyDecoder): ChuckerKtorPluginBuilder =
        apply {
            this.decoders += decoder
        }

    /** If set to `true`, Chucker will create a launcher shortcut. */
    public fun createShortcut(enable: Boolean): ChuckerKtorPluginBuilder =
        apply {
            this.createShortcut = enable
        }

    /** Sets a list of [String] paths to skip. */
    public fun skipPaths(vararg paths: String): ChuckerKtorPluginBuilder =
        apply {
            paths.forEach { candidatePath ->
                val httpUrl =
                    HttpUrl
                        .Builder()
                        .scheme("https")
                        .host("example.com")
                        .addNonBlankPathSegments(candidatePath)
                        .build()
                this@ChuckerKtorPluginBuilder.skipPaths.add(httpUrl.encodedPath)
            }
        }

    /** Sets a [Regex] path to skip. */
    public fun skipPaths(paths: Regex): ChuckerKtorPluginBuilder =
        apply {
            this.skipPathsRegex.add(paths)
        }

    /** Sets a list of domain names to skip. */
    public fun skipDomains(vararg domains: String): ChuckerKtorPluginBuilder =
        apply {
            this.skipDomains.addAll(domains.map { it.lowercase() })
        }

    /** Sets a [Regex] domain to skip. */
    public fun skipDomains(domains: Regex): ChuckerKtorPluginBuilder =
        apply {
            this.skipDomainRegex.add(domains)
        }
}

/**
 * A Ktor client plugin which persists and displays HTTP activity in your application for later inspection.
 */
public val ChuckerKtorPlugin: ClientPlugin<ChuckerKtorPluginBuilder> =
    createClientPlugin("ChuckerKtor", ::ChuckerKtorPluginBuilder) {
        val context = requireNotNull(pluginConfig.context) { "ChuckerKtorPlugin requires an Android Context." }
        val collector = pluginConfig.collector ?: ChuckerCollector(context)
        val decoders = pluginConfig.decoders + KtorPlainTextDecoder
        val requestProcessor =
            KtorRequestProcessor(
                context = context,
                collector = collector,
                maxContentLength = pluginConfig.maxContentLength,
                headersToRedact = pluginConfig.headersToRedact,
                bodyDecoders = decoders,
            )
        val responseProcessor =
            KtorResponseProcessor(
                collector = collector,
                maxContentLength = pluginConfig.maxContentLength,
                headersToRedact = pluginConfig.headersToRedact,
                bodyDecoders = decoders,
            )
        val skipPaths = pluginConfig.skipPaths.toSet()
        val skipPathsRegex = pluginConfig.skipPathsRegex.toSet()
        val skipDomains = pluginConfig.skipDomains.toSet()
        val skipDomainRegex = pluginConfig.skipDomainRegex.toSet()

        if (pluginConfig.createShortcut) {
            Chucker.createShortcut(context)
        }

        on(SendingRequest) { request, content ->
            val transaction = HttpTransaction()
            if (request.url.build().shouldProcess(skipPaths, skipPathsRegex, skipDomains, skipDomainRegex)) {
                requestProcessor.process(request, content, transaction)
                request.attributes.put(transactionKey, transaction)
            }
        }

        on(Send) { request ->
            try {
                proceed(request)
            } catch (throwable: Throwable) {
                request.attributes.getOrNull(transactionKey)?.let { transaction ->
                    responseProcessor.processFailure(transaction, throwable)
                }
                throw throwable
            }
        }

        onResponse { response: HttpResponse ->
            response.call.attributes.getOrNull(transactionKey)?.let { transaction ->
                responseProcessor.processMetadata(response, transaction)
            }
        }

        transformResponseBody { response, content, _ ->
            val transaction = response.call.attributes.getOrNull(transactionKey)
            if (transaction == null) {
                content
            } else {
                responseProcessor.processPayload(response, content, transaction)
            }
        }
    }

private fun Url.shouldProcess(
    skipPaths: Set<String>,
    skipPathsRegex: Set<Regex>,
    skipDomains: Set<String>,
    skipDomainRegex: Set<Regex>,
): Boolean {
    val path = encodedPath.ifBlank { "/" }
    val lowerHost = host.lowercase()
    val shouldSkipPath = skipPaths.contains(path) || skipPathsRegex.any { it.matches(path) }
    val shouldSkipDomain =
        shouldSkipPath || skipDomains.contains(lowerHost) || skipDomainRegex.any { it.matches(lowerHost) }
    return !(shouldSkipPath || shouldSkipDomain)
}

private val transactionKey = AttributeKey<HttpTransaction>("ChuckerKtorTransaction")

private const val MAX_CONTENT_LENGTH = 250_000L
