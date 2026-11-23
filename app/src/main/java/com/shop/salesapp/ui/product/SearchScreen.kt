package com.shop.salesapp.ui.product

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.salesapp.Product
import com.shop.salesapp.createQrBitmap
import com.shop.salesapp.ui.common.BillClothThumbnail
import com.shop.salesapp.ui.common.DetailLine
import com.shop.salesapp.ui.model.formatQuantity
import com.shop.salesapp.ui.model.quantityUnit
import com.shop.salesapp.ui.model.unitEnglish
import com.shop.salesapp.ui.model.unitHindi
import com.shop.salesapp.ui.model.unitPriceEnglish
import com.shop.salesapp.ui.model.unitPriceHindi
import com.shop.salesapp.ui.model.unitShort
import com.shop.salesapp.ui.theme.ClothActionBlue
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.StockBadgeBlue
import com.shop.salesapp.ui.theme.StockIconBlue
import com.shop.salesapp.ui.theme.Teal
import com.shop.salesapp.ui.theme.tr

@Composable
fun SearchScreen(
    padding: PaddingValues,
    query: String,
    products: List<Product>,
    orderMode: Boolean,
    live: Boolean,
    refreshing: Boolean,
    onSync: () -> Unit,
    onQuery: (String) -> Unit,
    imageLoader: suspend (String) -> ByteArray?,
    onScan: () -> Unit,
    onProductQr: (Product) -> Unit,
    onAddToBill: (Product) -> Unit
) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val results = if (query.isBlank()) products else products.filter {
            it.name.contains(query, ignoreCase = true) || it.sku.contains(query, ignoreCase = true)
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Search clothes", "कपड़े खोजें"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
            IconButton(onClick = onSync, enabled = live && !refreshing, modifier = Modifier.size(48.dp)) {
                if (refreshing) CircularProgressIndicator(modifier = Modifier.size(21.dp), strokeWidth = 2.dp, color = Teal)
                else Icon(Icons.Rounded.Refresh, contentDescription = tr("Sync clothes", "कपड़े सिंक करें"), tint = if (live) Teal else Muted)
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 19.sp),
            placeholder = { Text(tr("Type cloth name or code", "कपड़े का नाम या कोड लिखें"), fontSize = 17.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                IconButton(onClick = onScan, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.QrCode2, contentDescription = tr("Scan clothes", "कपड़े स्कैन करें"), tint = Teal, modifier = Modifier.size(28.dp))
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
            items(results, key = { it.sku }) { product ->
                BillSearchProductCard(
                    product = product,
                    orderMode = orderMode,
                    imageLoader = imageLoader,
                    onAdd = { onAddToBill(product) },
                    onDetails = { keyboard?.hide(); selectedProduct = product }
                )
            }
            if (results.isEmpty()) item { Text(tr("No cloth found. Try another name or code.", "कपड़ा नहीं मिला। दूसरा नाम या कोड आज़माएँ।"), color = Muted, fontSize = 17.sp, modifier = Modifier.padding(8.dp)) }
        }
    }
    selectedProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { selectedProduct = null },
            title = { Text(product.name, color = Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BillClothThumbnail(product, imageLoader, Modifier.fillMaxWidth().height(190.dp))
                    if (product.category.isNotBlank()) DetailLine(tr("Category", "श्रेणी"), product.category)
                    DetailLine(tr("Cloth ID", "कपड़े की आईडी"), product.sku)
                    DetailLine(
                        tr("Price", "कीमत"),
                        "₹${product.price} ${tr(unitPriceEnglish(product.pricingUnit.ifBlank { product.quantityUnit() }), unitPriceHindi(product.pricingUnit.ifBlank { product.quantityUnit() }))}"
                    )
                    DetailLine(
                        tr("Stock left", "बाकी स्टॉक"),
                        "${formatQuantity(product.stock)} ${tr(unitEnglish(product.quantityUnit()), unitHindi(product.quantityUnit()))}"
                    )
                    val qrBitmap = remember(product.sku) { createQrBitmap(product.sku).asImageBitmap() }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Image(
                            qrBitmap,
                            contentDescription = tr("Cloth QR", "कपड़े का QR"),
                            modifier = Modifier.size(84.dp).clickable {
                                selectedProduct = null
                                onProductQr(product)
                            }
                        )
                        Column(Modifier.weight(1f)) {
                            Text(tr("Cloth QR", "कपड़े का QR"), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            TextButton(
                                onClick = {
                                    selectedProduct = null
                                    onProductQr(product)
                                },
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
                            ) { Text(tr("Print QR labels", "QR लेबल प्रिंट करें")) }
                        }
                    }
                    if (product.stock <= 0) Text(
                        if (orderMode) tr("Out of stock · order request is allowed", "स्टॉक नहीं है · ऑर्डर अनुरोध भेज सकते हैं")
                        else tr("Out of stock", "स्टॉक खत्म"),
                        color = Color(0xFFB42318),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedProduct = null; onAddToBill(product) },
                    enabled = orderMode || product.stock > 0
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (orderMode) tr("Add to order", "ऑर्डर में जोड़ें") else tr("Add to bill", "बिल में जोड़ें"))
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProduct = null }) { Text(tr("Close", "बंद करें")) }
            }
        )
    }
}

@Composable
fun BillSearchProductCard(
    product: Product,
    orderMode: Boolean,
    imageLoader: suspend (String) -> ByteArray?,
    onAdd: () -> Unit,
    onDetails: () -> Unit
) {
    Card(
        onClick = onDetails,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE1E8E7))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BillClothThumbnail(product, imageLoader, Modifier.size(76.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(product.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
                Text(tr("ID", "आईडी") + ": ${product.sku}", fontSize = 13.sp, color = Muted, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("₹${product.price}${if (product.pricingUnit.isNotBlank()) "/${unitShort(product.pricingUnit)}" else ""}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
                    Spacer(Modifier.width(7.dp))
                    Box(Modifier.width(1.dp).height(25.dp).background(Color(0xFFCCD2D8)))
                    Spacer(Modifier.width(7.dp))
                    val stockUnit = when {
                        product.quantityUnit() == "KG" -> "kg"
                        product.quantityUnit() == "METRE" -> tr("m", "मीटर")
                        product.stock == 1.0 -> tr("pc", "पीस")
                        else -> tr("pcs", "पीस")
                    }
                    Surface(shape = RoundedCornerShape(14.dp), color = StockBadgeBlue) {
                        Row(
                            Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (product.quantityUnit() != "PIECE") Icons.Rounded.Straighten else Icons.Rounded.Inventory2,
                                contentDescription = null,
                                tint = StockIconBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(tr("Stock:", "स्टॉक:"), fontSize = 12.sp, color = Ink, maxLines = 1)
                            Text("${formatQuantity(product.stock)} $stockUnit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
                        }
                    }
                }
            }
            IconButton(
                onClick = onAdd,
                enabled = orderMode || product.stock > 0,
                modifier = Modifier.size(48.dp).background(if (orderMode || product.stock > 0) ClothActionBlue else Color(0xFFE7E9EC), CircleShape)
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = if (orderMode) tr("Add to order", "ऑर्डर में जोड़ें") else tr("Add to bill", "बिल में जोड़ें"),
                    tint = if (orderMode || product.stock > 0) Ink else Muted,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
fun ProductCard(product: Product, live: Boolean, orderMode: Boolean, onAdd: () -> Unit, onQr: () -> Unit) {
    val qrBitmap = remember(product.sku) { createQrBitmap(product.sku).asImageBitmap() }
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE1E8E7))) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(product.name, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.height(4.dp))
                    Text("${product.sku} · ${product.category}", fontSize = 14.sp, color = Muted)
                    Spacer(Modifier.height(10.dp))
                    Text("₹${product.price}${if (product.pricingUnit.isNotBlank()) "/${unitShort(product.pricingUnit)}" else ""}", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Teal)
                    val stockText = "${formatQuantity(product.stock)}${if (product.quantityUnit() == "PIECE") "" else " ${unitShort(product.quantityUnit())}"}"
                    Text(if (product.stock <= 0) tr("Out of stock", "स्टॉक में नहीं") else if (live) tr("$stockText in stock", "$stockText स्टॉक में") else "$stockText in stock · preview", fontSize = 14.sp, color = Muted)
                    Spacer(Modifier.height(10.dp))
                    FilledTonalButton(onClick = onAdd, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp)) {
                        Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(5.dp)); Text(if (orderMode) tr("Add to order", "ऑर्डर में जोड़ें") else tr("Add to bill", "बिल में जोड़ें"), fontSize = 15.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(qrBitmap, contentDescription = "Create label PDF for ${product.name}", modifier = Modifier.size(76.dp).clickable(onClick = onQr))
                    Text(tr("Cloth QR", "कपड़े का QR"), fontSize = 11.sp, color = Muted)
                    TextButton(onClick = onQr, contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)) {
                        Text(tr("Create PDF", "PDF बनाएँ"), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
