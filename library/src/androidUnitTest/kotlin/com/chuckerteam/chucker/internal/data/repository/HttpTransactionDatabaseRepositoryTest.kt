package com.chuckerteam.chucker.internal.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chuckerteam.chucker.internal.data.entity.assertTuples
import com.chuckerteam.chucker.internal.data.entity.createRequest
import com.chuckerteam.chucker.internal.data.entity.randomString
import com.chuckerteam.chucker.internal.data.entity.withResponseData
import com.chuckerteam.chucker.internal.data.room.ChuckerDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class HttpTransactionDatabaseRepositoryTest {
    private lateinit var db: ChuckerDatabase
    private lateinit var testObject: HttpTransactionDatabaseRepository

    private val transaction =
        createRequest().apply {
            id = 123456L
            host = randomString()
            requestDate = 100
        }
    private val otherTransaction =
        createRequest().apply {
            id = 234567L
            host = randomString()
            requestDate = 200
        }

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db =
            Room
                .inMemoryDatabaseBuilder(context, ChuckerDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        testObject = HttpTransactionDatabaseRepository(db)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `get a single transaction`() =
        runBlocking {
            val data = createRequest().apply { id = 123L }
            db.transactionDao().insert(data)

            val result = testObject.getTransaction(data.id).first()
            assertThat(result?.id).isEqualTo(data.id)
            assertThat(result?.requestHeaders).isEqualTo(data.requestHeaders)
            assertThat(result?.url).isEqualTo(data.url)
            assertThat(result?.host).isEqualTo(data.host)
            assertThat(result?.path).isEqualTo(data.path)
            assertThat(result?.scheme).isEqualTo(data.scheme)
            assertThat(result?.isRequestBodyEncoded).isEqualTo(data.isRequestBodyEncoded)
            assertThat(result?.requestDate).isEqualTo(data.requestDate)
            assertThat(result?.method).isEqualTo(data.method)
            assertThat(result?.requestContentType).isEqualTo(data.requestContentType)
            assertThat(result?.requestPayloadSize).isEqualTo(data.requestPayloadSize)
        }

    @Test
    fun `insert a single transaction`() =
        runBlocking {
            val data = createRequest()

            testObject.insertTransaction(data)
            assertThat(data.id > 0).isTrue()

            val result = testObject.getTransaction(data.id).first()
            assertThat(result?.id).isEqualTo(data.id)
            assertThat(result?.requestHeaders).isEqualTo(data.requestHeaders)
            assertThat(result?.url).isEqualTo(data.url)
            assertThat(result?.host).isEqualTo(data.host)
            assertThat(result?.path).isEqualTo(data.path)
            assertThat(result?.scheme).isEqualTo(data.scheme)
            assertThat(result?.isRequestBodyEncoded).isEqualTo(data.isRequestBodyEncoded)
            assertThat(result?.requestDate).isEqualTo(data.requestDate)
            assertThat(result?.method).isEqualTo(data.method)
            assertThat(result?.requestContentType).isEqualTo(data.requestContentType)
            assertThat(result?.requestPayloadSize).isEqualTo(data.requestPayloadSize)
        }

    @Test
    fun `get all transactions`() =
        runBlocking {
            testObject.insertTransaction(transaction)
            testObject.insertTransaction(otherTransaction)

            val result = testObject.getAllTransactions()
            val first = result.firstOrNull { it.id == transaction.id }
            assertThat(first).isNotNull()
            assertThat(first?.host).isEqualTo(transaction.host)

            val second = result.firstOrNull { it.id == otherTransaction.id }
            assertThat(second).isNotNull()
            assertThat(second?.host).isEqualTo(otherTransaction.host)
        }

    @Test
    fun `get sorted transaction tuples`() =
        runBlocking {
            testObject.insertTransaction(transaction)
            testObject.insertTransaction(otherTransaction)

            assertTuples(listOf(otherTransaction, transaction), testObject.getSortedTransactionTuples().first())
        }

    @Test
    fun `transaction tuples are not filtered with empty inputs`() =
        runBlocking {
            val transactionOne =
                createRequest("abc").withResponseData().apply {
                    requestDate = 200L
                }
            val transactionTwo =
                createRequest("abcdef").withResponseData().apply {
                    requestDate = 100L
                }
            val transactionThree =
                createRequest("def").withResponseData().apply {
                    requestDate = 300L
                }

            testObject.insertTransaction(transactionOne)
            testObject.insertTransaction(transactionTwo)
            testObject.insertTransaction(transactionThree)

            assertTuples(
                listOf(transactionThree, transactionOne, transactionTwo),
                testObject.getFilteredTransactionTuples(code = "", path = "").first(),
            )
        }

    @Test
    fun `transaction tuples are filtered by path`() =
        runBlocking {
            val transactionOne =
                createRequest("abc").withResponseData().apply {
                    requestDate = 200L
                }
            val transactionTwo =
                createRequest("abcdef").withResponseData().apply {
                    requestDate = 100L
                }
            val transactionThree =
                createRequest("def").withResponseData().apply {
                    requestDate = 300L
                }

            testObject.insertTransaction(transactionOne)
            testObject.insertTransaction(transactionTwo)
            testObject.insertTransaction(transactionThree)

            assertTuples(
                listOf(transactionThree, transactionTwo),
                testObject.getFilteredTransactionTuples(code = "", path = "def").first(),
            )
        }

    @Test
    fun `transaction tuples are filtered by code`() =
        runBlocking {
            val transactionOne =
                createRequest("abc").withResponseData().apply {
                    requestDate = 200L
                    responseCode = 418
                }
            val transactionTwo =
                createRequest("abcdef").withResponseData().apply {
                    requestDate = 100L
                    responseCode = 200
                }
            val transactionThree =
                createRequest("def").withResponseData().apply {
                    requestDate = 300L
                    responseCode = 400
                }

            testObject.insertTransaction(transactionOne)
            testObject.insertTransaction(transactionTwo)
            testObject.insertTransaction(transactionThree)

            assertTuples(
                listOf(transactionThree, transactionOne),
                testObject.getFilteredTransactionTuples(code = "4", path = "").first(),
            )
        }

    @Test
    fun `transaction tuples are filtered by qraphQlOperationName`() =
        runBlocking {
            val transactionOne =
                createRequest("abc").withResponseData().apply {
                    requestDate = 200L
                    responseCode = 418
                }
            val transactionTwo =
                createRequest("abcdef").withResponseData().apply {
                    requestDate = 100L
                    responseCode = 200
                }
            val transactionThree =
                createRequest("def").withResponseData().apply {
                    requestDate = 300L
                    responseCode = 400
                }
            val transactionFour =
                createRequest("def").withResponseData().apply {
                    requestDate = 400L
                    responseCode = 200
                    graphQlOperationName = "GetDefQuery"
                }
            testObject.insertTransaction(transactionOne)
            testObject.insertTransaction(transactionTwo)
            testObject.insertTransaction(transactionThree)
            testObject.insertTransaction(transactionFour)
            assertTuples(
                listOf(transactionFour),
                testObject.getFilteredTransactionTuples(code = "", path = "GetDe").first(),
            )
        }

    @Test
    fun `transaction tuples are not filtered by qraphQlOperationName when graphQLQuery is empty`() =
        runBlocking {
            val transactionOne =
                createRequest("abc").withResponseData().apply {
                    requestDate = 200L
                    responseCode = 418
                }
            val transactionTwo =
                createRequest("abcdef").withResponseData().apply {
                    requestDate = 100L
                    responseCode = 200
                }
            val transactionThree =
                createRequest("def").withResponseData().apply {
                    requestDate = 300L
                    responseCode = 400
                }
            val transactionFour =
                createRequest("graphql").withResponseData().apply {
                    requestDate = 400L
                    responseCode = 200
                    graphQlOperationName = "GetDefQuery"
                }
            testObject.insertTransaction(transactionOne)
            testObject.insertTransaction(transactionTwo)
            testObject.insertTransaction(transactionThree)
            testObject.insertTransaction(transactionFour)
            assertTuples(
                listOf(transactionFour, transactionThree, transactionOne, transactionTwo),
                testObject.getFilteredTransactionTuples(code = "", path = "").first(),
            )
        }

    @Test
    fun `delete all transactions`() =
        runBlocking {
            testObject.insertTransaction(transaction)
            testObject.insertTransaction(otherTransaction)

            val result = testObject.getAllTransactions()
            assertThat(result.size).isEqualTo(2)

            testObject.deleteAllTransactions()
            val empty = testObject.getAllTransactions()
            assertThat(empty.isEmpty()).isTrue()
        }

    @Test
    fun `delete old transactions`() =
        runBlocking {
            testObject.insertTransaction(transaction)
            testObject.insertTransaction(otherTransaction)

            val result = testObject.getAllTransactions()
            assertThat(result.size).isEqualTo(2)

            testObject.deleteOldTransactions(150L)

            val secondResult = testObject.getAllTransactions()
            assertThat(secondResult.size).isEqualTo(1)
            val second = secondResult.firstOrNull { it.id == otherTransaction.id }
            assertThat(second).isNotNull()
            assertThat(second?.host).isEqualTo(otherTransaction.host)
        }

    @Test
    fun `update a transaction`() =
        runBlocking {
            testObject.insertTransaction(transaction)

            val newHost = randomString()
            transaction.host = newHost
            testObject.updateTransaction(transaction)

            val result = testObject.getTransaction(123456L).first()
            assertThat(result?.id).isEqualTo(transaction.id)
            assertThat(result?.host).isEqualTo(newHost)
        }
}
