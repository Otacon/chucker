package com.chuckerteam.chucker.internal.data.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

internal fun createChuckerDatabase(databasePath: String): ChuckerDatabase =
    Room
        .databaseBuilder<ChuckerDatabase>(name = databasePath)
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
