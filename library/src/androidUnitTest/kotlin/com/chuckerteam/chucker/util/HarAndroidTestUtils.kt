package com.chuckerteam.chucker.util

import android.content.Context
import com.chuckerteam.chucker.R
import com.chuckerteam.chucker.internal.data.har.Har
import com.chuckerteam.chucker.internal.data.har.log.Creator
import com.chuckerteam.chucker.internal.support.HarUtils
import kotlinx.coroutines.runBlocking

internal fun Context.createSingleTransactionHar(method: String): Har =
    HarUtils.fromHttpTransactions(
        listOf(HarTestUtils.createTransaction(method)),
        Creator(
            getString(R.string.chucker_name),
            getString(R.string.chucker_version),
        ),
    )

internal fun Context.createListTransactionHar(): Har =
    HarUtils.fromHttpTransactions(
        listOf(HarTestUtils.createTransaction("GET"), HarTestUtils.createTransaction("POST")),
        Creator(
            getString(R.string.chucker_name),
            getString(R.string.chucker_version),
        ),
    )

internal fun Context.createHarString(): String =
    runBlocking {
        HarUtils.harStringFromTransactions(
            listOf(HarTestUtils.createTransaction("POST")),
            getString(R.string.chucker_name),
            getString(R.string.chucker_version),
        )
    }
