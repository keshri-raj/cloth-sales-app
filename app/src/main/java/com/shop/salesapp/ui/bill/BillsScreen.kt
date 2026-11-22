package com.shop.salesapp.ui.bill

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.shop.salesapp.CustomerCache
import com.shop.salesapp.DeletionRequest
import com.shop.salesapp.ShopInvoice
import com.shop.salesapp.ui.common.AdminDeletionRequestsSection
import com.shop.salesapp.ui.common.DetailLine
import com.shop.salesapp.ui.common.ScreenHeader
import com.shop.salesapp.ui.model.formatQuantity
import com.shop.salesapp.ui.model.quantityUnit
import com.shop.salesapp.ui.model.unitShort
import com.shop.salesapp.ui.model.isWithinBillCancellationWindow
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.ClothActionBlue
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.SoftBlue
import com.shop.salesapp.ui.theme.Teal
import com.shop.salesapp.ui.theme.tr

@Composable
fun WhatsAppShareIconButton(onClick: () -> Unit, enabled: Boolean, sharing: Boolean) {
    IconButton(onClick = onClick, enabled = enabled) {
        WhatsAppShareIcon(sharing)
    }
}

@Composable
fun WhatsAppShareIcon(sharing: Boolean, contentDescription: String = tr("Share on WhatsApp", "WhatsApp पर साझा करें")) {
    val context = LocalContext.current
    val icon = remember(context) {
        runCatching { context.packageManager.getApplicationIcon("com.whatsapp").toBitmap().asImageBitmap() }.getOrNull()
    }
    when {
        sharing -> CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        icon != null -> Image(icon, contentDescription = contentDescription, modifier = Modifier.size(26.dp))
        else -> Icon(Icons.Rounded.Share, contentDescription = contentDescription, tint = Teal)
    }
}

@Composable
fun BillsScreen(
    padding: PaddingValues,
    phone: String,
    bills: List<ShopInvoice>,
    loading: Boolean,
    isAdministrator: Boolean,
    requests: List<DeletionRequest>,
    onRequestDeletion: (ShopInvoice) -> Unit,
    onReviewRequest: (DeletionRequest, Boolean) -> Unit,
    onPhone: (String) -> Unit,
    onBack: () -> Unit,
    onSync: () -> Unit,
    onOpenPdf: (ShopInvoice) -> Unit,
    onShareBill: (ShopInvoice) -> Unit,
    sharingBillNumber: String?
) {
    val searchText = phone.trim()
    val searchDigits = CustomerCache.normalizePhone(searchText)
    var statusFilter by remember { mutableStateOf("All") }
    var selectedBillDetails by remember { mutableStateOf<ShopInvoice?>(null) }
    val searchedBills = if (searchText.isBlank()) bills else bills.filter {
        it.number.contains(searchText, ignoreCase = true) ||
            it.customerName.contains(searchText, ignoreCase = true) ||
            (searchDigits.isNotBlank() && CustomerCache.normalizePhone(it.phone).contains(searchDigits))
    }
    val visibleBills = when (statusFilter) {
        "Paid" -> searchedBills.filter { !it.isVoided && it.due <= 0 }
        "Due" -> searchedBills.filter { !it.isVoided && it.due > 0 }
        "Cancelled" -> searchedBills.filter { it.isVoided }
        else -> searchedBills
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(14.dp))
        ScreenHeader(tr("Bills", "बिल"), onBack, onSync, loading, true)
        OutlinedTextField(
            value = phone,
            onValueChange = onPhone,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Muted) },
            placeholder = { Text(tr("Search name, phone or bill no.", "नाम, फ़ोन या बिल नंबर खोजें")) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            shape = RoundedCornerShape(16.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp)
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("All", "Paid", "Due", "Cancelled").forEach { filter ->
                val selected = statusFilter == filter
                Surface(
                    modifier = Modifier.weight(1f).height(46.dp).clickable { statusFilter = filter },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) ClothActionBlue else Color.Transparent,
                    border = if (selected) null else BorderStroke(1.dp, Color(0xFFB9C1CC))
                ) {
                    Box(Modifier.padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                        Text(tr(filter, when (filter) { "Paid" -> "चुका हुआ"; "Due" -> "बाकी"; "Cancelled" -> "रद्द"; else -> "सभी" }), color = Ink, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(tr("Recent bills", "हाल के बिल"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
            Surface(shape = RoundedCornerShape(50), color = Color(0xFFE5F3E7)) {
                Text(
                    tr("Total: ${visibleBills.size}", "कुल: ${visibleBills.size}"),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    color = Color(0xFF176B3A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
            if (isAdministrator) item { AdminDeletionRequestsSection("BILL", requests, onReviewRequest) }
            itemsIndexed(visibleBills, key = { index, bill -> "bill:$index:${bill.number}" }) { _, bill ->
                val paid = bill.due <= 0 && !bill.isVoided
                val statusColor = when { bill.isVoided -> Color(0xFFB3261E); paid -> Color(0xFF188038); else -> Color(0xFFB05A00) }
                val statusBackground = when { bill.isVoided -> Color(0xFFFFE7E5); paid -> Color(0xFFE0F2E3); else -> Color(0xFFFFF0D9) }
                Card(
                    onClick = { selectedBillDetails = bill },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = if (bill.isVoided) Color(0xFFFFF1F0) else Color.White),
                    border = BorderStroke(1.dp, if (bill.isVoided) Color(0xFFD96C65) else Color(0xFFE3E7E8))
                ) {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        Box(Modifier.fillMaxHeight().width(6.dp).background(statusColor))
                        Column(Modifier.weight(1f).padding(start = 12.dp, end = 10.dp, top = 12.dp, bottom = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFE5F3E7), modifier = Modifier.size(46.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.PersonOutline, contentDescription = null, tint = Color(0xFF176B3A), modifier = Modifier.size(26.dp))
                                    }
                                }
                                Column(Modifier.weight(1f).padding(start = 10.dp, end = 6.dp)) {
                                    Text(bill.customerName.ifBlank { tr("Customer", "ग्राहक") }, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = Muted, modifier = Modifier.size(17.dp))
                                        Text(bill.date, modifier = Modifier.padding(start = 6.dp), fontSize = 14.sp, color = Muted, maxLines = 1)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                                        Icon(Icons.Rounded.Description, contentDescription = null, tint = Muted, modifier = Modifier.size(17.dp))
                                        Text(bill.number, modifier = Modifier.padding(start = 6.dp), fontSize = 13.sp, color = Muted, maxLines = 1)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(shape = RoundedCornerShape(50), color = statusBackground) {
                                        Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                            if (paid) Icon(Icons.Rounded.CheckCircle, null, tint = statusColor, modifier = Modifier.size(16.dp))
                                            Text(
                                                when { bill.isVoided -> tr("CANCELLED", "रद्द"); paid -> tr("PAID", "चुका"); else -> tr("DUE", "बाकी") },
                                                modifier = Modifier.padding(start = if (paid) 4.dp else 0.dp),
                                                color = statusColor,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text("₹${bill.total}", modifier = Modifier.padding(top = 5.dp), fontSize = 21.sp, fontWeight = FontWeight.Bold, color = statusColor)
                                    if (bill.isVoided) Text(tr("Wrong entry", "गलत प्रविष्टि"), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                                    else if (!paid) Text(tr("₹${bill.due} due", "₹${bill.due} बाकी"), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Divider(color = Color(0xFFE5E8E9))
                            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { onOpenPdf(bill) },
                                    enabled = !bill.isVoided,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.ReceiptLong, null, tint = Color(0xFFB85B12), modifier = Modifier.size(19.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(tr("View PDF", "PDF देखें"), color = if (bill.isVoided) Muted else Color(0xFFB85B12), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                                Divider(modifier = Modifier.height(26.dp).width(1.dp), color = Color(0xFFD9DEE5))
                                Row(
                                    Modifier.weight(1f).clickable(enabled = sharingBillNumber == null && !bill.isVoided) { onShareBill(bill) },
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    WhatsAppShareIcon(sharing = sharingBillNumber == bill.number)
                                    Text(tr("WhatsApp", "WhatsApp"), modifier = Modifier.padding(start = 4.dp), color = if (bill.isVoided) Muted else Color(0xFF176B3A), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                                Divider(modifier = Modifier.height(26.dp).width(1.dp), color = Color(0xFFD9DEE5))
                                if (!bill.isVoided && isWithinBillCancellationWindow(bill.date)) TextButton(
                                    onClick = { onRequestDeletion(bill) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    Icon(Icons.Rounded.Cancel, contentDescription = tr("Request bill cancellation", "बिल रद्द करने का अनुरोध"), tint = Color(0xFFB3261E), modifier = Modifier.size(19.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(tr("Cancel", "रद्द करें"), color = Color(0xFFB3261E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            if (visibleBills.isEmpty() && !loading) {
                item {
                    Text(
                        if (bills.isEmpty()) tr("No saved bills yet. Tap Sync bills to load them from Shop.", "अभी कोई बिल सहेजा नहीं गया। Shop से बिल लाने के लिए ‘बिल सिंक करें’ दबाएँ।")
                        else tr("No saved bill matches your search.", "आपकी खोज से कोई सहेजा हुआ बिल नहीं मिला।"),
                        color = Muted, fontSize = 16.sp, modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
    selectedBillDetails?.let { selectedBill ->
        val bill = bills.firstOrNull { it.number == selectedBill.number } ?: selectedBill
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { selectedBillDetails = null },
            title = { Text(tr("Bill details", "बिल की जानकारी"), color = Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(bill.customerName.ifBlank { tr("Customer", "ग्राहक") }, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    DetailLine(tr("Bill number", "बिल नंबर"), bill.number)
                    DetailLine(tr("Date", "तारीख"), bill.date)
                    DetailLine(tr("Payment", "भुगतान"), bill.paymentMode)
                    DetailLine(tr("Paid", "मिला"), "₹${bill.paid}")
                    DetailLine(tr("Due", "बाकी"), "₹${bill.due}")
                    Divider(color = Color(0xFFDDE5E4))
                    Text(tr("Clothes in this bill", "इस बिल के कपड़े"), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    if (bill.items.isEmpty()) {
                        OutlinedButton(onClick = onSync, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                            if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (loading) tr("Syncing bills…", "बिल सिंक हो रहे हैं…") else tr("Sync bills", "बिल सिंक करें"))
                        }
                    } else {
                        bill.items.forEachIndexed { index, line ->
                            Surface(
                                color = if (line.voided) Color(0xFFFFF1F0) else SoftBlue,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (line.voided) Color(0xFFD96C65) else Color(0xFFD5E5EA))
                            ) {
                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${index + 1}. ${line.name}", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        if (line.voided) Text(tr("VOIDED", "रद्द"), color = Color(0xFFB3261E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("${line.sku} · ${line.category}", color = Muted, fontSize = 13.sp)
                                    val unit = line.quantityUnit()
                                    val measured = line.pricingUnit.isNotBlank()
                                    val quantityLabel = if (measured) "${formatQuantity(line.quantity)} pcs · ${formatQuantity(line.measureQuantity)} ${unitShort(line.pricingUnit)}" else "${formatQuantity(line.quantity)}${if (unit == "PIECE") "" else " ${unitShort(unit)}"}"
                                    val priceLabelUnit = if (measured) line.pricingUnit else unit
                                    Text("$quantityLabel × ₹${line.unitPrice}${if (priceLabelUnit == "PIECE") "" else "/${unitShort(priceLabelUnit)}"} = ₹${line.lineTotal}", color = Teal, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    if (line.discount > 0) Text(tr("Discount: ₹${line.discount}", "छूट: ₹${line.discount}"), color = Muted, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    Divider(color = Color(0xFFDDE5E4))
                    DetailLine(tr("Bill total", "कुल बिल"), "₹${bill.total}")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedBillDetails = null }) { Text(tr("Close", "बंद करें")) }
            }
        )
    }
}
