package com.chuckerteam.chucker.api

import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.data.repository.RepositoryProvider
import com.chuckerteam.chucker.internal.data.repository.initialize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.io.File

/**
 * An OkHttp interceptor that records HTTP activity for [ChuckerDesktopScreen].
 * Add it to a Desktop OkHttp client with `addInterceptor(ChuckerInterceptor())`.
 */
public class ChuckerInterceptor public constructor() : Interceptor {
    init {
        initializeDesktopRepository()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val transaction =
            HttpTransaction().apply {
                requestDate = System.currentTimeMillis()
                method = request.method
                protocol = null
                populateUrl(request.url)
                setRequestHeaders(request.headers)
                requestContentType = request.body?.contentType()?.toString()
                requestPayloadSize = request.body?.contentLength()?.takeIf { it >= 0 }
                requestHeadersSize = headerBytes(request.headers)
                requestBody =
                    request.body
                        ?.takeIf { !it.isOneShot() && it.contentLength() in 0..MAX_CAPTURE_BYTES }
                        ?.let { body ->
                            Buffer().also(body::writeTo).readUtf8()
                        }
            }
        persist { insertTransaction(transaction) }

        val startedAt = System.currentTimeMillis()
        val response =
            try {
                chain.proceed(request)
            } catch (exception: Exception) {
                transaction.error = exception.toString()
                transaction.responseDate = System.currentTimeMillis()
                transaction.tookMs = transaction.responseDate!! - startedAt
                persist { updateTransaction(transaction) }
                throw exception
            }

        transaction.apply {
            responseDate = System.currentTimeMillis()
            tookMs = responseDate!! - startedAt
            protocol = response.protocol.toString()
            responseCode = response.code
            responseMessage = response.message
            responseContentType = response.body.contentType()?.toString()
            responsePayloadSize = response.body.contentLength().takeIf { it >= 0 }
            responseHeadersSize = headerBytes(response.headers)
            setResponseHeaders(response.headers)
            responseBody = response.peekBody(MAX_CAPTURE_BYTES).string()
        }
        persist { updateTransaction(transaction) }
        return response
    }

    private fun persist(block: suspend com.chuckerteam.chucker.internal.data.repository.HttpTransactionRepository.() -> Unit) {
        runBlocking(Dispatchers.IO) { RepositoryProvider.transaction().block() }
    }

    private fun headerBytes(headers: okhttp3.Headers): Long =
        (0 until headers.size).sumOf { index ->
            headers.name(index).length.toLong() + headers.value(index).length + 4L
        }

    private companion object {
        const val MAX_CAPTURE_BYTES = 250_000L
    }
}

private val repositoryLock = Any()
private var desktopRepositoryInitialized = false

internal fun initializeDesktopRepository(databasePath: String? = null) {
    synchronized(repositoryLock) {
        if (desktopRepositoryInitialized) return
        val path =
            databasePath ?: run {
                val databaseDirectory = File(System.getProperty("user.home"), ".chucker")
                check(databaseDirectory.exists() || databaseDirectory.mkdirs()) {
                    "Unable to create Chucker data directory: $databaseDirectory"
                }
                File(databaseDirectory, "transactions.db").absolutePath
            }
        RepositoryProvider.initialize(path)
        desktopRepositoryInitialized = true
    }
}
