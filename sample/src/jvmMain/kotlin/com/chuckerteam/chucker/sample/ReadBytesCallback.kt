package com.chuckerteam.chucker.sample

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okio.IOException

class ReadBytesCallback(
    private val byteCount: Long? = null,
) : Callback {
    override fun onFailure(
        call: Call,
        e: IOException,
    ) {
        e.printStackTrace()
    }

    override fun onResponse(
        call: Call,
        response: Response,
    ) {
        response.body?.source()?.use {
            try {
                if (byteCount == null) {
                    it.readByteString()
                } else {
                    it.readByteString(byteCount)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
