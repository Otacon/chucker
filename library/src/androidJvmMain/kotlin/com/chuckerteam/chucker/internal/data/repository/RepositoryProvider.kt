package com.chuckerteam.chucker.internal.data.repository

import androidx.annotation.VisibleForTesting
import com.chuckerteam.chucker.internal.data.repository.RepositoryProvider.initialize

/**
 * A singleton to hold the [HttpTransactionRepository] instance.
 * Make sure you call [initialize] before accessing the stored instance.
 */
internal object RepositoryProvider {
    private var transactionRepository: HttpTransactionRepository? = null

    fun transaction(): HttpTransactionRepository =
        checkNotNull(transactionRepository) {
            "You can't access the transaction repository if you don't initialize it!"
        }

    /**
     * Idempotent method. Must be called before accessing the repositories.
     */
    fun initialize(repository: HttpTransactionRepository) {
        if (transactionRepository == null) {
            transactionRepository = repository
        }
    }

    /**
     * Cleanup stored singleton objects
     */
    @VisibleForTesting
    fun close() {
        transactionRepository = null
    }
}
