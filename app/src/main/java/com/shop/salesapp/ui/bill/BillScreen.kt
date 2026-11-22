package com.shop.salesapp.ui.bill

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.salesapp.CustomerCache
import com.shop.salesapp.Product
import com.shop.salesapp.ShopCustomer
import com.shop.salesapp.ui.common.BillClothThumbnail
import com.shop.salesapp.ui.common.DetailLine
import com.shop.salesapp.ui.common.ScreenHeader
import com.shop.salesapp.ui.common.SectionTitle
import com.shop.salesapp.ui.model.CartLine
import com.shop.salesapp.ui.model.formatQuantity
import com.shop.salesapp.ui.model.quantityUnit
import com.shop.salesapp.ui.model.unitHindi
import com.shop.salesapp.ui.model.unitEnglish
import com.shop.salesapp.ui.model.unitPriceEnglish
import com.shop.salesapp.ui.model.unitPriceHindi
import com.shop.salesapp.ui.model.unitShort
import com.shop.salesapp.ui.model.lineAmount
import com.shop.salesapp.ui.model.cartLineAmount
import com.shop.salesapp.ui.model.validCartQuantity
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.Paper
import com.shop.salesapp.ui.theme.SoftBlue
import com.shop.salesapp.ui.theme.SoftGreen
import com.shop.salesapp.ui.theme.Teal
import com.shop.salesapp.ui.theme.tr
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CartQuantityControl(
    line: CartLine,
    onChange: (String, Double) -> Unit,
    onSet: (String, Double) -> Unit,
    compact: Boolean = false,
    stockLimit: Boolean = true
) {
    var quantityText by remember(line.product.sku) { mutableStateOf(formatQuantity(line.quantity)) }
    var isEditing by remember(line.product.sku) { mutableStateOf(false) }
    LaunchedEffect(line.quantity, isEditing) {
        if (!isEditing) quantityText = formatQuantity(line.quantity)
    }
    val step = if (line.product.fractionalQuantity) 0.1 else 1.0
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            val next = (line.quantity - step).coerceAtLeast(0.0)
            quantityText = formatQuantity(next)
            onChange(line.product.sku, -step)
        }, modifier = if (compact) Modifier.size(36.dp) else Modifier) { Icon(Icons.Rounded.Remove, contentDescription = tr("Decrease quantity", "मात्रा घटाएँ"), modifier = Modifier.size(if (compact) 18.dp else 24.dp)) }
        if (line.product.fractionalQuantity) {
            OutlinedTextField(
                value = quantityText,
                onValueChange = { value ->
                    quantityText = value
                    onSet(line.product.sku, value.toDoubleOrNull() ?: 0.0)
                },
                modifier = Modifier.width(if (compact) 68.dp else 82.dp).onFocusChanged { isEditing = it.isFocused },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = if (compact) 15.sp else 17.sp, fontWeight = FontWeight.Bold),
                shape = RoundedCornerShape(10.dp)
            )
        } else {
            Text(formatQuantity(line.quantity), fontSize = if (compact) 16.sp else 18.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = {
            val next = if (stockLimit) minOf(line.product.stock, line.quantity + step) else line.quantity + step
            quantityText = formatQuantity(next)
            onChange(line.product.sku, step)
        }, modifier = if (compact) Modifier.size(36.dp) else Modifier) { Icon(Icons.Rounded.Add, contentDescription = tr("Increase quantity", "मात्रा बढ़ाएँ"), modifier = Modifier.size(if (compact) 18.dp else 24.dp)) }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BillScreen(
    padding: PaddingValues,
    connected: Boolean,
    orderMode: Boolean,
    customerName: String,
    customerPhone: String,
    customerEmail: String,
    customerAddress: String,
    customerExists: Boolean?,
    customerSearching: Boolean,
    customerMatches: List<ShopCustomer>,
    customers: List<ShopCustomer>,
    saving: Boolean,
    savingCustomer: Boolean,
    paymentMode: String,
    discountValue: String,
    discountEnabled: Boolean,
    creditAmountReceived: String,
    creditPaymentMethod: String,
    cart: List<CartLine>,
    onName: (String) -> Unit,
    onPhone: (String) -> Unit,
    onCustomerEmail: (String) -> Unit,
    onCustomerAddress: (String) -> Unit,
    onSaveCustomer: () -> Unit,
    onPaymentMode: (String) -> Unit,
    onDiscountValue: (String) -> Unit,
    onDiscountEnabled: (Boolean) -> Unit,
    onCreditAmountReceived: (String) -> Unit,
    onCreditPaymentMethod: (String) -> Unit,
    onSelectCustomer: (ShopCustomer) -> Unit,
    onPickContact: () -> Unit,
    onScan: () -> Unit,
    onFindProduct: () -> Unit,
    imageLoader: suspend (String) -> ByteArray?,
    onQuantity: (String, Double) -> Unit,
    onSetQuantity: (String, Double) -> Unit,
    onSetMeasureQuantity: (String, Double?) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    var selectedProductDetails by remember { mutableStateOf<Product?>(null) }
    var showNewCustomerDialog by remember { mutableStateOf(false) }
    var showSavedCustomerPicker by remember { mutableStateOf(false) }
    var savedCustomerQuery by remember { mutableStateOf("") }
    var promptedCustomerPhone by remember { mutableStateOf("") }
    val subtotal = cart.sumOf { cartLineAmount(it) }
    val parsedDiscount = discountValue.toDoubleOrNull() ?: 0.0
    val discountAmount = parsedDiscount.roundToInt()
    val total = subtotal - discountAmount
    val validDiscount = !discountEnabled || (discountValue.toDoubleOrNull() != null && parsedDiscount >= 0 && discountAmount <= subtotal && total > 0)
    val validCart = cart.isNotEmpty() && cart.all { validCartQuantity(it, enforceStock = !orderMode, requirePriceMeasure = !orderMode) }
    val paidNow = if (paymentMode == "Credit") creditAmountReceived.toIntOrNull() else total
    val validCreditAmount = paymentMode != "Credit" || (paidNow != null && paidNow in 0..total)
    val hasCreditPaymentReceived = paymentMode == "Credit" && paidNow != null && paidNow > 0
    val canSubmit = !saving && customerPhone.isNotBlank() && customerExists != null &&
        (customerExists || customerName.isNotBlank()) && validCart &&
        (orderMode || (validCreditAmount && validDiscount))
    val submitHint = when {
        saving -> if (orderMode) tr("Sending order request…", "ऑर्डर अनुरोध भेज रहे हैं…") else tr("Creating bill…", "बिल बना रहे हैं…")
        customerPhone.isBlank() -> tr("Enter customer phone to continue", "आगे बढ़ने के लिए ग्राहक का फ़ोन नंबर लिखें")
        customerExists == null -> tr("Checking customer details…", "ग्राहक की जानकारी जाँची जा रही है…")
        customerExists == false && customerName.isBlank() -> tr("Enter customer name to continue", "आगे बढ़ने के लिए ग्राहक का नाम लिखें")
        cart.isEmpty() -> tr("Add at least one cloth to continue", "आगे बढ़ने के लिए कम से कम एक कपड़ा जोड़ें")
        !validCart -> tr("Check each quantity and enter weight/length where needed", "मात्रा जाँचें और जहाँ ज़रूरी हो वज़न/लंबाई लिखें")
        !orderMode && !validDiscount -> tr("Enter a valid discount", "सही छूट दर्ज करें")
        !orderMode && !validCreditAmount -> tr("Enter a valid amount received", "मिली हुई सही राशि दर्ज करें")
        else -> when {
            orderMode -> tr("Ready to send the order request", "ऑर्डर अनुरोध भेजने के लिए तैयार")
            connected -> tr("Ready to create this bill", "बिल बनाने के लिए तैयार")
            else -> tr("Preview only — connect to Shop before saving", "केवल पूर्वावलोकन — सहेजने से पहले Shop से जुड़ें")
        }
    }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val discountBringIntoViewRequester = remember { BringIntoViewRequester() }
    val discountFocusRequester = remember { FocusRequester() }
    val creditAmountBringIntoViewRequester = remember { BringIntoViewRequester() }
    val billListState = rememberLazyListState()
    val screenScope = rememberCoroutineScope()
    LaunchedEffect(discountEnabled, orderMode) {
        if (discountEnabled && !orderMode) {
            delay(100)
            discountFocusRequester.requestFocus()
            keyboard?.show()
        }
    }
    LaunchedEffect(customerExists, customerSearching, customerPhone, customerMatches.size) {
        val normalizedPhone = CustomerCache.normalizePhone(customerPhone)
        if (customerExists == false && !customerSearching && normalizedPhone.length >= 8 &&
            promptedCustomerPhone != normalizedPhone
        ) {
            promptedCustomerPhone = normalizedPhone
            keyboard?.hide()
            focusManager.clearFocus()
            showNewCustomerDialog = true
        }
    }
    LaunchedEffect(customerExists) {
        if (customerExists == true) {
            showNewCustomerDialog = false
            keyboard?.hide()
            focusManager.clearFocus()
        }
    }
    Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
      LazyColumn(
        state = billListState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 142.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        item {
            ScreenHeader(if (orderMode) tr("Place an order", "ऑर्डर भेजें") else tr("Create a bill", "बिल बनाएँ"), onBack)
        }
        item {
            SectionTitle("1", tr("Customer", "ग्राहक"))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = onPhone,
                    modifier = Modifier.weight(if (customerExists == true) 0.5f else 1f),
                    label = { Text(tr("Phone number", "फ़ोन नंबर")) },
                    placeholder = { Text(tr("Type number", "नंबर लिखें")) },
                    singleLine = true,
                    readOnly = customerExists == true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        if (customerExists == true) {
                            IconButton(onClick = {
                                keyboard?.hide()
                                focusManager.clearFocus()
                                onPhone("")
                            }) { Icon(Icons.Rounded.Edit, tr("Change customer", "ग्राहक बदलें")) }
                        } else {
                            IconButton(onClick = {
                                savedCustomerQuery = ""
                                showSavedCustomerPicker = true
                            }) { Icon(Icons.Rounded.Contacts, tr("Choose customer or contact", "ग्राहक या संपर्क चुनें")) }
                        }
                    }
                )
                if (customerExists == true) {
                    Surface(
                        modifier = Modifier.weight(0.5f).height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = SoftGreen
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = Teal, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(customerName, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }
            if (customerSearching) Text(tr("Checking saved customers…", "सहेजे ग्राहक खोज रहे हैं…"), color = Muted, fontSize = 14.sp)
            if (customerExists == false && !customerSearching && !showNewCustomerDialog) {
                TextButton(onClick = { showNewCustomerDialog = true }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                    Text(tr("Add customer details", "ग्राहक की जानकारी जोड़ें"), color = Teal, fontSize = 14.sp)
                }
            }
            if (customerMatches.isNotEmpty() && customerExists != true) {
                Spacer(Modifier.height(4.dp))
                Text(tr("Matching customers · tap to select", "मिलते-जुलते ग्राहक · चुनने के लिए दबाएँ"), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                customerMatches.forEach { match ->
                    Card(
                        onClick = {
                            keyboard?.hide()
                            focusManager.clearFocus()
                            onSelectCustomer(match)
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE1E8E7))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(match.name, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text(match.phone, color = Teal, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
        item {
            Divider(color = Color(0xFFDDE5E4))
            Spacer(Modifier.height(4.dp))
            SectionTitle("2", tr("Add items", "सामान जोड़ें"))
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().height(58.dp).clickable(onClick = onFindProduct),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Muted)
            ) {
                Row(Modifier.fillMaxSize().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = Muted)
                    Spacer(Modifier.width(10.dp))
                    Text(tr("Search cloth name or code", "कपड़े का नाम या कोड खोजें"), Modifier.weight(1f), color = Muted, fontSize = 16.sp, maxLines = 1)
                    IconButton(onClick = onScan, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.QrCode2, contentDescription = tr("Scan cloth QR", "कपड़े का QR स्कैन करें"), tint = Teal)
                    }
                }
            }
        }
        if (cart.isEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = SoftBlue) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ShoppingBag, null, tint = Ink); Spacer(Modifier.width(12.dp)); Text(tr("No items added yet", "अभी कोई सामान नहीं जोड़ा गया"), fontSize = 17.sp, color = Ink)
                    }
                }
            }
        } else {
            itemsIndexed(cart, key = { index, line -> "cart:$index:${line.product.sku}" }) { _, line ->
                Card(
                    onClick = { selectedProductDetails = line.product },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE1E8E7))
                ) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BillClothThumbnail(line.product, imageLoader, Modifier.size(64.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(line.product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink, maxLines = 1)
                            val priceUnit = if (line.product.pricingUnit.isNotBlank()) line.product.pricingUnit else line.product.quantityUnit()
                            Text("${line.product.sku} · ₹${line.product.price} ${unitPriceHindi(priceUnit)}", color = Muted, fontSize = 12.sp, maxLines = 1)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CartQuantityControl(line, onQuantity, onSetQuantity, compact = true, stockLimit = !orderMode)
                                Spacer(Modifier.weight(1f))
                                Text("₹${cartLineAmount(line)}", color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            if (line.product.pricingUnit.isNotBlank() && !orderMode) {
                                OutlinedTextField(
                                    value = line.measureQuantity?.let(::formatQuantity).orEmpty(),
                                    onValueChange = { value ->
                                        val filtered = value.filter { it.isDigit() || it == '.' }.let { text -> if (text.count { it == '.' } <= 1) text else line.measureQuantity?.let(::formatQuantity).orEmpty() }
                                        onSetMeasureQuantity(line.product.sku, filtered.toDoubleOrNull())
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(tr("Total ${unitEnglish(line.product.pricingUnit)} sold", "कुल ${unitHindi(line.product.pricingUnit)}")) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(11.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        if (!orderMode) item {
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE1E8E7))) {
                Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Total amount", "कुल राशि"), Modifier.weight(1f), fontSize = 16.sp, color = Ink)
                        Text("₹$subtotal", fontSize = 16.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Discount", "छूट"), Modifier.weight(1f), fontSize = 16.sp, color = Ink)
                        Switch(checked = discountEnabled, onCheckedChange = onDiscountEnabled)
                    }
                    if (discountEnabled) {
                        OutlinedTextField(
                            value = discountValue,
                            onValueChange = onDiscountValue,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(discountFocusRequester)
                                .bringIntoViewRequester(discountBringIntoViewRequester)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        screenScope.launch {
                                            delay(350)
                                            val discountItemIndex = if (cart.isEmpty()) 4 else 3 + cart.size
                                            billListState.animateScrollToItem(discountItemIndex)
                                            delay(100)
                                            discountBringIntoViewRequester.bringIntoView()
                                        }
                                    }
                                },
                            label = { Text(tr("Discount amount (₹)", "छूट की राशि (₹)"), maxLines = 1) },
                            placeholder = { Text(tr("Enter amount", "राशि लिखें")) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Divider(color = Color(0xFFE1E8E7))
                    Row(Modifier.fillMaxWidth().background(SoftBlue, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Final amount", "अंतिम राशि"), Modifier.weight(1f), fontSize = 18.sp, color = Ink, fontWeight = FontWeight.Bold)
                        Text("₹${total.coerceAtLeast(0)}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Teal)
                    }
                    if (!validDiscount) Text(tr("Enter a valid discount below the total amount.", "कुल राशि से कम सही छूट दर्ज करें।"), color = Color(0xFFB42318), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (!orderMode) item {
            Divider(color = Color(0xFFDDE5E4))
            Spacer(Modifier.height(4.dp))
            SectionTitle("3", tr("Payment", "भुगतान"))
            Spacer(Modifier.height(8.dp))
            val isCredit = paymentMode == "Credit"
            Text(tr("Payment status", "भुगतान की स्थिति"), color = Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isCredit) {
                    Button(onClick = {}, modifier = Modifier.weight(1f).height(50.dp), contentPadding = PaddingValues(2.dp), shape = RoundedCornerShape(14.dp)) {
                        Text(tr("Paid in full", "पूरा भुगतान"), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(onClick = { onPaymentMode("Cash") }, modifier = Modifier.weight(1f).height(50.dp), contentPadding = PaddingValues(2.dp), shape = RoundedCornerShape(14.dp)) {
                        Text(tr("Paid in full", "पूरा भुगतान"), fontSize = 14.sp)
                    }
                }
                if (isCredit) {
                    Button(onClick = {}, modifier = Modifier.weight(1f).height(50.dp), contentPadding = PaddingValues(2.dp), shape = RoundedCornerShape(14.dp)) {
                        Text(tr("Credit", "उधार"), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(onClick = { onPaymentMode("Credit") }, modifier = Modifier.weight(1f).height(50.dp), contentPadding = PaddingValues(2.dp), shape = RoundedCornerShape(14.dp)) {
                        Text(tr("Credit", "उधार"), fontSize = 14.sp)
                    }
                }
            }
            if (!isCredit) {
                Spacer(Modifier.height(8.dp))
                Text(tr("Paid by", "भुगतान का तरीका"), color = Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Cash", "UPI", "Card").forEach { method ->
                        if (paymentMode == method) Button(onClick = { onPaymentMode(method) }, modifier = Modifier.weight(1f).height(46.dp), contentPadding = PaddingValues(2.dp)) { Text(tr(method, method), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                        else OutlinedButton(onClick = { onPaymentMode(method) }, modifier = Modifier.weight(1f).height(46.dp), contentPadding = PaddingValues(2.dp)) { Text(tr(method, method), fontSize = 14.sp) }
                    }
                }
            }
            if (paymentMode == "Credit") {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = creditAmountReceived,
                    onValueChange = onCreditAmountReceived,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(creditAmountBringIntoViewRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                screenScope.launch {
                                    delay(350)
                                    val paymentItemIndex = if (cart.isEmpty()) 5 else 4 + cart.size
                                    billListState.animateScrollToItem(paymentItemIndex)
                                    delay(100)
                                    creditAmountBringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                    label = { Text(tr("Amount received now (₹)", "अभी मिली राशि (₹)")) },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                    shape = RoundedCornerShape(14.dp)
                )
                if (hasCreditPaymentReceived) {
                    Spacer(Modifier.height(6.dp))
                    Text(tr("Received via", "राशि मिली via"), color = Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Cash", "UPI", "Card").forEach { method ->
                            if (creditPaymentMethod == method) Button(onClick = { onCreditPaymentMethod(method) }, modifier = Modifier.weight(1f).height(46.dp), contentPadding = PaddingValues(2.dp)) { Text(tr(method, method), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                            else OutlinedButton(onClick = { onCreditPaymentMethod(method) }, modifier = Modifier.weight(1f).height(46.dp), contentPadding = PaddingValues(2.dp)) { Text(tr(method, method), fontSize = 14.sp) }
                        }
                    }
                }
                val dueNow = if (paidNow != null) (total - paidNow).coerceAtLeast(0) else total
                Text(
                    if (validCreditAmount) tr("Due after this payment: ₹$dueNow", "इस भुगतान के बाद बकाया: ₹$dueNow") else tr("Enter an amount from ₹0 to ₹$total.", "₹0 से ₹$total तक राशि लिखें।"),
                    color = if (validCreditAmount) Color(0xFFC62828) else Color(0xFFC62828), fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
      }
      Column(
          Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Paper).padding(horizontal = 14.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(5.dp)
      ) {
          Text(submitHint, Modifier.padding(horizontal = 4.dp), fontSize = 13.sp, color = if (canSubmit) Teal else Muted, maxLines = 1)
          Button(
              onClick = onSave,
              enabled = canSubmit,
              modifier = Modifier.fillMaxWidth().height(58.dp),
              shape = RoundedCornerShape(16.dp)
          ) {
              if (saving) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
              else Icon(if (orderMode) Icons.Rounded.ShoppingBag else Icons.AutoMirrored.Rounded.ReceiptLong, null)
              Spacer(Modifier.width(10.dp))
              Text(if (orderMode) tr("Send order request", "ऑर्डर अनुरोध भेजें") else tr("Create bill", "बिल बनाएँ"), fontSize = 19.sp, fontWeight = FontWeight.Bold)
          }
      }
    }
    if (showSavedCustomerPicker) {
        val matchingCustomers = customers.filter {
            savedCustomerQuery.isBlank() ||
                it.name.contains(savedCustomerQuery, ignoreCase = true) ||
                it.phone.contains(savedCustomerQuery)
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSavedCustomerPicker = false },
            title = { Text(tr("Choose customer", "ग्राहक चुनें"), color = Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        LazyColumn(Modifier.heightIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            itemsIndexed(matchingCustomers, key = { _, customer -> "saved:${customer.id}:${customer.phone}" }) { _, customer ->
                                Card(
                                    onClick = {
                                        onSelectCustomer(customer)
                                        showSavedCustomerPicker = false
                                        keyboard?.hide()
                                        focusManager.clearFocus()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE1E8E7))
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
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
                    OutlinedButton(
                        onClick = {
                            showSavedCustomerPicker = false
                            onPickContact()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Contacts, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Choose from phone contacts", "फ़ोन संपर्कों से चुनें"))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSavedCustomerPicker = false }) { Text(tr("Close", "बंद करें")) }
            }
        )
    }
    if (showNewCustomerDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!savingCustomer) showNewCustomerDialog = false },
            title = { Text(tr("Add customer", "ग्राहक जोड़ें"), color = Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        tr("No saved customer found for $customerPhone. Add their details to continue.", "$customerPhone के लिए ग्राहक नहीं मिला। आगे बढ़ने के लिए जानकारी जोड़ें।"),
                        color = Muted,
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = onName,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr("Customer name", "ग्राहक का नाम")) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = customerEmail,
                        onValueChange = onCustomerEmail,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr("Email (optional)", "ईमेल (वैकल्पिक)")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = customerAddress,
                        onValueChange = onCustomerAddress,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr("City or address (optional)", "शहर या पता (वैकल्पिक)")) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onSaveCustomer,
                    enabled = customerName.isNotBlank() && !savingCustomer
                ) { Text(if (savingCustomer) tr("Saving…", "सहेज रहे हैं…") else tr("Save customer", "ग्राहक सहेजें")) }
            },
            dismissButton = {
                TextButton(onClick = { showNewCustomerDialog = false }, enabled = !savingCustomer) { Text(tr("Cancel", "रद्द करें")) }
            }
        )
    }
    selectedProductDetails?.let { product ->
        val quantityInBill = cart.firstOrNull { it.product.sku == product.sku }?.quantity ?: 0.0
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { selectedProductDetails = null },
            title = { Text(tr("Cloth details", "कपड़े की जानकारी"), color = Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(product.name, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Divider(color = Color(0xFFDDE5E4))
                    DetailLine(tr("Cloth ID", "कपड़े की आईडी"), product.sku)
                    DetailLine(tr("Category", "श्रेणी"), product.category)
                    val priceUnit = if (product.pricingUnit.isNotBlank()) product.pricingUnit else product.quantityUnit()
                    DetailLine(tr(unitPriceEnglish(priceUnit), unitPriceHindi(priceUnit)), "₹${product.price}${if (priceUnit == "PIECE") "" else "/${unitShort(priceUnit)}"}")
                    DetailLine(tr("In this bill", "इस बिल में मात्रा"), formatQuantity(quantityInBill) + " ${tr(unitEnglish(product.quantityUnit()), unitHindi(product.quantityUnit()))}")
                    val selectedLine = cart.firstOrNull { it.product.sku == product.sku }
                    if (product.pricingUnit.isNotBlank()) DetailLine(tr("Measured amount", "नापा हुआ कुल"), selectedLine?.measureQuantity?.let(::formatQuantity)?.let { "$it ${unitShort(product.pricingUnit)}" } ?: "—")
                    DetailLine(tr("Line total", "कुल कीमत"), "₹${selectedLine?.let(::cartLineAmount) ?: 0}")
                    DetailLine(tr("Available stock", "उपलब्ध स्टॉक"), formatQuantity(product.stock) + " ${tr(unitEnglish(product.quantityUnit()), unitHindi(product.quantityUnit()))}")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedProductDetails = null }) { Text(tr("Close", "बंद करें")) }
            }
        )
    }
}
