package com.chuckerteam.chucker.internal.data.repository

import android.content.Context
import com.chuckerteam.chucker.internal.data.room.createChuckerDatabase

internal fun RepositoryProvider.initialize(applicationContext: Context) {
    initialize(HttpTransactionDatabaseRepository(createChuckerDatabase(applicationContext)))
}
