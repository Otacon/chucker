package com.chuckerteam.chucker.internal.ui

import android.app.Application
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.chuckerteam.chucker.R
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.data.entity.HttpTransactionTuple
import com.chuckerteam.chucker.internal.data.repository.HttpTransactionRepository
import com.chuckerteam.chucker.internal.data.repository.RepositoryProvider
import com.chuckerteam.chucker.internal.support.FileSaver
import com.chuckerteam.chucker.internal.support.HarUtils
import com.chuckerteam.chucker.internal.support.NotificationHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Suppress(
    "standard:multiline-expression-wrapping",
    "standard:chain-method-continuation",
    "standard:function-signature"
)
internal class MainActivityViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: HttpTransactionRepository
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var events: Channel<MainActivityEvents>
    private lateinit var eventCollector: Job
    private val sortedTuples = MutableLiveData<List<HttpTransactionTuple>>(emptyList())
    private val digitFilteredTuples = MutableLiveData<List<HttpTransactionTuple>>(emptyList())
    private val textFilteredTuples = MutableLiveData<List<HttpTransactionTuple>>(emptyList())
    private val application: Application = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        every { repository.getSortedTransactionTuples() } returns sortedTuples
        every { repository.getFilteredTransactionTuples(any(), any()) } answers {
            if (firstArg<String>().isEmpty()) textFilteredTuples else digitFilteredTuples
        }
        mockkObject(RepositoryProvider)
        every { RepositoryProvider.transaction() } returns repository
        mockkObject(NotificationHelper)
        every { NotificationHelper.clearBuffer() } just runs

        viewModel = MainActivityViewModel(application)
        events = Channel(Channel.UNLIMITED)

        eventCollector = CoroutineScope(SupervisorJob() + dispatcher).launch {
            viewModel.events.collect { events.send(it) }
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        eventCollector.cancel()
        unmockkAll()
        events.close()
    }

    @Test
    fun `initial screen loads sorted transactions including empty result`() =
        runTest(dispatcher) {
            val tuple = tuple(id = 21L)
            sortedTuples.value = listOf(tuple)

            advanceUntilIdle()

            assertEquals(listOf(21L), viewModel.state.value.transactions.map { it.id })
            assertFalse(viewModel.state.value.isSelectionMode)
            verify { repository.getSortedTransactionTuples() }

            sortedTuples.value = emptyList()
            advanceUntilIdle()
            assertTrue(viewModel.state.value.transactions.isEmpty())
        }

    @Test
    fun `query changes select numeric and text filtered repository sources`() =
        runTest(dispatcher) {
            digitFilteredTuples.value = listOf(tuple(id = 10L))
            textFilteredTuples.value = listOf(tuple(id = 11L))

            viewModel.onQueryChanged("404")
            advanceUntilIdle()
            assertEquals(listOf(10L), viewModel.state.value.transactions.map { it.id })
            verify { repository.getFilteredTransactionTuples("404", "") }

            viewModel.onQueryChanged("example.org")
            advanceUntilIdle()
            assertEquals(listOf(11L), viewModel.state.value.transactions.map { it.id })
            verify { repository.getFilteredTransactionTuples("", "example.org") }
        }

    @Test
    fun `long press selects transaction click deselects it and back clears remaining selections`() =
        runTest(dispatcher) {
            sortedTuples.value = listOf(tuple(id = 1), tuple(id = 2))
            advanceUntilIdle()
            val first = viewModel.state.value.transactions.first()
            val second = viewModel.state.value.transactions.last()

            viewModel.onTransactionLongClick(first)
            assertEquals(listOf(1L), viewModel.selectedTransactionIds)
            assertTrue(viewModel.state.value.isSelectionMode)
            assertTrue(viewModel.state.value.transactions.first { it.id == 1L }.isSelected)

            viewModel.onTransactionClick(second)
            assertEquals(listOf(1L, 2L), viewModel.selectedTransactionIds)
            viewModel.onTransactionClick(first)
            assertEquals(listOf(2L), viewModel.selectedTransactionIds)

            viewModel.clearSelection()
            assertTrue(viewModel.selectedTransactionIds.isEmpty())
            assertFalse(viewModel.state.value.isSelectionMode)
            assertTrue(viewModel.state.value.transactions.none { it.isSelected })
        }

    @Test
    fun `restoring selected IDs marks already loaded rows selected`() = runTest(dispatcher) {
        sortedTuples.value = listOf(tuple(id = 3), tuple(id = 4))
        advanceUntilIdle()

        viewModel.onCreate(selectedIds = listOf(4L), query = "")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isSelectionMode)
        assertTrue(viewModel.state.value.transactions.single { it.id == 4L }.isSelected)
        assertFalse(viewModel.state.value.transactions.single { it.id == 3L }.isSelected)
    }

    @Test
    fun `unselected transaction click emits navigation event`() = runTest(dispatcher) {
        viewModel.onTransactionClick(transactionUi(id = 42L))
        advanceUntilIdle()

        assertEquals(MainActivityEvents.NavigateToTransaction(42L), events.receive())
        assertFalse(viewModel.state.value.isSelectionMode)
    }

    @Test
    fun `share actions show appropriate confirmation according to selection`() =
        runTest(dispatcher) {
            viewModel.onShareTextClick()
            advanceUntilIdle()
            assertEquals(MainActivityEvents.ShowShareTextAllDialog, events.receive())

            viewModel.onTransactionLongClick(transactionUi(7L))
            viewModel.onShareTextClick()
            viewModel.onShareHarClick()
            advanceUntilIdle()
            assertEquals(MainActivityEvents.ShowShareTextSelectedDialog, events.receive())
            assertEquals(MainActivityEvents.ShowShareHarSelectedDialog, events.receive())
        }

    @Test
    fun `sharing with no transactions shows empty export message`() = runTest(dispatcher) {
        coEvery { repository.getAllTransactions() } returns emptyList()

        viewModel.onConfirmExportText()
        viewModel.onConfirmExportHar()
        advanceUntilIdle()

        assertEquals(
            MainActivityEvents.ShowToast(R.string.chucker_export_empty_text),
            events.receive()
        )
        assertEquals(
            MainActivityEvents.ShowToast(R.string.chucker_export_empty_text),
            events.receive()
        )
        coVerify(exactly = 2) { repository.getAllTransactions() }
    }

    @Test
    fun `export uses selected transactions when selection exists`() = runTest(dispatcher) {
        val transaction = mockk<HttpTransaction>(relaxed = true)
        coEvery { repository.getSelectedTransactions(listOf(5L)) } returns listOf(transaction)
        coEvery { repository.getAllTransactions() } returns emptyList()
        viewModel.onTransactionLongClick(transactionUi(5L))

        viewModel.onConfirmExportText()
        advanceUntilIdle()

        val event = events.receive()
        assertTrue(event is MainActivityEvents.Share)
        val shareEvent = event as MainActivityEvents.Share
        assertEquals(ExportType.TEXT.fileName, shareEvent.fileName)
        coVerify { repository.getSelectedTransactions(listOf(5L)) }
        coVerify(exactly = 0) { repository.getAllTransactions() }
    }

    @Test
    fun `HAR export builds a share event from all transactions`() = runTest(dispatcher) {
        val transaction = mockk<HttpTransaction>(relaxed = true)
        coEvery { repository.getAllTransactions() } returns listOf(transaction)
        mockkObject(HarUtils)
        coEvery {
            HarUtils.harStringFromTransactions(any(), any(), any())
        } returns "{\"log\":{}}"

        viewModel.onConfirmExportHar()
        advanceUntilIdle()

        val event = events.receive()
        assertTrue(event is MainActivityEvents.Share)
        assertEquals(ExportType.HAR.fileName, (event as MainActivityEvents.Share).fileName)
        coVerify { HarUtils.harStringFromTransactions(listOf(transaction), any(), any()) }
    }

    @Test
    fun `clear deletes only selected records and exits selection mode`() = runTest(dispatcher) {
        coEvery { repository.deleteSelectedTransactions(listOf(2L)) } just runs
        viewModel.onTransactionLongClick(transactionUi(2L))

        viewModel.onConfirmClearTransactions()
        advanceUntilIdle()

        coVerify { repository.deleteSelectedTransactions(listOf(2L)) }
        coVerify(exactly = 0) { repository.deleteAllTransactions() }
        verify { NotificationHelper.clearBuffer() }
        assertFalse(viewModel.state.value.isSelectionMode)
        assertTrue(viewModel.selectedTransactionIds.isEmpty())
    }

    @Test
    fun `save requests document picker and null result reports cancelled picker`() =
        runTest(dispatcher) {
            viewModel.onConfirmSave(ExportType.HAR)
            advanceUntilIdle()
            assertEquals(MainActivityEvents.LaunchSaveDocument(ExportType.HAR), events.receive())

            viewModel.onSaveDocumentResult(null, ExportType.HAR)
            advanceUntilIdle()
            assertEquals(
                MainActivityEvents.ShowToast(R.string.chucker_save_failed_to_open_document),
                events.receive()
            )
        }

    @Test
    fun `saving with no transactions reports empty save and does not invoke file saver`() =
        runTest(dispatcher) {
            mockkObject(FileSaver)
            coEvery { repository.getAllTransactions() } returns emptyList()

            viewModel.onSaveDocumentResult(Uri.parse("content://destination"), ExportType.TEXT)
            advanceUntilIdle()

            assertEquals(
                MainActivityEvents.ShowToast(R.string.chucker_save_empty_text),
                events.receive()
            )
            coVerify(exactly = 0) { FileSaver.saveFile(any(), any(), any()) }
        }

    private fun tuple(id: Long) = HttpTransactionTuple(
        id = id,
        requestDate = 1L,
        tookMs = 12L,
        protocol = "HTTP/1.1",
        method = "GET",
        host = "example.org",
        path = "/$id",
        scheme = "https",
        responseCode = 200,
        requestPayloadSize = 3L,
        responsePayloadSize = 4L,
        error = null,
        graphQlDetected = false,
        graphQlOperationName = null,
    )

    private fun transactionUi(id: Long) = tuple(id).let { tuple ->
        TransactionUi(
            id = tuple.id,
            requestDate = "time",
            tookMs = tuple.tookMs,
            protocol = tuple.protocol,
            host = tuple.host,
            path = tuple.path,
            scheme = tuple.scheme,
            responseCode = tuple.responseCode,
            requestPayloadSize = tuple.requestPayloadSize,
            responsePayloadSize = tuple.responsePayloadSize,
            error = tuple.error,
            graphQlOperationName = tuple.graphQlOperationName,
            isSsl = true,
            status = HttpTransaction.Status.Complete,
            durationString = "12 ms",
            totalSizeString = "7 B",
            formattedPath = tuple.path.orEmpty(),
        )
    }
}
