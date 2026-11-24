package com.shop.salesapp.ui.bill

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.key
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.salesapp.CustomerCache
import com.shop.salesapp.Product
import com.shop.salesapp.ShopCustomer
import com.shop.salesapp.ui.common.BillClothThumbnail
import com.shop.salesapp.ui.common.DetailLine
import com.shop.salesapp.ui.common.ScreenHeader
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CartQuantityControl(
    line: CartLine,
    onChange: (String, Double) -> Unit,
    onSet: (String, Double) -> Unit,
    compact: Boolean = false,
    stockLimit: Boolean = true
) {
    var quantityText by remember(line.product.sku) { mutableStateOf(formatQuantity(line.quantity)) }
    var showQuantityEditor by remember(line.product.sku) { mutableStateOf(false) }
    var quantityDraft by remember(line.product.sku) {
        val initial = formatQuantity(line.quantity)
        mutableStateOf(TextFieldValue(initial, selection = TextRange(initial.length)))
    }
    val quantityFocusRequester = remember(line.product.sku) { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(line.quantity) { if (!showQuantityEditor) quantityText = formatQuantity(line.quantity) }
    val step = if (line.product.fractionalQuantity) 0.1 else 1.0
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            val next = (line.quantity - step).coerceAtLeast(0.0)
            quantityText = formatQuantity(next)
            onChange(line.product.sku, -step)
        }, modifier = if (compact) Modifier.size(32.dp) else Modifier) { Icon(Icons.Rounded.Remove, contentDescription = tr("Decrease quantity", "मात्रा घटाएँ"), modifier = Modifier.size(if (compact) 18.dp else 24.dp)) }
        Surface(
            modifier = Modifier.width(if (compact) 56.dp else 82.dp).clickable {
                val initial = formatQuantity(line.quantity)
                quantityDraft = TextFieldValue(initial, selection = TextRange(initial.length))
                showQuantityEditor = true
            },
            shape = RoundedCornerShape(10.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, Color(0xFFD5DDE9))
        ) {
            Box(Modifier.padding(horizontal = if (compact) 4.dp else 8.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text(quantityText, fontSize = if (compact) 16.sp else 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        IconButton(onClick = {
            val next = if (stockLimit) minOf(line.product.stock, line.quantity + step) else line.quantity + step
            quantityText = formatQuantity(next)
            onChange(line.product.sku, step)
        }, modifier = if (compact) Modifier.size(32.dp) else Modifier) { Icon(Icons.Rounded.Add, contentDescription = tr("Increase quantity", "मात्रा बढ़ाएँ"), modifier = Modifier.size(if (compact) 18.dp else 24.dp)) }
    }
    if (showQuantityEditor) {
        LaunchedEffect(line.product.sku) {
            delay(180)
            quantityFocusRequester.requestFocus()
            keyboard?.show()
        }
        AlertDialog(
            onDismissRequest = { showQuantityEditor = false; keyboard?.hide() },
            title = { Text(tr("Edit quantity", "मात्रा बदलें"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${line.product.name} · ${line.product.quantityUnit()}", color = Muted)
                    OutlinedTextField(
                        value = quantityDraft,
                        onValueChange = { input ->
                            val allowDecimal = line.product.fractionalQuantity
                            val filtered = input.text.filter { it.isDigit() || (allowDecimal && it == '.') }
                            if (!allowDecimal || filtered.count { it == '.' } <= 1) {
                                val start = input.selection.start.coerceAtMost(filtered.length)
                                val end = input.selection.end.coerceAtMost(filtered.length)
                                quantityDraft = input.copy(text = filtered, selection = TextRange(start, end))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().focusRequester(quantityFocusRequester),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = if (line.product.fractionalQuantity) KeyboardType.Decimal else KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newQuantity = quantityDraft.text.toDoubleOrNull()
                    if (newQuantity != null && newQuantity >= 0 && (!stockLimit || newQuantity <= line.product.stock)) {
                        quantityText = formatQuantity(newQuantity)
                        onSet(line.product.sku, newQuantity)
                    }
                    showQuantityEditor = false
                    keyboard?.hide()
                }) { Text(tr("Done", "हो गया")) }
            },
            dismissButton = {
                TextButton(onClick = { showQuantityEditor = false; keyboard?.hide() }) { Text(tr("Cancel", "रद्द करें")) }
            }
        )
    }
}
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun BillScreen(
    padding: PaddingValues,
    shouldScrollToDiscount: Boolean,
    onDiscountScrollComplete: () -> Unit,
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
    onSetUnitPrice: (String, Int, Boolean) -> Unit,
    onSellAllAtCost: (Boolean) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit
) {
    var selectedProductDetails by remember { mutableStateOf<Product?>(null) }
    var showNewCustomerDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var discountDraft by remember { mutableStateOf("") }
    var editingPriceSku by remember { mutableStateOf<String?>(null) }
    var priceDraft by remember { mutableStateOf("") }
    var manualPriceDraft by remember { mutableStateOf("") }
    var sellAtCostDraft by remember { mutableStateOf(false) }
    var showSavedCustomerPicker by remember { mutableStateOf(false) }
    var savedCustomerQuery by remember { mutableStateOf("") }
    var promptedCustomerPhone by remember { mutableStateOf("") }
    var showClearConfirmation by remember { mutableStateOf(false) }
    val subtotal = cart.sumOf { cartLineAmount(it) }
    val editingPriceLine = cart.firstOrNull { it.product.sku == editingPriceSku }
    val billWideAtCost = cart.isNotEmpty() && cart.all { it.sellAtCost }
    val canSellAllAtCost = cart.isNotEmpty() && cart.all { it.product.costPrice > 0 }
    val hasSomeCostPricedItems = cart.any { it.sellAtCost }
    val parsedDiscount = discountValue.toDoubleOrNull() ?: 0.0
    val discountAmount = if (billWideAtCost) 0 else parsedDiscount.roundToInt()
    val total = subtotal - discountAmount
    val validDiscount = billWideAtCost || !discountEnabled || (discountValue.toDoubleOrNull() != null && parsedDiscount >= 0 && discountAmount <= subtotal && total > 0)
    val validCartQuantityAndPrice = cart.all {
        validCartQuantity(it, enforceStock = !orderMode, requirePriceMeasure = !orderMode) && it.unitPrice > 0 &&
            (orderMode || (it.unitPrice >= it.product.costPrice && (!it.sellAtCost || it.unitPrice == it.product.costPrice)))
    }
    val validCart = cart.isNotEmpty() && validCartQuantityAndPrice
    val hasBelowCostPrice = !orderMode && cart.any { it.unitPrice < it.product.costPrice }
    val paidPaymentMethods = setOf("Cash", "UPI", "Card")
    val isCredit = paymentMode == "Credit"
    val paidStatusSelected = paymentMode == "Paid" || paymentMode in paidPaymentMethods
    val paidNow = if (paymentMode == "Credit") creditAmountReceived.toIntOrNull() else total
    val validCreditAmount = paymentMode != "Credit" || (paidNow != null && paidNow in 0..total)
    val hasCreditPaymentReceived = paymentMode == "Credit" && paidNow != null && paidNow > 0
    val validPaymentSelection = orderMode || (
        paymentMode in paidPaymentMethods ||
            (isCredit && (!hasCreditPaymentReceived || creditPaymentMethod in paidPaymentMethods))
        )
    val canSubmit = !saving && customerPhone.isNotBlank() && customerExists != null &&
        (customerExists || customerName.isNotBlank()) && validCart &&
        (orderMode || (validPaymentSelection && validCreditAmount && validDiscount))
    val submitHint = when {
        saving -> if (orderMode) tr("Sending order request…", "ऑर्डर अनुरोध भेज रहे हैं…") else tr("Creating bill…", "बिल बना रहे हैं…")
        customerPhone.isBlank() -> tr("Enter customer phone to continue", "आगे बढ़ने के लिए ग्राहक का फ़ोन नंबर लिखें")
        customerExists == null -> tr("Checking customer details…", "ग्राहक की जानकारी जाँची जा रही है…")
        customerExists == false && customerName.isBlank() -> tr("Enter customer name to continue", "आगे बढ़ने के लिए ग्राहक का नाम लिखें")
        cart.isEmpty() -> tr("Add at least one cloth to continue", "आगे बढ़ने के लिए कम से कम एक कपड़ा जोड़ें")
        !validCart && hasBelowCostPrice -> tr("A unit price cannot be below cost. Use Sell at cost price to charge cost.", "यूनिट कीमत लागत से कम नहीं हो सकती। लागत पर बेचने के लिए लागत वाला विकल्प चुनें।")
        !validCart -> tr("Check each quantity and enter weight/length where needed", "मात्रा जाँचें और जहाँ ज़रूरी हो वज़न/लंबाई लिखें")
        !orderMode && !validDiscount -> tr("Enter a valid discount", "सही छूट दर्ज करें")
        !orderMode && !validCreditAmount -> tr("Enter a valid amount received", "मिली हुई सही राशि दर्ज करें")
        !orderMode && !validPaymentSelection -> tr("Choose payment status and payment method", "भुगतान की स्थिति और तरीका चुनें")
        else -> when {
            orderMode -> tr("Ready to send the order request", "ऑर्डर अनुरोध भेजने के लिए तैयार")
            connected -> tr("Ready to create this bill", "बिल बनाने के लिए तैयार")
            else -> tr("Preview only — connect to Shop before saving", "केवल पूर्वावलोकन — सहेजने से पहले Shop से जुड़ें")
        }
    }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val discountDialogFocusRequester = remember { FocusRequester() }
    val priceDialogFocusRequester = remember { FocusRequester() }
    val phoneFocusRequester = remember { FocusRequester() }
    val phoneBringIntoViewRequester = remember { BringIntoViewRequester() }
    val creditAmountBringIntoViewRequester = remember { BringIntoViewRequester() }
    val billListState = rememberLazyListState()
    val screenScope = rememberCoroutineScope()
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val dialogContentMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.34f).coerceAtLeast(180f).dp
    LaunchedEffect(shouldScrollToDiscount, orderMode, cart.size) {
        if (shouldScrollToDiscount && !orderMode && cart.isNotEmpty()) {
            delay(250)
            billListState.animateScrollToItem(3)
            onDiscountScrollComplete()
        }
    }
    LaunchedEffect(showDiscountDialog) {
        if (showDiscountDialog) {
            delay(180)
            discountDialogFocusRequester.requestFocus()
            keyboard?.show()
        }
    }
    LaunchedEffect(editingPriceSku, sellAtCostDraft) {
        if (editingPriceSku != null) {
            if (sellAtCostDraft) {
                keyboard?.hide()
                focusManager.clearFocus()
            } else {
                delay(180)
                priceDialogFocusRequester.requestFocus()
                keyboard?.show()
            }
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
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = if (keyboardVisible) 24.dp else if (!orderMode && cart.isNotEmpty()) 250.dp else 142.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        item {
            ScreenHeader(
                if (orderMode) tr("Place an order", "ऑर्डर भेजें") else tr("New bill", "नया बिल"),
                onBack,
                onClear = { showClearConfirmation = true },
                clearEnabled = !saving && (customerName.isNotBlank() || customerPhone.isNotBlank() || customerEmail.isNotBlank() ||
                    customerAddress.isNotBlank() || cart.isNotEmpty() || paymentMode.isNotBlank() ||
                    discountValue.isNotBlank() || creditAmountReceived != "0" || creditPaymentMethod.isNotBlank()
                )
            )
        }
        item {
            Text(
                tr("Customer phone", "ग्राहक का फ़ोन"),
                modifier = Modifier.clickable {
                    phoneFocusRequester.requestFocus()
                    keyboard?.show()
                },
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = onPhone,
                    modifier = Modifier
                        .weight(if (customerExists == true) 0.5f else 1f)
                        .focusRequester(phoneFocusRequester)
                        .bringIntoViewRequester(phoneBringIntoViewRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) screenScope.launch {
                                delay(320)
                                phoneBringIntoViewRequester.bringIntoView()
                                delay(120)
                                phoneBringIntoViewRequester.bringIntoView()
                                keyboard?.show()
                            }
                        },
                    placeholder = { Text(tr("Enter phone number", "फ़ोन नंबर लिखें")) },
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
            Text(tr("Required to create bill", "बिल बनाने के लिए ज़रूरी"), color = Muted, fontSize = 14.sp)
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFBCC9DD))
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(54.dp).clickable(onClick = onFindProduct),
                        shape = RoundedCornerShape(13.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Muted)
                    ) {
                        Row(Modifier.fillMaxSize().padding(start = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = Ink)
                            Spacer(Modifier.width(10.dp))
                            Text(tr("Select cloths", "कपड़े चुनें"), Modifier.weight(1f), color = Muted, fontSize = 16.sp, maxLines = 1)
                            IconButton(onClick = onScan, modifier = Modifier.size(46.dp)) {
                                Icon(Icons.Rounded.QrCode2, contentDescription = tr("Scan cloth QR", "कपड़े का QR स्कैन करें"), tint = Teal)
                            }
                        }
                    }
                    if (cart.isEmpty()) {
                        Surface(shape = RoundedCornerShape(13.dp), color = SoftBlue) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.ShoppingBag, null, tint = Ink)
                                Spacer(Modifier.width(10.dp))
                                Text(tr("No items added yet", "अभी कोई सामान नहीं जोड़ा गया"), fontSize = 16.sp, color = Ink)
                            }
                        }
                    } else {
                        Divider(color = Color(0xFFDDE5E4))
                        cart.forEachIndexed { index, line ->
                            key(line.product.sku) {
                                val measureQuantityBringIntoViewRequester = remember { BringIntoViewRequester() }
                                if (index > 0) Divider(color = Color(0xFFE8EBF0))
                                Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                BillClothThumbnail(
                                    line.product,
                                    imageLoader,
                                    Modifier.weight(1f).aspectRatio(1f).clickable { selectedProductDetails = line.product }
                                )
                                Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val priceUnit = if (line.product.pricingUnit.isNotBlank()) line.product.pricingUnit else line.product.quantityUnit()
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                line.product.name,
                                                modifier = Modifier.clickable { selectedProductDetails = line.product },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = Ink,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                listOf(line.product.sizeVariant, line.product.sku).filter { it.isNotBlank() }.joinToString(" · "),
                                                color = Muted,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Surface(
                                            modifier = Modifier.width(91.dp).then(
                                                if (!orderMode) Modifier.clickable {
                                                    sellAtCostDraft = line.sellAtCost
                                                    manualPriceDraft = (
                                                        if (line.sellAtCost) line.unitPriceBeforeCost ?: line.product.price
                                                        else line.unitPrice
                                                    ).toString()
                                                    priceDraft = line.unitPrice.toString()
                                                    editingPriceSku = line.product.sku
                                                } else Modifier
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (orderMode) Color(0xFFF3F6FA) else Color(0xFFEFF6FA),
                                            border = BorderStroke(1.dp, Color(0xFFD5DDE9))
                                        ) {
                                            Column(Modifier.padding(horizontal = 6.dp, vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(tr("Price / unit", "एक यूनिट की कीमत"), color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(
                                                    "₹${line.unitPrice}${if (priceUnit == "PIECE") "" else "/${unitShort(priceUnit)}"}",
                                                    color = Ink,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(tr("Qty", "मात्रा"), color = Muted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(13.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFFD5DDE9))
                                        ) {
                                            CartQuantityControl(line, onQuantity, onSetQuantity, compact = true, stockLimit = !orderMode)
                                        }
                                        Spacer(Modifier.weight(1f))
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(tr("Final price", "अंतिम कीमत"), color = Muted, fontSize = 12.sp, maxLines = 1)
                                            Text("₹${cartLineAmount(line)}", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                    }
                                    if (line.product.pricingUnit.isNotBlank() && !orderMode) {
                                        OutlinedTextField(
                                            value = line.measureQuantity?.let(::formatQuantity).orEmpty(),
                                            onValueChange = { value ->
                                                val filtered = value.filter { it.isDigit() || it == '.' }.let { text -> if (text.count { it == '.' } <= 1) text else line.measureQuantity?.let(::formatQuantity).orEmpty() }
                                                onSetMeasureQuantity(line.product.sku, filtered.toDoubleOrNull())
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .bringIntoViewRequester(measureQuantityBringIntoViewRequester)
                                                .onFocusChanged { focusState ->
                                                    if (focusState.isFocused) screenScope.launch {
                                                        delay(200)
                                                        billListState.animateScrollToItem(2)
                                                        delay(100)
                                                        measureQuantityBringIntoViewRequester.bringIntoView()
                                                        keyboard?.show()
                                                    }
                                                },
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
                    Surface(
                        modifier = Modifier.align(Alignment.End).size(86.dp).clickable(onClick = onFindProduct),
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.5.dp, Teal)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = Teal, modifier = Modifier.size(21.dp))
                            //Text(tr("Add more items", "और सामान जोड़ें"), color = Teal, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
        }
        if (!orderMode) item(key = "bill.discount") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE0E6EE))
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(tr("Price", "कीमत"), Modifier.weight(1f), color = Ink, fontSize = 15.sp)
                            Text("₹$subtotal", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(tr("Discount", "छूट"), Modifier.weight(1f), color = Ink, fontSize = 15.sp)
                            Text(
                                if (discountAmount > 0) "− ₹$discountAmount" else "₹0",
                                color = if (discountAmount > 0) Color(0xFFB64B15) else Muted,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (discountEnabled && !billWideAtCost) {
                                TextButton(
                                    onClick = {
                                        discountDraft = discountValue
                                        showDiscountDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 5.dp)
                                ) { Text(tr("Edit", "बदलें"), color = Teal, fontSize = 13.sp) }
                            }
                            Switch(
                                checked = discountEnabled && !billWideAtCost,
                                enabled = !billWideAtCost,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        discountDraft = discountValue
                                        showDiscountDialog = true
                                    } else {
                                        onDiscountEnabled(false)
                                    }
                                }
                            )
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(tr("Premium", "प्रीमियम"), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    when {
                                        !canSellAllAtCost -> tr("Set a cost price for every item first", "पहले हर सामान की लागत कीमत दर्ज करें")
                                        hasSomeCostPricedItems && !billWideAtCost -> tr("Some items are individually at cost", "कुछ सामान अलग से लागत पर हैं")
                                        else -> tr("Sets every item to its cost price", "हर सामान की कीमत उसकी लागत के बराबर होगी")
                                    },
                                    color = Muted,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = billWideAtCost,
                                enabled = canSellAllAtCost || billWideAtCost,
                                onCheckedChange = onSellAllAtCost
                            )
                        }
                        Divider(color = Color(0xFFE7EBF0), thickness = 1.dp)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(tr("Total", "कुल"), color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Text(tr("Price − Discount", "कीमत − छूट"), color = Muted, fontSize = 11.sp)
                            }
                            Text("₹${total.coerceAtLeast(0)}", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Teal)
                        }
                    }
                }
                if (!validDiscount) Text(tr("Enter a valid discount below the total amount.", "कुल राशि से कम सही छूट दर्ज करें।"), color = Color(0xFFB42318), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (!orderMode && paymentMode == "Credit") item(key = "bill.credit-details") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Credit payment", "उधार भुगतान"), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = creditAmountReceived,
                    onValueChange = onCreditAmountReceived,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(creditAmountBringIntoViewRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                screenScope.launch {
                                    delay(180)
                                    creditAmountBringIntoViewRequester.bringIntoView()
                                    keyboard?.show()
                                }
                            }
                        },
                    placeholder = { Text(tr("Amount received now (₹)", "अभी मिली राशि (₹)"), color = Muted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                    shape = RoundedCornerShape(14.dp)
                )
                if (hasCreditPaymentReceived) {
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
                    color = Color(0xFFC62828), fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
      }
      if (!keyboardVisible) Column(
          Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Paper).padding(horizontal = 14.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(5.dp)
      ) {
          if (!orderMode && cart.isNotEmpty()) {
              Surface(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(14.dp),
                  color = Color.White,
                  border = BorderStroke(1.dp, Color(0xFFD9E3EE))
              ) {
                  Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      Text(tr("Payment status", "भुगतान की स्थिति"), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                      Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                          if (paidStatusSelected) {
                              Button(onClick = { onPaymentMode("Paid") }, modifier = Modifier.weight(1f).height(48.dp), contentPadding = PaddingValues(2.dp), shape = RoundedCornerShape(12.dp)) {
                                  Text(tr("Paid in full", "पूरा भुगतान"), fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                              }
                          } else {
                              OutlinedButton(onClick = { onPaymentMode("Paid") }, modifier = Modifier.weight(1f).height(48.dp), contentPadding = PaddingValues(2.dp), shape = RoundedCornerShape(12.dp)) {
                                  Text(tr("Paid in full", "पूरा भुगतान"), fontSize = 16.sp, maxLines = 1)
                              }
                          }
                          if (isCredit) {
                              Button(
                                  onClick = { onPaymentMode("Credit") },
                                  modifier = Modifier.weight(1f).height(48.dp),
                                  contentPadding = PaddingValues(2.dp),
                                  shape = RoundedCornerShape(12.dp)
                              ) { Text(tr("Due / credit", "बाकी / उधार"), fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                          } else {
                              OutlinedButton(
                                  onClick = {
                                      onPaymentMode("Credit")
                                      screenScope.launch {
                                          delay(150)
                                          billListState.animateScrollToItem(4)
                                      }
                                  },
                                  modifier = Modifier.weight(1f).height(48.dp),
                                  contentPadding = PaddingValues(2.dp),
                                  shape = RoundedCornerShape(12.dp)
                              ) { Text(tr("Due / credit", "बाकी / उधार"), fontSize = 16.sp, maxLines = 1) }
                          }
                      }
                      if (paidStatusSelected) {
                          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                              listOf("Cash", "UPI", "Card").forEach { method ->
                                  if (paymentMode == method) Button(onClick = { onPaymentMode(method) }, modifier = Modifier.weight(1f).height(46.dp), contentPadding = PaddingValues(1.dp), shape = RoundedCornerShape(12.dp)) {
                                      Text(tr(method, method), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                  } else OutlinedButton(onClick = { onPaymentMode(method) }, modifier = Modifier.weight(1f).height(46.dp), contentPadding = PaddingValues(1.dp), shape = RoundedCornerShape(12.dp)) {
                                      Text(tr(method, method), fontSize = 14.sp)
                                  }
                              }
                          }
                      }
                  }
              }
          }
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
    if (editingPriceSku != null && editingPriceLine != null) {
        val editedUnitPrice = priceDraft.toIntOrNull()
        val belowCost = editedUnitPrice != null && editedUnitPrice < editingPriceLine.product.costPrice
        val editedLineTotal = editedUnitPrice?.let { price ->
            if (editingPriceLine.product.pricingUnit.isNotBlank()) lineAmount(price, editingPriceLine.measureQuantity ?: 0.0)
            else lineAmount(price, editingPriceLine.quantity)
        }
        AlertDialog(
            onDismissRequest = {
                editingPriceSku = null
                keyboard?.hide()
                focusManager.clearFocus()
            },
            title = { Text(tr("Price per unit", "एक यूनिट की कीमत"), color = Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    Modifier.heightIn(max = dialogContentMaxHeight).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(editingPriceLine.product.name, color = Muted, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(tr("Sell at cost price", "लागत कीमत पर बेचें"), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (editingPriceLine.product.costPrice > 0) tr("Sets price to ₹${editingPriceLine.product.costPrice} exactly", "कीमत ठीक ₹${editingPriceLine.product.costPrice} कर देगा")
                                else tr("Cost price is not set for this cloth", "इस कपड़े की लागत कीमत दर्ज नहीं है"),
                                color = Muted,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = sellAtCostDraft,
                            enabled = editingPriceLine.product.costPrice > 0,
                            onCheckedChange = { enabled ->
                                sellAtCostDraft = enabled
                                priceDraft = if (enabled) editingPriceLine.product.costPrice.toString() else manualPriceDraft
                            }
                        )
                    }
                    OutlinedTextField(
                        value = priceDraft,
                        onValueChange = { value ->
                            if (!sellAtCostDraft) {
                                manualPriceDraft = value.filter { it.isDigit() }
                                priceDraft = manualPriceDraft
                            }
                        },
                        modifier = Modifier.fillMaxWidth().focusRequester(priceDialogFocusRequester),
                        placeholder = { Text(tr("Enter unit price (₹)", "यूनिट कीमत लिखें (₹)"), color = Muted) },
                        enabled = !sellAtCostDraft,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp),
                        shape = RoundedCornerShape(14.dp)
                    )
                    if (belowCost) {
                        Text(
                            tr("Price cannot be below cost (₹${editingPriceLine.product.costPrice}).", "कीमत लागत (₹${editingPriceLine.product.costPrice}) से कम नहीं हो सकती।"),
                            color = Color(0xFFB3261E), fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (editedLineTotal != null && editedLineTotal > 0) {
                        Text(tr("Final item price: ₹$editedLineTotal", "सामान की अंतिम कीमत: ₹$editedLineTotal"), color = Teal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        editedUnitPrice?.let { onSetUnitPrice(editingPriceLine.product.sku, it, sellAtCostDraft) }
                        editingPriceSku = null
                        keyboard?.hide()
                        focusManager.clearFocus()
                    },
                    enabled = editedUnitPrice != null && editedUnitPrice > 0 && editedUnitPrice >= editingPriceLine.product.costPrice &&
                        (!sellAtCostDraft || editedUnitPrice == editingPriceLine.product.costPrice)
                ) { Text(tr("Update price", "कीमत बदलें")) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editingPriceSku = null
                        keyboard?.hide()
                        focusManager.clearFocus()
                    }
                ) { Text(tr("Cancel", "रद्द करें")) }
            }
        )
    }
    if (showDiscountDialog) {
        val draftAmount = discountDraft.toDoubleOrNull()?.roundToInt()
        val validDraft = discountDraft.toDoubleOrNull()?.let { amount ->
            amount >= 0 && (subtotal == 0 || (draftAmount != null && draftAmount <= subtotal && subtotal - draftAmount > 0))
        } == true
        AlertDialog(
            onDismissRequest = {
                showDiscountDialog = false
                keyboard?.hide()
                focusManager.clearFocus()
            },
            title = { Text(tr("Bill discount", "बिल में छूट"), color = Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    Modifier.heightIn(max = dialogContentMaxHeight).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(tr("Enter the rupee amount to subtract from this bill.", "इस बिल से घटाने के लिए रुपये की राशि लिखें।"), color = Muted, fontSize = 14.sp)
                    OutlinedTextField(
                        value = discountDraft,
                        onValueChange = { value ->
                            val filtered = value.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1) discountDraft = filtered
                        },
                        modifier = Modifier.fillMaxWidth().focusRequester(discountDialogFocusRequester),
                        placeholder = { Text(tr("Discount amount (₹)", "छूट की राशि (₹)"), color = Muted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp),
                        shape = RoundedCornerShape(14.dp)
                    )
                    if (draftAmount != null && draftAmount > 0) {
                        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFFFF0E7)) {
                            Text(
                                tr("Discount: − ₹$draftAmount · New total: ₹${(subtotal - draftAmount).coerceAtLeast(0)}", "छूट: − ₹$draftAmount · नया कुल: ₹${(subtotal - draftAmount).coerceAtLeast(0)}"),
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                color = Color(0xFFB64B15),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDiscountValue(discountDraft)
                        onDiscountEnabled(true)
                        showDiscountDialog = false
                        keyboard?.hide()
                        focusManager.clearFocus()
                    },
                    enabled = validDraft
                ) { Text(tr("Apply", "लागू करें")) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDiscountDialog = false
                        keyboard?.hide()
                        focusManager.clearFocus()
                    }
                ) { Text(tr("Cancel", "रद्द करें")) }
            }
        )
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
                Column(
                    Modifier.heightIn(max = dialogContentMaxHeight).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
    if (showClearConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text(if (orderMode) tr("Clear order request?", "ऑर्डर अनुरोध साफ़ करें?") else tr("Clear bill?", "बिल साफ़ करें?")) },
            text = { Text(tr("This will remove the current entries and saved draft.", "इससे मौजूदा जानकारी और सेव किया हुआ ड्राफ़्ट हट जाएगा।")) },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirmation = false
                    showDiscountDialog = false
                    showNewCustomerDialog = false
                    showSavedCustomerPicker = false
                    editingPriceSku = null
                    promptedCustomerPhone = ""
                    keyboard?.hide()
                    focusManager.clearFocus()
                    onClear()
                }) { Text(tr("Clear form", "फ़ॉर्म साफ़ करें")) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text(tr("Keep editing", "एडिट जारी रखें")) }
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
