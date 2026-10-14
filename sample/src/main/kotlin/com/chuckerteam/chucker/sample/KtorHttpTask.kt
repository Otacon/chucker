package com.chuckerteam.chucker.sample

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KtorHttpTask(
    context: Context,
    private val scope: CoroutineScope,
) : HttpTask {
    private val context = context.applicationContext

    private var client: HttpClient? = null

    override fun run() {
        scope.launch {
            withContext(Dispatchers.IO) {
                val client = getClient()
                runHttpBinCalls(client)
                runDummyImageCalls(client)
                runPostmanEchoCalls(client)
                runCloudflareCalls(client)
            }
        }
    }

    @Suppress("MagicNumber", "LongMethod")
    private suspend fun runHttpBinCalls(client: HttpClient) {
        client.readText { get("https://httpbin.org/get") }
        client.readText { postJson("https://httpbin.org/post", "posted") }
        client.readText { patchJson("https://httpbin.org/patch", "patched") }
        client.readText { putJson("https://httpbin.org/put", "put") }
        client.readText { delete("https://httpbin.org/delete") }
        client.readText { get("https://httpbin.org/status/201") }
        client.readText { get("https://httpbin.org/status/401") }
        client.readText { get("https://httpbin.org/status/500") }
        client.readText { get("https://httpbin.org/delay/9") }
        client.readText { get("https://httpbin.org/delay/15") }
        client.readText {
            get("https://httpbin.org/redirect-to") {
                parameter("url", "https://http2.akamai.com")
            }
        }
        client.readText { get("https://httpbin.org/redirect/3") }
        client.readText { get("https://httpbin.org/relative-redirect/2") }
        client.readText { get("https://httpbin.org/absolute-redirect/4") }
        client.readText { get("https://httpbin.org/stream/500") }
        client.readText { get("https://httpbin.org/stream-bytes/2048") }
        client.readBytes {
            get("https://httpbin.org/image") {
                accept(ContentType.Image.PNG)
            }
        }
        client.readText {
            get("https://httpbin.org/brotli") {
                header(HttpHeaders.AcceptEncoding, "br")
            }
        }
        client.readText {
            get("https://httpbin.org/gzip") {
                header(HttpHeaders.AcceptEncoding, "gzip")
            }
        }
        client.readText {
            postJson("https://httpbin.org/post", "Some gzip request") {
                header(HttpHeaders.ContentEncoding, "gzip")
            }
        }
        client.readText { get("https://httpbin.org/xml") }
        client.readText { get("https://httpbin.org/encoding/utf8") }
        client.readText { get("https://httpbin.org/deflate") }
        client.readText {
            get("https://httpbin.org/cookies/set") {
                parameter("k1", "v")
            }
        }
        client.readText { get("https://httpbin.org/basic-auth/me/pass") }
        client.readText {
            get("https://httpbin.org/drip") {
                parameter("numbytes", 512)
                parameter("duration", 10)
                parameter("delay", 1)
                parameter("code", 200)
            }
        }
        client.readText { get("https://httpbin.org/deny") }
        client.readText {
            get("https://httpbin.org/cache") {
                header("If-Modified-Since", "Mon")
            }
        }
        client.readText { get("https://httpbin.org/cache/30") }
        client.readText {
            get("https://httpbin.org/redirect-to") {
                parameter("url", "https://ascii.cl?parameter=%22Click+on+%27URL+Encode%27%21%22")
            }
        }
        client.readText {
            get("https://httpbin.org/redirect-to") {
                parameter("url", "https://ascii.cl?parameter=\"Click on 'URL Encode'!\"")
            }
        }
        client.readText {
            post("https://httpbin.org/post") {
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("key1", "Value 1")
                            append("key2", "Value with symbols &$%")
                        },
                    ),
                )
            }
        }
        client.readText {
            post("https://httpbin.org/post") {
                contentType(ContentType.Text.Plain)
                setBody("Hello, world!")
            }
        }
        client.readText { get("https://httpbin.org/anything") }
    }

    private suspend fun runDummyImageCalls(client: HttpClient) {
        client.readBytes { get("https://dummyimage.com/200x200/fff/fff.png") }
        client.readBytes { get("https://dummyimage.com/200x200/000/000.png") }
    }

    private suspend fun runPostmanEchoCalls(client: HttpClient) {
        client.readText {
            post("https://postman-echo.com/post") {
                contentType(ContentType.Application.Json)
                setBody(LARGE_JSON)
            }
        }
        client.readText {
            post("https://postman-echo.com/post") {
                contentType(ContentType.parse("application/protobuf"))
                setBody(Pokemon("Pikachu", level = 99).encodeByteString().toByteArray())
            }
        }
        client.readText {
            post("https://postman-echo.com/post") {
                contentType(ContentType.Application.Json)
                setBody(LARGE_BASE_64_JSON)
            }
        }
    }

    private suspend fun runCloudflareCalls(client: HttpClient) {
        repeat(API_CALL_REPEAT_VALUE) {
            client.readText { get("https://cloudflare-quic.com/") }
        }
    }

    private suspend fun getClient(): HttpClient =
        client
            ?: withContext(Dispatchers.IO) {
                createKtorClient(context)
            }.also { createdClient ->
                client = createdClient
            }

    private suspend fun HttpClient.postJson(
        url: String,
        thing: String,
        block: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse =
        post(url) {
            contentType(ContentType.Application.Json)
            setBody("""{"thing":"$thing"}""")
            block()
        }

    private suspend fun HttpClient.patchJson(
        url: String,
        thing: String,
    ): HttpResponse =
        patch(url) {
            contentType(ContentType.Application.Json)
            setBody("""{"thing":"$thing"}""")
        }

    private suspend fun HttpClient.putJson(
        url: String,
        thing: String,
    ): HttpResponse =
        put(url) {
            contentType(ContentType.Application.Json)
            setBody("""{"thing":"$thing"}""")
        }

    private suspend fun HttpClient.readText(block: suspend HttpClient.() -> HttpResponse) {
        runCatching {
            block().bodyAsText()
        }.onFailure { throwable ->
            throwable.printStackTrace()
        }
    }

    private suspend fun HttpClient.readBytes(block: suspend HttpClient.() -> HttpResponse) {
        runCatching {
            block().body<ByteArray>()
        }.onFailure { throwable ->
            throwable.printStackTrace()
        }
    }

    private companion object {
        const val API_CALL_REPEAT_VALUE = 5
    }
}
