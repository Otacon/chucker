package com.chuckerteam.chucker.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.ChuckerDesktopScreen
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import okio.BufferedSink
import java.util.Base64

fun main() =
    application {
        val client =
            remember {
                OkHttpClient
                    .Builder()
                    .addInterceptor(ChuckerInterceptor())
                    .build()
        }
        var isChuckerWindowOpen by remember { mutableStateOf(false) }

        Window(
            onCloseRequest = ::exitApplication,
            title = "Chucker Sample",
        ) {
            MaterialTheme {
                Surface {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { runHttpActivity(client) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Do HTTP activity")
                        }
                        Button(
                            onClick = { isChuckerWindowOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Launch Chucker directly")
                        }
                    }
                }
            }
        }

        if (isChuckerWindowOpen) {
            Window(
                onCloseRequest = { isChuckerWindowOpen = false },
                title = "Chucker",
            ) {
                ChuckerDesktopScreen()
            }
        }
    }

private fun runHttpActivity(client: OkHttpClient) {
    runHttpBinRequests(client)
    runImageRequests(client)
    runPostmanEchoRequests(client)
    runQuicEndpointRequests(client)
    runGraphQlRequests(client)
}

@Suppress("MagicNumber")
private fun runHttpBinRequests(client: OkHttpClient) {
    val baseUrl = "https://httpbin.org/".toHttpUrl()
    val jsonBody = """{"thing":"sample"}""".toRequestBody(JSON)

    fun send(
        path: String,
        method: String = "GET",
        body: RequestBody? = null,
        headers: Map<String, String> = emptyMap(),
        readByteCount: Long? = null,
    ) {
        val request =
            Request
                .Builder()
                .url(baseUrl.resolve(path)!!)
                .apply { headers.forEach { (name, value) -> header(name, value) } }
                .method(method, body)
                .build()
        client.newCall(request).enqueue(ReadBytesCallback(readByteCount))
    }

    send("get")
    send("post", "POST", jsonBody)
    send("patch", "PATCH", jsonBody)
    send("put", "PUT", jsonBody)
    send("delete", "DELETE")
    listOf(201, 401, 500).forEach { code -> send("status/$code") }
    listOf(9, 15).forEach { seconds -> send("delay/$seconds") }
    send("redirect-to?url=https%3A%2F%2Fhttp2.akamai.com")
    send("redirect/3")
    send("relative-redirect/2")
    send("absolute-redirect/4")
    send("stream/500", readByteCount = SEGMENT_SIZE)
    send("stream-bytes/2048", readByteCount = SEGMENT_SIZE)
    send("image", headers = mapOf("Accept" to "image/png"))
    send("brotli", headers = mapOf("Accept-Encoding" to "br"))
    send("gzip", headers = mapOf("Accept-Encoding" to "gzip"))
    send(
        "post",
        "POST",
        jsonBody,
        headers = mapOf("Content-Encoding" to "gzip"),
    )
    send("xml")
    send("encoding/utf8")
    send("deflate")
    send("cookies/set?k1=v")
    send("basic-auth/me/pass")
    send("drip?numbytes=512&duration=10&delay=1&code=200")
    send("deny")
    send("cache", headers = mapOf("If-Modified-Since" to "Mon"))
    send("cache/30")
    send(
        "post",
        "POST",
        "key1=Value%201&key2=Value%20with%20symbols%20%26%24%25".toRequestBody(FORM_URL_ENCODED),
        headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"),
    )
    send("post", "POST", oneShotRequestBody())
    send("anything")
}

private fun runImageRequests(client: OkHttpClient) {
    listOf("fff", "000").forEach { color ->
        val request = Request.Builder().url("https://dummyimage.com/200x200/$color/$color.png").get().build()
        client.newCall(request).enqueue(ReadBytesCallback())
    }
}

private fun runPostmanEchoRequests(client: OkHttpClient) {
    val url = "https://postman-echo.com/post"
    val largeJson = """{"payload":"${"chucker-sample-".repeat(512)}"}"""
    val protoBytes = byteArrayOf(0x0A, 0x07, 0x50, 0x69, 0x6B, 0x61, 0x63, 0x68, 0x75, 0x10, 0x63)
    val base64Payload = Base64.getEncoder().encodeToString(ByteArray(2048) { it.toByte() })

    listOf(
        largeJson.toRequestBody(JSON),
        protoBytes.toRequestBody("application/protobuf".toMediaType()),
        """{"data":"$base64Payload","ext":"png"}""".toRequestBody(JSON),
    ).forEachIndexed { index, body ->
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(ReadBytesCallback(if (index == 0 || index == 2) SEGMENT_SIZE else null))
    }
}

/** Cronet is Android-only, so Desktop makes the same endpoint requests with its OkHttp client. */
private fun runQuicEndpointRequests(client: OkHttpClient) {
    repeat(5) {
        val request = Request.Builder().url("https://cloudflare-quic.com/").get().build()
        client.newCall(request).enqueue(ReadBytesCallback())
    }
}

private fun runGraphQlRequests(client: OkHttpClient) {
    val baseUrl = "https://rickandmortyapi.com/graphql/"
    val getQuery =
        """query GetCharacter( ${'$'}id: ID! ) {
            character(id: ${'$'}id) {
                id
                name
                status
            }
        }""".trimIndent()
    val getUrl =
        "https://rickandmortyapi.com/graphql"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("query", getQuery)
            .addQueryParameter("variables", """{"id":1}""")
            .build()
    client.newCall(Request.Builder().url(getUrl).get().build()).enqueue(ReadBytesCallback())

    val searchQuery =
        """query SearchCharacters( ${'$'}searchTerm: String ) {
            characters(page: 1, filter: { name: ${'$'}searchTerm }) {
                info { count }
                results { name }
            }
            location(id: 1) { id }
            episodesByIds(ids: [1, 2]) { id }
        }""".trimIndent()
    val searchBody =
        """{"query":${searchQuery.toJsonString()},"variables":{"searchTerm":"Morty"}}"""
            .toRequestBody(JSON)
    val postRequest = Request.Builder().url(baseUrl).post(searchBody).build()
    client.newCall(postRequest).enqueue(ReadBytesCallback())
}

private fun oneShotRequestBody(): RequestBody =
    object : RequestBody() {
        private val content = Buffer().writeUtf8("Hello, world!")

        override fun isOneShot() = true

        override fun contentType() = "text/plain".toMediaType()

        override fun writeTo(sink: BufferedSink) {
            content.readAll(sink)
        }
    }

private fun String.toJsonString(): String =
    buildString {
        append('"')
        this@toJsonString.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
        append('"')
    }

private val JSON = "application/json".toMediaType()
private val FORM_URL_ENCODED = "application/x-www-form-urlencoded".toMediaType()
