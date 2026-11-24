package com.shop.salesapp.ui.product

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.salesapp.ui.common.ScreenHeader
import com.shop.salesapp.ui.model.unitHindi
import com.shop.salesapp.ui.model.unitShort
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.SoftBlue
import com.shop.salesapp.ui.theme.clothCategories
import com.shop.salesapp.ui.theme.tr
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun AddProductScreen(
    padding: PaddingValues,
    connected: Boolean,
    saving: Boolean,
    name: String,
    sizeVariant: String,
    category: String,
    price: String,
    cost: String,
    stock: String,
    fractionalQuantity: Boolean,
    unit: String,
    photo: Bitmap?,
    onTakePhoto: () -> Unit,
    onChoosePhoto: () -> Unit,
    onName: (String) -> Unit,
    onSizeVariant: (String) -> Unit,
    onCategory: (String) -> Unit,
    onPrice: (String) -> Unit,
    onCost: (String) -> Unit,
    onStock: (String) -> Unit,
    onFractionalQuantity: (Boolean) -> Unit,
    onUnit: (String) -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit
) {
    var categoryPickerOpen by remember { mutableStateOf(false) }
    var categorySearch by remember { mutableStateOf("") }
    var showClearConfirmation by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val initialScreenHeightDp = remember { screenHeightDp }
    val photoHeight = (initialScreenHeightDp * 0.16f).coerceIn(108f, 132f).dp
    val priceFocus = remember { FocusRequester() }
    val costFocus = remember { FocusRequester() }
    val stockFocus = remember { FocusRequester() }
    val nameBringIntoView = remember { BringIntoViewRequester() }
    val priceBringIntoView = remember { BringIntoViewRequester() }
    val costBringIntoView = remember { BringIntoViewRequester() }
    val stockBringIntoView = remember { BringIntoViewRequester() }
    val sizeBringIntoView = remember { BringIntoViewRequester() }
    val focusScope = androidx.compose.runtime.rememberCoroutineScope()
    var fieldScrollJob by remember { mutableStateOf<Job?>(null) }
    fun bringFocusedFieldIntoView(requester: BringIntoViewRequester) {
        fieldScrollJob?.cancel()
        fieldScrollJob = focusScope.launch {
            delay(320)
            requester.bringIntoView()
            delay(120)
            requester.bringIntoView()
        }
    }
    val parsedStock = stock.toDoubleOrNull()
    val stockPrecisionValid = parsedStock != null && kotlin.math.abs(parsedStock * 1000 - (parsedStock * 1000).roundToInt()) < 0.001
    val validOpeningStock = parsedStock != null && parsedStock >= 0 && parsedStock <= 100000 &&
        stockPrecisionValid && (fractionalQuantity || parsedStock % 1.0 == 0.0)
    Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = 78.dp)
                .verticalScroll(scrollState)
                .padding(top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                ScreenHeader(
                    tr("Add a cloth", "कपड़ा जोड़ें"),
                    onBack,
                    onClear = { showClearConfirmation = true },
                    clearEnabled = !saving && (name.isNotBlank() || sizeVariant.isNotBlank() || category.isNotBlank() ||
                        price.isNotBlank() || cost.isNotBlank() || stock.isNotBlank() || photo != null
                    )
                )
            }
            ProductPhoto(
                sku = "new-product-preview",
                imageId = "",
                imageLoader = { null },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(photoHeight),
                selectedPhoto = photo,
                onTakePhoto = onTakePhoto,
                onChoosePhoto = onChoosePhoto
            )
            Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                OutlinedTextField(
                    name, onName,
                    Modifier.fillMaxWidth().bringIntoViewRequester(nameBringIntoView).onFocusChanged {
                        if (it.isFocused) bringFocusedFieldIntoView(nameBringIntoView)
                    },
                    label = { Text(tr("Cloth name", "कपड़े का नाम"), maxLines = 1) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { costFocus.requestFocus() }),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp),
                    shape = RoundedCornerShape(14.dp)
                )
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                OutlinedTextField(
                    sizeVariant, { onSizeVariant(it.take(80)) },
                    Modifier.fillMaxWidth().bringIntoViewRequester(sizeBringIntoView).onFocusChanged {
                        if (it.isFocused) bringFocusedFieldIntoView(sizeBringIntoView)
                    },
                    label = { Text(tr("Size / variant (optional)", "साइज़ / वेरिएंट (वैकल्पिक)"), maxLines = 1) },
                    placeholder = { Text(tr("e.g. Size L, Maroon, Design 20", "जैसे साइज़ L, मरून, डिज़ाइन 20"), maxLines = 1) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { costFocus.requestFocus() }),
                    shape = RoundedCornerShape(14.dp)
                )
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                OutlinedButton(
                    onClick = { keyboard?.hide(); focusManager.clearFocus(); categorySearch = ""; categoryPickerOpen = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    Text(category.ifBlank { tr("Choose category (required)", "श्रेणी चुनें (ज़रूरी)") }, Modifier.weight(1f), fontSize = 15.sp, color = if (category.isBlank()) Muted else Ink)
                    Text("⌄", fontSize = 20.sp, color = Ink)
                }
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tr("Sold in", "बेचने की इकाई"), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("PIECE" to tr("Unit", "पीस"), "METRE" to tr("Metre", "मीटर"), "KG" to tr("Kg", "किलो")).forEach { (code, label) ->
                            if (unit == code) Button(onClick = { }, modifier = Modifier.weight(1f).height(42.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                                Text(label, maxLines = 1, fontSize = 14.sp)
                            } else OutlinedButton(onClick = { onUnit(code) }, modifier = Modifier.weight(1f).height(42.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                                Text(label, maxLines = 1, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        stock, onStock, Modifier.weight(0.9f).focusRequester(stockFocus).bringIntoViewRequester(stockBringIntoView).onFocusChanged {
                            if (it.isFocused) bringFocusedFieldIntoView(stockBringIntoView)
                        },
                        label = { Text(tr("Stock (${unitShort(unit)})", "स्टॉक (${unitHindi(unit)})"), maxLines = 1, fontSize = 13.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = if (fractionalQuantity) KeyboardType.Decimal else KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { costFocus.requestFocus() }),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp), shape = RoundedCornerShape(14.dp)
                    )
                    if (unit != "PIECE") Surface(Modifier.weight(1.1f), shape = RoundedCornerShape(14.dp), color = SoftBlue) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(tr("Allow decimals", "दशमलव मात्रा"), Modifier.weight(1f), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            Switch(checked = fractionalQuantity, onCheckedChange = onFractionalQuantity)
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        cost, onCost, Modifier.weight(1f).focusRequester(costFocus).bringIntoViewRequester(costBringIntoView).onFocusChanged {
                            if (it.isFocused) bringFocusedFieldIntoView(costBringIntoView)
                        },
                        label = { Text(tr("Cost / ${unitShort(unit)} ₹", "लागत / ${unitHindi(unit)} ₹"), maxLines = 1, fontSize = 13.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { priceFocus.requestFocus() }),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp), shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        price, onPrice, Modifier.weight(1f).focusRequester(priceFocus).bringIntoViewRequester(priceBringIntoView).onFocusChanged {
                            if (it.isFocused) bringFocusedFieldIntoView(priceBringIntoView)
                        },
                        label = { Text(tr("Selling price / ${unitShort(unit)} ₹", "विक्रय मूल्य / ${unitHindi(unit)} ₹"), maxLines = 1, fontSize = 13.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); focusManager.clearFocus() }),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp), shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        }
        Button(
            onClick = onSave,
            enabled = !saving && name.isNotBlank() && category.isNotBlank() && price.isNotBlank() && cost.isNotBlank() && validOpeningStock,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp).height(58.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (saving) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
            else Icon(Icons.Rounded.CheckCircle, null)
            Spacer(Modifier.width(10.dp)); Text(
                when {
                    saving -> tr("Saving cloth…", "कपड़ा सहेज रहे हैं…")
                    !connected -> tr("Sign in to save", "सहेजने के लिए साइन इन करें")
                    else -> tr("Save cloth", "कपड़ा सहेजें")
                },
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    if (categoryPickerOpen) {
        val categories = (clothCategories + "Other").distinct()
        val filteredCategories = categories.filter {
            it.contains(categorySearch.trim(), ignoreCase = true) ||
                (it == "Other" && tr("Other", "अन्य").contains(categorySearch.trim(), ignoreCase = true))
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { categoryPickerOpen = false },
            title = { Text(tr("Choose a category", "श्रेणी चुनें")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = categorySearch,
                        onValueChange = { categorySearch = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(tr("Search categories", "श्रेणी खोजें")) },
                        shape = RoundedCornerShape(14.dp)
                    )
                    LazyColumn(Modifier.heightIn(max = (screenHeightDp * 0.42f).coerceAtLeast(180f).dp)) {
                        items(filteredCategories) { option ->
                            TextButton(
                                onClick = { onCategory(option); categoryPickerOpen = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (option == "Other") tr("Other", "अन्य") else option, modifier = Modifier.fillMaxWidth(), fontSize = 15.sp, color = Ink)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { categoryPickerOpen = false }) { Text(tr("Close", "बंद करें")) }
            }
        )
    }
    if (showClearConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text(tr("Clear cloth form?", "कपड़े का फ़ॉर्म साफ़ करें?")) },
            text = { Text(tr("This will remove the current entries and saved draft.", "इससे मौजूदा जानकारी और सेव किया हुआ ड्राफ़्ट हट जाएगा।")) },
            confirmButton = {
                TextButton(onClick = { showClearConfirmation = false; focusManager.clearFocus(); keyboard?.hide(); onClear() }) {
                    Text(tr("Clear form", "फ़ॉर्म साफ़ करें"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text(tr("Keep editing", "एडिट जारी रखें")) }
            }
        )
    }
}
