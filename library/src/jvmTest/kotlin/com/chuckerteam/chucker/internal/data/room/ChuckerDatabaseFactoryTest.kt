package com.chuckerteam.chucker.internal.data.room

import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.nio.file.Files

internal class ChuckerDatabaseFactoryTest {
    @Test
    fun `factory opens a Room database using the bundled SQLite driver`() =
        runBlocking {
            val directory = Files.createTempDirectory("chucker-room-test")
            val database = createChuckerDatabase(directory.resolve("transactions.db").toString())

            try {
                val id = database.transactionDao().insert(HttpTransaction())!!
                val inserted = database.transactionDao().getById(id).first()

                assertThat(inserted?.id).isEqualTo(id)
            } finally {
                database.close()
                directory.toFile().deleteRecursively()
            }
        }
}
