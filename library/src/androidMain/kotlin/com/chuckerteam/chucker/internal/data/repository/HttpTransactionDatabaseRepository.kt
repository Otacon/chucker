package com.chuckerteam.chucker.internal.data.repository

import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.data.entity.HttpTransactionTuple
import com.chuckerteam.chucker.internal.data.room.ChuckerDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

internal class HttpTransactionDatabaseRepository(
    private val database: ChuckerDatabase,
) : HttpTransactionRepository {
    private val transactionDao get() = database.transactionDao()

    override fun getFilteredTransactionTuples(
        code: String,
        path: String,
    ): Flow<List<HttpTransactionTuple>> {
        val pathQuery = if (path.isNotEmpty()) "%$path%" else "%"
        return transactionDao.getFilteredTuples(
            "$code%",
            pathQuery = pathQuery,
            /*
             * Refer <a href='https://github.com/ChuckerTeam/chucker/issues/847">Issue #847</a> for
             * more context
             */
            graphQlQuery = pathQuery,
        )
    }

    override fun getTransaction(transactionId: Long): Flow<HttpTransaction?> =
        transactionDao
            .getById(transactionId)
            .distinctUntilChanged { old, new -> old?.hasTheSameContent(new) != false }

    override fun getSortedTransactionTuples(): Flow<List<HttpTransactionTuple>> = transactionDao.getSortedTuples()

    override suspend fun deleteAllTransactions() {
        transactionDao.deleteAll()
    }

    override suspend fun insertTransaction(transaction: HttpTransaction) {
        val id = transactionDao.insert(transaction)
        transaction.id = id ?: 0
    }

    override suspend fun updateTransaction(transaction: HttpTransaction): Int = transactionDao.update(transaction)

    override suspend fun deleteOldTransactions(threshold: Long) {
        transactionDao.deleteBefore(threshold)
    }

    override suspend fun getAllTransactions(): List<HttpTransaction> = transactionDao.getAll()

    override fun getTransactionsInTimeRange(minTimestamp: Long?): List<HttpTransaction> {
        val timestamp = minTimestamp ?: 0L
        return transactionDao.getTransactionsInTimeRange(timestamp)
    }

    override suspend fun deleteSelectedTransactions(selectedTransactions: List<Long>) {
        transactionDao.deleteSelected(selectedTransactions)
    }

    override suspend fun getSelectedTransactions(selectedTransactions: List<Long>): List<HttpTransaction> =
        transactionDao.getSelectedTransactions(selectedTransactions)
}
