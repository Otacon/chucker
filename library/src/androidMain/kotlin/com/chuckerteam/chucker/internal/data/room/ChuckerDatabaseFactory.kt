package com.chuckerteam.chucker.internal.data.room

import android.content.Context
import androidx.room.Room

internal fun createChuckerDatabase(applicationContext: Context): ChuckerDatabase =
    Room
        .databaseBuilder(applicationContext, ChuckerDatabase::class.java, "chucker.db")
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
