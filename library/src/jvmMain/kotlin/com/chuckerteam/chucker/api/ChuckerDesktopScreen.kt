@file:Suppress("ktlint:standard:function-naming")

package com.chuckerteam.chucker.api

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.data.entity.HttpTransactionTuple
import com.chuckerteam.chucker.internal.data.repository.RepositoryProvider
import java.text.DateFormat
import java.util.Date

private val ChuckerBackground = Color(0xFFFAFAFA)
private val ChuckerSurface = Color.White
private val ChuckerText = Color(0xFF212121)
private val ChuckerSecondaryText = Color(0xFF616161)
private val ChuckerDivider = Color(0xFFE0E0E0)
private val ChuckerBlue = Color(0xFF1976D2)

@Composable
public fun ChuckerDesktopScreen() {
    initializeDesktopRepository()
    val transactions by RepositoryProvider
        .transaction()
        .getSortedTransactionTuples()
        .collectAsState(initial = emptyList())
    var selectedTransactionId by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().background(ChuckerBackground),
    ) {
        if (selectedTransactionId == null) {
            ScreenHeader(title = "Chucker")
            if (transactions.isEmpty()) {
                EmptyTransactions()
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(transactions, key = { it.id }) { transaction ->
                        TransactionRow(transaction) { selectedTransactionId = transaction.id }
                        Divider()
                    }
                }
            }
        } else {
            val transaction by rememberTransaction(selectedTransactionId!!)
            ScreenHeader(title = "Transaction details", onBack = { selectedTransactionId = null })
            if (transaction == null) {
                EmptyTransactions(message = "Transaction no longer exists")
            } else {
                TransactionDetails(transaction!!)
            }
        }
    }
}

@Composable
private fun rememberTransaction(transactionId: Long): androidx.compose.runtime.State<HttpTransaction?> {
    val transactionFlow =
        remember(transactionId) {
            RepositoryProvider.transaction().getTransaction(transactionId)
        }
    return transactionFlow.collectAsState(initial = null)
}

@Composable
private fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(ChuckerSurface).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            BasicText("‹  Back", modifier = Modifier.clickable(onClick = onBack), style = textStyle(color = ChuckerBlue))
            Spacer(Modifier.width(20.dp))
        }
        BasicText(title, style = textStyle(size = 20, weight = FontWeight.SemiBold))
    }
    Divider()
}

@Composable
private fun EmptyTransactions(message: String = "No API calls yet") {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText(message, style = textStyle(size = 18, color = ChuckerSecondaryText))
        Spacer(Modifier.height(8.dp))
        if (message == "No API calls yet") {
            BasicText("Requests captured by Chucker will appear here.", style = textStyle(color = ChuckerSecondaryText))
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: HttpTransactionTuple,
    onClick: () -> Unit,
) {
    val status =
        when (transaction.status) {
            HttpTransaction.Status.Complete -> transaction.responseCode?.toString().orEmpty()
            HttpTransaction.Status.Failed -> "!!!"
            HttpTransaction.Status.Requested -> "…"
        }
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            BasicText(status, modifier = Modifier.width(56.dp), style = textStyle(color = statusColor(transaction)))
            BasicText(
                "${transaction.method.orEmpty()} ${transaction.getFormattedPath(encode = false)}",
                modifier = Modifier.weight(1f),
                style = textStyle(weight = FontWeight.Medium),
            )
        }
        Row(modifier = Modifier.padding(start = 56.dp, top = 5.dp)) {
            BasicText(if (transaction.isSsl) "🔒" else "○", style = textStyle(color = ChuckerSecondaryText))
            Spacer(Modifier.width(6.dp))
            BasicText(transaction.host.orEmpty(), style = textStyle(color = ChuckerSecondaryText))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 56.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BasicText(
                transaction.requestDate
                    ?.let {
                        DateFormat.getTimeInstance().format(Date(it))
                    }.orEmpty(),
                style = textStyle(color = ChuckerSecondaryText, size = 12),
            )
            BasicText(transaction.durationString.orEmpty(), style = textStyle(color = ChuckerSecondaryText, size = 12))
            BasicText(transaction.totalSizeString, style = textStyle(color = ChuckerSecondaryText, size = 12))
        }
    }
}

@Composable
private fun TransactionDetails(transaction: HttpTransaction) {
    var selectedTab by remember(transaction.id) { mutableStateOf("Overview") }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().background(ChuckerSurface)) {
            listOf("Overview", "Request", "Response").forEach { tab ->
                Column(
                    modifier = Modifier.weight(1f).clickable { selectedTab = tab }.padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BasicText(
                        tab,
                        style =
                            textStyle(
                                color =
                                    if (selectedTab ==
                                        tab
                                    ) {
                                        ChuckerBlue
                                    } else {
                                        ChuckerSecondaryText
                                    },
                                weight = FontWeight.Medium,
                            ),
                    )
                    if (selectedTab == tab) {
                        Spacer(Modifier.height(8.dp))
                        Spacer(Modifier.fillMaxWidth().height(2.dp).background(ChuckerBlue))
                    }
                }
            }
        }
        Divider()
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            when (selectedTab) {
                "Overview" -> OverviewDetails(transaction)
                "Request" ->
                    PayloadDetails(
                        headers = transaction.getRequestHeadersString(withMarkup = false),
                        body = transaction.requestBody,
                        contentType = transaction.requestContentType,
                    )
                else ->
                    PayloadDetails(
                        headers = transaction.getResponseHeadersString(withMarkup = false),
                        body = transaction.responseBody,
                        contentType = transaction.responseContentType,
                    )
            }
        }
    }
}

@Composable
private fun OverviewDetails(transaction: HttpTransaction) {
    DetailSection("Request") {
        DetailField("URL", transaction.getFormattedUrl(encode = false))
        DetailField("Host IP", transaction.hostIp)
        DetailField("Method", transaction.method)
        DetailField("Protocol", transaction.protocol)
        DetailField("SSL", if (transaction.isSsl) "Yes" else "No")
    }
    DetailSection("Response") {
        DetailField("Status", transaction.responseSummaryText ?: transaction.status.toString())
        DetailField("TLS version", transaction.responseTlsVersion)
        DetailField("Cipher suite", transaction.responseCipherSuite)
    }
    DetailSection("Timing & size") {
        DetailField("Request time", transaction.requestDateString)
        DetailField("Response time", transaction.responseDateString)
        DetailField("Duration", transaction.durationString)
        DetailField("Request size", transaction.requestSizeString)
        DetailField("Response size", transaction.responseSizeString)
        DetailField("Total size", transaction.totalSizeString)
    }
}

@Composable
private fun PayloadDetails(
    headers: String,
    body: String?,
    contentType: String?,
) {
    DetailSection("Headers") {
        if (headers.isBlank()) DetailField("", "No headers") else CodeBlock(headers)
    }
    DetailSection("Body${contentType?.let { " · $it" }.orEmpty()}") {
        if (body.isNullOrBlank()) DetailField("", "No body") else CodeBlock(body)
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, ChuckerDivider)
                .background(ChuckerSurface),
    ) {
        BasicText(
            title,
            modifier = Modifier.fillMaxWidth().background(Color(0xFFF2F2F2)).padding(12.dp),
            style = textStyle(weight = FontWeight.SemiBold),
        )
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun DetailField(
    label: String,
    value: String?,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) BasicText(label, modifier = Modifier.width(130.dp), style = textStyle(color = ChuckerSecondaryText))
        BasicText(value ?: "—", modifier = Modifier.weight(1f), style = textStyle())
    }
}

@Composable
private fun CodeBlock(value: String) {
    BasicText(
        value,
        modifier = Modifier.fillMaxWidth().background(Color(0xFFF7F7F7)).padding(10.dp),
        style = textStyle(family = FontFamily.Monospace, size = 12),
    )
}

@Composable
private fun Divider() {
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(ChuckerDivider))
}

private fun textStyle(
    color: Color = ChuckerText,
    size: Int = 14,
    weight: FontWeight = FontWeight.Normal,
    family: FontFamily? = null,
) = androidx.compose.ui.text
    .TextStyle(color = color, fontSize = size.sp, fontWeight = weight, fontFamily = family)

private fun statusColor(transaction: HttpTransactionTuple): Color =
    when (transaction.status) {
        HttpTransaction.Status.Complete -> if ((transaction.responseCode ?: 0) >= 400) Color(0xFFC62828) else Color(0xFF2E7D32)
        HttpTransaction.Status.Failed -> Color(0xFFC62828)
        HttpTransaction.Status.Requested -> Color(0xFFEF6C00)
    }
