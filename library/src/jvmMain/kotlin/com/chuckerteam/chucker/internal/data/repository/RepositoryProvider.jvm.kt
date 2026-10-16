package com.chuckerteam.chucker.internal.data.repository

import com.chuckerteam.chucker.internal.data.room.ChuckerDatabase

internal fun RepositoryProvider.initialize(database: ChuckerDatabase) {
    initialize(HttpTransactionDatabaseRepository(database))
}
