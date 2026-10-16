package com.chuckerteam.chucker.internal.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction

@Database(entities = [HttpTransaction::class], version = 11, exportSchema = false)
internal abstract class ChuckerDatabase : RoomDatabase() {
    abstract fun transactionDao(): HttpTransactionDao
}
