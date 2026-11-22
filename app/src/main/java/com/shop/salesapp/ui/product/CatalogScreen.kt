package com.shop.salesapp.ui.product

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.salesapp.DeletionRequest
import com.shop.salesapp.Product
import com.shop.salesapp.createQrBitmap
import com.shop.salesapp.ui.common.AdminDeletionRequestsSection
import com.shop.salesapp.ui.common.BillClothThumbnail
import com.shop.salesapp.ui.common.ScreenHeader
import com.shop.salesapp.ui.model.formatQuantity
import com.shop.salesapp.ui.model.quantityUnit
import com.shop.salesapp.ui.model.unitHindi
import com.shop.salesapp.ui.model.unitPriceEnglish
import com.shop.salesapp.ui.model.unitPriceHindi
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.StockBadgeBlue
import com.shop.salesapp.ui.theme.Teal
import com.shop.salesapp.ui.theme.tr

@Composable
fun CatalogScreen(
    padding: PaddingValues,
    products: List<Product>,
    live: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    onScan: () -> Unit,
    syncing: Boolean,
    onSync: () -> Unit,
    isAdministrator: Boolean,
    requests: List<DeletionRequest>,
    onRequestDeletion: (Product) -> Unit,
    onReviewRequest: (DeletionRequest, Boolean) -> Unit,
    onAvailabilityChange: (Product, Boolean) -> Unit,
    imageLoader: suspend (String) -> ByteArray?,
    onProductQr: (Product) -> Unit,
    onProductDetails: (Product) -> Unit,
    onShareCatalog: () -> Unit,
    onBack: () -> Unit
) {
    var showUnavailable by remember { mutableStateOf(false) }
    val activeProducts = products.filter {
        it.available && (query.isBlank() || it.name.contains(query, ignoreCase = true) || it.sku.contains(query, ignoreCase = true))
    }
    val unavailableProducts = products.filterNot { it.available }.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true) || it.sku.contains(query, ignoreCase = true)
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(14.dp)); ScreenHeader(tr("Cloth catalogue", "कपड़ों का कैटलॉग"), onBack, onSync, syncing, live, onShare = onShareCatalog)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
            placeholder = { Text(tr("Search cloth name or code", "कपड़े का नाम या कोड खोजें"), fontSize = 16.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                IconButton(onClick = onScan, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.QrCode2, contentDescription = tr("Scan clothes", "कपड़े स्कैन करें"), tint = Teal, modifier = Modifier.size(28.dp))
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
            if (isAdministrator) item {
                AdminDeletionRequestsSection("PRODUCT", requests, onReviewRequest)
            }
            itemsIndexed(activeProducts, key = { index, product -> "product:$index:${product.sku}" }) { _, product ->
                CatalogCompactProductCard(
                    product = product,
                    imageLoader = imageLoader,
                    onDetails = { onProductDetails(product) },
                    onShowQr = { onProductQr(product) }
                )
            }
            if (isAdministrator && unavailableProducts.isNotEmpty()) item {
                OutlinedButton(
                    onClick = { showUnavailable = !showUnavailable },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (showUnavailable) tr("Hide unavailable clothes", "अनुपलब्ध कपड़े छिपाएँ")
                        else tr("Unavailable clothes (${unavailableProducts.size})", "अनुपलब्ध कपड़े (${unavailableProducts.size})")
                    )
                }
            }
            if (showUnavailable) itemsIndexed(unavailableProducts, key = { index, product -> "unavailable:$index:${product.sku}" }) { _, product ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F6F7))) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(product.name, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            val priceUnit = if (product.pricingUnit.isNotBlank()) product.pricingUnit else product.quantityUnit()
                            Text("${product.sku} · ₹${product.price}${if (priceUnit == "PIECE") "" else "/${priceUnit.lowercase()}"}", color = Muted, fontSize = 13.sp)
                        }
                        TextButton(onClick = { onAvailabilityChange(product, true) }) {
                            Text(tr("Restore", "वापस लाएँ"), color = Teal)
                        }
                    }
                }
            }
            if (activeProducts.isEmpty() && (!isAdministrator || unavailableProducts.isEmpty())) item {
                Text(
                    when {
                        !live -> tr("Connect to Shop to load the catalogue.", "कैटलॉग लोड करने के लिए Shop से जुड़ें।")
                        query.isNotBlank() -> tr("No clothes match this search.", "इस खोज से कोई कपड़ा नहीं मिला।")
                        else -> tr("No available clothes found.", "कोई उपलब्ध कपड़ा नहीं मिला।")
                    },
                    color = Muted,
                    fontSize = 17.sp
                )
            }
        }
    }
}

@Composable
fun CatalogCompactProductCard(
    product: Product,
    imageLoader: suspend (String) -> ByteArray?,
    onDetails: () -> Unit,
    onShowQr: () -> Unit
) {
    val qrBitmap = remember(product.sku) { createQrBitmap(product.sku).asImageBitmap() }
    Card(
        onClick = onDetails,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE1E8E7))
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BillClothThumbnail(product, imageLoader, Modifier.size(76.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(product.name, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val priceUnit = if (product.pricingUnit.isNotBlank()) product.pricingUnit else product.quantityUnit()
                        Text("₹${product.price}${if (priceUnit != "PIECE") "/${priceUnit.lowercase()}" else ""}", color = Teal, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Surface(shape = RoundedCornerShape(12.dp), color = StockBadgeBlue) {
                            Text(
                                "${formatQuantity(product.stock)} ${if (product.quantityUnit() == "PIECE") tr("left", "बाकी") else tr(unitPriceEnglish(product.quantityUnit()).removePrefix("per "), unitHindi(product.quantityUnit()))}",
                                color = Ink,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(product.sku, color = Muted, fontSize = 12.sp, maxLines = 1)
                }
                Surface(
                    onClick = onShowQr,
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFCCE3EC)),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Image(
                            qrBitmap,
                            contentDescription = tr("Show QR", "QR दिखाएँ"),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = tr("View cloth details", "कपड़े की जानकारी देखें"), tint = Ink)
            }
        }
    }
}
