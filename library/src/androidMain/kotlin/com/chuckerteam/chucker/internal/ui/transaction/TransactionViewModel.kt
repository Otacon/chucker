package com.chuckerteam.chucker.internal.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chuckerteam.chucker.internal.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class TransactionViewModel(
    transactionId: Long,
) : ViewModel() {
    private val mutableEncodeUrl = MutableStateFlow(false)

    val encodeUrl = mutableEncodeUrl.asStateFlow()

    val transaction =
        RepositoryProvider
            .transaction()
            .getTransaction(transactionId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val transactionTitle =
        RepositoryProvider
            .transaction()
            .getTransaction(transactionId)
            .combine(encodeUrl) { transaction, encodeUrl ->
                if (transaction != null) {
                    "${transaction.method} ${transaction.getFormattedPath(encode = encodeUrl)}"
                } else {
                    ""
                }
            }

    val doesUrlRequireEncoding =
        RepositoryProvider
            .transaction()
            .getTransaction(transactionId)
            .map { transaction ->
                if (transaction == null) {
                    false
                } else {
                    transaction.getFormattedPath(encode = true) != transaction.getFormattedPath(encode = false)
                }
            }

    val doesRequestBodyRequireEncoding =
        RepositoryProvider
            .transaction()
            .getTransaction(transactionId)
            .map { transaction ->
                transaction?.requestContentType?.contains("x-www-form-urlencoded", ignoreCase = true) ?: false
            }

    val formatRequestBody =
        doesRequestBodyRequireEncoding
            .combine(encodeUrl) { requiresEncoding, encodeUrl ->
                !(requiresEncoding && encodeUrl)
            }

    fun switchUrlEncoding() = encodeUrl(!encodeUrl.value)

    fun encodeUrl(encode: Boolean) {
        mutableEncodeUrl.value = encode
    }
}

internal class TransactionViewModelFactory(
    private val transactionId: Long = 0L,
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == TransactionViewModel::class.java) { "Cannot create $modelClass" }
        @Suppress("UNCHECKED_CAST")
        return TransactionViewModel(transactionId) as T
    }
}
