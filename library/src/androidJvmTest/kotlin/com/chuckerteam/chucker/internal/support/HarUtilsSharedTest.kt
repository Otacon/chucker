package com.chuckerteam.chucker.internal.support

import com.chuckerteam.chucker.internal.data.har.log.Creator
import com.chuckerteam.chucker.util.HarTestUtils
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class HarUtilsSharedTest {
    @Test
    fun `transactions are mapped to HAR entries with creator metadata`() {
        val transactions =
            listOf(
                HarTestUtils.createTransaction("GET"),
                HarTestUtils.createTransaction("POST"),
            )
        val creator = Creator("Chucker", "test")

        val har = HarUtils.fromHttpTransactions(transactions, creator)

        assertThat(har.log.creator).isEqualTo(creator)
        assertThat(har.log.entries.map { it.request.method }).containsExactly("GET", "POST").inOrder()
    }
}
