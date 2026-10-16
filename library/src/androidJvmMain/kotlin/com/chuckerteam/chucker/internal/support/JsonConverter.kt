package com.chuckerteam.chucker.internal.support

import kotlinx.serialization.json.Json

internal object JsonConverter {
    val nonNullSerializerInstance: Json by lazy {
        Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            explicitNulls = false
            encodeDefaults = true
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    val instance: Json by lazy {
        Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            explicitNulls = true
            encodeDefaults = true
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    inline fun <reified T> fromJson(json: String?): T? {
        if (json.isNullOrBlank() || json.trim() == "null") return null
        return instance.decodeFromString<T>(json)
    }

    inline fun <reified T> toJson(value: T): String = instance.encodeToString(value)
}
