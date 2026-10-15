package com.chuckerteam.chucker.internal.ui

import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.chuckerteam.chucker.R
import com.chuckerteam.chucker.api.Chucker
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.data.entity.HttpTransactionTuple
import com.chuckerteam.chucker.internal.data.repository.RepositoryProvider
import com.chuckerteam.chucker.internal.support.FileSaver
import com.chuckerteam.chucker.internal.support.FormatUtils
import com.chuckerteam.chucker.internal.support.FormattedUrl
import com.chuckerteam.chucker.internal.support.HarUtils
import com.chuckerteam.chucker.internal.support.NotificationHelper
import com.chuckerteam.chucker.internal.support.Sharable
import com.chuckerteam.chucker.internal.support.TransactionDetailsHarSharable
import com.chuckerteam.chucker.internal.support.TransactionListDetailsSharable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okio.Source
import okio.buffer
import okio.source
import java.text.DateFormat
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("TooManyFunctions")
internal class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository by lazy { RepositoryProvider.transaction() }

    private val _events = MutableSharedFlow<MainActivityEvents>()
    val events = _events.asSharedFlow()

    private val _state = MutableStateFlow(MainActivityState())
    val state = _state.asStateFlow()

    internal val selectedTransactionIds: List<Long>
        get() = _state.value.selectedTransactions

    init {
        viewModelScope.launch {
            _state
                .map { it.query }
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    val transactions = when {
                        query.isBlank() -> repository.getSortedTransactionTuples()
                        query.isDigitsOnly() -> repository.getFilteredTransactionTuples(query, "")
                        else -> repository.getFilteredTransactionTuples("", query)
                    }
                    transactions.asFlow()
                }
                .collect { transactions ->
                    _state.update { current ->
                        val uiTransactions = transactions.map { transaction ->
                            val isSelected = transaction.id in current.selectedTransactions
                            transaction.toTransactionUi(isSelected = isSelected)
                        }
                        current.copy(transactions = uiTransactions)
                    }
                }
        }
    }

    fun onCreate(selectedIds: List<Long>, query: String) = viewModelScope.launch {
        _state.update {
            it.copy(
                selectedTransactions = selectedIds,
                isSelectionMode = selectedIds.isNotEmpty(),
                transactions = it.transactions.map { transaction ->
                    transaction.copy(isSelected = transaction.id in selectedIds)
                },
                query = query,
            )
        }
        if (Chucker.showNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            _events.emit(MainActivityEvents.RequestNotificationPermissions)
        }
    }

    fun onQueryChanged(text: String) = viewModelScope.launch {
        _state.update { it.copy(query = text) }
    }

    fun onClearTransactionsClick() = viewModelScope.launch {
        _events.emit(MainActivityEvents.ShowClearTransactionsDialog)
    }

    fun onConfirmClearTransactions() = viewModelScope.launch {
        val ids = selectedTransactionIds
        if (ids.isNotEmpty()) {
            repository.deleteSelectedTransactions(ids)
        } else {
            repository.deleteAllTransactions()
        }
        _state.update { it.copy(selectedTransactions = emptyList(), isSelectionMode = false) }
        NotificationHelper.clearBuffer()
    }

    fun onTransactionClick(transaction: TransactionUi) {
        if (_state.value.isSelectionMode) {
            toggleTransactionSelection(transaction.id)
        } else {
            viewModelScope.launch {
                _events.emit(MainActivityEvents.NavigateToTransaction(transaction.id))
            }
        }
    }

    fun onTransactionLongClick(transaction: TransactionUi) {
        toggleTransactionSelection(transaction.id)
    }

    fun clearSelection() {
        _state.update { current ->
            current.copy(
                selectedTransactions = emptyList(),
                isSelectionMode = false,
                transactions = current.transactions.map { it.copy(isSelected = false) },
            )
        }
    }

    private fun toggleTransactionSelection(transactionId: Long) {
        _state.update { current ->
            val updatedIds = current.selectedTransactions.toMutableList().apply {
                if (transactionId in this) remove(transactionId) else add(transactionId)
            }
            current.copy(
                selectedTransactions = updatedIds,
                isSelectionMode = updatedIds.isNotEmpty(),
                transactions = current.transactions.map { item ->
                    item.copy(isSelected = item.id in updatedIds)
                },
            )
        }
    }

    fun onShareTextClick() = viewModelScope.launch {
        val event = if (selectedTransactionIds.isEmpty()) {
            MainActivityEvents.ShowShareTextAllDialog
        } else {
            MainActivityEvents.ShowShareTextSelectedDialog
        }
        _events.emit(event)
    }

    fun onShareHarClick() = viewModelScope.launch {
        _events.emit(
            if (selectedTransactionIds.isEmpty()) MainActivityEvents.ShowShareHarAllDialog
            else MainActivityEvents.ShowShareHarSelectedDialog,
        )
    }

    fun onConfirmExportText() = viewModelScope.launch {
        val selectedTransactions = selectedTransactionIds
        val transactions = if (selectedTransactions.isNotEmpty()) {
            repository.getSelectedTransactions(selectedTransactions)
        } else {
            repository.getAllTransactions()
        }
        if (transactions.isEmpty()) {
            _events.emit(MainActivityEvents.ShowToast(R.string.chucker_export_empty_text))
            return@launch
        }
        _events.emit(
            MainActivityEvents.Share(
                TransactionListDetailsSharable(transactions, encodeUrls = false),
                ExportType.TEXT.fileName,
            ),
        )
    }

    fun onConfirmExportHar() = viewModelScope.launch {
        val selectedTransactions = selectedTransactionIds
        val transactions = if (selectedTransactions.isNotEmpty()) {
            repository.getSelectedTransactions(selectedTransactions)
        } else {
            repository.getAllTransactions()
        }
        if (transactions.isEmpty()) {
            _events.emit(MainActivityEvents.ShowToast(R.string.chucker_export_empty_text))
            return@launch
        }
        val content = HarUtils.harStringFromTransactions(
            transactions,
            getApplication<Application>().getString(R.string.chucker_name),
            getApplication<Application>().getString(R.string.chucker_version),
        )
        val sharable = TransactionDetailsHarSharable(content)
        _events.emit(MainActivityEvents.Share(sharable, ExportType.HAR.fileName))
    }

    fun onSaveTextClick() = viewModelScope.launch {
        _events.emit(MainActivityEvents.ShowSaveTextDialog)
    }

    fun onSaveHarClick() = viewModelScope.launch {
        _events.emit(MainActivityEvents.ShowSaveHarDialog)
    }

    fun onConfirmSave(type: ExportType) = viewModelScope.launch {
        _events.emit(MainActivityEvents.LaunchSaveDocument(type))
    }

    fun onSaveDocumentResult(uri: Uri?, type: ExportType) = viewModelScope.launch {
        if (uri == null) {
            _events.emit(MainActivityEvents.ShowToast(R.string.chucker_save_failed_to_open_document))
            return@launch
        }
        val source = prepareDataToSave(type)
        if (source == null) {
            _events.emit(MainActivityEvents.ShowToast(R.string.chucker_save_empty_text))
            return@launch
        }
        val saved = FileSaver.saveFile(source, uri, getApplication<Application>().contentResolver)
        val text = if (saved) R.string.chucker_file_saved else R.string.chucker_file_not_saved
        _events.emit(MainActivityEvents.ShowToast(text))
    }

    private suspend fun prepareDataToSave(type: ExportType): Source? {
        val ids = selectedTransactionIds
        val transactions =
            if (ids.isNotEmpty()) repository.getSelectedTransactions(ids) else repository.getAllTransactions()
        if (transactions.isEmpty()) return null
        val context = getApplication<Application>()
        return when (type) {
            ExportType.TEXT -> TransactionListDetailsSharable(
                transactions,
                encodeUrls = false
            ).toSharableContent(context)

            ExportType.HAR -> HarUtils.harStringFromTransactions(
                transactions,
                context.getString(R.string.chucker_name),
                context.getString(R.string.chucker_version),
            ).byteInputStream().source().buffer()
        }
    }

}

internal sealed interface MainActivityEvents {
    data object RequestNotificationPermissions : MainActivityEvents
    data object ShowClearTransactionsDialog : MainActivityEvents
    data class NavigateToTransaction(val transactionId: Long) : MainActivityEvents
    data object ShowShareTextAllDialog : MainActivityEvents
    data object ShowShareTextSelectedDialog : MainActivityEvents
    data object ShowShareHarAllDialog : MainActivityEvents
    data object ShowShareHarSelectedDialog : MainActivityEvents
    data object ShowSaveTextDialog : MainActivityEvents
    data object ShowSaveHarDialog : MainActivityEvents
    data class LaunchSaveDocument(val type: ExportType) : MainActivityEvents

    data class Share(val data: Sharable, val fileName: String) : MainActivityEvents

    data class ShowToast(@androidx.annotation.StringRes val stringId: Int) : MainActivityEvents
}

internal enum class ExportType(val mimeType: String, val fileName: String) {
    TEXT("text/plain", "transactions.txt"),
    HAR("application/har+json", "transactions.har"),
}

internal data class MainActivityState(
    val transactions: List<TransactionUi> = emptyList(),
    val selectedTransactions: List<Long> = emptyList(),
    val isSelectionMode: Boolean = false,
    val query: String = "",
)

@Suppress("LongParameterList")
internal data class TransactionUi(
    val id: Long,
    val requestDate: String,
    val tookMs: Long?,
    val protocol: String?,
    val host: String?,
    val path: String?,
    val scheme: String?,
    val responseCode: Int?,
    val requestPayloadSize: Long?,
    val responsePayloadSize: Long?,
    val error: String?,
    val isGraphQl: Boolean = false,
    val graphQlOperationName: String?,
    val isSelected: Boolean = false,
    val isSsl: Boolean,
    val status: HttpTransaction.Status,
    val durationString: String?,
    val totalSizeString: String,
    val formattedPath: String,
)

private fun HttpTransactionTuple.toTransactionUi(isSelected: Boolean) =
    TransactionUi(
        id = id,
        requestDate = DateFormat.getTimeInstance().format(requestDate),
        tookMs = tookMs,
        protocol = protocol,
        host = host,
        path = "$method $path",
        scheme = scheme,
        responseCode = responseCode,
        requestPayloadSize = requestPayloadSize,
        responsePayloadSize = responsePayloadSize,
        error = error,
        isGraphQl = graphQlDetected,
        graphQlOperationName = graphQlOperationName,
        isSelected = isSelected,
        isSsl = scheme.equals("https", ignoreCase = true),
        status = when {
            error != null -> HttpTransaction.Status.Failed
            responseCode == null -> HttpTransaction.Status.Requested
            else -> HttpTransaction.Status.Complete
        },
        durationString = tookMs?.toDuration(DurationUnit.MILLISECONDS)?.toString(),
        totalSizeString = FormatUtils.formatByteCount(
            (requestPayloadSize ?: 0) + (responsePayloadSize ?: 0),
            true,
        ),
        formattedPath = path?.let { transactionPath ->
            "https://www.example.com$transactionPath".toHttpUrlOrNull()?.let { url ->
                FormattedUrl.fromHttpUrl(url, encoded = false).pathWithQuery
            }
        }.orEmpty(),
    )
