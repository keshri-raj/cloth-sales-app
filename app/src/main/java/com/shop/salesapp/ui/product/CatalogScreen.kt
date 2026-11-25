package com.shop.salesapp.ui.product

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.shop.salesapp.ui.model.priceUnit
import com.shop.salesapp.ui.model.unitPriceHindi
import com.shop.salesapp.ui.model.unitShort
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.StockBadgeBlue
import com.shop.salesapp.ui.theme.Teal
import com.shop.salesapp.ui.theme.tr

private enum class CatalogFilter { All, LabelPending, OutOfStock, Attached }

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
    printedQrSkus: Set<String>,
    onQrPrintedChange: (List<Product>, Boolean) -> Unit,
    onRequestDeletion: (Product) -> Unit,
    onReviewRequest: (DeletionRequest, Boolean) -> Unit,
    onAvailabilityChange: (Product, Boolean) -> Unit,
    imageLoader: suspend (String) -> ByteArray?,
    onProductQr: (Product) -> Unit,
    onProductDetails: (Product) -> Unit,
    onPrintLabels: (List<Product>) -> Unit,
    onShareCatalog: () -> Unit,
    onBack: () -> Unit
) {
    var showUnavailable by remember { mutableStateOf(false) }
    var showRequests by remember { mutableStateOf(false) }
    var selectingForPrint by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(CatalogFilter.All) }
    val selectedSkus = remember { mutableStateListOf<String>() }
    val labelCopies = remember { mutableStateMapOf<String, Int>() }
    val matchingProducts = products.filter {
        it.available && (query.isBlank() || it.name.contains(query, ignoreCase = true) || it.sku.contains(query, ignoreCase = true))
    }
    val visibleProducts = when (selectedFilter) {
        CatalogFilter.All -> matchingProducts
        CatalogFilter.LabelPending -> matchingProducts.filter { it.sku !in printedQrSkus }
        CatalogFilter.OutOfStock -> matchingProducts.filter { it.stock <= 0.0 }
        CatalogFilter.Attached -> matchingProducts.filter { it.sku in printedQrSkus }
    }
    val selectedProducts = products.filter { it.sku in selectedSkus && it.available }
    val selectedLabelCount = selectedProducts.sumOf { labelCopies[it.sku] ?: 1 }
    val pendingLabels = matchingProducts.count { it.sku !in printedQrSkus }
    val outOfStockCount = matchingProducts.count { it.stock <= 0.0 }
    val attachedCount = matchingProducts.count { it.sku in printedQrSkus }
    val pendingRequests = requests.count { it.status.equals("PENDING", ignoreCase = true) }
    val unavailableProducts = products.filterNot { it.available }.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true) || it.sku.contains(query, ignoreCase = true)
    }

    fun selectUnprintedVisible() {
        selectedSkus.clear()
        labelCopies.clear()
        visibleProducts.filter { it.sku !in printedQrSkus }.forEach { product ->
            selectedSkus.add(product.sku)
            labelCopies[product.sku] = 1
        }
        selectingForPrint = true
    }

    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(10.dp))
        ScreenHeader(tr("Cloth catalogue", "कपड़ों का कैटलॉग"), onBack, onSync, syncing, live, onShare = onShareCatalog)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp),
            placeholder = { Text(tr("Search cloth name or code", "कपड़े का नाम या कोड खोजें"), fontSize = 15.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                IconButton(onClick = onScan, modifier = Modifier.size(46.dp)) {
                    Icon(Icons.Rounded.QrCode2, contentDescription = tr("Scan clothes", "कपड़े स्कैन करें"), tint = Teal, modifier = Modifier.size(26.dp))
                }
            },
            shape = RoundedCornerShape(15.dp)
        )
        Spacer(Modifier.height(9.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            CatalogFilterChip(tr("All", "सभी"), selectedFilter == CatalogFilter.All) { selectedFilter = CatalogFilter.All }
            CatalogFilterChip("${tr("Label pending", "लेबल बाकी")} $pendingLabels", selectedFilter == CatalogFilter.LabelPending) { selectedFilter = CatalogFilter.LabelPending }
            CatalogFilterChip("${tr("Out of stock", "स्टॉक ख़त्म")} $outOfStockCount", selectedFilter == CatalogFilter.OutOfStock) { selectedFilter = CatalogFilter.OutOfStock }
            CatalogFilterChip("${tr("Attached", "लगा हुआ")} $attachedCount", selectedFilter == CatalogFilter.Attached) { selectedFilter = CatalogFilter.Attached }
        }
        Spacer(Modifier.height(8.dp))

        if (selectingForPrint) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tr("${selectedProducts.size} selected · $pendingLabels unprinted", "${selectedProducts.size} चुने · $pendingLabels बिना प्रिंट"),
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    TextButton(onClick = ::selectUnprintedVisible, contentPadding = PaddingValues(horizontal = 5.dp)) {
                        Text(tr("Select unprinted", "बिना प्रिंट चुनें"), color = Color(0xFFB64B15), fontSize = 13.sp, maxLines = 1)
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { selectedSkus.clear(); labelCopies.clear(); selectingForPrint = false },
                        contentPadding = PaddingValues(horizontal = 7.dp)
                    ) { Text(tr("Cancel", "रद्द करें"), color = Color(0xFFB64B15)) }
                    TextButton(
                        onClick = {
                            val markPrinted = !selectedProducts.all { it.sku in printedQrSkus }
                            onQrPrintedChange(selectedProducts, markPrinted)
                        },
                        enabled = selectedProducts.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 7.dp)
                    ) {
                        Text(
                            if (selectedProducts.isNotEmpty() && selectedProducts.all { it.sku in printedQrSkus }) tr("Mark unprinted", "बिना प्रिंट चिह्नित करें")
                            else tr("Mark printed", "प्रिंट चिह्नित करें"),
                            color = Color(0xFFB64B15)
                        )
                    }
                }
                Button(
                    onClick = {
                        val productsToPrint = selectedProducts.flatMap { product -> List(labelCopies[product.sku] ?: 1) { product } }
                        onPrintLabels(productsToPrint)
                    },
                    enabled = selectedLabelCount > 0,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(13.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(tr("Create label PDF · $selectedLabelCount labels", "लेबल PDF बनाएँ · $selectedLabelCount लेबल"), fontSize = 14.sp, maxLines = 1)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("$pendingLabels labels pending", "$pendingLabels लेबल बाकी"), color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = { selectingForPrint = true; selectedSkus.clear(); labelCopies.clear() }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Icon(Icons.Rounded.Print, contentDescription = null, tint = Color(0xFFB64B15), modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tr("Print labels", "लेबल प्रिंट करें"), color = Color(0xFFB64B15), fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(visibleProducts, key = { "catalog:${it.sku}" }) { product ->
                CatalogProductRow(
                    product = product,
                    imageLoader = imageLoader,
                    selectionMode = selectingForPrint,
                    selected = product.sku in selectedSkus,
                    labelCopies = labelCopies[product.sku] ?: 1,
                    labelPrinted = product.sku in printedQrSkus,
                    onLabelCopiesChange = { labelCopies[product.sku] = it },
                    onToggleSelection = {
                        if (product.sku in selectedSkus) {
                            selectedSkus.remove(product.sku)
                            labelCopies.remove(product.sku)
                        } else {
                            selectedSkus.add(product.sku)
                            labelCopies[product.sku] = 1
                        }
                    },
                    onDetails = { onProductDetails(product) },
                    onShowQr = { onProductQr(product) }
                )
            }
            if (visibleProducts.isEmpty()) item {
                Text(
                    when {
                        !live -> tr("Connect to Shop to load the catalogue.", "कैटलॉग लोड करने के लिए Shop से जुड़ें।")
                        query.isNotBlank() -> tr("No clothes match this search.", "इस खोज से कोई कपड़ा नहीं मिला।")
                        selectedFilter == CatalogFilter.LabelPending -> tr("All available labels are attached.", "सभी उपलब्ध लेबल लग चुके हैं।")
                        selectedFilter == CatalogFilter.OutOfStock -> tr("No out-of-stock clothes.", "स्टॉक ख़त्म वाला कोई कपड़ा नहीं है।")
                        selectedFilter == CatalogFilter.Attached -> tr("No attached labels yet.", "अभी कोई लेबल नहीं लगा है।")
                        else -> tr("No available clothes found.", "कोई उपलब्ध कपड़ा नहीं मिला।")
                    },
                    color = Muted,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
            if (isAdministrator && unavailableProducts.isNotEmpty()) item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showUnavailable = !showUnavailable },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (showUnavailable) tr("Hide unavailable clothes", "अनुपलब्ध कपड़े छिपाएँ") else tr("Unavailable clothes (${unavailableProducts.size})", "अनुपलब्ध कपड़े (${unavailableProducts.size})"))
                }
            }
            if (showUnavailable && isAdministrator) items(unavailableProducts, key = { "unavailable:${it.sku}" }) { product ->
                Card(shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F6F7))) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(product.name, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            val priceUnit = product.priceUnit()
                            Text("${product.sku} · ₹${product.price}${if (priceUnit == "PIECE") "" else "/${unitShort(priceUnit)}"}", color = Muted, fontSize = 12.sp)
                        }
                        TextButton(onClick = { onAvailabilityChange(product, true) }) { Text(tr("Restore", "वापस लाएँ"), color = Teal) }
                    }
                }
            }
            if (isAdministrator) item {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { showRequests = !showRequests },
                    shape = RoundedCornerShape(15.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE1E7EE))
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Cloth removal requests", "कपड़ा हटाने के अनुरोध"), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Surface(shape = CircleShape, color = Color(0xFFEDF1F6)) {
                            Text(pendingRequests.toString(), modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = if (showRequests) tr("Hide requests", "अनुरोध छिपाएँ") else tr("Show requests", "अनुरोध दिखाएँ"), tint = Muted)
                    }
                }
                if (showRequests) {
                    Spacer(Modifier.height(8.dp))
                    AdminDeletionRequestsSection("PRODUCT", requests, onReviewRequest)
                }
            }
        }
    }
}

@Composable
private fun CatalogFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) Color(0xFF173B60) else Color.White,
        border = if (selected) null else BorderStroke(1.dp, Color(0xFFD9E0E8))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            color = if (selected) Color.White else Color(0xFF31465D),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun CatalogProductRow(
    product: Product,
    imageLoader: suspend (String) -> ByteArray?,
    selectionMode: Boolean,
    selected: Boolean,
    labelCopies: Int,
    labelPrinted: Boolean,
    onLabelCopiesChange: (Int) -> Unit,
    onToggleSelection: () -> Unit,
    onDetails: () -> Unit,
    onShowQr: () -> Unit
) {
    val qrBitmap = remember(product.sku) { createQrBitmap(product.sku).asImageBitmap() }
    val priceUnit = product.priceUnit()
    val price = "₹${product.price}${if (priceUnit == "PIECE") "" else "/${unitShort(priceUnit)}"}"
    val stockUnit = product.quantityUnit()
    val rowColor = if (selectionMode && selected) Color(0xFFFFF7E4) else Color.White
    val rowBorder = if (selectionMode && selected) Color(0xFFF0D49B) else Color(0xFFE1E7EE)

    Column {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable {
                if (selectionMode) onToggleSelection() else onDetails()
            },
            shape = RoundedCornerShape(15.dp),
            color = rowColor,
            border = BorderStroke(1.dp, rowBorder)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f).aspectRatio(1f)) {
                    BillClothThumbnail(product, imageLoader, Modifier.fillMaxWidth().aspectRatio(1f))
                }
                Row(Modifier.weight(2f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(product.name, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(product.sku, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Surface(shape = CircleShape, color = StockBadgeBlue) {
                            Text(
                                product.sizeVariant.ifBlank { tr("One size", "एक साइज़") },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = Ink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                            Text(price, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(
                                tr("${formatQuantity(product.stock)}${if (stockUnit == "PIECE") "" else " ${unitShort(stockUnit)}"} in stock", "स्टॉक में ${formatQuantity(product.stock)}${if (stockUnit == "PIECE") "" else " ${unitShort(stockUnit)}"}"),
                                color = if (product.stock <= 0.0) Color(0xFFAD352E) else Ink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        if (selectionMode) {
                            Checkbox(checked = selected, onCheckedChange = { onToggleSelection() }, modifier = Modifier.size(34.dp))
                        }
                        Surface(
                            modifier = Modifier.size(54.dp).clickable(onClick = onShowQr),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFD6E1EB))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(qrBitmap, contentDescription = tr("Show QR code", "QR कोड दिखाएँ"), modifier = Modifier.size(46.dp))
                            }
                        }
                        Text(
                            if (labelPrinted) tr("Marked", "चिह्नित") else tr("Unmarked", "अचिह्नित"),
                            color = if (labelPrinted) Color(0xFF28723A) else Color(0xFFB64B15),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        if (selectionMode && selected) {
            Row(
                Modifier.fillMaxWidth().padding(start = 50.dp, end = 6.dp, top = 3.dp, bottom = 5.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFFFF2E4), border = BorderStroke(1.dp, Color(0xFFFFDFC3))) {
                    Row(Modifier.padding(start = 6.dp, end = 3.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Print, contentDescription = tr("Label copies", "लेबल संख्या"), tint = Color(0xFFB64B15), modifier = Modifier.size(16.dp))
                        IconButton(onClick = { onLabelCopiesChange((labelCopies - 1).coerceAtLeast(1)) }, enabled = labelCopies > 1, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.Remove, contentDescription = tr("Remove one label", "एक लेबल घटाएँ"), modifier = Modifier.size(16.dp))
                        }
                        Text(labelCopies.toString(), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(21.dp), textAlign = TextAlign.Center)
                        IconButton(onClick = { onLabelCopiesChange(labelCopies + 1) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.Add, contentDescription = tr("Add one label", "एक लेबल बढ़ाएँ"), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

