package com.chuckerteam.chucker.api

import com.chuckerteam.chucker.internal.data.repository.RepositoryProvider
import com.chuckerteam.chucker.internal.data.repository.initialize
import com.chuckerteam.chucker.internal.data.room.createChuckerDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import java.nio.file.Files

internal class ChuckerInterceptorTest {
    @Test
    fun `captures request and response details for the desktop screen`() = runBlocking {
        val directory = Files.createTempDirectory("chucker-interceptor-test")
        val database = createChuckerDatabase(directory.resolve("transactions.db").toString())
        val server = MockWebServer()
        RepositoryProvider.initialize(database)
        initializeDesktopRepository(directory.resolve("transactions.db").toString())

        try {
            server.enqueue(
                MockResponse()
                    .setResponseCode(201)
                    .addHeader("X-Test-Response", "captured")
                    .setBody("response payload"),
            )
            server.start()

            val response =
                OkHttpClient
                    .Builder()
                    .addInterceptor(ChuckerInterceptor())
                    .build()
                    .newCall(
                        Request
                            .Builder()
                            .url(server.url("/api/items?source=desktop"))
                            .header("X-Test-Request", "captured")
                            .post("request payload".toRequestBody())
                            .build(),
                    ).execute()

            response.close()
            val tuple = RepositoryProvider.transaction().getSortedTransactionTuples().first().single()
            val transaction = RepositoryProvider.transaction().getTransaction(tuple.id).first()!!

            assertThat(tuple.method).isEqualTo("POST")
            assertThat(tuple.responseCode).isEqualTo(201)
            assertThat(transaction.url).contains("/api/items?source=desktop")
            assertThat(transaction.requestBody).isEqualTo("request payload")
            assertThat(transaction.responseBody).isEqualTo("response payload")
            assertThat(transaction.getRequestHeadersString(withMarkup = false)).contains("X-Test-Request: captured")
            assertThat(transaction.getResponseHeadersString(withMarkup = false)).contains("X-Test-Response: captured")
        } finally {
            server.shutdown()
            database.close()
            RepositoryProvider.close()
            directory.toFile().deleteRecursively()
        }
    }
}
