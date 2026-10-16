package com.chuckerteam.chucker.internal.ui.transaction

import app.cash.turbine.test
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.data.repository.HttpTransactionRepository
import com.chuckerteam.chucker.internal.data.repository.RepositoryProvider
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
internal class TransactionViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @MockK
    private lateinit var transactionRepository: HttpTransactionRepository

    private val transaction = MutableSharedFlow<HttpTransaction?>(replay = 1)

    private lateinit var viewModel: TransactionViewModel

    private val mockTransactionId = 0L

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        every { transactionRepository.getTransaction(any()) } returns transaction
        mockkObject(RepositoryProvider)
        every { RepositoryProvider.transaction() } returns transactionRepository
        viewModel = TransactionViewModel(transactionId = mockTransactionId)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `encodeUrl defaults to false`() {
        assertFalse(viewModel.encodeUrl.value!!)
    }

    @Test
    fun `encodeUrl setter changes value`() {
        viewModel.encodeUrl(true)
        assertTrue(viewModel.encodeUrl.value!!)

        viewModel.encodeUrl(false)
        assertFalse(viewModel.encodeUrl.value!!)
    }

    @Test
    fun `switchUrlEncoding toggles encodeUrl`() {
        viewModel.switchUrlEncoding()
        assertTrue(viewModel.encodeUrl.value!!)

        viewModel.switchUrlEncoding()
        assertFalse(viewModel.encodeUrl.value!!)
    }

    @Test
    fun `transactionTitle formats raw path when encoding off`() {
        runTest {
            viewModel.transactionTitle.test {
                val txn = mockk<HttpTransaction>()
                every { txn.method } returns "POST"
                every { txn.getFormattedPath(encode = false) } returns "/foo?bar=1"
                transaction.emit(txn)
                assertEquals("POST /foo?bar=1", awaitItem())
            }
        }
    }

    @Test
    fun `transactionTitle formats encoded path when encoding on`() {
        runTest {
            viewModel.transactionTitle.test {
                val txn = mockk<HttpTransaction>()
                every { txn.method } returns "GET"
                every { txn.getFormattedPath(encode = false) } returns "/baz"
                every { txn.getFormattedPath(encode = true) } returns "/baz%20encoded"
                transaction.emit(txn)
                assertEquals("GET /baz", awaitItem())
                viewModel.encodeUrl(true)
                assertEquals("GET /baz%20encoded", awaitItem())
            }
        }
    }

    @Test
    fun `doesUrlRequireEncoding false when paths identical`() {
        runTest {
            viewModel.doesUrlRequireEncoding.test {
                val txn = mockk<HttpTransaction>()
                every { txn.getFormattedPath(true) } returns "/same"
                every { txn.getFormattedPath(false) } returns "/same"
                transaction.emit(txn)
                assertFalse(awaitItem())
            }
        }
    }

    @Test
    fun `doesUrlRequireEncoding true when paths differ`() {
        runTest {
            viewModel.doesUrlRequireEncoding.test {
                val txn = mockk<HttpTransaction>()
                every { txn.getFormattedPath(true) } returns "/enc"
                every { txn.getFormattedPath(false) } returns "/raw"
                transaction.emit(txn)
                assertTrue(awaitItem())
            }
        }
    }

    @Test
    fun `doesRequestBodyRequireEncoding handles null and non‐form content`() {
        runTest {
            viewModel.doesRequestBodyRequireEncoding.test {
                transaction.emit(mockk { every { requestContentType } returns null })
                assertFalse(awaitItem())
                transaction.emit(mockk { every { requestContentType } returns "application/json" })
                assertFalse(awaitItem())
            }
        }
    }

    @Test
    fun `doesRequestBodyRequireEncoding true for form-urlencoded`() {
        runTest {
            viewModel.doesRequestBodyRequireEncoding.test {
                transaction.emit(mockk { every { requestContentType } returns "application/x-www-form-urlencoded; charset=UTF-8" })
                assertTrue(awaitItem())
            }
        }
    }

    @Test
    fun `formatRequestBody true when bodyReq false, encodeUrl false`() {
        runTest {
            viewModel.formatRequestBody.test {
                viewModel.encodeUrl(false)
                transaction.emit(mockk { every { requestContentType } returns null })
                assertTrue(awaitItem())
            }
        }
    }

    @Test
    fun `formatRequestBody true when bodyReq false, encodeUrl true`() {
        runTest {
            viewModel.formatRequestBody.test {
                viewModel.encodeUrl(true)
                transaction.emit(mockk { every { requestContentType } returns null })
                assertTrue(awaitItem())
            }
        }
    }

    @Test
    fun `formatRequestBody true when bodyReq true, encodeUrl false`() {
        runTest {
            viewModel.formatRequestBody.test {
                viewModel.encodeUrl(false)
                transaction.emit(mockk { every { requestContentType } returns "x-www-form-urlencoded" })
                assertTrue(awaitItem())
            }
        }
    }

    @Test
    fun `formatRequestBody false when bodyReq true, encodeUrl true`() {
        runTest {
            viewModel.formatRequestBody.test {
                viewModel.encodeUrl(true)
                transaction.emit(mockk { every { requestContentType } returns "x-www-form-urlencoded" })
                assertFalse(awaitItem())
            }
        }
    }

    @Test
    fun `transaction exposes repository data as a flow`() {
        runTest {
            viewModel.transaction.test {
                transaction.emit(null)
                assertEquals(null, awaitItem())
            }
        }
    }
}
