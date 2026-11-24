package com.shop.salesapp.ui.product

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.salesapp.Product
import com.shop.salesapp.ProductImageCache
import com.shop.salesapp.createQrBitmap
import com.shop.salesapp.ui.common.DetailLine
import com.shop.salesapp.ui.common.ScreenHeader
import com.shop.salesapp.ui.model.formatQuantity
import com.shop.salesapp.ui.model.quantityUnit
import com.shop.salesapp.ui.model.unitEnglish
import com.shop.salesapp.ui.model.unitHindi
import com.shop.salesapp.ui.model.unitShort
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.SoftBlue
import com.shop.salesapp.ui.theme.StockBadgeBlue
import com.shop.salesapp.ui.theme.Teal
import com.shop.salesapp.ui.theme.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProductDetailScreen(
    padding: PaddingValues,
    product: Product,
    isAdministrator: Boolean,
    connected: Boolean,
    uploading: Boolean,
    imageLoader: suspend (String) -> ByteArray?,
    onTakePhoto: () -> Unit,
    onChoosePhoto: () -> Unit,
    onAvailabilityChange: (Boolean) -> Unit,
    onAddStock: (Double, String, (Boolean) -> Unit) -> Unit,
    onDelete: () -> Unit,
    onProductQr: () -> Unit,
    onEdit: (String, String, String, Int, Boolean, String, (Boolean) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    var showEdit by remember(product.sku) { mutableStateOf(false) }
    var showDeleteConfirmation by remember(product.sku) { mutableStateOf(false) }
    var savingEdit by remember(product.sku) { mutableStateOf(false) }
    var showAddStock by remember(product.sku) { mutableStateOf(false) }
    var savingStock by remember(product.sku) { mutableStateOf(false) }
    val qrBitmap = remember(product.sku) { createQrBitmap(product.sku).asImageBitmap() }
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(8.dp))
        ScreenHeader(tr("Item details", "कपड़े की जानकारी"), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProductPhoto(
                sku = product.sku,
                imageId = product.imageId,
                imageLoader = imageLoader,
                modifier = Modifier.fillMaxWidth().height(240.dp),
                onTakePhoto = onTakePhoto,
                onChoosePhoto = onChoosePhoto,
                uploading = uploading
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(product.name, color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                if (product.sizeVariant.isNotBlank()) Text(product.sizeVariant, color = Muted, fontSize = 16.sp)
                if (product.category.isNotBlank()) Text(product.category, color = Muted, fontSize = 16.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE1E8E7))) {
                    Column(Modifier.padding(12.dp)) {
                        Text(tr("Price", "कीमत"), color = Muted, fontSize = 14.sp)
                        val priceUnit = if (product.pricingUnit.isNotBlank()) product.pricingUnit else product.quantityUnit()
                        Text("₹${product.price}${if (priceUnit != "PIECE") "/${unitShort(priceUnit)}" else ""}", color = Teal, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = StockBadgeBlue) {
                    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(tr("Stock left", "बाकी स्टॉक"), color = Muted, fontSize = 14.sp)
                            Text("${formatQuantity(product.stock)} ${tr(unitEnglish(product.quantityUnit()), unitHindi(product.quantityUnit()))}", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        if (isAdministrator) TextButton(onClick = { showAddStock = true }, enabled = connected) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Text(tr("Add", "जोड़ें"))
                        }
                    }
                }
            }
            DetailLine(tr("Cloth ID", "कपड़े की आईडी"), product.sku)
            Card(
                onClick = onProductQr,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE1E8E7))
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(qrBitmap, contentDescription = tr("Show QR", "QR दिखाएँ"), modifier = Modifier.size(88.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("Cloth QR", "कपड़े का QR"), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onProductQr, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)) {
                            Text(tr("Show QR", "QR दिखाएँ"))
                        }
                    }
                }
            }
            if (isAdministrator) {
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE1E8E7))) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(tr("Availability", "उपलब्धता"), color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Switch(checked = product.available, onCheckedChange = onAvailabilityChange, enabled = connected)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isAdministrator) OutlinedButton(
                    onClick = { showEdit = true },
                    modifier = Modifier.weight(1f).height(52.dp),
                    enabled = connected,
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(tr("Edit", "बदलें"))
                }
                OutlinedButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.weight(1f).height(52.dp),
                    enabled = connected,
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB3261E))
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (isAdministrator) tr("Delete", "हटाएँ") else tr("Request delete", "हटाने का अनुरोध"))
                }
            }
        }
    }
    if (showEdit) {
        EditClothDialog(
            product = product,
            saving = savingEdit,
            onDismiss = { if (!savingEdit) showEdit = false },
            onSave = { name, sizeVariant, category, price, fractional, unit ->
                savingEdit = true
                onEdit(name, sizeVariant, category, price, fractional, unit) { success ->
                    savingEdit = false
                    if (success) showEdit = false
                }
            }
        )
    }
    if (showAddStock) {
        AddStockDialog(
            product = product,
            saving = savingStock,
            onDismiss = { if (!savingStock) showAddStock = false },
            onSave = { quantity, reason ->
                savingStock = true
                onAddStock(quantity, reason) { success ->
                    savingStock = false
                    if (success) showAddStock = false
                }
            }
        )
    }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(if (isAdministrator) tr("Remove this cloth?", "यह कपड़ा हटाएँ?") else tr("Request cloth removal?", "कपड़ा हटाने का अनुरोध करें?"), color = Ink, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (isAdministrator) tr("It will be hidden from the catalogue. Stock and past bills will remain in Shop.", "यह कैटलॉग से छिप जाएगा। स्टॉक और पुराने बिल Shop में बने रहेंगे।")
                    else tr("The administrator will review this request before the cloth is removed.", "कपड़ा हटाने से पहले प्रशासक इस अनुरोध की समीक्षा करेंगे।"),
                    color = Muted
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirmation = false; onDelete() }) {
                    Text(if (isAdministrator) tr("Make unavailable", "अनुपलब्ध करें") else tr("Send request", "अनुरोध भेजें"), color = Color(0xFFB3261E))
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text(tr("Cancel", "रद्द करें")) } }
        )
    }
}

@Composable
fun AddStockDialog(
    product: Product,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit
) {
    var quantity by remember(product.sku) { mutableStateOf("") }
    var reason by remember(product.sku) { mutableStateOf("Restock") }
    val parsedQuantity = quantity.toDoubleOrNull()
    val valid = parsedQuantity != null && parsedQuantity > 0 && parsedQuantity <= 100000 &&
        (product.fractionalQuantity || parsedQuantity % 1.0 == 0.0)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Add stock", "स्टॉक जोड़ें"), color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${product.name} · ${tr("Current", "अभी")}: ${formatQuantity(product.stock)} ${tr(unitEnglish(product.quantityUnit()), unitHindi(product.quantityUnit()))}", color = Muted)
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { value -> quantity = value.filter { it.isDigit() || (product.fractionalQuantity && it == '.') }.let { text -> if (text.count { it == '.' } <= 1) text else quantity } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("${unitEnglish(product.quantityUnit()).replaceFirstChar { it.uppercase() }} to add", "${unitHindi(product.quantityUnit())} जोड़ें")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Reason", "कारण")) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(parsedQuantity ?: 0.0, reason.trim().ifBlank { "Restock" }) }, enabled = valid && !saving) {
                Text(if (saving) tr("Saving…", "सहेज रहे हैं…") else tr("Add stock", "स्टॉक जोड़ें"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text(tr("Cancel", "रद्द करें")) } }
    )
}

@Composable
fun EditClothDialog(
    product: Product,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, Boolean, String) -> Unit
) {
    var name by remember(product.sku) { mutableStateOf(product.name) }
    var sizeVariant by remember(product.sku) { mutableStateOf(product.sizeVariant) }
    var category by remember(product.sku) { mutableStateOf(product.category) }
    var price by remember(product.sku) { mutableStateOf(product.price.toString()) }
    var unit by remember(product.sku) { mutableStateOf(product.quantityUnit()) }
    var fractional by remember(product.sku) { mutableStateOf(product.fractionalQuantity) }
    val valid = name.isNotBlank() && category.isNotBlank() && (price.toIntOrNull() ?: -1) >= 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Edit cloth", "कपड़ा बदलें"), color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Cloth name", "कपड़े का नाम"), maxLines = 1) }, singleLine = true)
                OutlinedTextField(
                    sizeVariant, { sizeVariant = it.take(80) }, modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Size / variant (optional)", "साइज़ / वेरिएंट (वैकल्पिक)"), maxLines = 1) },
                    placeholder = { Text(tr("e.g. Size L, Maroon, Design 20", "जैसे साइज़ L, मरून, डिज़ाइन 20"), maxLines = 1) },
                    singleLine = true
                )
                OutlinedTextField(category, { category = it }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Category", "श्रेणी")) }, singleLine = true)
                OutlinedTextField(
                    price,
                    { price = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (product.pricingUnit.isNotBlank()) tr("Rate / ${unitShort(product.pricingUnit)} (₹)", "दर / ${unitHindi(product.pricingUnit)} (₹)") else tr("Selling price (₹)", "बिक्री कीमत (₹)")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Text(tr("Sold in", "बेचने की इकाई"), color = Ink, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("PIECE" to tr("Unit", "पीस"), "METRE" to tr("Metre", "मीटर"), "KG" to tr("Kg", "किलो")).forEach { (code, label) ->
                        if (unit == code) Button(onClick = {}, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 3.dp)) { Text(label, maxLines = 1) }
                        else OutlinedButton(onClick = { unit = code; fractional = code != "PIECE" }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 3.dp)) { Text(label, maxLines = 1) }
                    }
                }
                if (unit != "PIECE") Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Allow decimal quantity", "दशमलव मात्रा की अनुमति"), Modifier.weight(1f), color = Ink)
                    Switch(checked = fractional, onCheckedChange = { fractional = it })
                }
                Text(tr("Current stock stays unchanged.", "मौजूदा स्टॉक में बदलाव नहीं होगा।"), color = Muted, fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), sizeVariant.trim(), category.trim(), price.toIntOrNull() ?: 0, fractional, unit) }, enabled = valid && !saving) {
                Text(if (saving) tr("Saving…", "सहेज रहे हैं…") else tr("Save changes", "बदलाव सहेजें"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text(tr("Cancel", "रद्द करें")) } }
    )
}

@Composable
fun ProductPhoto(
    sku: String,
    imageId: String,
    imageLoader: suspend (String) -> ByteArray?,
    modifier: Modifier = Modifier,
    selectedPhoto: Bitmap? = null,
    onTakePhoto: () -> Unit,
    onChoosePhoto: () -> Unit,
    uploading: Boolean = false
) {
    val context = LocalContext.current
    var bitmap by remember(sku, imageId) { mutableStateOf(ProductImageCache.memoryHit(sku, imageId)) }
    var photoMenuExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(sku, imageId) {
        if (imageId.isNotBlank()) {
            val cached = withContext(Dispatchers.IO) { ProductImageCache.load(context, sku, imageId) }
            if (cached != null) {
                bitmap = cached
            } else {
                val bytes = runCatching { imageLoader(imageId) }.getOrNull()
                if (bytes != null) {
                    bitmap = withContext(Dispatchers.IO) {
                        ProductImageCache.save(context, sku, imageId, bytes) ?: BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                }
            }
        }
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = SoftBlue) {
        Box(Modifier.fillMaxSize()) {
            val displayPhoto = selectedPhoto ?: bitmap
            if (displayPhoto != null) {
                Image(displayPhoto.asImageBitmap(), contentDescription = "Cloth photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Inventory2, null, tint = Muted, modifier = Modifier.size(48.dp))
                    Text(tr("Add a product picture", "उत्पाद की तस्वीर जोड़ें"), color = Muted, fontSize = 14.sp)
                }
            }
            IconButton(
                onClick = { photoMenuExpanded = true },
                enabled = !uploading,
                modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).background(Color.White.copy(alpha = 0.94f), CircleShape).size(46.dp)
            ) {
                if (uploading) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else Icon(Icons.Rounded.AddAPhoto, contentDescription = tr("Add or change picture", "तस्वीर जोड़ें या बदलें"), tint = Teal)
            }
            DropdownMenu(expanded = photoMenuExpanded, onDismissRequest = { photoMenuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(tr("Take a picture", "तस्वीर लें")) },
                    leadingIcon = { Icon(Icons.Rounded.AddAPhoto, null) },
                    onClick = { photoMenuExpanded = false; onTakePhoto() }
                )
                DropdownMenuItem(
                    text = { Text(tr("Upload from gallery", "गैलरी से अपलोड करें")) },
                    leadingIcon = { Icon(Icons.Rounded.PhotoLibrary, null) },
                    onClick = { photoMenuExpanded = false; onChoosePhoto() }
                )
            }
        }
    }
}
