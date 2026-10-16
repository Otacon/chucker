package com.chuckerteam.chucker.internal.data.repository

import com.chuckerteam.chucker.internal.data.room.ChuckerDatabase
import com.chuckerteam.chucker.internal.data.room.createChuckerDatabase

internal fun RepositoryProvider.initialize(database: ChuckerDatabase) {
    initialize(HttpTransactionDatabaseRepository(database))
}

internal fun RepositoryProvider.initialize(databasePath: String) {
    initialize(createChuckerDatabase(databasePath))
}
