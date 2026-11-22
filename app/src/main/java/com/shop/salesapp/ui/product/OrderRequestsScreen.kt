package com.shop.salesapp.ui.product

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.salesapp.CustomerOrderRequest
import com.shop.salesapp.CustomerCache
import com.shop.salesapp.OrderRequestItem
import com.shop.salesapp.Product
import com.shop.salesapp.ShopCustomer
import com.shop.salesapp.DeletionRequest
import com.shop.salesapp.ui.common.AdminDeletionRequestsSection
import com.shop.salesapp.ui.common.DetailLine
import com.shop.salesapp.ui.common.ScreenHeader
import com.shop.salesapp.ui.model.formatQuantity
import com.shop.salesapp.ui.model.quantityUnit
import com.shop.salesapp.ui.model.unitShort
import com.shop.salesapp.ui.bill.WhatsAppShareIcon
import kotlin.math.roundToInt
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.Teal
import com.shop.salesapp.ui.theme.tr

@Composable
fun OrderRequestsScreen(
    padding: PaddingValues,
    requests: List<CustomerOrderRequest>,
    customers: List<ShopCustomer>,
    products: List<Product>,
    loading: Boolean,
    connected: Boolean,
    isAdministrator: Boolean,
    cancellationRequests: List<DeletionRequest>,
    onReviewCancellation: (DeletionRequest, Boolean) -> Unit,
    onBack: () -> Unit,
    onSync: () -> Unit,
    onRequestClick: (CustomerOrderRequest) -> Unit,
    onShareQuotation: (CustomerOrderRequest) -> Unit,
    sharing: Boolean
) {
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(14.dp))
        ScreenHeader(tr("Order requests", "ऑर्डर अनुरोध"), onBack, onSync, loading, connected)
        if (!connected) {
            Text(tr("Sign in to Shop to load order requests.", "ऑर्डर अनुरोध देखने के लिए Shop में साइन इन करें।"), color = Muted, modifier = Modifier.padding(12.dp))
        } else if (!loading && requests.isEmpty()) {
            Text(tr("No order requests found.", "कोई ऑर्डर अनुरोध नहीं मिला।"), color = Muted, fontSize = 16.sp, modifier = Modifier.padding(12.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp)) {
            if (isAdministrator) item { AdminDeletionRequestsSection("ORDER", cancellationRequests, onReviewCancellation) }
            itemsIndexed(requests, key = { index, request -> "order-request:$index:${request.number}" }) { _, request ->
                val customerPhone = CustomerCache.normalizePhone(request.customerPhone)
                val savedCustomerName = customers.firstOrNull {
                    customerPhone.isNotBlank() && CustomerCache.normalizePhone(it.phone) == customerPhone
                }?.name?.trim().orEmpty()
                val requestCustomerName = request.customerName.trim()
                val hasPlaceholderName = requestCustomerName.equals("Customer", ignoreCase = true) ||
                    requestCustomerName == "ग्राहक"
                val resolvedCustomerName = savedCustomerName.ifBlank {
                    requestCustomerName.takeUnless { it.isBlank() || hasPlaceholderName }.orEmpty()
                }
                val resolvedRequest = if (resolvedCustomerName == request.customerName) request else request.copy(customerName = resolvedCustomerName)
                val estimate = roughRequestEstimate(request, products)
                Card(
                    onClick = { onRequestClick(resolvedRequest) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8F4)),
                    border = BorderStroke(1.dp, Color(0xFFB7DCC4))
                ) {
                    Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(resolvedCustomerName.ifBlank { tr("Customer", "ग्राहक") }, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
                                Text("${request.number} · ${request.date}", color = Muted, fontSize = 13.sp)
                            }
                            IconButton(onClick = { onShareQuotation(resolvedRequest) }, enabled = !sharing) {
                                WhatsAppShareIcon(sharing, tr("Share request on WhatsApp", "अनुरोध WhatsApp पर साझा करें"))
                            }
                        }
                        if (request.customerPhone.isNotBlank()) Text(request.customerPhone, color = Teal, fontSize = 15.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(request.status.ifBlank { tr("Pending", "लंबित") }, color = Color(0xFF2E7D32), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(tr("Amount: ${estimate?.label() ?: "To be confirmed"}", "राशि: ${estimate?.label() ?: "पुष्टि बाकी"}"), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderRequestDetailScreen(
    padding: PaddingValues,
    request: CustomerOrderRequest?,
    products: List<Product>,
    onShareQuotation: (CustomerOrderRequest) -> Unit,
    sharing: Boolean,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = tr("Go back", "वापस जाएँ"), tint = Ink)
            }
            Text(tr("Order request", "ऑर्डर अनुरोध"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
            IconButton(onClick = { request?.let(onShareQuotation) }, enabled = request != null && !sharing) {
                WhatsAppShareIcon(sharing, tr("Share request on WhatsApp", "अनुरोध WhatsApp पर साझा करें"))
            }
        }
        if (request == null) {
            Text(tr("Request details are unavailable. Go back and select a request again.", "अनुरोध की जानकारी उपलब्ध नहीं है। वापस जाकर फिर से अनुरोध चुनें।"), color = Muted, fontSize = 16.sp)
            return@Column
        }
        val amount = roughRequestEstimate(request, products)?.amount
        val listedItems = request.items.ifEmpty {
            request.productName.split(", ").mapNotNull { summary ->
                val match = Regex("^(.*?) ×([0-9]+(?:\\.[0-9]+)?)$").matchEntire(summary.trim()) ?: return@mapNotNull null
                val name = match.groupValues[1].trim()
                val quantity = match.groupValues[2].toDoubleOrNull() ?: return@mapNotNull null
                val sku = products.firstOrNull { it.name.equals(name, ignoreCase = true) }?.sku.orEmpty()
                OrderRequestItem(name, sku, quantity)
            }
        }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8F4)), border = BorderStroke(1.dp, Color(0xFFB7DCC4))) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(request.customerName.ifBlank { tr("Customer", "ग्राहक") }, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                    Text(request.status.ifBlank { tr("Pending", "लंबित") }, color = Color(0xFF2E7D32), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                if (request.customerPhone.isNotBlank()) DetailLine(tr("Phone", "फ़ोन"), request.customerPhone)
                DetailLine(tr("Request number", "अनुरोध नंबर"), request.number)
                DetailLine(tr("Date", "तारीख"), request.date)
            }
        }
        Text(tr("Requested clothes", "माँगे गए कपड़े"), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFD5E5EA))) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Name", "नाम"), color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.42f))
                    Text(tr("ID", "आईडी"), color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.34f))
                    Text(tr("Quantity", "मात्रा"), color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.24f))
                }
                Divider(color = Color(0xFFD5E5EA))
                if (listedItems.isEmpty()) {
                    Text(request.productName.ifBlank { tr("No item details", "सामान की जानकारी नहीं है") }, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Ink)
                } else {
                    listedItems.forEachIndexed { index, item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Text("${index + 1}. ${item.name}", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.42f))
                            Text(item.sku.ifBlank { "—" }, color = Teal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.34f))
                            val product = products.firstOrNull { it.sku == item.sku }
                            val unit = product?.quantityUnit() ?: "PIECE"
                            Text(formatQuantity(item.quantity) + if (unit == "PIECE") "" else " ${unitShort(unit)}", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.24f))
                        }
                    }
                }
                Divider(color = Color(0xFFD5E5EA))
                DetailLine(tr("Total quantity", "कुल मात्रा"), formatQuantity(request.quantity))
                DetailLine(tr("Amount", "राशि"), amount?.let { "₹$it" } ?: tr("To be confirmed", "पुष्टि बाकी"))
                if (request.notes.isNotBlank()) DetailLine(tr("Notes", "नोट"), request.notes)
            }
        }
    }
}

private data class RoughRequestEstimate(val amount: Int?) {
    @Composable
    fun label(): String = amount?.let { "₹$it" } ?: tr("To be confirmed", "पुष्टि बाकी")
}

private fun roughRequestEstimate(request: CustomerOrderRequest, products: List<Product>): RoughRequestEstimate? {
    val items = request.items.ifEmpty {
        request.productName.split(", ").mapNotNull { summary ->
            val match = Regex("^(.*?) ×([0-9]+(?:\\.[0-9]+)?)$").matchEntire(summary.trim()) ?: return@mapNotNull null
            val name = match.groupValues[1].trim()
            val quantity = match.groupValues[2].toDoubleOrNull() ?: return@mapNotNull null
            val product = products.firstOrNull { it.name.equals(name, ignoreCase = true) }
            OrderRequestItem(name, product?.sku.orEmpty(), quantity)
        }
    }
    if (items.isEmpty()) return null
    var estimate = 0.0
    var hasFixedPrice = false
    items.forEach { item ->
        val product = products.firstOrNull { it.sku == item.sku }
        if (product != null && product.pricingUnit.isBlank()) {
            estimate += product.price * item.quantity
            hasFixedPrice = true
        }
    }
    return RoughRequestEstimate(estimate.roundToInt().takeIf { hasFixedPrice })
}
