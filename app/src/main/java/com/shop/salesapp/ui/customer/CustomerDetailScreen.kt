package com.shop.salesapp.ui.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.shop.salesapp.CustomerFinance
import com.shop.salesapp.CustomerOrderRequest
import com.shop.salesapp.PaymentReceipt
import com.shop.salesapp.ShopCustomer
import com.shop.salesapp.ShopInvoice
import com.shop.salesapp.ui.bill.WhatsAppShareIconButton
import com.shop.salesapp.ui.common.ScreenHeader
import com.shop.salesapp.ui.model.CustomerHistoryItem
import com.shop.salesapp.ui.model.formatQuantity
import com.shop.salesapp.ui.model.isOrderRequestCancellable
import com.shop.salesapp.ui.model.isWithinBillCancellationWindow
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.ClothActionBlue
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.SoftBlue
import com.shop.salesapp.ui.theme.Teal
import com.shop.salesapp.ui.theme.tr

@Composable
fun CustomerDetailScreen(
    padding: PaddingValues, customer: ShopCustomer, sales: List<ShopInvoice>, orders: List<CustomerOrderRequest>,
    finance: CustomerFinance?, financeLoading: Boolean, receivingPayment: Boolean,
    historyTab: String, onHistoryTab: (String) -> Unit,
    loading: Boolean, ordersLoading: Boolean, onBack: () -> Unit, onSync: () -> Unit, onOpenPdf: (ShopInvoice) -> Unit,
    onShareBill: (ShopInvoice) -> Unit, onOpenOrderRequest: (CustomerOrderRequest) -> Unit,
    onRequestBillCancellation: (ShopInvoice) -> Unit, onRequestOrderCancellation: (CustomerOrderRequest) -> Unit,
    sharingBillNumber: String?,
    onReceivePayment: (String, Int, String, String) -> Unit,
    onReceiveCustomerPayment: (Int, String, String) -> Unit,
    onRetryPayment: (PaymentReceipt) -> Unit
) {
    var paymentBill by remember(customer.phone) { mutableStateOf<ShopInvoice?>(null) }
    var paymentAmount by remember(customer.phone) { mutableStateOf("") }
    var paymentMethod by remember(customer.phone) { mutableStateOf("Cash") }
    var paymentNote by remember(customer.phone) { mutableStateOf("") }
    var customerPaymentDialog by remember(customer.phone) { mutableStateOf(false) }
    var customerPaymentAmount by remember(customer.phone) { mutableStateOf("") }
    var customerPaymentMethod by remember(customer.phone) { mutableStateOf("Cash") }
    var customerPaymentNote by remember(customer.phone) { mutableStateOf("") }
    var customerPaymentTab by remember(customer.phone) { mutableStateOf("DUE") }
    var showVoidedCustomerPayments by remember(customer.phone) { mutableStateOf(false) }
    val history = buildList {
        sales.forEach { add(CustomerHistoryItem("sale:${it.number}", it.date, invoice = it)) }
        orders.forEach { add(CustomerHistoryItem("request:${it.number}", it.date, orderRequest = it)) }
    }.sortedWith(compareByDescending<CustomerHistoryItem> { it.date }.thenByDescending { it.key })
    val dueBills = sales.filter { it.due > 0 }.sortedByDescending { it.date }
    val recentPayments = finance?.payments.orEmpty()
    val voidedPaymentCount = recentPayments.count { it.isVoided }
    val visibleRecentPayments = recentPayments.filter { showVoidedCustomerPayments || !it.isVoided }

    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), contentPadding = PaddingValues(top = 14.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ScreenHeader(customer.name, onBack, onSync, loading || ordersLoading || financeLoading, true)
            Text(customer.phone, color = Teal, fontSize = 18.sp, modifier = Modifier.padding(start = 10.dp))
            if (customer.email.isNotBlank()) Text(customer.email, color = Muted, modifier = Modifier.padding(start = 10.dp))
        }
        item {
            val summary = finance
            if (summary == null) {
                if (financeLoading) CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            } else {
                Surface(shape = RoundedCornerShape(16.dp), color = SoftBlue, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tr("Account summary", "खाते का सार"), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        FinanceSummaryRow(tr("Total purchases", "कुल खरीद"), "₹${summary.totalPurchases}", Ink)
                        FinanceSummaryRow(tr("Received", "मिला"), "₹${summary.totalPaid}", Teal)
                        FinanceSummaryRow(tr("Current due", "कुल बकाया"), "₹${summary.currentDue}", if (summary.currentDue > 0) Color(0xFFC62828) else Teal)
                        if (summary.advanceBalance > 0) FinanceSummaryRow(tr("Advance credit", "अग्रिम जमा"), "₹${summary.advanceBalance}", Color(0xFF1769AA))
                        FinanceSummaryRow(tr("Bills", "बिल"), summary.invoiceCount.toString(), Muted)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (historyTab == "Sales") Button(onClick = { onHistoryTab("Sales") }, modifier = Modifier.weight(1f).height(50.dp)) { Text(tr("Sales & orders", "बिक्री और ऑर्डर"), fontSize = 14.sp) }
                else OutlinedButton(onClick = { onHistoryTab("Sales") }, modifier = Modifier.weight(1f).height(50.dp)) { Text(tr("Sales & orders", "बिक्री और ऑर्डर"), fontSize = 14.sp) }
                if (historyTab == "Payments") Button(onClick = { onHistoryTab("Payments") }, modifier = Modifier.weight(1f).height(50.dp)) { Text(tr("Payments", "भुगतान"), fontSize = 14.sp) }
                else OutlinedButton(onClick = { onHistoryTab("Payments") }, modifier = Modifier.weight(1f).height(50.dp)) { Text(tr("Payments", "भुगतान"), fontSize = 14.sp) }
            }
        }
        if (historyTab == "Sales") {
            if (history.isEmpty() && (loading || ordersLoading)) item { CircularProgressIndicator(modifier = Modifier.padding(12.dp)) }
            else if (history.isEmpty()) item { Text(tr("No sales or order requests for this customer yet.", "इस ग्राहक की अभी कोई बिक्री या ऑर्डर अनुरोध नहीं है।"), color = Muted, modifier = Modifier.padding(8.dp)) }
            else itemsIndexed(history, key = { index, entry -> "history:$index:${entry.key}" }) { _, entry ->
                val bill = entry.invoice
                val order = entry.orderRequest
                val isCredit = bill != null && bill.due > 0 && !bill.isVoided
                val isVoided = bill?.isVoided == true
                val orderCancelled = order?.status.equals("CANCELLED", ignoreCase = true)
                val statusColor = when {
                    order != null && !orderCancelled -> Color(0xFF1769AA)
                    isVoided || isCredit || orderCancelled -> Color(0xFFB3261E)
                    else -> Color(0xFF188038)
                }
                val statusBackground = when {
                    order != null && !orderCancelled -> Color(0xFFE8F1FC)
                    isVoided || isCredit || orderCancelled -> Color(0xFFFFE7E5)
                    else -> Color(0xFFE0F2E3)
                }
                Card(
                    onClick = {
                        if (bill != null && !bill.isVoided) onOpenPdf(bill)
                        else order?.let(onOpenOrderRequest)
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = if (order != null) Color(0xFFF3F7FD) else Color.White),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.28f))
                ) {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        Box(Modifier.fillMaxHeight().width(6.dp).background(statusColor))
                        Column(Modifier.weight(1f).padding(start = 12.dp, end = 10.dp, top = 12.dp, bottom = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(12.dp), color = statusBackground, modifier = Modifier.size(46.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null, tint = statusColor, modifier = Modifier.size(25.dp))
                                    }
                                }
                                Column(Modifier.weight(1f).padding(start = 10.dp, end = 6.dp)) {
                                    Text(
                                        when {
                                            orderCancelled -> tr("CANCELLED", "रद्द")
                                            order != null -> tr("Order request", "ऑर्डर अनुरोध")
                                            isVoided -> tr("CANCELLED", "रद्द")
                                            isCredit -> tr("CREDIT", "उधार")
                                            else -> tr("PAID · ${bill?.paymentMode.orEmpty()}", "चुका · ${bill?.paymentMode.orEmpty()}")
                                        },
                                        color = statusColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
                                        Text(bill?.date ?: order?.date.orEmpty(), modifier = Modifier.padding(start = 6.dp), fontSize = 13.sp, color = Muted, maxLines = 1)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                                        Icon(Icons.Rounded.Description, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
                                        Text(bill?.number ?: order?.number.orEmpty(), modifier = Modifier.padding(start = 6.dp), fontSize = 13.sp, color = Muted, maxLines = 1)
                                    }
                                }
                                Text(
                                    if (bill != null) "₹${bill.total}" else order?.status.orEmpty(),
                                    color = statusColor,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = if (bill != null) tr("Paid ₹${bill.paid} · Due ₹${bill.due}", "मिला ₹${bill.paid} · बकाया ₹${bill.due}")
                                else tr("${order?.productName.orEmpty()} · Qty ${order?.let { formatQuantity(it.quantity) }.orEmpty()}", "${order?.productName.orEmpty()} · मात्रा ${order?.let { formatQuantity(it.quantity) }.orEmpty()}"),
                                modifier = Modifier.padding(start = 56.dp, top = 6.dp, bottom = 8.dp),
                                color = if (isCredit) Color(0xFFB3261E) else if (order != null) Color(0xFF1769AA) else Teal,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Divider(color = Color(0xFFE4E8EB))
                            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = {
                                        if (bill != null && !bill.isVoided) onOpenPdf(bill)
                                        else order?.let(onOpenOrderRequest)
                                    },
                                    enabled = order != null || (bill != null && !isVoided),
                                    modifier = Modifier.weight(if (bill != null && !isVoided && isWithinBillCancellationWindow(bill.date)) 0.9f else 1f),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.ReceiptLong,
                                        contentDescription = null,
                                        tint = if (order != null) statusColor else Color(0xFFB85B12),
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (bill != null) tr("View bill", "बिल देखें") else tr("View request", "अनुरोध देखें"),
                                        color = if (order != null) statusColor else Color(0xFFB85B12),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                                if (bill != null) {
                                    Divider(Modifier.height(26.dp).width(1.dp), color = Color(0xFFD9DEE5))
                                    WhatsAppShareIconButton(
                                        onClick = { onShareBill(bill) },
                                        enabled = sharingBillNumber == null && !isVoided,
                                        sharing = sharingBillNumber == bill.number
                                    )
                                }
                                val canCancel = when {
                                    bill != null -> !isVoided && isWithinBillCancellationWindow(bill.date)
                                    else -> order != null && isOrderRequestCancellable(order.status)
                                }
                                if (canCancel) {
                                    Divider(Modifier.height(26.dp).width(1.dp), color = Color(0xFFD9DEE5))
                                    TextButton(
                                        onClick = {
                                            if (bill != null) onRequestBillCancellation(bill)
                                            else order?.let(onRequestOrderCancellation)
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                                    ) {
                                        Icon(Icons.Rounded.Cancel, contentDescription = null, tint = Color(0xFFB3261E), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text(tr("Cancel", "रद्द करें"), color = Color(0xFFB3261E), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (history.isNotEmpty() && (loading || ordersLoading)) item { CircularProgressIndicator(modifier = Modifier.padding(12.dp)) }
        } else {
            item {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color(0xFFF0F3F2)) {
                    Row(Modifier.padding(4.dp)) {
                        listOf("MADE" to tr("Payment history", "भुगतान इतिहास"), "DUE" to tr("Bills to settle", "बकाया बिल")).forEach { (tab, label) ->
                            val selected = customerPaymentTab == tab
                            Box(
                                Modifier.weight(1f).height(48.dp)
                                    .background(if (selected) ClothActionBlue else Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable { customerPaymentTab = tab },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            if (customerPaymentTab == "DUE") {
                if ((finance?.currentDue ?: 0) > 0) item {
                    Button(
                        onClick = {
                            customerPaymentAmount = finance?.currentDue?.toString().orEmpty()
                            customerPaymentMethod = "Cash"
                            customerPaymentNote = ""
                            customerPaymentDialog = true
                        },
                        enabled = !receivingPayment,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text(tr("Receive customer payment · ₹${finance?.currentDue}", "ग्राहक से भुगतान लें · ₹${finance?.currentDue}"), fontSize = 16.sp) }
                }
                if (dueBills.isEmpty() && (loading || financeLoading)) item { CircularProgressIndicator(modifier = Modifier.padding(10.dp)) }
                else if (dueBills.isEmpty()) item { Text(tr("No unpaid bills. This account is settled.", "कोई बकाया बिल नहीं है। यह खाता पूरा चुका दिया गया है।"), color = Teal, modifier = Modifier.padding(8.dp)) }
                else itemsIndexed(dueBills, key = { index, bill -> "due:$index:${bill.number}" }) { _, bill ->
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3F1)), border = BorderStroke(1.dp, Color(0xFFC62828).copy(alpha = 0.28f))) {
                        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(bill.number, Modifier.weight(1f), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("₹${bill.due}", color = Color(0xFFC62828), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(tr("Bill total ₹${bill.total} · Paid ₹${bill.paid} · ${bill.date}", "कुल ₹${bill.total} · मिला ₹${bill.paid} · ${bill.date}"), color = Muted, fontSize = 14.sp)
                            TextButton(onClick = {
                                paymentBill = bill
                                paymentAmount = bill.due.toString()
                                paymentMethod = "Cash"
                                paymentNote = ""
                            }, enabled = !receivingPayment) { Text(tr("Settle this bill", "इस बिल का भुगतान लें"), color = Color(0xFFC62828), fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            } else {
                item {
                    val activePayments = recentPayments.filterNot { it.isVoided }
                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFE8F5F3), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(tr("Payments recorded", "दर्ज भुगतान"), color = Muted, fontSize = 14.sp)
                                Text("${activePayments.size}", color = Teal, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(tr("₹${activePayments.sumOf { it.amount }} received", "₹${activePayments.sumOf { it.amount }} मिले"), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (voidedPaymentCount > 0) item {
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color(0xFFFFF1F0)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(tr("Show cancelled payments", "रद्द भुगतान दिखाएँ"), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(tr("$voidedPaymentCount hidden", "$voidedPaymentCount छिपे हुए"), color = Color(0xFFB3261E), fontSize = 13.sp)
                            }
                            Switch(checked = showVoidedCustomerPayments, onCheckedChange = { showVoidedCustomerPayments = it })
                        }
                    }
                }
                if (visibleRecentPayments.isEmpty()) item {
                    Text(
                        if (financeLoading) tr("Loading payment history…", "भुगतान इतिहास लोड हो रहा है…")
                        else if (recentPayments.isEmpty()) tr("No payments have been recorded yet.", "अभी तक कोई भुगतान दर्ज नहीं है।")
                        else tr("All recorded payments are cancelled. Turn on “Show cancelled payments” to view them.", "सभी दर्ज भुगतान रद्द हैं। देखने के लिए “रद्द भुगतान दिखाएँ” चालू करें।"),
                        color = Muted, modifier = Modifier.padding(8.dp)
                    )
                } else itemsIndexed(visibleRecentPayments, key = { index, payment -> "recent-payment:$index:${payment.id}" }) { _, payment ->
                    val paymentColor = when {
                        payment.isVoided -> Color(0xFFB3261E)
                        payment.syncStatus == "SYNCED" -> Teal
                        else -> Color(0xFF9A6200)
                    }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (payment.isVoided) Color(0xFFFFF1F0) else Color.White),
                        border = BorderStroke(1.dp, paymentColor.copy(alpha = 0.28f))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("₹${payment.amount} · ${payment.method}", color = paymentColor, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                                if (payment.isVoided) Text(tr("VOIDED", "रद्द"), color = paymentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                if (payment.syncStatus == "SYNCED") "${payment.receiptNumber} · ${payment.date}"
                                else tr("Local receipt · ${payment.date}", "स्थानीय रसीद · ${payment.date}"),
                                color = Muted, fontSize = 13.sp
                            )
                            if (payment.invoiceNumber.isNotBlank()) Text(tr("Applied to ${payment.invoiceNumber}", "इन बिलों में जोड़ा: ${payment.invoiceNumber}"), color = Muted, fontSize = 13.sp)
                            if (payment.advanceAmount > 0) Text(tr("Advance credit ₹${payment.advanceAmount}", "अग्रिम जमा ₹${payment.advanceAmount}"), color = Color(0xFF1769AA), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            if (payment.note.isNotBlank()) Text(payment.note, color = Muted, fontSize = 13.sp)
                            if (!payment.isVoided && payment.syncStatus != "SYNCED") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (payment.syncStatus == "SYNCING") tr("Syncing in background…", "बैकग्राउंड में सिंक हो रहा है…") else tr("Waiting to sync · not yet confirmed", "सिंक बाकी · अभी पुष्टि नहीं हुई"),
                                        color = Color(0xFF9A6200), fontSize = 13.sp, modifier = Modifier.weight(1f)
                                    )
                                    if (!receivingPayment) TextButton(onClick = { onRetryPayment(payment) }) {
                                        Text(tr("Sync now", "अभी सिंक करें"), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    paymentBill?.let { bill ->
        val amount = paymentAmount.toIntOrNull()
        AlertDialog(
            onDismissRequest = { if (!receivingPayment) paymentBill = null },
            title = { Text(tr("Receive payment", "भुगतान लें")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${bill.number} · ${tr("Due ₹${bill.due}", "बकाया ₹${bill.due}")}", color = Muted)
                    OutlinedTextField(
                        value = paymentAmount,
                        onValueChange = { paymentAmount = it },
                        label = { Text(tr("Amount received (₹)", "मिली राशि (₹)")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("Cash", "UPI", "Card").forEach { method ->
                            if (paymentMethod == method) Button(onClick = { paymentMethod = method }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(2.dp)) { Text(method, fontSize = 13.sp) }
                            else OutlinedButton(onClick = { paymentMethod = method }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(2.dp)) { Text(method, fontSize = 13.sp) }
                        }
                    }
                    OutlinedTextField(
                        value = paymentNote,
                        onValueChange = { paymentNote = it },
                        label = { Text(tr("Note (optional)", "नोट (वैकल्पिक)")) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !receivingPayment && amount != null && amount in 1..bill.due,
                    onClick = {
                        val safeAmount = amount ?: return@TextButton
                        onReceivePayment(bill.number, safeAmount, paymentMethod, paymentNote)
                        paymentBill = null
                    }
                ) { Text(if (receivingPayment) tr("Saving…", "सहेज रहे हैं…") else tr("Save payment", "भुगतान सहेजें")) }
            },
            dismissButton = { TextButton(onClick = { paymentBill = null }, enabled = !receivingPayment) { Text(tr("Cancel", "रद्द करें")) } }
        )
    }

    if (customerPaymentDialog) {
        val amount = customerPaymentAmount.toIntOrNull()
        val due = finance?.currentDue ?: 0
        AlertDialog(
            onDismissRequest = { if (!receivingPayment) customerPaymentDialog = false },
            title = { Text(tr("Receive customer payment", "ग्राहक से भुगतान लें")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tr("Current due: ₹$due · applied to oldest bills first", "कुल बकाया: ₹$due · पहले पुराने बिलों में जोड़ा जाएगा"), color = Muted)
                    OutlinedTextField(
                        value = customerPaymentAmount,
                        onValueChange = { customerPaymentAmount = it },
                        label = { Text(tr("Amount received (₹)", "मिली राशि (₹)")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("Cash", "UPI", "Card").forEach { method ->
                            if (customerPaymentMethod == method) Button(onClick = { customerPaymentMethod = method }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(2.dp)) { Text(method, fontSize = 13.sp) }
                            else OutlinedButton(onClick = { customerPaymentMethod = method }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(2.dp)) { Text(method, fontSize = 13.sp) }
                        }
                    }
                    OutlinedTextField(
                        value = customerPaymentNote,
                        onValueChange = { customerPaymentNote = it },
                        label = { Text(tr("Note (optional)", "नोट (वैकल्पिक)")) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !receivingPayment && amount != null && amount in 1..due,
                    onClick = {
                        val safeAmount = amount ?: return@TextButton
                        onReceiveCustomerPayment(safeAmount, customerPaymentMethod, customerPaymentNote)
                        customerPaymentDialog = false
                    }
                ) { Text(if (receivingPayment) tr("Saving…", "सहेज रहे हैं…") else tr("Save payment", "भुगतान सहेजें")) }
            },
            dismissButton = { TextButton(onClick = { customerPaymentDialog = false }, enabled = !receivingPayment) { Text(tr("Cancel", "रद्द करें")) } }
        )
    }
}

@Composable
fun FinanceSummaryRow(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = Muted, fontSize = 15.sp)
        Text(value, color = valueColor, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}
