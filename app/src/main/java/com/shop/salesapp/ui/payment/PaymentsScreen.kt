package com.shop.salesapp.ui.payment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.salesapp.CustomerCache
import com.shop.salesapp.DeletionRequest
import com.shop.salesapp.PaymentReceipt
import com.shop.salesapp.ShopCustomer
import com.shop.salesapp.ShopInvoice
import com.shop.salesapp.ui.common.AdminDeletionRequestsSection
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.SoftBlue
import com.shop.salesapp.ui.theme.Teal
import com.shop.salesapp.ui.theme.tr

@Composable
fun PaymentsScreen(
    padding: PaddingValues,
    onBack: () -> Unit,
    bills: List<ShopInvoice>,
    customers: List<ShopCustomer>,
    paymentsMade: List<PaymentReceipt>,
    listTab: String,
    query: String,
    loading: Boolean,
    connected: Boolean,
    isAdministrator: Boolean,
    requests: List<DeletionRequest>,
    onRequestDeletion: (PaymentReceipt) -> Unit,
    onReviewRequest: (DeletionRequest, Boolean) -> Unit,
    onQuery: (String) -> Unit,
    onListTab: (String) -> Unit,
    onSync: () -> Unit,
    onPickPhoneContact: () -> Unit,
    onCustomer: (ShopCustomer) -> Unit,
    onRetryPayment: (PaymentReceipt) -> Unit,
    onStartCollect: () -> Unit
) {
    var showVoidedPayments by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var savedCustomerQuery by remember { mutableStateOf("") }
    val openBills = bills.filter { !it.isVoided && it.due > 0 }
    val grouped = openBills.groupBy { CustomerCache.normalizePhone(it.phone) }
        .mapNotNull { (_, customerBills) ->
            val first = customerBills.firstOrNull() ?: return@mapNotNull null
            val customer = customers.firstOrNull { CustomerCache.normalizePhone(it.phone) == CustomerCache.normalizePhone(first.phone) }
                ?: ShopCustomer("", first.customerName, first.phone, "")
            Triple(customer, customerBills.sumOf { it.due }, customerBills.size)
        }
        .filter { (customer, _, _) ->
            query.isBlank() || customer.name.contains(query.trim(), ignoreCase = true) ||
                CustomerCache.normalizePhone(customer.phone).contains(CustomerCache.normalizePhone(query))
        }
        .sortedByDescending { it.second }
    val totalDue = openBills.sumOf { it.due }
    val accountCount = openBills.map { CustomerCache.normalizePhone(it.phone) }.distinct().size
    val voidedPaymentCount = paymentsMade.count { it.isVoided }
    val visiblePayments = paymentsMade.filter { payment ->
        (showVoidedPayments || !payment.isVoided) &&
            (query.isBlank() || payment.customerName.contains(query.trim(), ignoreCase = true) ||
                CustomerCache.normalizePhone(payment.customerPhone).contains(CustomerCache.normalizePhone(query)) ||
                payment.invoiceNumber.contains(query.trim(), ignoreCase = true))
    }
    val pendingSyncCount = paymentsMade.count { !it.isVoided && it.syncStatus != "SYNCED" }

    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = tr("Go back", "वापस जाएँ"), tint = Ink)
            }
            Text(tr("Khata", "खाता"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
            if (pendingSyncCount > 0) {
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFE7E5)) {
                    Text(
                        tr("$pendingSyncCount offline", "$pendingSyncCount ऑफ़लाइन"),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        color = Color(0xFFB3261E), fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(onClick = onSync, enabled = !loading && connected, modifier = Modifier.size(44.dp)) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(21.dp), strokeWidth = 2.dp, color = Teal)
                else Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = tr("Sync payments from Shop", "Shop से भुगतान सिंक करें"),
                    tint = if (connected) Teal else Muted
                )
            }
        }

        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF0F3F2), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val duesSelected = listTab == "DUE"
                Box(
                    Modifier.weight(1f).height(48.dp)
                        .background(if (duesSelected) Teal else Color.Transparent, RoundedCornerShape(11.dp))
                        .clickable { onListTab("DUE") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        tr("Udhar · $accountCount", "उधार · $accountCount"),
                        color = if (duesSelected) Color.White else Ink,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                    )
                }
                val historySelected = listTab == "MADE"
                Box(
                    Modifier.weight(1f).height(48.dp)
                        .background(if (historySelected) Teal else Color.Transparent, RoundedCornerShape(11.dp))
                        .clickable { onListTab("MADE") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        tr("Receipts · ${paymentsMade.count { !it.isVoided }}", "जमा · ${paymentsMade.count { !it.isVoided }}"),
                        color = if (historySelected) Color.White else Ink,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp),
            label = { Text(if (listTab == "MADE") tr("Search receipts", "रसीद खोजें") else tr("Search customer", "ग्राहक खोजें")) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            trailingIcon = if (listTab == "MADE") null else ({
                IconButton(onClick = {
                    savedCustomerQuery = ""
                    showCustomerPicker = true
                }) {
                    Icon(Icons.Rounded.Contacts, contentDescription = tr("Choose customer or contact", "ग्राहक या संपर्क चुनें"), tint = Teal)
                }
            }),
            shape = RoundedCornerShape(14.dp)
        )

        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 2.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            if (isAdministrator) item { AdminDeletionRequestsSection("PAYMENT", requests, onReviewRequest) }

            if (listTab == "DUE") {
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = SoftBlue, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(tr("Customer outstanding", "ग्राहक बकाया"), color = Muted, fontSize = 14.sp)
                                Text("₹$totalDue", color = if (totalDue > 0) Color(0xFFC62828) else Teal, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(tr("$accountCount customers", "$accountCount ग्राहक"), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                    }
                }
                if (grouped.isEmpty()) item {
                    Text(
                        if (loading) tr("Loading outstanding balances…", "बकाया राशि लोड हो रही है…")
                        else if (openBills.isEmpty()) tr("No customer payments are due.", "किसी ग्राहक का भुगतान बाकी नहीं है।")
                        else tr("No customer matches this search.", "इस खोज से कोई ग्राहक नहीं मिला।"),
                        color = Muted, fontSize = 16.sp, modifier = Modifier.padding(8.dp)
                    )
                }
                itemsIndexed(grouped, key = { index, entry -> "due-customer:$index:${CustomerCache.normalizePhone(entry.first.phone)}" }) { _, (customer, due, count) ->
                    Card(
                        onClick = { onCustomer(customer) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFC62828).copy(alpha = 0.25f))
                    ) {
                        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(customer.name.ifBlank { tr("Customer", "ग्राहक") }, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(customer.phone, color = Muted, fontSize = 15.sp)
                                Text(tr("$count bills to settle", "$count बकाया बिल"), color = Muted, fontSize = 13.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹$due", color = Color(0xFFC62828), fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                Text(tr("Open", "खोलें"), color = Teal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                if (voidedPaymentCount > 0) item {
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color(0xFFFFF1F0)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(tr("Show cancelled receipts", "रद्द रसीदें दिखाएँ"), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(tr("$voidedPaymentCount hidden", "$voidedPaymentCount छिपी हुई"), color = Color(0xFFB3261E), fontSize = 12.sp)
                            }
                            Switch(checked = showVoidedPayments, onCheckedChange = { showVoidedPayments = it })
                        }
                    }
                }
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFE8F5F3), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(tr("Receipts recorded", "दर्ज रसीदें"), color = Muted, fontSize = 14.sp)
                                Text("${paymentsMade.count { !it.isVoided }}", color = Teal, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(tr("₹${paymentsMade.filterNot { it.isVoided }.sumOf { it.amount }} received", "₹${paymentsMade.filterNot { it.isVoided }.sumOf { it.amount }} जमा"), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (visiblePayments.isEmpty()) item {
                    Text(
                        if (loading) tr("Loading receipts…", "रसीदें लोड हो रही हैं…")
                        else if (paymentsMade.isEmpty()) tr("No receipts recorded yet.", "अभी तक कोई रसीद दर्ज नहीं है।")
                        else if (!showVoidedPayments && voidedPaymentCount == paymentsMade.size) tr("All recorded receipts are cancelled. Turn on “Show cancelled receipts” to view them.", "सभी रसीदें रद्द हैं। देखने के लिए “रद्द रसीदें दिखाएँ” चालू करें।")
                        else tr("No receipts match this search.", "इस खोज से कोई रसीद नहीं मिली।"),
                        color = Muted, fontSize = 16.sp, modifier = Modifier.padding(8.dp)
                    )
                }
                itemsIndexed(visiblePayments, key = { index, payment -> "receipt:$index:${payment.requestId}:${payment.receiptNumber}" }) { _, payment ->
                    val payerName = payment.customerName.ifBlank {
                        customers.firstOrNull { CustomerCache.normalizePhone(it.phone) == CustomerCache.normalizePhone(payment.customerPhone) }?.name
                    }.orEmpty().ifBlank { payment.customerPhone.ifBlank { tr("Unknown customer", "अज्ञात ग्राहक") } }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (payment.isVoided) Color(0xFFFFF1F0) else Color.White),
                        border = BorderStroke(1.dp, if (payment.isVoided) Color(0xFFD96C65) else Color(0xFFDCE7E7))
                    ) {
                        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${tr("Paid by", "भुगतानकर्ता")}: $payerName", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("${payment.date}  ·  ${payment.method}", color = Muted, fontSize = 13.sp)
                                if (payment.invoiceNumber.isNotBlank()) Text(tr("Bill ${payment.invoiceNumber}", "बिल ${payment.invoiceNumber}"), color = Muted, fontSize = 13.sp)
                                if (payment.advanceAmount > 0) Text(tr("Advance credit ₹${payment.advanceAmount}", "अग्रिम जमा ₹${payment.advanceAmount}"), color = Color(0xFF1769AA), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    when {
                                        payment.isVoided -> tr("Cancelled by administrator", "प्रशासक ने रद्द किया")
                                        payment.syncStatus == "SYNCED" -> tr("Saved in Shop", "Shop में सहेजा गया")
                                        payment.syncStatus == "SYNCING" -> tr("Syncing in background…", "बैकग्राउंड में सिंक हो रहा है…")
                                        else -> tr("Waiting to sync", "सिंक होना बाकी है")
                                    },
                                    color = when {
                                        payment.isVoided -> Color(0xFFB3261E)
                                        payment.syncStatus == "SYNCED" -> Color(0xFF2E7D32)
                                        payment.syncStatus == "SYNCING" -> Teal
                                        else -> Color(0xFFC62828)
                                    }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("₹${payment.amount}", color = if (payment.isVoided) Color(0xFFB3261E) else Color(0xFF1565C0), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                if (payment.syncStatus == "SYNCED" && !payment.isVoided) {
                                    IconButton(onClick = { onRequestDeletion(payment) }, modifier = Modifier.size(42.dp)) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = tr("Request payment deletion", "भुगतान हटाने का अनुरोध"), tint = Color(0xFFB3261E))
                                    }
                                }
                                if (payment.syncStatus != "SYNCED") TextButton(onClick = { onRetryPayment(payment) }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                                    Text(tr("Sync now", "अभी सिंक करें"), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
            }
            Row(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(shape = RoundedCornerShape(50), color = Color.White, shadowElevation = 4.dp) {
                    Text(
                        tr("Collect", "जमा करें"),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                        color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                }
                FloatingActionButton(
                    onClick = onStartCollect,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    containerColor = Teal,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = tr("Collect payment", "जमा करें"), modifier = Modifier.size(42.dp))
                }
            }
        }
    }

    if (showCustomerPicker) {
        val matchingCustomers = customers.filter {
            savedCustomerQuery.isBlank() ||
                it.name.contains(savedCustomerQuery.trim(), ignoreCase = true) ||
                CustomerCache.normalizePhone(it.phone).contains(CustomerCache.normalizePhone(savedCustomerQuery))
        }.take(80)
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = { Text(tr("Choose customer", "ग्राहक चुनें"), color = Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = savedCustomerQuery,
                        onValueChange = { savedCustomerQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(tr("Search saved customers", "सहेजे ग्राहक खोजें")) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        shape = RoundedCornerShape(14.dp)
                    )
                    if (matchingCustomers.isEmpty()) {
                        Text(tr("No saved customers found.", "कोई सहेजा ग्राहक नहीं मिला।"), color = Muted)
                    } else {
                        LazyColumn(Modifier.height(220.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            itemsIndexed(matchingCustomers, key = { _, customer -> "khata-customer:${customer.id}:${customer.phone}" }) { _, customer ->
                                Card(
                                    onClick = {
                                        onQuery(customer.phone)
                                        showCustomerPicker = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE1E8E7))
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(customer.name, modifier = Modifier.weight(1f), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text(customer.phone, color = Teal, fontSize = 13.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            showCustomerPicker = false
                            onPickPhoneContact()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Contacts, contentDescription = null)
                        Text(tr("Choose from phone contacts", "फ़ोन संपर्कों से चुनें"), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomerPicker = false }) { Text(tr("Close", "बंद करें")) }
            }
        )
    }
}
