package com.shop.salesapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import android.util.Log
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

private val Ink = Color(0xFF0A3155)
private val Teal = Color(0xFFB84A26)
private val Paper = Color(0xFFFFFAF2)
private val SoftBlue = Color(0xFFF5EAE0)
private val ClothActionBlue = Color(0xFFDCEBFA)
private val StockBadgeBlue = Color(0xFFEAF2F8)
private val StockIconBlue = Color(0xFF477DA8)
private val SoftGreen = Color(0xFFE5F3EF)
private val Muted = Color(0xFF626B73)
private val salesAppHindi = mutableStateOf(false)

private val clothCategories = listOf(
    "Innerwear · बनियान / अंडरवियर",
    "Socks & Handkerchiefs · मोज़े / रुमाल",
    "Nightwear & Home Wear · नाइटी / घर के कपड़े",
    "Dhoti, Lungi & Gamchha · धोती / लुंगी / गमछा",
    "Blankets & Razais · कंबल / रज़ाई",
    "Shawls, Loi & Stoles · शॉल / लोई / स्टोल",
    "Thermals · थर्मल",
    "Sweaters & Jackets · स्वेटर / जैकेट",
    "Winter Accessories · टोपी / मफलर / दस्ताने",
    "Sarees · साड़ियाँ",
    "Blouses & Petticoats · ब्लाउज़ / पेटीकोट",
    "Suit Materials & Dupattas · सूट का कपड़ा / दुपट्टा",
    "Women’s Readymade · लेडीज़ रेडीमेड",
    "Wedding & Party Wear · शादी / पार्टी के कपड़े",
    "Men’s Readymade · जेंट्स रेडीमेड",
    "Men’s Ethnic Wear · कुर्ता / पायजामा",
    "Kids’ Clothing · बच्चों के कपड़े",
    "Baby Wear & Cloth Essentials · नवजात बच्चों का सामान",
    "School Uniforms · स्कूल ड्रेस",
    "Bedsheets & Bed Covers · चादर / बेडशीट",
    "Pillows & Toshaks · तकिया / तोशक",
    "Mosquito Nets · मच्छरदानी",
    "Towels · तौलिया",
    "Curtains & Furnishing Covers · पर्दा / कवर",
    "Mats & Floor Textiles · दरी / पायदान",
    "Loose Fabric · मीटर वाला कपड़ा",
    "Tailoring Supplies · सिलाई का सामान",
    "Puja & Ceremony Cloth · पूजा का कपड़ा",
    "Stitching & Alterations · सिलाई / फिटिंग"
)

private suspend fun <T> runCatchingCancellable(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Throwable) {
    Result.failure(error)
}

@Composable
private fun tr(english: String, hindi: String): String = if (salesAppHindi.value) hindi else english

private fun localizedAppMessage(message: String, hindi: Boolean): String {
    if (!hindi) return message
    val translations = mapOf(
        "The request could not be completed. Try again or contact the manager." to "अनुरोध पूरा नहीं हो सका। कृपया फिर कोशिश करें या मैनेजर से बात करें।",
        "Request is empty or too large." to "अनुरोध खाली है या बहुत बड़ा है।",
        "Sales App is not fully configured. Ask the manager to finish setup." to "ऐप का सेटअप पूरा नहीं है। मैनेजर से सेटअप पूरा करने को कहें।",
        "Sign in to use Sales App." to "ऐप इस्तेमाल करने के लिए साइन इन करें।",
        "Your sign-in expired. Sign in again." to "आपका साइन-इन समाप्त हो गया है। फिर से साइन इन करें।",
        "Your sign-in is invalid or expired. Sign in again." to "आपका साइन-इन सही नहीं है या समाप्त हो गया है। फिर से साइन इन करें।",
        "Could not verify your Google sign-in." to "Google साइन-इन की पुष्टि नहीं हो सकी।",
        "Use a verified Google account." to "पुष्टि किया हुआ Google खाता इस्तेमाल करें।",
        "This Google account is not allowed to use Sales App." to "इस Google खाते को ऐप इस्तेमाल करने की अनुमति नहीं है।",
        "Google sign-in completed, but this build needs the server OAuth client ID." to "Google साइन-इन पूरा हुआ, लेकिन ऐप सेटअप अधूरा है। मैनेजर से संपर्क करें।",
        "Could not sign in. Try again or ask the manager to check access." to "साइन-इन नहीं हो सका। फिर कोशिश करें या मैनेजर से अनुमति जाँचने को कहें।",
        "Enter the customer name." to "ग्राहक का नाम लिखें।",
        "Enter a valid customer phone number." to "ग्राहक का सही फ़ोन नंबर लिखें।",
        "Enter a valid phone number first." to "पहले सही फ़ोन नंबर लिखें।",
        "A customer with this phone number already exists." to "इस फ़ोन नंबर वाला ग्राहक पहले से मौजूद है।",
        "That contact has no phone number. You can type it instead." to "इस संपर्क में फ़ोन नंबर नहीं है। नंबर खुद लिखें।",
        "Could not read the selected phone number. You can type it instead." to "संपर्क से फ़ोन नंबर नहीं मिल सका। नंबर खुद लिखें।",
        "Could not search customers." to "ग्राहक खोजे नहीं जा सके। इंटरनेट जाँचकर फिर कोशिश करें।",
        "Could not load this customer's sales." to "इस ग्राहक की बिक्री की जानकारी नहीं मिल सकी।",
        "Could not load this customer's order requests." to "इस ग्राहक के ऑर्डर की जानकारी नहीं मिल सकी।",
        "Sign in to Shop before loading order requests." to "ऑर्डर अनुरोध देखने से पहले Shop में साइन इन करें।",
        "Could not load order requests." to "ऑर्डर अनुरोध लोड नहीं हो सके।",
        "Could not load this customer's payment balance." to "इस ग्राहक का बकाया नहीं मिल सका।",
        "Sign in to Shop before saving a product picture." to "तस्वीर सहेजने से पहले Shop में साइन इन करें।",
        "Shop did not save the product picture. Try again." to "Shop में तस्वीर सहेजी नहीं गई। फिर कोशिश करें।",
        "Product picture saved to Shop." to "कपड़े की तस्वीर Shop में सहेज दी गई।",
        "Could not save this product picture." to "कपड़े की तस्वीर सहेजी नहीं जा सकी।",
        "Could not use this picture." to "यह तस्वीर इस्तेमाल नहीं हो सकी। दूसरी तस्वीर चुनें।",
        "The picture was not captured. Try again." to "तस्वीर नहीं ली जा सकी। फिर कोशिश करें।",
        "Could not open the camera." to "कैमरा नहीं खुल सका। कैमरे की अनुमति जाँचें।",
        "Sign in before syncing this payment. It is saved on this phone." to "भुगतान सिंक करने के लिए साइन इन करें। यह इस फ़ोन में सुरक्षित है।",
        "Payment saved. Shop sync will continue in the background." to "भुगतान सुरक्षित है। Shop में सिंक जारी रहेगा।",
        "Sign in to Shop before sharing this bill." to "बिल साझा करने से पहले Shop में साइन इन करें।",
        "Could not prepare this bill for sharing." to "बिल साझा करने के लिए तैयार नहीं हो सका।",
        "Could not create the QR label PDF." to "QR लेबल PDF नहीं बन सका।",
        "Connect to Shop before sending an order request." to "ऑर्डर भेजने से पहले Shop से जुड़ें।",
        "Enter a valid customer phone number and customer name." to "ग्राहक का सही फ़ोन नंबर और नाम लिखें।",
        "Add at least one cloth before sending the order." to "ऑर्डर भेजने से पहले कम से कम एक कपड़ा जोड़ें।",
        "Could not send the order request." to "ऑर्डर अनुरोध नहीं भेजा जा सका।",
        "Connect to Shop before saving customers." to "ग्राहक सहेजने से पहले Shop से जुड़ें।",
        "Could not save this customer." to "ग्राहक सहेजा नहीं जा सका।",
        "Could not scan. Type the cloth name or code." to "स्कैन नहीं हो सका। कपड़े का नाम या कोड लिखें।",
        "Could not scan. You can search by cloth name." to "स्कैन नहीं हो सका। कपड़े का नाम लिखकर खोजें।",
        "QR scanner could not start. Check your internet connection and Google Play services, then try again." to "QR स्कैनर शुरू नहीं हो सका। इंटरनेट और Google Play सेवाएँ जाँचकर फिर कोशिश करें।",
        "Connect the Shop API and sign in before saving clothes." to "कपड़े सहेजने से पहले Shop से जुड़ें और साइन इन करें।",
        "Could not save this cloth." to "कपड़ा सहेजा नहीं जा सका।",
        "Cloth not found in Shop. Check the QR label." to "Shop में यह कपड़ा नहीं मिला। QR लेबल जाँचें।",
        "Cloth not found in preview catalog. Search by name or code." to "नमूना सूची में कपड़ा नहीं मिला। नाम या कोड से खोजें।",
        "Could not look up this cloth." to "कपड़े की जानकारी नहीं मिल सकी।",
        "Connect the Shop API and sign in before saving bills." to "बिल सहेजने से पहले Shop से जुड़ें और साइन इन करें।",
        "Could not save the bill." to "बिल सहेजा नहीं जा सका।",
        "Sign in to Shop before syncing bills. Your saved bills are still available on this phone." to "बिल सिंक करने के लिए Shop में साइन इन करें। इस फ़ोन में सहेजे बिल उपलब्ध हैं।",
        "Could not sync bills. Saved bills are still available on this phone." to "बिल सिंक नहीं हो सके। इस फ़ोन में सहेजे बिल उपलब्ध हैं।",
        "Sign in to Shop before syncing balances. Saved balances are still available on this phone." to "बकाया सिंक करने के लिए Shop में साइन इन करें। इस फ़ोन में सहेजी जानकारी उपलब्ध है।",
        "Could not sync outstanding balances." to "बकाया सिंक नहीं हो सका।",
        "Could not open the bill PDF." to "बिल PDF नहीं खुल सका।",
        "Could not load clothes from Shop." to "Shop से कपड़ों की सूची नहीं मिल सकी। सिंक करके फिर कोशिश करें।",
        "Sign in to Shop before syncing the catalog." to "कैटलॉग सिंक करने से पहले Shop में साइन इन करें।",
        "Catalog synced from Shop." to "कैटलॉग Shop से सिंक हो गया।",
        "Could not sync the catalog from Shop." to "कैटलॉग Shop से सिंक नहीं हो सका। इंटरनेट जाँचकर फिर कोशिश करें।",
        "The Inventory tab was not found." to "Shop शीट में Inventory टैब नहीं मिला। मैनेजर से संपर्क करें।",
        "Product picture is too large. Choose a smaller picture." to "तस्वीर बहुत बड़ी है। छोटी तस्वीर चुनें।",
        "Could not load the product picture." to "कपड़े की तस्वीर लोड नहीं हो सकी।",
        "Choose a product before adding its picture." to "तस्वीर जोड़ने से पहले कपड़ा चुनें।",
        "Choose a picture before saving." to "सहेजने से पहले तस्वीर चुनें।",
        "This product was not found in Shop. Sync products and try again." to "Shop में यह कपड़ा नहीं मिला। सूची सिंक करके फिर कोशिश करें।",
        "Add between 1 and 100 clothes before sending the order." to "ऑर्डर भेजने के लिए 1 से 100 कपड़े जोड़ें।",
        "Enter the customer name before sending this order." to "ऑर्डर भेजने से पहले ग्राहक का नाम लिखें।",
        "Enter a valid phone number to search bills." to "बिल खोजने के लिए सही फ़ोन नंबर लिखें।",
        "Choose a bill before sharing its PDF." to "PDF साझा करने से पहले बिल चुनें।",
        "This bill was not found in Shop. Sync bills and try again." to "Shop में यह बिल नहीं मिला। बिल सिंक करके फिर कोशिश करें।",
        "The PDF link for this bill is missing. Ask the manager to check the invoice." to "इस बिल की PDF लिंक नहीं मिली। मैनेजर से बिल जाँचने को कहें।",
        "The bill PDF is not available in the private invoice folder." to "बिल PDF निजी फ़ोल्डर में उपलब्ध नहीं है। मैनेजर से संपर्क करें।",
        "Bill items are incomplete, so its PDF cannot be refreshed. Ask the manager to check this bill." to "बिल की जानकारी अधूरी है, इसलिए PDF अपडेट नहीं हुई। मैनेजर से बिल जाँचने को कहें।",
        "This bill PDF is too large to share from the phone." to "बिल PDF फ़ोन से साझा करने के लिए बहुत बड़ी है।",
        "Choose the bill this payment is for." to "भुगतान के लिए बिल चुनें।",
        "Enter a whole-number payment amount greater than zero." to "शून्य से अधिक, पूरे रुपये में भुगतान राशि लिखें।",
        "Choose Cash, UPI, or Card for this payment." to "भुगतान का तरीका Cash, UPI या Card चुनें।",
        "That bill was not found for this customer." to "इस ग्राहक का बिल नहीं मिला।",
        "This bill does not have a valid total." to "इस बिल की कुल राशि सही नहीं है।",
        "This payment reference was already used for a different bill." to "यह भुगतान संदर्भ किसी दूसरे बिल में इस्तेमाल हो चुका है।",
        "This bill is already paid in full." to "इस बिल का पूरा भुगतान हो चुका है।",
        "This payment reference was already used for a different customer." to "यह भुगतान संदर्भ किसी दूसरे ग्राहक में इस्तेमाल हो चुका है।",
        "This customer has no unpaid bills." to "इस ग्राहक का कोई बकाया बिल नहीं है।",
        "The payment reference is invalid. Try again." to "भुगतान संदर्भ सही नहीं है। फिर कोशिश करें।",
        "Enter a product name, valid prices, and a whole-number quantity." to "कपड़े का नाम, सही कीमतें और पूरी संख्या में मात्रा लिखें।",
        "Choose Cash, UPI, Card, or Credit." to "Cash, UPI, Card या उधार चुनें।",
        "Add between 1 and 100 clothes before saving the bill." to "बिल सहेजने से पहले 1 से 100 कपड़े जोड़ें।",
        "One or more bill items are missing a cloth code. Remove the incomplete item and add it again." to "एक या अधिक कपड़ों का कोड नहीं है। अधूरा कपड़ा हटाकर फिर जोड़ें।",
        "This number is new. Enter the customer name." to "यह नया नंबर है। ग्राहक का नाम लिखें।",
        "A selected cloth is missing its name in Inventory. Ask the manager to correct it before billing." to "Inventory में चुने कपड़े का नाम नहीं है। बिल बनाने से पहले मैनेजर से ठीक करवाएँ।",
        "Some bill details are incomplete. Check the cloth, price, and quantity, then try again." to "बिल की कुछ जानकारी अधूरी है। कपड़ा, कीमत और मात्रा जाँचकर फिर कोशिश करें।",
        "The bill total must be greater than zero. Check the item prices and discounts, then try again." to "बिल की कुल राशि शून्य से अधिक होनी चाहिए। कीमत और छूट जाँचकर फिर कोशिश करें।",
        "Discount is too high. The bill total cannot be below the combined purchase cost of the items." to "छूट बहुत ज़्यादा है। बिल की कुल राशि सभी कपड़ों की संयुक्त खरीद कीमत से कम नहीं हो सकती।",
        "Discount cannot be more than the bill subtotal." to "छूट बिल की उप-कुल राशि से अधिक नहीं हो सकती।",
        "Enter a valid bill discount." to "बिल की सही छूट दर्ज करें।",
        "Choose Cash, UPI, or Card for the amount received." to "मिली हुई राशि का तरीका Cash, UPI या Card चुनें।",
        "Another sale is being saved. Please try again in a moment." to "दूसरी बिक्री सहेजी जा रही है। थोड़ी देर में फिर कोशिश करें।",
        "There is not enough room for all bill items. Ask the manager to make space in Sales Items." to "बिल के सभी कपड़ों के लिए जगह नहीं है। मैनेजर से Sales Items में जगह बनाने को कहें।",
        "The Order Requests tab is full. Ask the manager to make space." to "Order Requests टैब भर गया है। मैनेजर से जगह बनाने को कहें।"
    )
    translations[message]?.let { return it }
    return when {
        message.startsWith("That product name already exists. SKU:") -> "यह कपड़ा पहले से मौजूद है। SKU: ${message.substringAfter("SKU:").trim()}"
        message.startsWith("Cloth ") && message.contains(" was not found.") -> "कपड़े का कोड ${message.substringAfter("Cloth ").substringBefore(" was not found.")} नहीं मिला। इसे हटाकर फिर जोड़ें।"
        message.startsWith("Enter a whole-number quantity for ") -> "${message.substringAfter("Enter a whole-number quantity for ").removeSuffix(".")} की मात्रा पूरी संख्या में लिखें।"
        message.startsWith("Only ") && message.contains(" units of ") -> {
            val match = Regex("Only (.+) units of (.+) are in stock\\.").matchEntire(message)
            if (match != null) "${match.groupValues[2]} का स्टॉक केवल ${match.groupValues[1]} है।" else "स्टॉक उपलब्ध नहीं है। मात्रा जाँचें।"
        }
        message.startsWith("A valid selling price is missing for ") -> "${message.substringAfter("A valid selling price is missing for ").substringBefore(". Ask")} की बेचने की कीमत दर्ज नहीं है। मैनेजर से ठीक करवाएँ।"
        message.startsWith("Stock information is incomplete for ") -> "${message.substringAfter("Stock information is incomplete for ").substringBefore(". Ask")} का स्टॉक दर्ज नहीं है। मैनेजर से ठीक करवाएँ।"
        message.startsWith("Cost information is invalid for ") -> "${message.substringAfter("Cost information is invalid for ").substringBefore(". Ask")} की खरीद कीमत सही नहीं है। मैनेजर से ठीक करवाएँ।"
        message.startsWith("Enter a whole-number quantity greater than zero for ") -> "${message.substringAfter("Enter a whole-number quantity greater than zero for ").removeSuffix(".")} की मात्रा शून्य से अधिक पूरी संख्या में लिखें।"
        message.startsWith("Payment is greater than the current due of ₹") -> "भुगतान की राशि मौजूदा बकाया से अधिक है। राशि जाँचें।"
        message.startsWith("Payment is greater than the customer due of ₹") -> "भुगतान की राशि ग्राहक के कुल बकाया से अधिक है। राशि जाँचें।"
        message.startsWith("Amount received must be between ₹") -> "मिली हुई राशि शून्य और बिल की कुल राशि के बीच होनी चाहिए। राशि जाँचें।"
        message.startsWith("Unsupported sheet tab:") -> "Shop शीट में जरूरी टैब नहीं मिला। मैनेजर से संपर्क करें।"
        message.startsWith("Required tab '") || message.startsWith("Tab ") -> "Shop शीट में जरूरी कॉलम या टैब नहीं मिला। मैनेजर से संपर्क करें।"
        message.startsWith("Payment saved.") -> "भुगतान सुरक्षित है। Shop में सिंक जारी रहेगा।"
        message.startsWith("Order ") && message.contains(" sent to the manager.") -> "ऑर्डर मैनेजर को भेज दिया गया है।"
        message.endsWith(" saved to customers.") -> "ग्राहक की जानकारी सहेज दी गई।"
        message.startsWith("Scanned ") -> "स्कैन हुआ: ${message.removePrefix("Scanned ")}"
        message.startsWith("Cloth saved, but") -> "कपड़ा सहेजा गया, लेकिन सूची अपडेट नहीं हुई। कपड़ों को सिंक करके QR लेबल बनाएँ।"
        else -> "कुछ समस्या हुई। कृपया फिर कोशिश करें।"
    }
}

data class CartLine(val product: Product, val quantity: Double)

private fun formatQuantity(quantity: Double): String = BigDecimal.valueOf(quantity)
    .setScale(3, RoundingMode.HALF_UP)
    .stripTrailingZeros()
    .toPlainString()

private fun lineAmount(price: Int, quantity: Double): Int = (price * quantity).roundToInt()

private fun validCartQuantity(line: CartLine): Boolean {
    val scaled = line.quantity * 1000
    val hasAtMostThreeDecimals = kotlin.math.abs(scaled - scaled.roundToInt()) < 0.001
    return line.quantity > 0 && line.quantity <= line.product.stock && hasAtMostThreeDecimals &&
        (line.product.fractionalQuantity || line.quantity % 1.0 == 0.0)
}

@Composable
private fun CartQuantityControl(
    line: CartLine,
    onChange: (String, Double) -> Unit,
    onSet: (String, Double) -> Unit,
    compact: Boolean = false
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
            val next = minOf(line.product.stock, line.quantity + step)
            quantityText = formatQuantity(next)
            onChange(line.product.sku, step)
        }, modifier = if (compact) Modifier.size(36.dp) else Modifier) { Icon(Icons.Rounded.Add, contentDescription = tr("Increase quantity", "मात्रा बढ़ाएँ"), modifier = Modifier.size(if (compact) 18.dp else 24.dp)) }
    }
}
private data class CustomerHistoryItem(
    val key: String,
    val date: String,
    val invoice: ShopInvoice? = null,
    val orderRequest: CustomerOrderRequest? = null
)

private fun addOptimisticPayment(finance: CustomerFinance, payment: PaymentReceipt): CustomerFinance {
    if (finance.payments.any { it.requestId == payment.requestId || it.receiptNumber == payment.receiptNumber }) return finance
    return finance.copy(
        totalPaid = finance.totalPaid + payment.amount,
        currentDue = (finance.currentDue - payment.amount).coerceAtLeast(0),
        payments = (listOf(payment) + finance.payments).take(50)
    )
}

private fun mergeLocalPaymentLog(finance: CustomerFinance, localPayments: List<PaymentReceipt>): CustomerFinance {
    val unmatched = localPayments.filter { local ->
        local.syncStatus != "SYNCED" &&
        finance.payments.none { remote ->
            (local.requestId.isNotBlank() && remote.requestId == local.requestId) ||
                (local.receiptNumber.isNotBlank() && remote.receiptNumber == local.receiptNumber)
        }
    }
    val pendingAmount = unmatched.sumOf { it.amount }
    return finance.copy(
        totalPaid = finance.totalPaid + pendingAmount,
        currentDue = (finance.currentDue - pendingAmount).coerceAtLeast(0),
        payments = (unmatched + finance.payments).sortedByDescending { it.date }.take(50)
    )
}

private fun confirmOptimisticPayment(finance: CustomerFinance, requestId: String, payment: PaymentReceipt): CustomerFinance {
    val represented = finance.payments.any {
        (requestId.isNotBlank() && it.requestId == requestId) || it.receiptNumber == payment.receiptNumber
    }
    val withoutPending = finance.payments.filterNot {
        (requestId.isNotBlank() && it.requestId == requestId) || it.receiptNumber == payment.receiptNumber
    }
    val updated = finance.copy(payments = (listOf(payment) + withoutPending).take(50))
    return if (represented) updated else addOptimisticPayment(updated, payment)
}

private fun replaceLocalPayment(finance: CustomerFinance, requestId: String, payment: PaymentReceipt): CustomerFinance =
    finance.copy(payments = (listOf(payment) + finance.payments.filterNot { it.requestId == requestId }).take(50))

private fun mergePaymentHistory(remote: List<PaymentReceipt>, local: List<PaymentReceipt>): List<PaymentReceipt> {
    val merged = remote.toMutableList()
    local.filter { it.syncStatus != "SYNCED" }.forEach { payment ->
        val alreadyPresent = merged.any {
            (payment.requestId.isNotBlank() && it.requestId == payment.requestId) ||
                (payment.receiptNumber.isNotBlank() && it.receiptNumber == payment.receiptNumber)
        }
        if (!alreadyPresent) merged.add(payment)
    }
    return merged.sortedByDescending { it.date }.take(200)
}

private fun newLocalPayment(
    invoiceNumber: String,
    amount: Int,
    method: String,
    note: String,
    customer: ShopCustomer
): PaymentReceipt {
    val requestId = java.util.UUID.randomUUID().toString()
    val timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    val localId = "LOCAL-$requestId"
    return PaymentReceipt(
        id = localId,
        receiptNumber = localId,
        date = timestamp,
        invoiceNumber = invoiceNumber,
        amount = amount,
        method = method,
        note = note,
        syncStatus = "SYNCING",
        requestId = requestId,
        customerName = customer.name,
        customerPhone = customer.phone
    )
}

private fun applyPaymentAllocations(
    invoices: List<ShopInvoice>,
    allocations: List<CustomerPaymentAllocation>,
    method: String
): List<ShopInvoice> {
    val byInvoice = allocations.associateBy { it.invoiceNumber }
    return invoices.map { invoice ->
        val allocation = byInvoice[invoice.number] ?: return@map invoice
        val due = allocation.dueAfter.coerceAtLeast(0)
        invoice.copy(paid = (invoice.total - due).coerceAtLeast(0), due = due, paymentMode = if (due == 0) method else "Credit")
    }
}

private enum class Screen { Home, BillCenter, Bill, Bills, Customer, CustomerDetail, AddCustomer, ProductCenter, Search, Catalog, ProductDetail, AddProduct, PlaceOrder, OrderRequests, OrderRequestDetail, Payments }
private data class DeletionTarget(val type: String, val recordId: String, val summary: String)

private val previewProducts = listOf(
    Product("SKU-001", "Wireless Bluetooth Earbuds", "Electronics", 1999, 37.0),
    Product("SKU-002", "Wireless Optical Mouse", "Peripherals", 799, 58.0),
    Product("SKU-004", "USB-C Fast Charging Cable", "Accessories", 349, 91.0)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SalesApp(operationScope = lifecycleScope) }
    }
}

@Composable
private fun SalesApp(operationScope: CoroutineScope) {
    var screen by remember { mutableStateOf(Screen.Home) }
    val backStack = remember { mutableStateListOf<Screen>() }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var isHindi by remember(context) {
        mutableStateOf(context.getSharedPreferences("sales_app_preferences", android.content.Context.MODE_PRIVATE).getBoolean("use_hindi", false))
    }
    LaunchedEffect(isHindi) { salesAppHindi.value = isHindi }
    val savedProducts = remember(context) { ProductCache.load(context) }
    var hasLiveProducts by remember(context) { mutableStateOf(savedProducts.isNotEmpty()) }
    var selectedTab by remember { mutableIntStateOf(-1) }
    var query by remember { mutableStateOf("") }
    var catalogQuery by remember { mutableStateOf("") }
    var selectedCatalogProduct by remember { mutableStateOf<Product?>(null) }
    var productSyncing by remember { mutableStateOf(false) }
    var customerDirectoryQuery by remember { mutableStateOf("") }
    var customerDirectoryLoading by remember { mutableStateOf(false) }
    var customerSyncing by remember { mutableStateOf(false) }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerEmail by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var customerExists by remember { mutableStateOf<Boolean?>(null) }
    var customerSearching by remember { mutableStateOf(false) }
    var customerMatches by remember { mutableStateOf<List<ShopCustomer>>(emptyList()) }
    var paymentMode by remember { mutableStateOf("Cash") }
    var creditAmountReceived by remember { mutableStateOf("0") }
    var creditPaymentMethod by remember { mutableStateOf("Cash") }
    var billDiscountValue by remember { mutableStateOf("0") }
    var productName by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("") }
    var productImageBase64 by remember { mutableStateOf("") }
    var productImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var productPhotoTargetSku by remember { mutableStateOf<String?>(null) }
    var uploadingProductPhotoSku by remember { mutableStateOf<String?>(null) }
    var pendingProductPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var sellingPrice by remember { mutableStateOf("") }
    var unitCost by remember { mutableStateOf("") }
    var openingStock by remember { mutableStateOf("") }
    var fractionalQuantity by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var userRole by remember { mutableStateOf("SALESPERSON") }
    var pendingDeletionTarget by remember { mutableStateOf<DeletionTarget?>(null) }
    var deletionReason by remember { mutableStateOf("Mistake") }
    var submittingDeletion by remember { mutableStateOf(false) }
    val paymentDeletionRequests = remember { mutableStateListOf<DeletionRequest>() }
    val billDeletionRequests = remember { mutableStateListOf<DeletionRequest>() }
    val productDeletionRequests = remember { mutableStateListOf<DeletionRequest>() }
    val isAdministrator = userRole == "ADMIN"
    var latestPdfUrl by remember { mutableStateOf("") }
    var latestInvoice by remember { mutableStateOf<ShopInvoice?>(null) }
    var sharingBillNumber by remember { mutableStateOf<String?>(null) }
    var createdQrSku by remember { mutableStateOf<String?>(null) }
    var qrPreviewProduct by remember { mutableStateOf<Product?>(null) }
    var savingInvoice by remember { mutableStateOf(false) }
    var savingCloth by remember { mutableStateOf(false) }
    var billsPhone by remember { mutableStateOf("") }
    var billsLoading by remember { mutableStateOf(false) }
    var paymentDirectoryQuery by remember { mutableStateOf("") }
    var orderMode by remember { mutableStateOf(false) }
    var orderNotes by remember { mutableStateOf("") }
    var sendingOrderRequest by remember { mutableStateOf(false) }
    var orderRequests by remember { mutableStateOf<List<CustomerOrderRequest>>(emptyList()) }
    var orderRequestsLoading by remember { mutableStateOf(false) }
    var selectedOrderRequest by remember { mutableStateOf<CustomerOrderRequest?>(null) }
    var selectedCustomer by remember { mutableStateOf<ShopCustomer?>(null) }
    var detailBillsLoading by remember { mutableStateOf(false) }
    var detailBills by remember { mutableStateOf<List<ShopInvoice>>(emptyList()) }
    var detailFinance by remember { mutableStateOf<CustomerFinance?>(null) }
    var detailFinanceLoading by remember { mutableStateOf(false) }
    var receivingPayment by remember { mutableStateOf(false) }
    var paymentListTab by remember { mutableStateOf("DUE") }
    var madePayments by remember(context) { mutableStateOf(LocalPaymentLog.loadAll(context)) }
    var detailOrders by remember { mutableStateOf<List<CustomerOrderRequest>>(emptyList()) }
    var detailOrdersLoading by remember { mutableStateOf(false) }
    var customerHistoryTab by remember(selectedCustomer?.phone) { mutableStateOf("Sales") }
    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerPhone by remember { mutableStateOf("") }
    var newCustomerEmail by remember { mutableStateOf("") }
    var newCustomerAddress by remember { mutableStateOf("") }
    var savingCustomer by remember { mutableStateOf(false) }
    val cart = remember { mutableStateListOf<CartLine>() }
    val bills = remember(context) { mutableStateListOf<ShopInvoice>().apply { addAll(BillCache.load(context)) } }
    val api = remember { ShopApiClient(BuildConfig.SHOP_API_BASE_URL) }
    val products = remember(context) {
        mutableStateListOf<Product>().apply { addAll(savedProducts.ifEmpty { previewProducts }) }
    }
    val cachedCustomers = remember(context) {
        mutableStateListOf<ShopCustomer>().apply { addAll(CustomerCache.load(context)) }
    }
    val lastAccount = remember { GoogleSignIn.getLastSignedInAccount(context) }
    var idToken by remember { mutableStateOf(lastAccount?.idToken) }

    fun dismissKeyboard() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }

    LaunchedEffect(screen) {
        if (screen != Screen.Payments) return@LaunchedEffect
        while (true) {
            madePayments = mergePaymentHistory(madePayments, LocalPaymentLog.loadAll(context))
            delay(1_500)
        }
    }

    fun navigateTo(destination: Screen) {
        if (screen != destination) {
            dismissKeyboard()
            backStack.add(screen)
            screen = destination
            selectedTab = when (destination) {
                Screen.BillCenter, Screen.Bill, Screen.Bills -> 0
                Screen.Customer, Screen.CustomerDetail, Screen.AddCustomer -> 1
                Screen.ProductCenter, Screen.Search, Screen.Catalog, Screen.ProductDetail, Screen.AddProduct, Screen.PlaceOrder, Screen.OrderRequests, Screen.OrderRequestDetail -> 2
                Screen.Payments -> 3
                else -> -1
            }
        }
    }

    fun navigateBack() {
        dismissKeyboard()
        screen = if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex) else Screen.Home
        selectedTab = when (screen) {
            Screen.BillCenter, Screen.Bill, Screen.Bills -> 0
            Screen.Customer, Screen.CustomerDetail, Screen.AddCustomer -> 1
            Screen.ProductCenter, Screen.Search, Screen.Catalog, Screen.ProductDetail, Screen.AddProduct, Screen.PlaceOrder, Screen.OrderRequests, Screen.OrderRequestDetail -> 2
            Screen.Payments -> 3
            else -> -1
        }
    }

    BackHandler(enabled = screen != Screen.Home) { navigateBack() }

    LaunchedEffect(idToken, api.isConfigured) {
        val token = idToken ?: return@LaunchedEffect
        if (!api.isConfigured) return@LaunchedEffect
        runCatchingCancellable {
            withContext(Dispatchers.IO) { api.bills(token, "") to api.payments(token) }
        }
            .onSuccess { (fresh, remotePayments) ->
                bills.clear(); bills.addAll(fresh)
                BillCache.save(context, fresh)
                madePayments = mergePaymentHistory(remotePayments, LocalPaymentLog.loadAll(context))
            }
            // This refresh is silent and runs at app launch/sign-in. Keep cached data
            // usable if the network is unavailable; the Payment tab has a manual retry.
    }

    val signInOptions = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .apply { if (BuildConfig.GOOGLE_SERVER_CLIENT_ID.isNotBlank()) requestIdToken(BuildConfig.GOOGLE_SERVER_CLIENT_ID) }
            .build()
    }
    val googleClient = remember(signInOptions) { GoogleSignIn.getClient(context, signInOptions) }
    var signingIn by remember { mutableStateOf(false) }
    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
        }.onSuccess { account ->
            idToken = account.idToken
            if (account.idToken.isNullOrBlank()) message = "Google sign-in completed, but this build needs the server OAuth client ID."
        }.onFailure { message = "Could not sign in. Try again or ask the manager to check access." }
    }

    fun beginSignIn() {
        if (signingIn) return
        dismissKeyboard()
        signingIn = true
        idToken = null
        backStack.clear()
        screen = Screen.Home
        selectedTab = -1
        googleClient.signOut().addOnCompleteListener {
            signingIn = false
            signInLauncher.launch(googleClient.signInIntent)
        }
    }

    LaunchedEffect(message) {
        val error = message.orEmpty().lowercase()
        if (error.contains("sign-in expired") || error.contains("expired sign-in") || error.contains("invalid or expired") || error.contains("sign in to use sales app")) {
            message = null
            beginSignIn()
        }
    }

    LaunchedEffect(idToken, api.isConfigured) {
        productSyncing = false
        val token = idToken ?: return@LaunchedEffect
        if (!api.isConfigured) return@LaunchedEffect
        productSyncing = true
        runCatchingCancellable { withContext(Dispatchers.IO) { api.products(token, "") } }
            .onSuccess { result ->
                val authoritative = ProductCache.merge(emptyList(), result)
                products.clear()
                products.addAll(authoritative)
                hasLiveProducts = true
                ProductCache.save(context, authoritative)
            }
            .onFailure { message = it.message ?: "Could not load clothes from Shop." }
        productSyncing = false
    }

    LaunchedEffect(idToken, api.isConfigured) {
        val token = idToken ?: return@LaunchedEffect
        if (!api.isConfigured) return@LaunchedEffect
        runCatchingCancellable { withContext(Dispatchers.IO) { api.customers(token) } }
            .onSuccess { downloaded ->
                val merged = CustomerCache.merge(cachedCustomers.toList(), downloaded)
                cachedCustomers.clear()
                cachedCustomers.addAll(merged)
                CustomerCache.save(context, merged)
            }
    }

    LaunchedEffect(customerPhone, idToken, api.isConfigured) {
        customerSearching = false
        customerExists = null
        val digits = CustomerCache.normalizePhone(customerPhone)
        customerMatches = if (digits.length >= 3) cachedCustomers
            .filter { CustomerCache.normalizePhone(it.phone).startsWith(digits) }
            .take(8) else emptyList()
        if (digits.length < 3) return@LaunchedEffect
        val exactLocal = customerMatches.firstOrNull { CustomerCache.normalizePhone(it.phone) == digits }
        if (exactLocal != null) {
            customerExists = true
            customerName = exactLocal.name
            customerEmail = exactLocal.email
            customerAddress = ""
        } else if (digits.length >= 10 && customerMatches.isEmpty()) {
            customerExists = false
        }
        val token = idToken ?: return@LaunchedEffect
        if (!api.isConfigured) return@LaunchedEffect
        customerSearching = true
        delay(350)
        runCatchingCancellable { withContext(Dispatchers.IO) { api.searchCustomers(token, customerPhone) } }
            .onSuccess { downloaded ->
                val merged = CustomerCache.merge(cachedCustomers.toList(), downloaded)
                cachedCustomers.clear()
                cachedCustomers.addAll(merged)
                CustomerCache.save(context, merged)
                val matches = downloaded.filter { CustomerCache.normalizePhone(it.phone).startsWith(digits) }.take(8)
                if (CustomerCache.normalizePhone(customerPhone) == digits) customerMatches = matches
                val exact = matches.firstOrNull { CustomerCache.normalizePhone(it.phone) == digits }
                if (exact != null) {
                    customerExists = true
                    customerName = exact.name
                    customerEmail = exact.email
                    customerAddress = ""
                    customerMatches = listOf(exact)
                } else if (digits.length >= 10) {
                    customerExists = false
                    customerMatches = matches
                    customerName = ""
                    customerEmail = ""
                    customerAddress = ""
                } else {
                    customerExists = null
                    customerMatches = matches
                }
            }
            .onFailure { message = it.message ?: "Could not search customers." }
        customerSearching = false
    }

    LaunchedEffect(screen, customerDirectoryQuery, idToken, api.isConfigured) {
        customerDirectoryLoading = false
        if (screen != Screen.Customer || idToken == null || !api.isConfigured) return@LaunchedEffect
        val digits = CustomerCache.normalizePhone(customerDirectoryQuery)
        if (digits.length < 8 || cachedCustomers.any { CustomerCache.normalizePhone(it.phone) == digits }) return@LaunchedEffect
        customerDirectoryLoading = true
        delay(250)
        val token = idToken ?: return@LaunchedEffect
        runCatchingCancellable { withContext(Dispatchers.IO) { api.findCustomer(token, customerDirectoryQuery) } }
            .onSuccess { customer ->
                if (customer != null) {
                    val merged = CustomerCache.merge(cachedCustomers.toList(), listOf(customer))
                    cachedCustomers.clear()
                    cachedCustomers.addAll(merged)
                    CustomerCache.save(context, merged)
                }
            }
            .onFailure { message = it.message ?: "Could not search customers." }
        customerDirectoryLoading = false
    }

    LaunchedEffect(screen, selectedCustomer?.phone, idToken, api.isConfigured) {
        val customer = selectedCustomer
        if (screen != Screen.CustomerDetail || customer == null) {
            detailBillsLoading = false
            detailOrdersLoading = false
            detailFinanceLoading = false
            return@LaunchedEffect
        }
        detailBillsLoading = true
        detailOrdersLoading = true
        detailFinanceLoading = true
        detailFinance = null
        val cached = withContext(Dispatchers.IO) { CustomerHistoryCache.load(context, customer.phone) }
        if (cached != null) {
            detailBills = cached.sales
            detailOrders = cached.orderRequests
            detailBillsLoading = false
            detailOrdersLoading = false
        }
        val token = idToken
        if (!api.isConfigured || token == null) {
            if (cached == null) {
                detailBills = emptyList()
                detailOrders = emptyList()
            }
            detailBillsLoading = false
            detailOrdersLoading = false
            detailFinanceLoading = false
            return@LaunchedEffect
        }
        val (salesResult, requestsResult, financeResult) = coroutineScope {
            val sales = async(Dispatchers.IO) { runCatchingCancellable { api.bills(token, customer.phone) } }
            val requests = async(Dispatchers.IO) { runCatchingCancellable { api.orderRequests(token, customer.phone) } }
            val finance = async(Dispatchers.IO) { runCatchingCancellable { api.customerFinance(token, customer.phone) } }
            Triple(sales.await(), requests.await(), finance.await())
        }
        salesResult.onSuccess { detailBills = it }
            .onFailure { message = it.message ?: "Could not load this customer's sales." }
        requestsResult.onSuccess { detailOrders = it }
            .onFailure { message = it.message ?: "Could not load this customer's order requests." }
        financeResult.onSuccess { serverFinance ->
            val localPayments = withContext(Dispatchers.IO) { LocalPaymentLog.load(context, customer.phone) }
            detailFinance = mergeLocalPaymentLog(serverFinance, localPayments)
        }
            .onFailure { message = it.message ?: "Could not load this customer's payment balance." }
        detailFinanceLoading = false
        if (salesResult.isSuccess && requestsResult.isSuccess) {
            withContext(Dispatchers.IO) {
                CustomerHistoryCache.save(context, customer.phone, salesResult.getOrThrow(), requestsResult.getOrThrow())
            }
        }
        detailBillsLoading = false
        detailOrdersLoading = false
    }

    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val phoneUri = result.data?.data ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.query(
                phoneUri,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                null
            )?.use { cursor ->
                val index = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index).orEmpty() else ""
            }.orEmpty()
        }.onSuccess { phone ->
            if (phone.isBlank()) message = "That contact has no phone number. You can type it instead."
            else customerPhone = phone
        }.onFailure { message = "Could not read the selected phone number. You can type it instead." }
    }

    fun processProductPhoto(original: Bitmap) {
        runCatching {
            val scale = minOf(1f, 900f / maxOf(original.width, original.height))
            val resized = if (scale < 1f) Bitmap.createScaledBitmap(original, (original.width * scale).toInt().coerceAtLeast(1), (original.height * scale).toInt().coerceAtLeast(1), true) else original
            val output = java.io.ByteArrayOutputStream()
            var quality = 82
            do {
                output.reset()
                resized.compress(Bitmap.CompressFormat.JPEG, quality, output)
                quality -= 8
            } while (output.size() > 110_000 && quality >= 42)
            Triple(Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP), resized, original)
        }.onSuccess { (imageBase64, resized, originalBitmap) ->
            productImageBase64 = imageBase64
            productImageBitmap = resized
            if (resized !== originalBitmap) originalBitmap.recycle()
            val targetSku = productPhotoTargetSku
            if (targetSku != null) {
                productPhotoTargetSku = null
                val token = idToken
                if (token == null || !api.isConfigured) {
                    message = "Sign in to Shop before saving a product picture."
                } else if (uploadingProductPhotoSku == null) {
                    operationScope.launch {
                        uploadingProductPhotoSku = targetSku
                        runCatchingCancellable {
                            withContext(Dispatchers.IO) { api.updateProductImage(token, targetSku, imageBase64) }
                        }.onSuccess { imageId ->
                            if (imageId.isBlank()) {
                                message = "Shop did not save the product picture. Try again."
                            } else {
                                withContext(Dispatchers.IO) {
                                    ProductImageCache.save(context, targetSku, imageId, Base64.decode(imageBase64, Base64.DEFAULT))
                                }
                                val updated = products.map { product ->
                                    if (product.sku == targetSku) product.copy(imageId = imageId) else product
                                }
                                products.clear()
                                products.addAll(updated)
                                ProductCache.save(context, updated)
                                selectedCatalogProduct = updated.firstOrNull { it.sku == targetSku } ?: selectedCatalogProduct
                                message = "Product picture saved to Shop."
                            }
                        }.onFailure { message = it.message ?: "Could not save this product picture." }
                        uploadingProductPhotoSku = null
                    }
                }
            }
        }.onFailure {
            productPhotoTargetSku = null
            message = it.message ?: "Could not use this picture."
        }
    }

    val productPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) {
            productPhotoTargetSku = null
        } else {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    ?: throw IllegalStateException("Could not open this picture.")
            }.onSuccess(::processProductPhoto).onFailure {
                productPhotoTargetSku = null
                message = it.message ?: "Could not use this picture."
            }
        }
    }

    val productCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingProductPhotoUri
        pendingProductPhotoUri = null
        if (!saved || uri == null) {
            productPhotoTargetSku = null
            if (!saved) message = "The picture was not captured. Try again."
        } else {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    ?: throw IllegalStateException("Could not open the captured picture.")
            }.onSuccess(::processProductPhoto).onFailure {
                productPhotoTargetSku = null
                message = it.message ?: "Could not use the captured picture."
            }
        }
    }

    fun takeProductPhoto() {
        runCatching {
            val directory = File(context.cacheDir, "product-captures")
            if (!directory.exists() && !directory.mkdirs()) throw IllegalStateException("Could not prepare the camera picture.")
            val output = File.createTempFile("product_", ".jpg", directory)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", output)
        }.onSuccess { uri ->
            pendingProductPhotoUri = uri
            productCamera.launch(uri)
        }.onFailure {
            productPhotoTargetSku = null
            message = it.message ?: "Could not open the camera."
        }
    }

    val scanner = remember {
        GmsBarcodeScanning.getClient(
            context,
            GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .enableAutoZoom()
                .build()
        )
    }
    val scannerModuleClient = remember { ModuleInstall.getClient(context) }
    var scannerPreparing by remember { mutableStateOf(false) }

    fun scanQr(onScanned: (String) -> Unit) {
        if (scannerPreparing) return
        scannerPreparing = true

        fun showScannerFailure(error: Exception) {
            scannerPreparing = false
            Log.e("SalesAppScanner", "Google Code Scanner failed", error)
            message = "QR scanner could not start. Check your internet connection and Google Play services, then try again."
        }

        fun openScanner() {
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    scannerPreparing = false
                    onScanned(barcode.rawValue.orEmpty())
                }
                .addOnCanceledListener { scannerPreparing = false }
                .addOnFailureListener(::showScannerFailure)
        }

        val installListener = object : InstallStatusListener {
            override fun onInstallStatusUpdated(update: ModuleInstallStatusUpdate) {
                when (update.installState) {
                    ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED -> {
                        scannerModuleClient.unregisterListener(this)
                        openScanner()
                    }
                    ModuleInstallStatusUpdate.InstallState.STATE_FAILED,
                    ModuleInstallStatusUpdate.InstallState.STATE_CANCELED -> {
                        scannerModuleClient.unregisterListener(this)
                        showScannerFailure(IllegalStateException("Google Play services could not install the QR scanner module (state ${update.installState})."))
                    }
                }
            }
        }

        scannerModuleClient.areModulesAvailable(scanner)
            .addOnSuccessListener { availability ->
                if (availability.areModulesAvailable()) {
                    openScanner()
                } else {
                    val request = ModuleInstallRequest.newBuilder()
                        .addApi(scanner)
                        .setListener(installListener)
                        .build()
                    scannerModuleClient.installModules(request)
                        .addOnSuccessListener { response ->
                            if (response.areModulesAlreadyInstalled()) openScanner()
                        }
                        .addOnFailureListener(::showScannerFailure)
                }
            }
            .addOnFailureListener(::showScannerFailure)
    }

    fun syncPayment(customer: ShopCustomer, payment: PaymentReceipt) {
        val token = idToken
        if (token == null || !api.isConfigured) {
            message = "Sign in before syncing this payment. It is saved on this phone."
            return
        }
        val syncing = payment.copy(syncStatus = "SYNCING", customerName = customer.name, customerPhone = customer.phone)
        LocalPaymentLog.save(context, customer.phone, syncing)
        madePayments = mergePaymentHistory(madePayments, LocalPaymentLog.loadAll(context))
        if (selectedCustomer?.phone == customer.phone) {
            detailFinance = detailFinance?.let { replaceLocalPayment(it, syncing.requestId, syncing) }
        }
        PaymentSyncWorker.enqueue(context, syncing.requestId)
        message = "Payment saved. Shop sync will continue in the background."
    }

    fun syncProductsFromShop(showSuccess: Boolean = true) {
        val token = idToken
        if (token == null || !api.isConfigured) {
            message = "Sign in to Shop before syncing the catalog."
            return
        }
        if (productSyncing) return
        operationScope.launch {
            productSyncing = true
            try {
                runCatchingCancellable { withContext(Dispatchers.IO) { api.products(token, "") } }
                    .onSuccess { freshProducts ->
                        val synced = ProductCache.merge(emptyList(), freshProducts)
                        products.clear()
                        products.addAll(synced)
                        hasLiveProducts = true
                        ProductCache.save(context, synced)
                        if (showSuccess) message = "Catalog synced from Shop."
                    }
                    .onFailure { message = it.message ?: "Could not sync the catalog from Shop." }
            } finally {
                productSyncing = false
            }
        }
    }

    fun syncCustomersFromShop() {
        val token = idToken
        if (token == null || !api.isConfigured) {
            message = "Sign in to Shop before syncing customers."
            return
        }
        if (customerSyncing) return
        operationScope.launch {
            customerSyncing = true
            runCatchingCancellable { withContext(Dispatchers.IO) { api.customers(token) } }
                .onSuccess { freshCustomers ->
                    val authoritative = CustomerCache.merge(emptyList(), freshCustomers)
                    cachedCustomers.clear()
                    cachedCustomers.addAll(authoritative)
                    CustomerCache.save(context, authoritative)
                    message = "Customers synced from Shop."
                }
                .onFailure { message = it.message ?: "Could not sync customers from Shop." }
            customerSyncing = false
        }
    }

    fun syncCustomerDetails(customer: ShopCustomer) {
        val token = idToken
        if (token == null || !api.isConfigured) {
            message = "Sign in to Shop before syncing this customer."
            return
        }
        if (detailBillsLoading || detailOrdersLoading || detailFinanceLoading) return
        operationScope.launch {
            detailBillsLoading = true
            detailOrdersLoading = true
            detailFinanceLoading = true
            coroutineScope {
                val sales = async(Dispatchers.IO) { runCatchingCancellable { api.bills(token, customer.phone) } }
                val requests = async(Dispatchers.IO) { runCatchingCancellable { api.orderRequests(token, customer.phone) } }
                val finance = async(Dispatchers.IO) { runCatchingCancellable { api.customerFinance(token, customer.phone) } }
                val salesResult = sales.await()
                val requestsResult = requests.await()
                val financeResult = finance.await()
                salesResult.onSuccess { detailBills = it }
                    .onFailure { message = it.message ?: "Could not sync this customer's bills." }
                requestsResult.onSuccess { detailOrders = it }
                    .onFailure { message = it.message ?: "Could not sync this customer's order requests." }
                financeResult.onSuccess { serverFinance ->
                    val localPayments = withContext(Dispatchers.IO) { LocalPaymentLog.load(context, customer.phone) }
                    detailFinance = mergeLocalPaymentLog(serverFinance, localPayments)
                }.onFailure { message = it.message ?: "Could not sync this customer's payment history." }
                if (salesResult.isSuccess && requestsResult.isSuccess) {
                    withContext(Dispatchers.IO) {
                        CustomerHistoryCache.save(context, customer.phone, salesResult.getOrThrow(), requestsResult.getOrThrow())
                    }
                }
            }
            detailBillsLoading = false
            detailOrdersLoading = false
            detailFinanceLoading = false
        }
    }

    fun refreshDeletionRequests(type: String) {
        val token = idToken ?: return
        if (!isAdministrator || !api.isConfigured) return
        operationScope.launch {
            runCatchingCancellable { withContext(Dispatchers.IO) { api.deletionRequests(token, type) } }
                .onSuccess { requests ->
                    val target = when (type) {
                        "PAYMENT" -> paymentDeletionRequests
                        "BILL" -> billDeletionRequests
                        else -> productDeletionRequests
                    }
                    target.clear()
                    target.addAll(requests)
                }
        }
    }

    fun syncBillsAndPayments(token: String) {
        operationScope.launch {
            runCatchingCancellable {
                withContext(Dispatchers.IO) { api.bills(token, "") to api.payments(token) }
            }.onSuccess { (freshBills, freshPayments) ->
                bills.clear(); bills.addAll(freshBills)
                BillCache.save(context, freshBills)
                madePayments = mergePaymentHistory(freshPayments, LocalPaymentLog.loadAll(context))
            }
        }
    }

    fun requestEntryDeletion(type: String, recordId: String, summary: String) {
        if (idToken == null || !api.isConfigured) {
            message = "Sign in to Shop before requesting a correction."
            return
        }
        deletionReason = "Mistake"
        pendingDeletionTarget = DeletionTarget(type, recordId, summary)
    }

    fun submitEntryDeletionRequest() {
        val token = idToken ?: return
        val target = pendingDeletionTarget ?: return
        if (submittingDeletion) return
        operationScope.launch {
            submittingDeletion = true
            runCatchingCancellable {
                withContext(Dispatchers.IO) { api.requestDeletion(token, target.type, target.recordId, deletionReason) }
            }.onSuccess {
                pendingDeletionTarget = null
                message = "Deletion request sent to the administrator."
                if (isAdministrator) refreshDeletionRequests(target.type)
            }.onFailure { message = it.message ?: "Could not send the request." }
            submittingDeletion = false
        }
    }

    fun reviewEntryDeletionRequest(type: String, request: DeletionRequest, approve: Boolean) {
        val token = idToken ?: return
        operationScope.launch {
            runCatchingCancellable {
                withContext(Dispatchers.IO) { api.reviewDeletionRequest(token, request.id, approve) }
                if (approve && type != "PRODUCT") {
                    val (freshBills, freshPayments) = withContext(Dispatchers.IO) {
                        api.bills(token, "") to api.payments(token)
                    }
                    bills.clear()
                    bills.addAll(freshBills)
                    BillCache.save(context, freshBills)
                    madePayments = mergePaymentHistory(freshPayments, LocalPaymentLog.loadAll(context))
                }
            }.onSuccess {
                message = if (approve) "Request approved. Shop records were updated." else "Request rejected."
                refreshDeletionRequests(type)
                if (approve && type == "PRODUCT") syncProductsFromShop(showSuccess = false)
            }.onFailure { message = it.message ?: "Could not review this request." }
        }
    }

    fun setClothAvailability(product: Product, available: Boolean) {
        val token = idToken ?: return
        operationScope.launch {
            runCatchingCancellable {
                withContext(Dispatchers.IO) {
                    api.setProductAvailability(token, product.sku, available)
                    api.products(token, "")
                }
            }.onSuccess { fresh ->
                products.clear()
                products.addAll(ProductCache.merge(emptyList(), fresh))
                ProductCache.save(context, products)
                selectedCatalogProduct = products.firstOrNull { it.sku == product.sku } ?: selectedCatalogProduct
                message = if (available) "Cloth is available again." else "Cloth is now unavailable."
            }.onFailure { message = it.message ?: "Could not update cloth availability." }
        }
    }

    fun updateClothDetails(
        product: Product,
        name: String,
        category: String,
        price: Int,
        fractionalQuantity: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        val token = idToken
        if (token == null || !isAdministrator) {
            message = "Only an administrator can edit cloth details."
            onComplete(false)
            return
        }
        operationScope.launch {
            runCatchingCancellable {
                withContext(Dispatchers.IO) {
                    api.updateProduct(token, product.sku, name, category, price, fractionalQuantity)
                    api.products(token, "")
                }
            }.onSuccess { fresh ->
                products.clear()
                products.addAll(ProductCache.merge(emptyList(), fresh))
                ProductCache.save(context, products)
                selectedCatalogProduct = products.firstOrNull { it.sku == product.sku }
                message = "Cloth details updated."
                onComplete(true)
            }.onFailure { error ->
                message = error.message ?: "Could not update cloth details."
                onComplete(false)
            }
        }
    }

    fun addStockToCloth(product: Product, quantity: Double, reason: String, onComplete: (Boolean) -> Unit) {
        val token = idToken
        if (token == null || !isAdministrator) {
            message = "Only an administrator can add cloth stock."
            onComplete(false)
            return
        }
        operationScope.launch {
            runCatchingCancellable {
                withContext(Dispatchers.IO) {
                    api.addStockAdjustment(token, product.sku, quantity, reason)
                    api.products(token, "")
                }
            }.onSuccess { fresh ->
                products.clear()
                products.addAll(ProductCache.merge(emptyList(), fresh))
                ProductCache.save(context, products)
                selectedCatalogProduct = products.firstOrNull { it.sku == product.sku }
                message = "Stock added to Shop."
                onComplete(true)
            }.onFailure { error ->
                message = error.message ?: "Could not add cloth stock."
                onComplete(false)
            }
        }
    }

    LaunchedEffect(idToken, api.isConfigured) {
        userRole = "SALESPERSON"
        val token = idToken ?: return@LaunchedEffect
        if (!api.isConfigured) return@LaunchedEffect
        runCatchingCancellable { withContext(Dispatchers.IO) { api.session(token) } }
            .onSuccess { userRole = it.role }
    }

    LaunchedEffect(screen, userRole, idToken, api.isConfigured) {
        if (!isAdministrator || !api.isConfigured || idToken == null) return@LaunchedEffect
        val type = when (screen) {
            Screen.Payments -> "PAYMENT"
            Screen.Bills -> "BILL"
            Screen.Catalog, Screen.ProductCenter -> "PRODUCT"
            else -> return@LaunchedEffect
        }
        val token = idToken ?: return@LaunchedEffect
        while (true) {
            runCatchingCancellable { withContext(Dispatchers.IO) { api.deletionRequests(token, type) } }
                .onSuccess { requests ->
                    val target = when (type) {
                        "PAYMENT" -> paymentDeletionRequests
                        "BILL" -> billDeletionRequests
                        else -> productDeletionRequests
                    }
                    target.clear()
                    target.addAll(requests)
                }
            delay(30_000)
        }
    }

    LaunchedEffect(screen, idToken) {
        if (screen !in setOf(Screen.Catalog, Screen.Search, Screen.OrderRequests, Screen.OrderRequestDetail) || idToken == null || !api.isConfigured) return@LaunchedEffect
        while (true) {
            if (!productSyncing) syncProductsFromShop(showSuccess = false)
            delay(30_000)
        }
    }

    LaunchedEffect(screen, idToken, api.isConfigured) {
        if (screen !in setOf(Screen.Bills, Screen.Payments) || idToken == null || !api.isConfigured) return@LaunchedEffect
        val token = idToken ?: return@LaunchedEffect
        while (true) {
            if (!billsLoading) {
                billsLoading = true
                try {
                    runCatchingCancellable {
                        withContext(Dispatchers.IO) { api.bills(token, "") to api.payments(token) }
                    }.onSuccess { (freshBills, freshPayments) ->
                        bills.clear()
                        bills.addAll(freshBills)
                        BillCache.save(context, freshBills)
                        madePayments = mergePaymentHistory(freshPayments, LocalPaymentLog.loadAll(context))
                    }
                    // Keep showing the last cached data if the other phone is offline or Shop is unavailable.
                } finally {
                    billsLoading = false
                }
            }
            delay(30_000)
        }
    }

    fun shareBillPdf(bill: ShopInvoice) {
        val token = idToken
        if (!api.isConfigured || token == null) {
            message = "Sign in to Shop before sharing this bill."
            return
        }
        if (sharingBillNumber != null) return
        operationScope.launch {
            sharingBillNumber = bill.number
            runCatchingCancellable {
                val pdf = withContext(Dispatchers.IO) { api.invoicePdf(token, bill.number) }
                BillPdfShare.share(context, bill, pdf.bytes)
            }.onFailure { message = it.message ?: "Could not prepare this bill for sharing." }
            sharingBillNumber = null
        }
    }

    fun shareQrLabelPdf(selectedProducts: List<Product>) {
        runCatching {
            val pdf = QrLabelPrinter.createLabelPdf(context, selectedProducts)
            BillPdfShare.sharePdfFile(
                context,
                pdf,
                "Share QR label PDF",
                "Cloth QR labels",
                "${selectedProducts.size} cloth QR label(s). For PSF20 printing, choose the printer app and match the label stock size (50 × 25 mm)."
            )
        }.onFailure { message = it.message ?: "Could not create the QR label PDF." }
    }

    fun submitOrderRequest() {
        val token = idToken
        val requestCustomer = ShopCustomer("", customerName, customerPhone, customerEmail)
        val requestItems = cart.toList()
        val isNewCustomer = customerExists == false
        if (!api.isConfigured || token == null) {
            message = "Connect to Shop before sending an order request."
            return
        }
        if (CustomerCache.normalizePhone(customerPhone).length < 8 || customerExists == null || (isNewCustomer && customerName.isBlank())) {
            message = "Enter a valid customer phone number and customer name."
            return
        }
        if (requestItems.isEmpty()) {
            message = "Add at least one cloth before sending the order."
            return
        }
        if (sendingOrderRequest) return
        operationScope.launch {
            sendingOrderRequest = true
            runCatchingCancellable {
                withContext(Dispatchers.IO) {
                    api.createOrderRequest(token, requestCustomer.name, requestCustomer.phone, requestCustomer.email, customerAddress, requestItems, orderNotes)
                }
            }.onSuccess { request ->
                CustomerHistoryCache.invalidate(context, requestCustomer.phone)
                if (isNewCustomer) {
                    val merged = CustomerCache.merge(cachedCustomers.toList(), listOf(requestCustomer))
                    cachedCustomers.clear(); cachedCustomers.addAll(merged); CustomerCache.save(context, merged)
                }
                message = "Order ${request.requestNumber} sent to the manager. Status: ${request.status}."
                orderMode = false
                orderNotes = ""
                cart.clear()
                customerName = ""; customerPhone = ""; customerEmail = ""; customerAddress = ""
                customerExists = null; customerMatches = emptyList()
                backStack.clear(); selectedTab = -1; screen = Screen.Home
            }.onFailure { message = it.message ?: "Could not send the order request." }
            sendingOrderRequest = false
        }
    }

    fun syncOrderRequests() {
        val token = idToken
        if (!api.isConfigured || token == null) {
            message = "Sign in to Shop before loading order requests."
            return
        }
        if (orderRequestsLoading) return
        operationScope.launch {
            orderRequestsLoading = true
            runCatchingCancellable { withContext(Dispatchers.IO) { api.orderRequests(token, "") } }
                .onSuccess { requests -> orderRequests = requests }
                .onFailure { message = it.message ?: "Could not load order requests." }
            orderRequestsLoading = false
        }
    }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
    val baseDensity = LocalDensity.current
    val responsiveScale = (screenWidthDp / 393f).coerceIn(0.92f, 1.08f)

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Teal,
            onPrimary = Color.White,
            secondary = Ink,
            background = Paper,
            surface = Color.White,
            onSurface = Ink
        )
    ) {
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = baseDensity.density * responsiveScale,
                fontScale = baseDensity.fontScale * responsiveScale
            )
        ) {
        Scaffold(
            containerColor = Paper,
            topBar = {
                if (savingInvoice || savingCloth || receivingPayment || savingCustomer || sendingOrderRequest) {
                    Surface(color = SoftBlue, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = Teal, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            Text(
                                tr("Saving in background. You can switch tabs.", "पृष्ठभूमि में सहेजा जा रहा है। आप टैब बदल सकते हैं।"),
                                color = Ink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            bottomBar = {
        if (screen in setOf(Screen.Home, Screen.BillCenter, Screen.Bills, Screen.Customer, Screen.ProductCenter, Screen.Payments)) {
                    NavigationBar(containerColor = Color.White) {
                        NavigationBarItem(
                            selected = screen == Screen.Home,
                            onClick = { backStack.clear(); selectedTab = -1; screen = Screen.Home },
                            icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                            label = { Text(if (isHindi) "होम" else "Home", fontSize = 13.sp) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { backStack.clear(); selectedTab = 0; screen = Screen.BillCenter },
                            icon = { Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null) },
                            label = { Text(if (isHindi) "बिल" else "Bill", fontSize = 13.sp) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { backStack.clear(); selectedTab = 1; screen = Screen.Customer },
                            icon = { Icon(Icons.Rounded.Contacts, contentDescription = null) },
                            label = { Text(if (isHindi) "ग्राहक" else "Customer", fontSize = 13.sp) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { backStack.clear(); selectedTab = 2; screen = Screen.ProductCenter },
                            icon = { Icon(Icons.Rounded.Inventory2, contentDescription = null) },
                            label = { Text(if (isHindi) "कपड़ा" else "Cloth", fontSize = 13.sp) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { backStack.clear(); selectedTab = 3; screen = Screen.Payments },
                            icon = { Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null) },
                            label = { Text(if (isHindi) "भुगतान" else "Payment", fontSize = 13.sp) }
                        )
                    }
                }
            }
        ) { insets ->
            when (screen) {
                Screen.Home -> HomeScreen(
                    padding = insets,
                    apiConfigured = api.isConfigured,
                    isSignedIn = idToken != null,
                    signedInEmail = if (idToken != null) GoogleSignIn.getLastSignedInAccount(context)?.email else null,
                    userRole = userRole,
                    isHindi = isHindi,
                    onLanguageChange = { hindi ->
                        isHindi = hindi
                        context.getSharedPreferences("sales_app_preferences", android.content.Context.MODE_PRIVATE)
                            .edit().putBoolean("use_hindi", hindi).apply()
                    },
                    onSignIn = { beginSignIn() },
                    onNewBill = { orderMode = false; billDiscountValue = "0"; navigateTo(Screen.Bill) },
                    onMakePayment = { paymentListTab = "DUE"; navigateTo(Screen.Payments) },
                    onCatalog = { selectedTab = 2; navigateTo(Screen.Catalog) },
                    onAddProduct = {
                        productName = ""; productCategory = ""; sellingPrice = ""; unitCost = ""; openingStock = ""; fractionalQuantity = false
                        productImageBase64 = ""; productImageBitmap = null
                        navigateTo(Screen.AddProduct)
                    },
                    onPlaceOrder = {
                        orderMode = true
                        orderNotes = ""
                        cart.clear()
                        customerPhone = ""; customerName = ""; customerEmail = ""; customerAddress = ""
                        customerExists = null; customerMatches = emptyList()
                        navigateTo(Screen.PlaceOrder)
                    }
                )
                Screen.BillCenter -> BillCenterScreen(
                    padding = insets,
                    onCreateBill = { orderMode = false; billDiscountValue = "0"; navigateTo(Screen.Bill) },
                    onFindBill = { billsPhone = ""; navigateTo(Screen.Bills) }
                )
                Screen.ProductCenter -> ProductCenterScreen(
                    padding = insets,
                    onAddProduct = {
                        productName = ""; productCategory = ""; sellingPrice = ""; unitCost = ""; openingStock = ""; fractionalQuantity = false
                        productImageBase64 = ""; productImageBitmap = null
                        navigateTo(Screen.AddProduct)
                    },
                    onCatalog = { navigateTo(Screen.Catalog) },
                    onViewOrderRequests = { navigateTo(Screen.OrderRequests); syncOrderRequests() },
                    onPlaceOrder = {
                        orderMode = true
                        orderNotes = ""
                        cart.clear()
                        customerPhone = ""; customerName = ""; customerEmail = ""; customerAddress = ""
                        customerExists = null; customerMatches = emptyList()
                        navigateTo(Screen.PlaceOrder)
                    }
                )
                Screen.OrderRequests -> OrderRequestsScreen(
                    padding = insets,
                    requests = orderRequests,
                    loading = orderRequestsLoading,
                    connected = api.isConfigured && idToken != null,
                    onBack = { navigateBack() },
                    onSync = { syncOrderRequests() },
                    onRequestClick = { request -> selectedOrderRequest = request; navigateTo(Screen.OrderRequestDetail) }
                )
                Screen.OrderRequestDetail -> OrderRequestDetailScreen(
                    padding = insets,
                    request = selectedOrderRequest,
                    products = products,
                    onBack = { navigateBack() }
                )
                Screen.Customer -> CustomerDirectoryScreen(
                    padding = insets,
                    query = customerDirectoryQuery,
                    customers = cachedCustomers,
                    searching = customerDirectoryLoading,
                    syncing = customerSyncing,
                    live = idToken != null && api.isConfigured,
                    onQuery = { customerDirectoryQuery = it },
                    onSync = { syncCustomersFromShop() },
                    onCreate = {
                        newCustomerName = ""; newCustomerPhone = ""; newCustomerEmail = ""; newCustomerAddress = ""
                        navigateTo(Screen.AddCustomer)
                    },
                    onCustomer = { selectedCustomer = it; customerHistoryTab = "Sales"; detailBills = emptyList(); detailOrders = emptyList(); detailFinance = null; navigateTo(Screen.CustomerDetail) }
                )
                Screen.CustomerDetail -> selectedCustomer?.let { customer ->
                    CustomerDetailScreen(
                        padding = insets, customer = customer, sales = detailBills, orders = detailOrders,
                        finance = detailFinance, financeLoading = detailFinanceLoading, receivingPayment = receivingPayment,
                        historyTab = customerHistoryTab, onHistoryTab = { customerHistoryTab = it },
                        loading = detailBillsLoading, ordersLoading = detailOrdersLoading,
                        onBack = { navigateBack() },
                        onSync = { syncCustomerDetails(customer) },
                        onOpenPdf = { url -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } },
                        onShareBill = { bill -> shareBillPdf(bill) },
                        onOpenOrderRequest = { request -> selectedOrderRequest = request; navigateTo(Screen.OrderRequestDetail) },
                        sharingBillNumber = sharingBillNumber,
                        onReceivePayment = { invoiceNumber, amount, method, note ->
                            val local = newLocalPayment(invoiceNumber, amount, method, note, customer)
                            LocalPaymentLog.save(context, customer.phone, local)
                            detailFinance = detailFinance?.let { addOptimisticPayment(it, local) }
                            syncPayment(customer, local)
                        },
                        onReceiveCustomerPayment = { amount, method, note ->
                            val local = newLocalPayment("", amount, method, note, customer)
                            LocalPaymentLog.save(context, customer.phone, local)
                            detailFinance = detailFinance?.let { addOptimisticPayment(it, local) }
                            syncPayment(customer, local)
                        },
                        onRetryPayment = { syncPayment(customer, it) }
                    )
                }
                Screen.AddCustomer -> AddCustomerScreen(
                    padding = insets, name = newCustomerName, phone = newCustomerPhone, email = newCustomerEmail,
                    address = newCustomerAddress, saving = savingCustomer, connected = api.isConfigured && idToken != null,
                    onName = { newCustomerName = it }, onPhone = { newCustomerPhone = it },
                    onEmail = { newCustomerEmail = it }, onAddress = { newCustomerAddress = it },
                    onBack = { navigateBack() },
                    onSave = {
                        val token = idToken
                        if (token == null || !api.isConfigured) message = "Connect to Shop before saving customers."
                        else if (!savingCustomer) operationScope.launch {
                            savingCustomer = true
                            runCatchingCancellable { withContext(Dispatchers.IO) { api.createCustomer(token, newCustomerName, newCustomerPhone, newCustomerEmail, newCustomerAddress) } }
                                .onSuccess { created ->
                                    val merged = CustomerCache.merge(cachedCustomers.toList(), listOf(created))
                                    cachedCustomers.clear(); cachedCustomers.addAll(merged); CustomerCache.save(context, merged)
                                    customerDirectoryQuery = created.phone
                                    message = "${created.name} saved to customers."
                                    navigateBack()
                                }.onFailure { message = it.message ?: "Could not save this customer." }
                            savingCustomer = false
                        }
                    }
                )
                Screen.Search -> SearchScreen(
                    padding = insets,
                    query = query,
                    products = products.filter { it.available },
                    orderMode = orderMode,
                    live = api.isConfigured && idToken != null,
                    refreshing = productSyncing,
                    onSync = { syncProductsFromShop() },
                    onQuery = { query = it },
                    imageLoader = { imageId ->
                        val token = idToken
                        if (token.isNullOrBlank()) null else api.productImage(token, imageId)
                    },
                    onScan = {
                        scanQr { scannedCode ->
                            query = scannedCode
                            message = "Scanned $scannedCode"
                        }
                    },
                    onProductQr = { qrPreviewProduct = it },
                    onAddToBill = { product ->
                        val index = cart.indexOfFirst { it.product.sku == product.sku }
                        val step = if (product.fractionalQuantity) 0.1 else 1.0
                        if (index >= 0) cart[index] = cart[index].copy(quantity = (cart[index].quantity + step).coerceAtMost(product.stock))
                        else cart.add(CartLine(product, minOf(1.0, product.stock)))
                        if (orderMode) {
                            navigateBack()
                        } else {
                            if (backStack.lastOrNull() == Screen.Bill) backStack.removeAt(backStack.lastIndex)
                            else if (screen != Screen.Bill) backStack.add(screen)
                            selectedTab = 0
                            screen = Screen.Bill
                        }
                    }
                )
                Screen.AddProduct -> AddProductScreen(
                    padding = insets,
                    connected = api.isConfigured && idToken != null,
                    saving = savingCloth,
                    name = productName,
                    category = productCategory,
                    price = sellingPrice,
                    cost = unitCost,
                    stock = openingStock,
                    fractionalQuantity = fractionalQuantity,
                    photo = productImageBitmap,
                    onTakePhoto = { takeProductPhoto() },
                    onChoosePhoto = { productPhotoPicker.launch("image/*") },
                    onName = { productName = it },
                    onCategory = { productCategory = it },
                    onPrice = { sellingPrice = it },
                    onCost = { unitCost = it },
                    onStock = { openingStock = it },
                    onFractionalQuantity = { fractionalQuantity = it },
                    onBack = { navigateBack() },
                    onSave = {
                        val token = idToken
                        if (!api.isConfigured || token == null) message = "Connect the Shop API and sign in before saving clothes."
                        else if (!savingCloth) operationScope.launch {
                            savingCloth = true
                            try {
                                runCatchingCancellable {
                                    withContext(Dispatchers.IO) {
                                        api.createProduct(token, productName, productCategory, sellingPrice.toDouble(), unitCost.toDouble(), openingStock.toDouble(), productImageBase64, fractionalQuantity)
                                    }
                                }.onSuccess { created ->
                                    val newProduct = Product(created.sku, created.name, productCategory, created.price, created.stock, created.imageId, true, created.fractionalQuantity)
                                    val merged = ProductCache.merge(products.toList(), listOf(newProduct))
                                    products.clear()
                                    products.addAll(merged)
                                    hasLiveProducts = true
                                    ProductCache.save(context, merged)
                                    createdQrSku = created.sku
                                    query = created.sku
                                }.onFailure { message = it.message ?: "Could not save this cloth." }
                            } finally {
                                savingCloth = false
                            }
                        }
                    }
                )
                Screen.Bill, Screen.PlaceOrder -> BillScreen(
                    padding = insets,
                    connected = api.isConfigured && idToken != null,
                    orderMode = screen == Screen.PlaceOrder,
                    orderNotes = orderNotes,
                    onOrderNotes = { orderNotes = it },
                    customerName = customerName,
                    customerPhone = customerPhone,
                    customerEmail = customerEmail,
                    customerAddress = customerAddress,
                    customerExists = customerExists,
                    customerSearching = customerSearching,
                    customerMatches = customerMatches,
                    saving = if (screen == Screen.PlaceOrder) sendingOrderRequest else savingInvoice,
                    savingCustomer = savingCustomer,
                    paymentMode = paymentMode,
                    discountValue = billDiscountValue,
                    creditAmountReceived = creditAmountReceived,
                    creditPaymentMethod = creditPaymentMethod,
                    cart = cart,
                    imageLoader = { imageId ->
                        val token = idToken
                        if (token.isNullOrBlank()) null else api.productImage(token, imageId)
                    },
                    onName = { customerName = it },
                    onPhone = {
                        customerPhone = it
                        customerExists = null
                        customerMatches = emptyList()
                        customerName = ""
                        customerEmail = ""
                        customerAddress = ""
                    },
                    onCustomerEmail = { customerEmail = it },
                    onCustomerAddress = { customerAddress = it },
                    onSaveCustomer = {
                        val token = idToken
                        if (token == null || !api.isConfigured) {
                            message = "Connect to Shop before saving customers."
                        } else if (!savingCustomer) {
                            val nameToSave = customerName.trim()
                            val phoneToSave = customerPhone.trim()
                            val emailToSave = customerEmail.trim()
                            val addressToSave = customerAddress.trim()
                            operationScope.launch {
                                savingCustomer = true
                                runCatchingCancellable {
                                    withContext(Dispatchers.IO) {
                                        api.createCustomer(token, nameToSave, phoneToSave, emailToSave, addressToSave)
                                    }
                                }.onSuccess { created ->
                                    val merged = CustomerCache.merge(cachedCustomers.toList(), listOf(created))
                                    cachedCustomers.clear()
                                    cachedCustomers.addAll(merged)
                                    CustomerCache.save(context, merged)
                                    customerName = created.name
                                    customerPhone = created.phone
                                    customerEmail = created.email
                                    customerExists = true
                                    customerMatches = listOf(created)
                                    message = "${created.name} saved to customers."
                                }.onFailure { error ->
                                    message = error.message ?: "Could not save this customer."
                                }
                                savingCustomer = false
                            }
                        }
                    },
                    onPaymentMode = { paymentMode = it },
                    onDiscountValue = { billDiscountValue = it },
                    onCreditAmountReceived = { creditAmountReceived = it },
                    onCreditPaymentMethod = { creditPaymentMethod = it },
                    onSelectCustomer = { selected ->
                        customerPhone = selected.phone
                        customerName = selected.name
                        customerEmail = selected.email
                        customerAddress = ""
                        customerExists = true
                        customerMatches = emptyList()
                    },
                    onPickContact = {
                        contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
                    },
                    onScan = {
                        scanQr { scannedCode ->
                                val token = idToken
                                val cached = products.firstOrNull { it.sku == scannedCode }
                                if (cached != null) {
                                    if (!cached.available) message = "This cloth has been marked unavailable."
                                    else {
                                        val index = cart.indexOfFirst { it.product.sku == cached.sku }
                                        val step = if (cached.fractionalQuantity) 0.1 else 1.0
                                        if (index >= 0) cart[index] = cart[index].copy(quantity = (cart[index].quantity + step).coerceAtMost(cached.stock))
                                        else cart.add(CartLine(cached, minOf(1.0, cached.stock)))
                                        query = scannedCode
                                    }
                                } else if (token != null && api.isConfigured) {
                                    operationScope.launch {
                                        runCatchingCancellable { withContext(Dispatchers.IO) { api.products(token, scannedCode).firstOrNull { it.sku == scannedCode } } }
                                            .onSuccess { product ->
                                                if (product == null) message = "Cloth not found in Shop. Check the QR label."
                                                else if (!product.available) message = "This cloth has been marked unavailable."
                                                else {
                                                    val merged = ProductCache.merge(products.toList(), listOf(product))
                                                    products.clear()
                                                    products.addAll(merged)
                                                    hasLiveProducts = true
                                                    ProductCache.save(context, merged)
                                                    val index = cart.indexOfFirst { it.product.sku == product.sku }
                                                    val step = if (product.fractionalQuantity) 0.1 else 1.0
                                                    if (index >= 0) cart[index] = cart[index].copy(quantity = (cart[index].quantity + step).coerceAtMost(product.stock))
                                                    else cart.add(CartLine(product, minOf(1.0, product.stock)))
                                                }
                                            }
                                            .onFailure { message = it.message ?: "Could not look up this cloth." }
                                    }
                                } else {
                                    val product = previewProducts.firstOrNull { it.sku == scannedCode }
                                    if (product != null) cart.add(CartLine(product, minOf(1.0, product.stock)))
                                    else message = "Cloth not found in preview catalog. Search by name or code."
                                }
                        }
                    },
                    onFindProduct = {
                        orderMode = screen == Screen.PlaceOrder
                        selectedTab = 2
                        navigateTo(Screen.Search)
                    },
                    onQuantity = { sku, change ->
                        val index = cart.indexOfFirst { it.product.sku == sku }
                        if (index >= 0) {
                            val line = cart[index]
                            val newQuantity = (line.quantity + change).coerceAtMost(line.product.stock)
                            if (newQuantity <= 0) cart.removeAt(index)
                            else cart[index] = line.copy(quantity = newQuantity)
                        }
                    },
                    onSetQuantity = { sku, newQuantity ->
                        val index = cart.indexOfFirst { it.product.sku == sku }
                        if (index >= 0 && newQuantity >= 0 && newQuantity <= cart[index].product.stock) {
                            cart[index] = cart[index].copy(quantity = newQuantity)
                        }
                    },
                    onSave = {
                        if (screen == Screen.PlaceOrder) {
                            submitOrderRequest()
                        } else {
                        val token = idToken
                        if (!api.isConfigured || token == null) message = "Connect the Shop API and sign in before saving bills."
                        else if (!savingInvoice) {
                            val invoiceCustomer = ShopCustomer("", customerName, customerPhone, customerEmail)
                            val invoiceAddress = customerAddress
                            val invoicePayment = paymentMode
                            val invoiceCreditAmount = creditAmountReceived.toIntOrNull() ?: 0
                            val invoicePaymentMethod = creditPaymentMethod
                            val invoiceItems = cart.toList()
                            val isNewCustomer = customerExists == false
                            operationScope.launch {
                                savingInvoice = true
                                runCatchingCancellable {
                                    withContext(Dispatchers.IO) {
                                        val subtotal = invoiceItems.sumOf { lineAmount(it.product.price, it.quantity) }
                                        val discountInput = billDiscountValue.toDoubleOrNull() ?: 0.0
                                        val discountAmount = discountInput.roundToInt()
                                        val total = subtotal - discountAmount
                                        val amountReceived = if (invoicePayment == "Credit") invoiceCreditAmount else total
                                        api.createInvoice(token, invoiceCustomer.name, invoiceCustomer.phone, invoiceCustomer.email, invoiceAddress, invoicePayment, invoiceItems, amountReceived, invoicePaymentMethod, discountInput)
                                    }
                                }.onSuccess { created ->
                                    dismissKeyboard()
                                    CustomerHistoryCache.invalidate(context, invoiceCustomer.phone)
                                    val newBill = ShopInvoice(
                                        number = created.invoiceNumber,
                                        date = java.time.LocalDate.now().toString(),
                                        customerName = invoiceCustomer.name,
                                        phone = invoiceCustomer.phone,
                                        total = created.total,
                                        paymentMode = created.paymentMode,
                                        pdfUrl = created.pdfUrl,
                                        paid = created.paid,
                                        due = created.due,
                                        items = created.items.ifEmpty { invoiceItems.map { line ->
                                            InvoiceLine(
                                                sku = line.product.sku,
                                                name = line.product.name,
                                                category = line.product.category,
                                                quantity = line.quantity,
                                                unitPrice = line.product.price,
                                                discount = 0,
                                                lineTotal = lineAmount(line.product.price, line.quantity),
                                                fractionalQuantity = line.product.fractionalQuantity
                                            )
                                        } }
                                    )
                                    val cachedBills = BillCache.merge(BillCache.load(context), listOf(newBill))
                                    BillCache.save(context, cachedBills)
                                    if (isNewCustomer) {
                                        val merged = CustomerCache.merge(cachedCustomers.toList(), listOf(invoiceCustomer))
                                        cachedCustomers.clear()
                                        cachedCustomers.addAll(merged)
                                        CustomerCache.save(context, merged)
                                    }
                                    latestPdfUrl = created.pdfUrl
                                    latestInvoice = newBill
                                    message = "Bill saved successfully."
                                    cart.clear()
                                    customerName = ""
                                    customerPhone = ""
                                    customerEmail = ""
                                    customerAddress = ""
                                    customerExists = null
                                    customerMatches = emptyList()
                                    paymentMode = "Cash"
                                    creditAmountReceived = "0"
                                    billDiscountValue = "0"
                                }.onFailure { message = it.message ?: "Could not save the bill." }
                                savingInvoice = false
                            }
                        }
                        }
                    },
                    onBack = { navigateBack() }
                )
                Screen.Bills -> BillsScreen(
                    padding = insets,
                    phone = billsPhone,
                    bills = bills,
                    loading = billsLoading,
                    isAdministrator = isAdministrator,
                    requests = billDeletionRequests,
                    onRequestDeletion = { bill -> requestEntryDeletion("BILL", bill.number, "${bill.number} · ${bill.customerName} · ₹${bill.total}") },
                    onReviewRequest = { request, approve -> reviewEntryDeletionRequest("BILL", request, approve) },
                    onPhone = { billsPhone = it },
                    onBack = { navigateBack() },
                    onSync = {
                        val token = idToken
                        if (!api.isConfigured || token == null) message = "Sign in to Shop before syncing bills. Your saved bills are still available on this phone."
                        else operationScope.launch {
                            billsLoading = true
                            runCatchingCancellable { withContext(Dispatchers.IO) { api.bills(token, "") } }
                                .onSuccess { result ->
                                    bills.clear(); bills.addAll(result)
                                    BillCache.save(context, result)
                                }
                                .onFailure { message = it.message ?: "Could not sync bills. Saved bills are still available on this phone." }
                            billsLoading = false
                        }
                    },
                    onOpenPdf = { url -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } },
                    onShareBill = { bill -> shareBillPdf(bill) },
                    sharingBillNumber = sharingBillNumber
                )
                Screen.Payments -> PaymentsScreen(
                    padding = insets,
                    bills = bills,
                    customers = cachedCustomers,
                    query = paymentDirectoryQuery,
                    paymentsMade = madePayments,
                    listTab = paymentListTab,
                    loading = billsLoading,
                    connected = api.isConfigured && idToken != null,
                    isAdministrator = isAdministrator,
                    requests = paymentDeletionRequests,
                    onRequestDeletion = { payment -> requestEntryDeletion("PAYMENT", payment.id, "${payment.customerName.ifBlank { payment.customerPhone }} · ₹${payment.amount}") },
                    onReviewRequest = { request, approve -> reviewEntryDeletionRequest("PAYMENT", request, approve) },
                    onQuery = { paymentDirectoryQuery = it },
                    onListTab = { paymentListTab = it },
                    onSync = {
                        val token = idToken
                        if (!api.isConfigured || token == null) message = "Sign in to Shop before syncing balances. Saved balances are still available on this phone."
                        else if (!billsLoading) operationScope.launch {
                            billsLoading = true
                            runCatchingCancellable { withContext(Dispatchers.IO) { api.bills(token, "") to api.payments(token) } }
                                .onSuccess { (freshBills, freshPayments) ->
                                    bills.clear(); bills.addAll(freshBills)
                                    BillCache.save(context, freshBills)
                                    madePayments = mergePaymentHistory(freshPayments, LocalPaymentLog.loadAll(context))
                                }
                                .onFailure { message = it.message ?: "Could not sync payments and balances." }
                            billsLoading = false
                        }
                    },
                    onCustomer = { customer ->
                        selectedCustomer = customer
                        customerHistoryTab = "Payments"
                        detailBills = emptyList(); detailOrders = emptyList(); detailFinance = null
                        navigateTo(Screen.CustomerDetail)
                        selectedTab = 3
                    },
                    onRetryPayment = { payment ->
                        val customer = cachedCustomers.firstOrNull { CustomerCache.normalizePhone(it.phone) == CustomerCache.normalizePhone(payment.customerPhone) }
                            ?: ShopCustomer("", payment.customerName, payment.customerPhone, "")
                        syncPayment(customer, payment)
                    }
                )
                Screen.Catalog -> CatalogScreen(
                    padding = insets, products = if (isAdministrator) products else products.filter { it.available }, live = api.isConfigured && idToken != null,
                    query = catalogQuery,
                    onQuery = { catalogQuery = it },
                    onScan = {
                        scanQr { scannedCode ->
                            catalogQuery = scannedCode
                            message = "Scanned $scannedCode"
                        }
                    },
                    syncing = productSyncing,
                    onSync = { syncProductsFromShop() },
                    isAdministrator = isAdministrator,
                    requests = productDeletionRequests,
                    onRequestDeletion = { product -> requestEntryDeletion("PRODUCT", product.sku, product.name) },
                    onReviewRequest = { request, approve -> reviewEntryDeletionRequest("PRODUCT", request, approve) },
                    onAvailabilityChange = { product, available -> setClothAvailability(product, available) },
                    imageLoader = { id ->
                        val token = idToken ?: return@CatalogScreen null
                        runCatchingCancellable { withContext(Dispatchers.IO) { api.productImage(token, id) } }.getOrNull()
                    },
                    onProductQr = { qrPreviewProduct = it },
                    onProductDetails = { product ->
                        selectedCatalogProduct = product
                        navigateTo(Screen.ProductDetail)
                    },
                    onBack = { navigateBack() }
                )
                Screen.ProductDetail -> selectedCatalogProduct?.let { product ->
                    ProductDetailScreen(
                        padding = insets,
                        product = product,
                        isAdministrator = isAdministrator,
                        connected = api.isConfigured && idToken != null,
                        uploading = uploadingProductPhotoSku == product.sku,
                        imageLoader = { id ->
                            val token = idToken ?: return@ProductDetailScreen null
                            runCatchingCancellable { withContext(Dispatchers.IO) { api.productImage(token, id) } }.getOrNull()
                        },
                        onTakePhoto = {
                            if (!api.isConfigured || idToken == null) message = "Sign in to Shop before saving a product picture."
                            else { productPhotoTargetSku = product.sku; takeProductPhoto() }
                        },
                        onChoosePhoto = {
                            if (!api.isConfigured || idToken == null) message = "Sign in to Shop before saving a product picture."
                            else { productPhotoTargetSku = product.sku; productPhotoPicker.launch("image/*") }
                        },
                        onAvailabilityChange = { setClothAvailability(product, it) },
                        onAddStock = { quantity, reason, complete -> addStockToCloth(product, quantity, reason, complete) },
                        onDelete = {
                            if (isAdministrator) {
                                setClothAvailability(product, false)
                                navigateBack()
                            } else requestEntryDeletion("PRODUCT", product.sku, product.name)
                        },
                        onProductQr = { qrPreviewProduct = product },
                        onEdit = { name, category, price, fractional, complete ->
                            updateClothDetails(product, name, category, price, fractional, complete)
                        },
                        onBack = { navigateBack() }
                    )
                }
            }
        }
        message?.let { text ->
            val savedBill = latestInvoice
            if (savedBill != null) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { latestPdfUrl = ""; latestInvoice = null; message = null },
                    icon = {
                        Surface(shape = CircleShape, color = Color(0xFFDDF5E4), modifier = Modifier.size(58.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF20A05A), modifier = Modifier.size(36.dp))
                            }
                        }
                    },
                    title = { Text(tr("Bill saved", "बिल सेव हो गया"), fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink) },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(tr("PDF saved to Drive and ready to share.", "PDF Drive में सेव है और साझा करने के लिए तैयार है।"), fontSize = 16.sp, color = Muted)
                            Spacer(Modifier.height(16.dp))
                            Surface(color = Color(0xFFEAF3EF), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(tr("Bill total", "बिल कुल"), modifier = Modifier.weight(1f), fontSize = 16.sp, color = Ink)
                                        Text("₹${savedBill.total}", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink)
                                    }
                                    Text(savedBill.number, modifier = Modifier.padding(top = 7.dp), fontSize = 14.sp, color = Ink)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(
                                onClick = {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(latestPdfUrl))) }
                                        .onFailure { message = it.message ?: "Could not open the bill PDF." }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Teal)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ReceiptLong, null)
                                Spacer(Modifier.width(10.dp))
                                Text(tr("Open PDF", "PDF खोलें"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { shareBillPdf(savedBill) },
                                enabled = sharingBillNumber == null,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.5.dp, Teal)
                            ) {
                                WhatsAppShareIcon(sharing = sharingBillNumber == savedBill.number)
                                Spacer(Modifier.width(10.dp))
                                Text(tr("Share on WhatsApp", "WhatsApp पर साझा करें"), color = Teal, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(onClick = { latestPdfUrl = ""; latestInvoice = null; message = null }, modifier = Modifier.padding(top = 4.dp)) {
                                Text(tr("Done", "हो गया"), color = Teal, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(26.dp)
                )
            } else {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { message = null },
                    title = { Text(tr("Avani Suto", "अवनि सूतो")) },
                    text = { Text(localizedAppMessage(text, isHindi), fontSize = 17.sp) },
                    confirmButton = { TextButton(onClick = { message = null }) { Text(tr("OK", "ठीक है")) } }
                )
            }
        }
        pendingDeletionTarget?.let { target ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { if (!submittingDeletion) pendingDeletionTarget = null },
                title = { Text(tr("Request deletion", "हटाने का अनुरोध")) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(target.summary, color = Ink, fontWeight = FontWeight.SemiBold)
                        Text(tr("The administrator will review this request.", "मैनेजर इस अनुरोध की समीक्षा करेंगे।"), color = Muted)
                        OutlinedTextField(
                            value = deletionReason,
                            onValueChange = { deletionReason = it.take(500) },
                            label = { Text(tr("Reason", "कारण")) },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { submitEntryDeletionRequest() }, enabled = !submittingDeletion && deletionReason.trim().length >= 4) {
                        if (submittingDeletion) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text(tr("Send request", "अनुरोध भेजें"))
                    }
                },
                dismissButton = { TextButton(onClick = { pendingDeletionTarget = null }, enabled = !submittingDeletion) { Text(tr("Cancel", "रद्द करें")) } }
            )
        }
        createdQrSku?.let { sku ->
            val qrBitmap = remember(sku) { createQrBitmap(sku).asImageBitmap() }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = {
                    createdQrSku = null
                    productName = ""; productCategory = ""; sellingPrice = ""; unitCost = ""; openingStock = ""; fractionalQuantity = false
                    backStack.clear(); selectedTab = 0; screen = Screen.Home
                },
                title = { Text(tr("QR label ready", "QR लेबल तैयार है")) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(qrBitmap, contentDescription = "QR code for $sku", modifier = Modifier.size(220.dp))
                        Text("$sku", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                        Text(tr("Stick this code on the cloth.", "इस कोड को कपड़े पर चिपकाएँ।"), fontSize = 15.sp, color = Muted)
                    }
                },
                confirmButton = {
                    Row {
                        TextButton(onClick = {
                            val product = products.firstOrNull { it.sku == sku }
                            if (product == null) message = "Cloth saved, but it is not in the local list yet. Use Find a cloth to print its QR label."
                            else shareQrLabelPdf(listOf(product))
                        }) { Text(tr("Share label PDF", "लेबल PDF साझा करें")) }
                        TextButton(onClick = {
                            createdQrSku = null
                            productName = ""; productCategory = ""; sellingPrice = ""; unitCost = ""; openingStock = ""; fractionalQuantity = false
                            backStack.clear(); selectedTab = 0; screen = Screen.Home
                        }) { Text(tr("Done", "हो गया")) }
                    }
                }
            )
        }
        qrPreviewProduct?.let { product ->
            val qrBitmap = remember(product.sku) { createQrBitmap(product.sku).asImageBitmap() }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { qrPreviewProduct = null },
                title = { Text(tr("Cloth QR label", "कपड़े का QR लेबल")) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Image(qrBitmap, contentDescription = "QR code for ${product.name}", modifier = Modifier.size(190.dp))
                        Text(product.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(product.sku, color = Muted, fontSize = 15.sp)
                        Text(tr("Create a 50 × 25 mm PDF, then choose a printer app or WhatsApp from Share.", "50 × 25 मिमी PDF बनाएँ, फिर साझा करें में प्रिंटर ऐप या WhatsApp चुनें।"), color = Muted, fontSize = 14.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { shareQrLabelPdf(listOf(product)) }) { Text(tr("Create & share PDF", "PDF बनाएँ और साझा करें")) }
                },
                dismissButton = {
                    TextButton(onClick = { qrPreviewProduct = null }) { Text(tr("Close", "बंद करें")) }
                }
            )
        }
        }
    }
}

@Composable
private fun HomeScreen(
    padding: PaddingValues,
    apiConfigured: Boolean,
    isSignedIn: Boolean,
    signedInEmail: String?,
    userRole: String,
    isHindi: Boolean,
    onLanguageChange: (Boolean) -> Unit,
    onSignIn: () -> Unit,
    onNewBill: () -> Unit,
    onMakePayment: () -> Unit,
    onCatalog: () -> Unit,
    onAddProduct: () -> Unit,
    onPlaceOrder: () -> Unit
) {
    var userMenuExpanded by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Surface(shape = CircleShape, color = Color.White, border = BorderStroke(1.dp, Color(0xFFD8E1E2))) {
                        IconButton(onClick = { userMenuExpanded = true }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.AccountCircle, contentDescription = if (isHindi) "प्रोफ़ाइल और भाषा" else "Profile and language", tint = Teal, modifier = Modifier.size(32.dp))
                        }
                    }
                    DropdownMenu(expanded = userMenuExpanded, onDismissRequest = { userMenuExpanded = false }) {
                        if (signedInEmail != null) {
                            DropdownMenuItem(
                                text = { Text(signedInEmail, fontWeight = FontWeight.SemiBold) },
                                onClick = { userMenuExpanded = false },
                                enabled = false
                            )
                            DropdownMenuItem(
                                text = { Text(if (userRole == "ADMIN") tr("Administrator", "प्रशासक") else tr("Salesperson", "बिक्री कर्मचारी"), color = Teal) },
                                onClick = { userMenuExpanded = false },
                                enabled = false
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (isHindi) "English" else "English  ✓", color = if (!isHindi) Teal else Ink) },
                            onClick = { onLanguageChange(false); userMenuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isHindi) "हिन्दी  ✓" else "हिन्दी", color = if (isHindi) Teal else Ink) },
                            onClick = { onLanguageChange(true); userMenuExpanded = false }
                        )
                        if (isSignedIn) {
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "साइन इन है" else "Signed in") },
                                onClick = { userMenuExpanded = false },
                                enabled = false
                            )
                        } else if (apiConfigured) {
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "Shop में साइन इन करें" else "Sign in to Shop", color = Teal, fontWeight = FontWeight.Bold) },
                                onClick = { userMenuExpanded = false; onSignIn() }
                            )
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (isHindi) "अवनी सूतो" else "Avani Suto", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSignedIn) SoftGreen else Color(0xFFFFF3D7)
                        ) {
                            Text(
                                when {
                                    isSignedIn -> tr("Connected", "जुड़ा हुआ")
                                    apiConfigured -> tr("Not connected", "कनेक्ट नहीं")
                                    else -> tr("Not set up", "सेटअप नहीं")
                                },
                                color = if (isSignedIn) Color(0xFF2E7D32) else Color(0xFF76551A),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        if (userRole == "ADMIN") tr("Administrator", "प्रशासक") else tr("Salesperson", "बिक्री कर्मचारी"),
                        color = Teal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        item {
            ActionCard(
                title = if (isHindi) "बिल बनाएँ" else "Create a bill",
                subtitle = "",
                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                fill = Ink,
                titleColor = Color.White,
                detailColor = Color(0xFFD5E5EA),
                onClick = onNewBill,
                compact = true
            )
        }
        item {
            ActionCard(
                title = if (isHindi) "ऑर्डर अनुरोध बनाएँ" else "Create order request",
                subtitle = "",
                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                fill = Color(0xFFE7F2F7),
                titleColor = Ink,
                detailColor = Muted,
                onClick = onPlaceOrder,
                outline = true,
                compact = true
            )
        }
        item {
            ActionCard(
                title = if (isHindi) "नए कपड़े जोड़ें" else "Add new clothes",
                subtitle = "",
                icon = Icons.Rounded.AddShoppingCart,
                fill = Color.White,
                titleColor = Ink,
                detailColor = Muted,
                onClick = onAddProduct,
                outline = true,
                compact = true
            )
        }
        item {
            ActionCard(
                title = if (isHindi) "भुगतान लें" else "Make a payment",
                subtitle = "",
                icon = Icons.Rounded.AccountBalanceWallet,
                fill = Color(0xFFE7F2F7),
                titleColor = Ink,
                detailColor = Muted,
                onClick = onMakePayment,
                compact = true
            )
        }
        item {
            ActionCard(
                title = if (isHindi) "कैटलॉग" else "Catalogue",
                subtitle = "",
                icon = Icons.Rounded.Storefront,
                fill = Color.White,
                titleColor = Ink,
                detailColor = Muted,
                onClick = onCatalog,
                outline = true,
                compact = true
            )
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    fill: Color,
    titleColor: Color,
    detailColor: Color,
    onClick: () -> Unit,
    outline: Boolean = false,
    compact: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(if (compact) 84.dp else 112.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = fill),
        border = if (outline) BorderStroke(1.dp, Color(0xFFD8E1E2)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (outline) 0.dp else 3.dp)
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = if (fill == Ink) Color(0xFF2D5367) else SoftBlue, modifier = Modifier.size(58.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = if (fill == Ink) Color.White else Ink, modifier = Modifier.size(30.dp)) }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(title, fontSize = if (compact) 19.sp else 21.sp, fontWeight = FontWeight.Bold, color = titleColor)
                if (!compact && subtitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, fontSize = 15.sp, color = detailColor, lineHeight = 19.sp)
                }
            }
        }
    }
}

@Composable
private fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    onSync: (() -> Unit)? = null,
    syncing: Boolean = false,
    syncEnabled: Boolean = true
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack, modifier = Modifier.size(52.dp)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Go back", tint = Ink) }
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
        if (onSync != null) {
            IconButton(onClick = onSync, enabled = syncEnabled && !syncing, modifier = Modifier.size(48.dp)) {
                if (syncing) CircularProgressIndicator(modifier = Modifier.size(21.dp), strokeWidth = 2.dp, color = Teal)
                else Icon(Icons.Rounded.Refresh, contentDescription = tr("Sync", "सिंक करें"), tint = if (syncEnabled) Teal else Muted)
            }
        }
    }
}

@Composable
private fun BillCenterScreen(padding: PaddingValues, onCreateBill: () -> Unit, onFindBill: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(14.dp)); Text(tr("Bills", "बिल"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
        ActionCard(tr("Create a bill", "बिल बनाएँ"), tr("Add customer and clothes", "ग्राहक और कपड़े जोड़ें"), Icons.AutoMirrored.Rounded.ReceiptLong, Ink, Color.White, Color(0xFFD5E5EA), onCreateBill)
        ActionCard(tr("Search bills", "बिल खोजें"), tr("Search by bill number, customer name, or phone", "बिल नंबर, ग्राहक के नाम या फ़ोन से खोजें"), Icons.Rounded.Search, Color.White, Ink, Muted, onFindBill, outline = true)
    }
}

@Composable
private fun ProductCenterScreen(padding: PaddingValues, onAddProduct: () -> Unit, onCatalog: () -> Unit, onPlaceOrder: () -> Unit, onViewOrderRequests: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(14.dp)); Text(tr("Cloth", "कपड़ा"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
        ActionCard(tr("Add new clothes", "नए कपड़े जोड़ें"), tr("Save cloth details and create its QR", "कपड़े की जानकारी सहेजें और QR बनाएँ"), Icons.Rounded.AddShoppingCart, Ink, Color.White, Color(0xFFD5E5EA), onAddProduct)
        ActionCard(tr("Create order request", "ऑर्डर अनुरोध बनाएँ"), tr("Request cloth for a customer — no bill or payment", "ग्राहक के लिए कपड़ा मँगाएँ — बिल या भुगतान नहीं"), Icons.AutoMirrored.Rounded.ReceiptLong, Ink, Color.White, Color(0xFFD5E5EA), onPlaceOrder)
        ActionCard(tr("Order requests", "ऑर्डर अनुरोध"), tr("Tap to see the full items and customer details", "पूरे सामान और ग्राहक की जानकारी देखने के लिए टैप करें"), Icons.Rounded.Inventory2, Color.White, Ink, Muted, onViewOrderRequests, outline = true)
        ActionCard(tr("Cloth catalogue", "कपड़ों का कैटलॉग"), tr("Search, scan, and view cloth QR codes", "कपड़े खोजें, स्कैन करें और QR देखें"), Icons.Rounded.Storefront, Color.White, Ink, Muted, onCatalog, outline = true)
    }
}

@Composable
private fun OrderRequestsScreen(
    padding: PaddingValues,
    requests: List<CustomerOrderRequest>,
    loading: Boolean,
    connected: Boolean,
    onBack: () -> Unit,
    onSync: () -> Unit,
    onRequestClick: (CustomerOrderRequest) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(14.dp))
        ScreenHeader(tr("Order requests", "ऑर्डर अनुरोध"), onBack, onSync, loading, connected)
        Text(tr("Requests sent for customers", "ग्राहकों के लिए भेजे गए अनुरोध"), color = Muted, fontSize = 15.sp, modifier = Modifier.padding(start = 8.dp, bottom = 10.dp))
        if (!connected) {
            Text(tr("Sign in to Shop to load order requests.", "ऑर्डर अनुरोध देखने के लिए Shop में साइन इन करें।"), color = Muted, modifier = Modifier.padding(12.dp))
        } else if (!loading && requests.isEmpty()) {
            Text(tr("No order requests found. Create one from the Cloth section.", "कोई ऑर्डर अनुरोध नहीं मिला। कपड़ा सेक्शन से अनुरोध बनाएँ।"), color = Muted, fontSize = 16.sp, modifier = Modifier.padding(12.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp)) {
            itemsIndexed(requests, key = { index, request -> "order-request:$index:${request.number}" }) { _, request ->
                Card(
                    onClick = { onRequestClick(request) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8F4)),
                    border = BorderStroke(1.dp, Color(0xFFB7DCC4))
                ) {
                    Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(request.customerName.ifBlank { tr("Customer", "ग्राहक") }, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                            Text(request.status.ifBlank { tr("Pending", "लंबित") }, color = Color(0xFF2E7D32), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        if (request.customerPhone.isNotBlank()) Text(request.customerPhone, color = Teal, fontSize = 15.sp)
                        Text("${request.number} · ${request.date}", color = Muted, fontSize = 13.sp)
                        Text(request.productName, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(tr("Quantity: ${formatQuantity(request.quantity)}", "मात्रा: ${formatQuantity(request.quantity)}"), color = Muted, fontSize = 14.sp)
                        if (request.notes.isNotBlank()) Text(tr("Note: ${request.notes}", "नोट: ${request.notes}"), color = Muted, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderRequestDetailScreen(
    padding: PaddingValues,
    request: CustomerOrderRequest?,
    products: List<Product>,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        ScreenHeader(tr("Order request", "ऑर्डर अनुरोध"), onBack)
        if (request == null) {
            Text(tr("Request details are unavailable. Go back and select a request again.", "अनुरोध की जानकारी उपलब्ध नहीं है। वापस जाकर फिर से अनुरोध चुनें।"), color = Muted, fontSize = 16.sp)
            return@Column
        }
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
                            val fractional = products.firstOrNull { it.sku == item.sku }?.fractionalQuantity == true
                            Text(formatQuantity(item.quantity) + if (fractional) " m" else "", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.24f))
                        }
                    }
                }
                Divider(color = Color(0xFFD5E5EA))
                DetailLine(tr("Total quantity", "कुल मात्रा"), formatQuantity(request.quantity))
                if (request.notes.isNotBlank()) DetailLine(tr("Notes", "नोट"), request.notes)
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Muted, fontSize = 15.sp, modifier = Modifier.weight(0.42f))
        Text(value.ifBlank { "—" }, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.58f))
    }
}

@Composable
private fun AdminDeletionRequestsSection(
    type: String,
    requests: List<DeletionRequest>,
    onReview: (DeletionRequest, Boolean) -> Unit
) {
    val pending = requests.filter { it.status.equals("PENDING", ignoreCase = true) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            when (type) {
                "PAYMENT" -> tr("Payment correction requests", "भुगतान सुधार अनुरोध")
                "BILL" -> tr("Bill cancellation requests", "बिल रद्द करने के अनुरोध")
                else -> tr("Cloth removal requests", "कपड़ा हटाने के अनुरोध")
            },
            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink
        )
        if (pending.isEmpty()) {
            Text(tr("No pending requests in this section.", "इस भाग में कोई लंबित अनुरोध नहीं है।"), color = Muted, fontSize = 14.sp)
        }
        pending.forEach { request ->
            Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E8)), border = BorderStroke(1.dp, Color(0xFFE7D7B2))) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(request.summary, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(request.reason, color = Ink, fontSize = 14.sp)
                    Text("${request.requestedBy} · ${request.date}", color = Muted, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onReview(request, false) }) { Text(tr("Reject", "अस्वीकार"), color = Color(0xFFB3261E)) }
                        Button(onClick = { onReview(request, true) }) { Text(tr("Approve", "स्वीकृत करें")) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogScreen(
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
        Spacer(Modifier.height(14.dp)); ScreenHeader(tr("Cloth catalogue", "कपड़ों का कैटलॉग"), onBack, onSync, syncing, live)
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
                    Icon(Icons.Rounded.QrCode2, contentDescription = tr("Scan clothes", "कपड़े स्कैन करें"), tint = Teal)
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
                            Text("${product.sku} · ₹${product.price}", color = Muted, fontSize = 13.sp)
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
private fun CatalogCompactProductCard(
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
                        Text("₹${product.price}${if (product.fractionalQuantity) "/m" else ""}", color = Teal, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Surface(shape = RoundedCornerShape(12.dp), color = StockBadgeBlue) {
                            Text(
                                "${formatQuantity(product.stock)} ${if (product.fractionalQuantity) tr("m", "मीटर") else tr("left", "बाकी")}",
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

@Composable
private fun ProductDetailScreen(
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
    onEdit: (String, String, Int, Boolean, (Boolean) -> Unit) -> Unit,
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
                if (product.category.isNotBlank()) Text(product.category, color = Muted, fontSize = 16.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE1E8E7))) {
                    Column(Modifier.padding(12.dp)) {
                        Text(tr("Price", "कीमत"), color = Muted, fontSize = 14.sp)
                        Text("₹${product.price}${if (product.fractionalQuantity) "/m" else ""}", color = Teal, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = StockBadgeBlue) {
                    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(tr("Stock left", "बाकी स्टॉक"), color = Muted, fontSize = 14.sp)
                            Text("${formatQuantity(product.stock)} ${if (product.fractionalQuantity) tr("m", "मीटर") else tr("pieces", "पीस")}", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                        Text(tr("Tap to view or share the QR label.", "QR लेबल देखने या साझा करने के लिए टैप करें।"), color = Muted, fontSize = 14.sp)
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
                            Text(tr("Show this cloth in the catalogue", "इस कपड़े को कैटलॉग में दिखाएँ"), color = Muted, fontSize = 13.sp)
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
            onSave = { name, category, price, fractional ->
                savingEdit = true
                onEdit(name, category, price, fractional) { success ->
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
            title = { Text(if (isAdministrator) tr("Remove this cloth?", "यह कपड़ा हटाएँ?" ) else tr("Request cloth removal?", "कपड़ा हटाने का अनुरोध करें?"), color = Ink, fontWeight = FontWeight.Bold) },
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
private fun AddStockDialog(
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
                Text("${product.name} · ${tr("Current", "अभी")}: ${formatQuantity(product.stock)} ${if (product.fractionalQuantity) tr("m", "मीटर") else tr("pieces", "पीस")}", color = Muted)
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { value -> quantity = value.filter { it.isDigit() || (product.fractionalQuantity && it == '.') }.let { text -> if (text.count { it == '.' } <= 1) text else quantity } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (product.fractionalQuantity) tr("Metres to add", "जोड़ने के मीटर") else tr("Pieces to add", "जोड़ने के पीस")) },
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
private fun EditClothDialog(
    product: Product,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Boolean) -> Unit
) {
    var name by remember(product.sku) { mutableStateOf(product.name) }
    var category by remember(product.sku) { mutableStateOf(product.category) }
    var price by remember(product.sku) { mutableStateOf(product.price.toString()) }
    var fractional by remember(product.sku) { mutableStateOf(product.fractionalQuantity) }
    val valid = name.isNotBlank() && category.isNotBlank() && (price.toIntOrNull() ?: -1) >= 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Edit cloth", "कपड़ा बदलें"), color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Cloth name", "कपड़े का नाम")) }, singleLine = true)
                OutlinedTextField(category, { category = it }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Category", "श्रेणी")) }, singleLine = true)
                OutlinedTextField(
                    price,
                    { price = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Selling price (₹)", "बिक्री कीमत (₹)")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Sold by metre", "मीटर के हिसाब से बेचें"), Modifier.weight(1f), color = Ink)
                    Switch(checked = fractional, onCheckedChange = { fractional = it })
                }
                Text(tr("Current stock stays unchanged.", "मौजूदा स्टॉक में बदलाव नहीं होगा।"), color = Muted, fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), category.trim(), price.toIntOrNull() ?: 0, fractional) }, enabled = valid && !saving) {
                Text(if (saving) tr("Saving…", "सहेज रहे हैं…") else tr("Save changes", "बदलाव सहेजें"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text(tr("Cancel", "रद्द करें")) } }
    )
}

@Composable
private fun ProductPhoto(
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
                Image(displayPhoto.asImageBitmap(), contentDescription = "Cloth photo", modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
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

@Composable
private fun WhatsAppShareIconButton(onClick: () -> Unit, enabled: Boolean, sharing: Boolean) {
    IconButton(onClick = onClick, enabled = enabled) {
        WhatsAppShareIcon(sharing)
    }
}

@Composable
private fun WhatsAppShareIcon(sharing: Boolean) {
    val context = LocalContext.current
    val icon = remember(context) {
        runCatching { context.packageManager.getApplicationIcon("com.whatsapp").toBitmap().asImageBitmap() }.getOrNull()
    }
    when {
        sharing -> CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        icon != null -> Image(icon, contentDescription = tr("Share bill on WhatsApp", "बिल WhatsApp पर साझा करें"), modifier = Modifier.size(26.dp))
        else -> Icon(Icons.Rounded.Share, contentDescription = tr("Share bill", "बिल साझा करें"), tint = Teal)
    }
}

@Composable
private fun CustomerDirectoryScreen(
    padding: PaddingValues,
    query: String,
    customers: List<ShopCustomer>,
    searching: Boolean,
    syncing: Boolean,
    live: Boolean,
    onQuery: (String) -> Unit,
    onSync: () -> Unit,
    onCreate: () -> Unit,
    onCustomer: (ShopCustomer) -> Unit
) {
    val digits = CustomerCache.normalizePhone(query)
    val results = when {
        query.isBlank() -> customers.take(50)
        digits.isNotBlank() -> customers.filter {
            CustomerCache.normalizePhone(it.phone).contains(digits) || it.name.contains(query, ignoreCase = true)
        }
        else -> customers.filter { it.name.contains(query, ignoreCase = true) || it.phone.contains(query) }
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Customers", "ग्राहक"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                IconButton(onClick = onSync, enabled = live && !syncing, modifier = Modifier.size(48.dp)) {
                    if (syncing) CircularProgressIndicator(modifier = Modifier.size(21.dp), strokeWidth = 2.dp, color = Teal)
                    else Icon(Icons.Rounded.Refresh, contentDescription = tr("Sync customers", "ग्राहक सिंक करें"), tint = if (live) Teal else Muted)
                }
            }
        }
        item {
            ActionCard(
                title = tr("Create new customer", "नया ग्राहक जोड़ें"),
                subtitle = tr("Save details for faster billing next time", "अगली बार जल्दी बिल बनाने के लिए जानकारी सहेजें"),
                icon = Icons.Rounded.Add,
                fill = Ink,
                titleColor = Color.White,
                detailColor = Color(0xFFD5E5EA),
                onClick = onCreate
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(tr("Customer name or phone", "ग्राहक का नाम या फ़ोन")) },
                placeholder = { Text(tr("Type to search", "खोजने के लिए लिखें")) },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                shape = RoundedCornerShape(14.dp)
            )
        }
        item {
            Text(
                when {
                    searching -> tr("Checking the Shop sheet…", "Shop शीट में खोज रहे हैं…")
                    syncing -> tr("Syncing customers from Shop…", "Shop से ग्राहक सिंक हो रहे हैं…")
                    live -> tr("Saved customers are searched on this phone first.", "पहले इस फ़ोन में सहेजे ग्राहक खोजे जाते हैं।")
                    else -> tr("Connect to Shop to sync customer details.", "ग्राहक जानकारी सिंक करने के लिए Shop से जुड़ें।")
                },
                color = Muted,
                fontSize = 14.sp
            )
        }
        itemsIndexed(results, key = { index, customer -> "customer:$index:${customer.id}:${customer.phone}" }) { _, customer ->
            Card(onClick = { onCustomer(customer) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE1E8E7))) {
                Column(Modifier.fillMaxWidth().padding(15.dp)) {
                    Text(customer.name, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(customer.phone, fontSize = 16.sp, color = Teal)
                    if (customer.email.isNotBlank()) Text(customer.email, fontSize = 14.sp, color = Muted)
                }
            }
        }
        if (results.isEmpty() && !searching) item {
            Text(
                if (query.isBlank()) tr("No saved customers yet.", "अभी कोई ग्राहक सहेजा नहीं गया।") else tr("No matching customer found.", "कोई मिलता-जुलता ग्राहक नहीं मिला।"),
                color = Muted,
                fontSize = 16.sp,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun AddCustomerScreen(
    padding: PaddingValues, name: String, phone: String, email: String, address: String,
    saving: Boolean, connected: Boolean, onName: (String) -> Unit, onPhone: (String) -> Unit,
    onEmail: (String) -> Unit, onAddress: (String) -> Unit, onBack: () -> Unit, onSave: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader(tr("Create customer", "ग्राहक जोड़ें"), onBack); Text(tr("Save their details for faster billing next time.", "अगली बार जल्दी बिल बनाने के लिए जानकारी सहेजें।"), color = Muted, fontSize = 15.sp) }
        item { OutlinedTextField(name, onName, Modifier.fillMaxWidth(), label = { Text(tr("Customer name", "ग्राहक का नाम")) }, singleLine = true, shape = RoundedCornerShape(14.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp)) }
        item { OutlinedTextField(phone, onPhone, Modifier.fillMaxWidth(), label = { Text(tr("Phone number", "फ़ोन नंबर")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), shape = RoundedCornerShape(14.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp)) }
        item { OutlinedTextField(email, onEmail, Modifier.fillMaxWidth(), label = { Text(tr("Email (optional)", "ईमेल (वैकल्पिक)")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), shape = RoundedCornerShape(14.dp)) }
        item { OutlinedTextField(address, onAddress, Modifier.fillMaxWidth(), label = { Text(tr("Address (optional)", "पता (वैकल्पिक)")) }, minLines = 2, shape = RoundedCornerShape(14.dp)) }
        item {
            Button(onClick = onSave, enabled = connected && name.isNotBlank() && CustomerCache.normalizePhone(phone).length >= 8 && !saving, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(15.dp)) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                else Icon(Icons.Rounded.CheckCircle, null)
                Spacer(Modifier.width(8.dp)); Text(if (saving) tr("Saving…", "सहेज रहे हैं…") else tr("Save customer", "ग्राहक सहेजें"), fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun CustomerDetailScreen(
    padding: PaddingValues, customer: ShopCustomer, sales: List<ShopInvoice>, orders: List<CustomerOrderRequest>,
    finance: CustomerFinance?, financeLoading: Boolean, receivingPayment: Boolean,
    historyTab: String, onHistoryTab: (String) -> Unit,
    loading: Boolean, ordersLoading: Boolean, onBack: () -> Unit, onSync: () -> Unit, onOpenPdf: (String) -> Unit,
    onShareBill: (ShopInvoice) -> Unit, onOpenOrderRequest: (CustomerOrderRequest) -> Unit, sharingBillNumber: String?,
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
            val payment = bill?.paymentMode.orEmpty()
            val isCredit = bill != null && bill.due > 0
            val tagColor = when {
                order != null -> Color(0xFF1565C0)
                isCredit -> Color(0xFFC62828)
                else -> Teal
            }
            Card(
                onClick = {
                    if (bill != null) bill.pdfUrl.takeIf(String::isNotBlank)?.let(onOpenPdf)
                    else order?.let(onOpenOrderRequest)
                },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = if (order != null) Color(0xFFEAF3FF) else Color.White),
                border = BorderStroke(1.dp, tagColor.copy(alpha = 0.35f))
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(50), color = tagColor.copy(alpha = 0.1f)) {
                            Text(
                                text = when {
                                    order != null -> tr("Order request", "ऑर्डर अनुरोध")
                                    isCredit -> tr("DUE", "बकाया")
                                    else -> tr("PAID · $payment", "चुका · $payment")
                                },
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                color = tagColor, fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(if (bill != null) "₹${bill.total}" else order?.status.orEmpty(), color = tagColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(bill?.number ?: order?.number.orEmpty(), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(bill?.date ?: order?.date.orEmpty(), color = Muted, fontSize = 13.sp)
                    }
                    Text(
                        text = if (bill != null) tr("Paid ₹${bill.paid} · Due ₹${bill.due}", "मिला ₹${bill.paid} · बकाया ₹${bill.due}")
                        else tr("${order?.productName.orEmpty()} · Qty ${order?.let { formatQuantity(it.quantity) }.orEmpty()}", "${order?.productName.orEmpty()} · मात्रा ${order?.let { formatQuantity(it.quantity) }.orEmpty()}"),
                        color = if ((bill?.due ?: 0) > 0) Color(0xFFC62828) else if (order != null) Ink else Teal,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1
                    )
                    Divider(color = Color(0xFFE4E8EB))
                    Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = {
                            if (bill != null) bill.pdfUrl.takeIf(String::isNotBlank)?.let(onOpenPdf)
                            else order?.let(onOpenOrderRequest)
                        }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                            Text(if (bill != null) tr("View bill", "बिल देखें") else tr("View request", "अनुरोध देखें"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.weight(1f))
                        if (bill != null) {
                            WhatsAppShareIconButton(
                                onClick = { onShareBill(bill) },
                                enabled = sharingBillNumber == null,
                                sharing = sharingBillNumber == bill.number
                            )
                        } else {
                            Spacer(Modifier.size(42.dp))
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
                                    .background(if (selected) Teal else Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable { customerPaymentTab = tab },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = if (selected) Color.White else Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
                if ((finance?.currentDue ?: 0) > 0) item {
                    Text(tr("A customer payment is applied to the oldest unpaid bills first.", "ग्राहक का भुगतान पहले सबसे पुराने बकाया बिलों में जोड़ा जाएगा।"), color = Muted, fontSize = 13.sp)
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
                                Text(tr("Show voided payments", "रद्द भुगतान दिखाएँ"), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
                        else tr("All recorded payments are voided. Turn on “Show voided payments” to view them.", "सभी दर्ज भुगतान रद्द हैं। देखने के लिए “रद्द भुगतान दिखाएँ” चालू करें।"),
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
        androidx.compose.material3.AlertDialog(
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
        androidx.compose.material3.AlertDialog(
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
private fun FinanceSummaryRow(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = Muted, fontSize = 15.sp)
        Text(value, color = valueColor, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SearchScreen(
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
                    Icon(Icons.Rounded.QrCode2, contentDescription = tr("Scan clothes", "कपड़े स्कैन करें"), tint = Teal)
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
                        "₹${product.price}${if (product.fractionalQuantity) tr(" per metre", " प्रति मीटर") else tr(" per piece", " प्रति पीस")}"
                    )
                    DetailLine(
                        tr("Stock left", "बाकी स्टॉक"),
                        "${formatQuantity(product.stock)}${if (product.fractionalQuantity) tr(" metres", " मीटर") else tr(" pieces", " पीस")}"
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
                            Text(tr("Tap to create or share its QR label.", "QR लेबल बनाने या साझा करने के लिए टैप करें।"), color = Muted, fontSize = 13.sp)
                            TextButton(
                                onClick = {
                                    selectedProduct = null
                                    onProductQr(product)
                                },
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
                            ) { Text(tr("Create & share QR PDF", "QR PDF बनाएँ और साझा करें")) }
                        }
                    }
                    if (product.stock <= 0) Text(tr("Out of stock", "स्टॉक खत्म"), color = Color(0xFFB42318), fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedProduct = null; onAddToBill(product) },
                    enabled = product.stock > 0
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
private fun BillSearchProductCard(
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
                    Text("₹${product.price}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
                    Spacer(Modifier.width(7.dp))
                    Box(Modifier.width(1.dp).height(25.dp).background(Color(0xFFCCD2D8)))
                    Spacer(Modifier.width(7.dp))
                    val stockUnit = when {
                        product.fractionalQuantity -> tr("m", "मीटर")
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
                                if (product.fractionalQuantity) Icons.Rounded.Straighten else Icons.Rounded.Inventory2,
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
                enabled = product.stock > 0,
                modifier = Modifier.size(48.dp).background(if (product.stock > 0) ClothActionBlue else Color(0xFFE7E9EC), CircleShape)
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = if (orderMode) tr("Add to order", "ऑर्डर में जोड़ें") else tr("Add to bill", "बिल में जोड़ें"),
                    tint = if (product.stock > 0) Ink else Muted,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product, live: Boolean, orderMode: Boolean, onAdd: () -> Unit, onQr: () -> Unit) {
    val qrBitmap = remember(product.sku) { createQrBitmap(product.sku).asImageBitmap() }
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE1E8E7))) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(product.name, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.height(4.dp))
                    Text("${product.sku} · ${product.category}", fontSize = 14.sp, color = Muted)
                    Spacer(Modifier.height(10.dp))
                    Text("₹${product.price}", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Teal)
                    val stockText = "${formatQuantity(product.stock)}${if (product.fractionalQuantity) " m" else ""}"
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

@Composable
private fun AddProductScreen(
    padding: PaddingValues,
    connected: Boolean,
    saving: Boolean,
    name: String,
    category: String,
    price: String,
    cost: String,
    stock: String,
    fractionalQuantity: Boolean,
    photo: Bitmap?,
    onTakePhoto: () -> Unit,
    onChoosePhoto: () -> Unit,
    onName: (String) -> Unit,
    onCategory: (String) -> Unit,
    onPrice: (String) -> Unit,
    onCost: (String) -> Unit,
    onStock: (String) -> Unit,
    onFractionalQuantity: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    var categoryPickerOpen by remember { mutableStateOf(false) }
    var categorySearch by remember { mutableStateOf("") }
    val parsedStock = stock.toDoubleOrNull()
    val stockPrecisionValid = parsedStock != null && kotlin.math.abs(parsedStock * 1000 - (parsedStock * 1000).roundToInt()) < 0.001
    val validOpeningStock = parsedStock != null && parsedStock >= 0 && parsedStock <= 100000 &&
        stockPrecisionValid && (fractionalQuantity || parsedStock % 1.0 == 0.0)
    Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 18.dp).padding(top = 8.dp, bottom = 78.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            ScreenHeader(tr("Add a cloth", "कपड़ा जोड़ें"), onBack)
            ProductPhoto(
                sku = "new-product-preview",
                imageId = "",
                imageLoader = { null },
                modifier = Modifier.fillMaxWidth().height(104.dp),
                selectedPhoto = photo,
                onTakePhoto = onTakePhoto,
                onChoosePhoto = onChoosePhoto
            )
            OutlinedTextField(name, onName, Modifier.fillMaxWidth(), label = { Text(tr("Cloth name", "कपड़े का नाम")) }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp), shape = RoundedCornerShape(14.dp))
            OutlinedButton(
                onClick = { categorySearch = ""; categoryPickerOpen = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Text(category.ifBlank { tr("Choose category (optional)", "श्रेणी चुनें (वैकल्पिक)") }, Modifier.weight(1f), fontSize = 15.sp, color = if (category.isBlank()) Muted else Ink)
                Text("⌄", fontSize = 20.sp, color = Ink)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    price, onPrice, Modifier.weight(1f),
                    label = { Text(if (fractionalQuantity) tr("Price / metre ₹", "कीमत / मीटर ₹") else tr("Selling price ₹", "बेचने की कीमत ₹"), maxLines = 1, fontSize = 13.sp) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp), shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    cost, onCost, Modifier.weight(1f),
                    label = { Text(if (fractionalQuantity) tr("Cost / metre ₹", "लागत / मीटर ₹") else tr("Cost price ₹", "खरीद कीमत ₹"), maxLines = 1, fontSize = 13.sp) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp), shape = RoundedCornerShape(14.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    stock, onStock, Modifier.weight(0.9f),
                    label = { Text(if (fractionalQuantity) tr("Stock (metres)", "स्टॉक (मीटर)") else tr("Starting stock", "शुरुआती स्टॉक"), maxLines = 1, fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = if (fractionalQuantity) KeyboardType.Decimal else KeyboardType.Number),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp), shape = RoundedCornerShape(14.dp)
                )
                Surface(Modifier.weight(1.1f), shape = RoundedCornerShape(14.dp), color = SoftBlue) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Decimal quantity", "दशमलव मात्रा"), Modifier.weight(1f), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        Switch(checked = fractionalQuantity, onCheckedChange = onFractionalQuantity)
                    }
                }
            }
        }
        Button(
            onClick = onSave,
            enabled = connected && !saving && name.isNotBlank() && price.isNotBlank() && cost.isNotBlank() && validOpeningStock,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp).height(58.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (saving) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
            else Icon(Icons.Rounded.CheckCircle, null)
            Spacer(Modifier.width(10.dp)); Text(if (saving) tr("Saving cloth…", "कपड़ा सहेज रहे हैं…") else tr("Save cloth", "कपड़ा सहेजें"), fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
    }
    if (categoryPickerOpen) {
        val filteredCategories = clothCategories.filter { it.contains(categorySearch.trim(), ignoreCase = true) }
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
                    LazyColumn(Modifier.heightIn(max = 400.dp)) {
                        items(filteredCategories) { option ->
                            TextButton(
                                onClick = { onCategory(option); categoryPickerOpen = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(option, modifier = Modifier.fillMaxWidth(), fontSize = 15.sp, color = Ink)
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BillScreen(
    padding: PaddingValues,
    connected: Boolean,
    orderMode: Boolean,
    orderNotes: String,
    onOrderNotes: (String) -> Unit,
    customerName: String,
    customerPhone: String,
    customerEmail: String,
    customerAddress: String,
    customerExists: Boolean?,
    customerSearching: Boolean,
    customerMatches: List<ShopCustomer>,
    saving: Boolean,
    savingCustomer: Boolean,
    paymentMode: String,
    discountValue: String,
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
    onCreditAmountReceived: (String) -> Unit,
    onCreditPaymentMethod: (String) -> Unit,
    onSelectCustomer: (ShopCustomer) -> Unit,
    onPickContact: () -> Unit,
    onScan: () -> Unit,
    onFindProduct: () -> Unit,
    imageLoader: suspend (String) -> ByteArray?,
    onQuantity: (String, Double) -> Unit,
    onSetQuantity: (String, Double) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    var selectedProductDetails by remember { mutableStateOf<Product?>(null) }
    var showNewCustomerDialog by remember { mutableStateOf(false) }
    var promptedCustomerPhone by remember { mutableStateOf("") }
    val subtotal = cart.sumOf { lineAmount(it.product.price, it.quantity) }
    val parsedDiscount = discountValue.toDoubleOrNull() ?: 0.0
    val discountAmount = parsedDiscount.roundToInt()
    val total = subtotal - discountAmount
    val validDiscount = discountValue.toDoubleOrNull() != null && parsedDiscount >= 0 && discountAmount <= subtotal && total > 0
    val validCart = cart.isNotEmpty() && cart.all(::validCartQuantity)
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
        !validCart -> tr("Check the cloth quantities", "कपड़ों की मात्रा जाँचें")
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
    val creditAmountBringIntoViewRequester = remember { BringIntoViewRequester() }
    val billListState = rememberLazyListState()
    val screenScope = rememberCoroutineScope()
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
            if (orderMode) Text(
                tr("Add the customer and clothes just like a bill. The manager will review this request; no payment is collected now.", "बिल की तरह ग्राहक और कपड़े जोड़ें। मैनेजर अनुरोध देखेंगे; अभी भुगतान नहीं लिया जाएगा।"),
                color = Muted, fontSize = 15.sp, lineHeight = 20.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
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
                            IconButton(onClick = onPickContact) { Icon(Icons.Rounded.Contacts, "Choose contact") }
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
                            Text("${line.product.sku} · ₹${line.product.price}${if (line.product.fractionalQuantity) " / m" else " each"}", color = Muted, fontSize = 12.sp, maxLines = 1)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CartQuantityControl(line, onQuantity, onSetQuantity, compact = true)
                                Spacer(Modifier.weight(1f))
                                Text("₹${lineAmount(line.product.price, line.quantity)}", color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(tr("Discount", "छूट"), fontSize = 16.sp, color = Ink)
                            Text(tr("Enter amount in ₹", "राशि ₹ में लिखें"), fontSize = 12.sp, color = Muted)
                        }
                        OutlinedTextField(
                            value = discountValue,
                            onValueChange = onDiscountValue,
                            modifier = Modifier
                                .width(148.dp)
                                .bringIntoViewRequester(discountBringIntoViewRequester)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        screenScope.launch {
                                            // The keyboard changes the viewport after focus. Scroll the
                                            // lazy list itself so the entire amount editor is visible.
                                            delay(350)
                                            val discountItemIndex = if (cart.isEmpty()) 4 else 3 + cart.size
                                            billListState.animateScrollToItem(discountItemIndex)
                                            delay(100)
                                            discountBringIntoViewRequester.bringIntoView()
                                        }
                                    }
                                },
                            label = { Text(tr("Amount (₹)", "राशि (₹)"), maxLines = 1) },
                            placeholder = { Text("0") },
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
                                    // Keep the active payment amount above the keyboard and footer.
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
        item {
            if (orderMode) {
                SectionTitle("3", tr("Order details", "ऑर्डर की जानकारी"))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = orderNotes,
                    onValueChange = onOrderNotes,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Note for manager (optional)", "मैनेजर के लिए नोट (वैकल्पिक)")) },
                    minLines = 2,
                    maxLines = 4,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp),
                    shape = RoundedCornerShape(14.dp)
                )
            }
            if (orderMode) Surface(shape = RoundedCornerShape(18.dp), color = SoftGreen) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Estimated total", "अनुमानित कुल"), Modifier.weight(1f), fontSize = 18.sp, color = Ink)
                    Text("₹$subtotal", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Teal)
                }
            }
            if (orderMode) Text(
                tr("This is an order request only. It will not create a bill or change stock.", "यह केवल ऑर्डर अनुरोध है। इससे बिल नहीं बनेगा और स्टॉक नहीं बदलेगा।"),
                color = Muted, fontSize = 14.sp, lineHeight = 19.sp
            )
            if (orderMode) Text(
                tr("The manager will see this request in Order Requests.", "मैनेजर यह अनुरोध Order Requests में देखेंगे।"),
                Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = Muted
            )
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
              else Icon(Icons.AutoMirrored.Rounded.ReceiptLong, null)
              Spacer(Modifier.width(10.dp))
              Text(if (orderMode) tr("Send order request", "ऑर्डर अनुरोध भेजें") else tr("Create bill", "बिल बनाएँ"), fontSize = 19.sp, fontWeight = FontWeight.Bold)
          }
      }
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
                    DetailLine(if (product.fractionalQuantity) tr("Price per metre", "प्रति मीटर कीमत") else tr("Price per piece", "प्रति पीस कीमत"), "₹${product.price}${if (product.fractionalQuantity) "/m" else ""}")
                    DetailLine(tr("In this bill", "इस बिल में मात्रा"), formatQuantity(quantityInBill) + if (product.fractionalQuantity) " m" else "")
                    DetailLine(tr("Line total", "कुल कीमत"), "₹${lineAmount(product.price, quantityInBill)}")
                    DetailLine(tr("Available stock", "उपलब्ध स्टॉक"), formatQuantity(product.stock) + if (product.fractionalQuantity) " m" else "")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedProductDetails = null }) { Text(tr("Close", "बंद करें")) }
            }
        )
    }
}

@Composable
private fun PaymentsScreen(
    padding: PaddingValues,
    bills: List<ShopInvoice>,
    customers: List<ShopCustomer>,
    paymentsMade: List<PaymentReceipt>,
    listTab: String,
    query: String,
    loading: Boolean,
    connected: Boolean,
    isAdministrator: Boolean,
    requests: List<DeletionRequest>,
    onRequestDeletion: (PaymentReceipt) -> Unit,
    onReviewRequest: (DeletionRequest, Boolean) -> Unit,
    onQuery: (String) -> Unit,
    onListTab: (String) -> Unit,
    onSync: () -> Unit,
    onCustomer: (ShopCustomer) -> Unit,
    onRetryPayment: (PaymentReceipt) -> Unit
) {
    var showVoidedPayments by remember { mutableStateOf(false) }
    val openBills = bills.filter { it.due > 0 }
    val grouped = openBills.groupBy { CustomerCache.normalizePhone(it.phone) }
        .filterKeys { it.isNotBlank() }
        .map { (phoneDigits, customerBills) ->
            val first = customerBills.first()
            val customer = customers.firstOrNull { CustomerCache.normalizePhone(it.phone) == phoneDigits }
                ?: ShopCustomer("", first.customerName, first.phone, "")
            Triple(customer, customerBills.sumOf { it.due }, customerBills.size)
        }
        .filter { (customer, _, _) ->
            query.isBlank() || customer.name.contains(query.trim(), ignoreCase = true) ||
                CustomerCache.normalizePhone(customer.phone).contains(CustomerCache.normalizePhone(query))
        }
        .sortedByDescending { it.second }
    val totalDue = openBills.sumOf { it.due }
    val accountCount = openBills.map { CustomerCache.normalizePhone(it.phone) }.distinct().size
    val voidedPaymentCount = paymentsMade.count { it.isVoided }
    val filteredPayments = paymentsMade.filter { payment ->
        (showVoidedPayments || !payment.isVoided) &&
            (query.isBlank() || payment.customerName.contains(query.trim(), ignoreCase = true) ||
                CustomerCache.normalizePhone(payment.customerPhone).contains(CustomerCache.normalizePhone(query)) ||
                payment.invoiceNumber.contains(query.trim(), ignoreCase = true))
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isAdministrator) item { AdminDeletionRequestsSection("PAYMENT", requests, onReviewRequest) }
        item {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Payments", "भुगतान"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = onSync,
                        enabled = !loading && connected,
                        modifier = Modifier.size(44.dp)
                    ) {
                        if (loading) CircularProgressIndicator(modifier = Modifier.size(21.dp), strokeWidth = 2.dp, color = Teal)
                        else Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = tr("Sync payments from Shop", "Shop से भुगतान सिंक करें"),
                            tint = if (connected) Teal else Muted
                        )
                    }
                }
                Text(
                    if (connected) tr("Settle customer balances and record receipts.", "ग्राहक का बकाया चुकता करें और भुगतान दर्ज करें।")
                    else tr("Showing saved balances from this phone. Sign in to sync.", "इस फ़ोन में सहेजे बकाया दिख रहे हैं। सिंक करने के लिए साइन इन करें।"),
                    color = Muted, fontSize = 15.sp
                )
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF0F3F2)
            ) {
                Row(Modifier.padding(4.dp)) {
                    val historySelected = listTab == "MADE"
                    Box(
                        Modifier.weight(1f).height(48.dp)
                            .background(if (historySelected) Teal else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { onListTab("MADE") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            tr("Payment history", "भुगतान इतिहास"),
                            color = if (historySelected) Color.White else Ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    val billsSelected = listTab == "DUE"
                    Box(
                        Modifier.weight(1f).height(48.dp)
                            .background(if (billsSelected) Teal else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { onListTab("DUE") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            tr("Bills to settle", "बकाया बिल"),
                            color = if (billsSelected) Color.White else Ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        if (listTab == "MADE" && voidedPaymentCount > 0) item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFFF1F0)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(tr("Show voided payments", "रद्द भुगतान दिखाएँ"), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(tr("$voidedPaymentCount hidden", "$voidedPaymentCount छिपे हुए"), color = Color(0xFFB3261E), fontSize = 13.sp)
                    }
                    Switch(checked = showVoidedPayments, onCheckedChange = { showVoidedPayments = it })
                }
            }
        }
        if (listTab == "DUE") item {
            Surface(shape = RoundedCornerShape(16.dp), color = SoftBlue, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Total outstanding", "कुल बकाया"), color = Muted, fontSize = 15.sp)
                        Text("₹$totalDue", color = if (totalDue > 0) Color(0xFFC62828) else Teal, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(tr("$accountCount customers", "$accountCount ग्राहक"), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (listTab == "MADE") item {
            Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFE8F5F3), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Payments recorded", "दर्ज भुगतान"), color = Muted, fontSize = 15.sp)
        Text("${paymentsMade.count { !it.isVoided }}", color = Teal, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(tr("₹${paymentsMade.filterNot { it.isVoided }.sumOf { it.amount }} received", "₹${paymentsMade.filterNot { it.isVoided }.sumOf { it.amount }} मिले"), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (listTab == "MADE") tr("Find payment by customer, phone or bill", "ग्राहक, फ़ोन या बिल से खोजें") else tr("Find customer by name or phone", "नाम या फ़ोन से ग्राहक खोजें")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = RoundedCornerShape(14.dp)
            )
        }
        if (listTab == "MADE" && filteredPayments.isEmpty()) item {
            Text(
                if (loading) tr("Loading payments…", "भुगतान लोड हो रहा है…")
                else if (paymentsMade.isEmpty()) tr("No payments recorded yet.", "अभी तक कोई भुगतान दर्ज नहीं है।")
                else if (!showVoidedPayments && voidedPaymentCount == paymentsMade.size) tr("All recorded payments are voided. Turn on “Show voided payments” to view them.", "सभी दर्ज भुगतान रद्द हैं। देखने के लिए “रद्द भुगतान दिखाएँ” चालू करें।")
                else tr("No payments match this search.", "इस खोज से कोई भुगतान नहीं मिला।"),
                color = Muted, fontSize = 16.sp, modifier = Modifier.padding(8.dp)
            )
        }
        if (listTab == "DUE" && grouped.isEmpty()) item {
            Text(
                if (loading) tr("Loading outstanding balances…", "बकाया राशि लोड हो रही है…")
                else if (openBills.isEmpty()) tr("No customer payments are due.", "किसी ग्राहक का भुगतान बाकी नहीं है।")
                else tr("No customer matches this search.", "इस खोज से कोई ग्राहक नहीं मिला।"),
                color = Muted, fontSize = 16.sp, modifier = Modifier.padding(8.dp)
            )
        }
        if (listTab == "MADE") itemsIndexed(filteredPayments, key = { index, payment -> "made-payment:$index:${payment.requestId}:${payment.receiptNumber}" }) { _, payment ->
            val payerName = payment.customerName.ifBlank {
                customers.firstOrNull {
                    CustomerCache.normalizePhone(it.phone) == CustomerCache.normalizePhone(payment.customerPhone)
                }?.name
            }.orEmpty().ifBlank {
                payment.customerPhone.ifBlank { tr("Unknown customer", "अज्ञात ग्राहक") }
            }
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (payment.isVoided) Color(0xFFFFF1F0) else Color.White),
                border = BorderStroke(1.dp, if (payment.isVoided) Color(0xFFD96C65) else Color(0xFFDCE7E7))
            ) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${tr("Paid by", "भुगतानकर्ता")}: $payerName", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("${payment.date}  ·  ${payment.method}", color = Muted, fontSize = 13.sp)
                        if (payment.invoiceNumber.isNotBlank()) Text(tr("Bill ${payment.invoiceNumber}", "बिल ${payment.invoiceNumber}"), color = Muted, fontSize = 13.sp)
                        Text(
                            when {
                                payment.isVoided -> tr("Voided by administrator", "प्रशासक ने रद्द किया")
                                payment.syncStatus == "SYNCED" -> tr("Saved in Shop", "Shop में सहेजा गया")
                                payment.syncStatus == "SYNCING" -> tr("Syncing in background…", "बैकग्राउंड में सिंक हो रहा है…")
                                else -> tr("Waiting to sync", "सिंक होना बाकी है")
                            },
                            color = when {
                                payment.isVoided -> Color(0xFFB3261E)
                                payment.syncStatus == "SYNCED" -> Color(0xFF2E7D32)
                                payment.syncStatus == "SYNCING" -> Teal
                                else -> Color(0xFFC62828)
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("₹${payment.amount}", color = if (payment.isVoided) Color(0xFFB3261E) else Color(0xFF1565C0), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        if (payment.syncStatus == "SYNCED" && !payment.isVoided) {
                            IconButton(onClick = { onRequestDeletion(payment) }, modifier = Modifier.size(42.dp)) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = tr("Request payment deletion", "भुगतान हटाने का अनुरोध"), tint = Color(0xFFB3261E))
                            }
                        }
                        if (payment.syncStatus != "SYNCED") TextButton(onClick = { onRetryPayment(payment) }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                            Text(tr("Sync now", "अभी सिंक करें"), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        if (listTab == "DUE") itemsIndexed(grouped, key = { index, entry -> "due-customer:$index:${CustomerCache.normalizePhone(entry.first.phone)}" }) { _, (customer, due, count) ->
            Card(
                onClick = { onCustomer(customer) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFC62828).copy(alpha = 0.25f))
            ) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(customer.name.ifBlank { tr("Customer", "ग्राहक") }, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(customer.phone, color = Muted, fontSize = 15.sp)
                        Text(tr("$count unpaid bills", "$count बकाया बिल"), color = Muted, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("₹$due", color = Color(0xFFC62828), fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text(tr("Settle", "भुगतान लें"), color = Teal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BillsScreen(
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
    onOpenPdf: (String) -> Unit,
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
                    color = if (selected) Teal else Color.Transparent,
                    border = if (selected) null else BorderStroke(1.dp, Color(0xFFB9C1CC))
                ) {
                    Box(Modifier.padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                        Text(tr(filter, when (filter) { "Paid" -> "चुका हुआ"; "Due" -> "बाकी"; "Cancelled" -> "रद्द"; else -> "सभी" }), color = if (selected) Color.White else Ink, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                    }
                }
            }
        }
        Text(tr("Recent bills", "हाल के बिल"), modifier = Modifier.padding(top = 18.dp, bottom = 10.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(tr("Tap a bill to see the clothes in it.", "बिल में शामिल कपड़े देखने के लिए बिल पर टैप करें।"), color = Muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
            if (isAdministrator) item { AdminDeletionRequestsSection("BILL", requests, onReviewRequest) }
            itemsIndexed(visibleBills, key = { index, bill -> "bill:$index:${bill.number}" }) { _, bill ->
                val paid = bill.due <= 0 && !bill.isVoided
                val stampColor = when { bill.isVoided -> Color(0xFFB3261E); paid -> Color(0xFF32845F); else -> Color(0xFFB76B2E) }
                Card(
                    onClick = { selectedBillDetails = bill },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = if (bill.isVoided) Color(0xFFFFF1F0) else Color.White),
                    border = BorderStroke(1.dp, if (bill.isVoided) Color(0xFFD96C65) else Color(0xFFD9DEE5))
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f).padding(end = 6.dp)) {
                                Text(bill.customerName.ifBlank { tr("Customer", "ग्राहक") }, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
                                Text(bill.date, modifier = Modifier.padding(top = 5.dp), fontSize = 15.sp, color = Muted)
                                Text(bill.number, modifier = Modifier.padding(top = 5.dp), fontSize = 14.sp, color = Muted, maxLines = 1)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${bill.total}", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = if (bill.isVoided) Color(0xFFB3261E) else Teal)
                                Box(Modifier.padding(top = 4.dp, end = 2.dp).rotate(-11f).border(2.dp, stampColor, RoundedCornerShape(7.dp)).padding(horizontal = 9.dp, vertical = 2.dp)) {
                                    Text(when { bill.isVoided -> tr("CANCELLED", "रद्द"); paid -> tr("PAID", "चुका"); else -> tr("DUE", "बाकी") }, color = stampColor, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                }
                                if (bill.isVoided) Text(tr("Wrong entry · cancelled", "गलत प्रविष्टि · रद्द"), modifier = Modifier.padding(top = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = stampColor)
                                else if (!paid) Text(tr("₹${bill.due} due", "₹${bill.due} बाकी"), modifier = Modifier.padding(top = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFB76B2E))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Divider(color = Color(0xFFE0E5EB))
                        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { if (bill.pdfUrl.isNotBlank()) onOpenPdf(bill.pdfUrl) }, enabled = bill.pdfUrl.isNotBlank() && !bill.isVoided, modifier = Modifier.weight(1f)) {
                                Icon(Icons.AutoMirrored.Rounded.ReceiptLong, null, tint = Teal, modifier = Modifier.size(21.dp))
                                Spacer(Modifier.width(7.dp))
                                Text(tr("View PDF", "PDF देखें"), color = Teal, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Divider(modifier = Modifier.height(28.dp).width(1.dp), color = Color(0xFFBCC5D0))
                            Row(Modifier.weight(1f).clickable(enabled = sharingBillNumber == null && !bill.isVoided) { onShareBill(bill) }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                WhatsAppShareIcon(sharing = sharingBillNumber == bill.number)
                                Text(tr("WhatsApp", "WhatsApp"), color = if (bill.isVoided) Muted else Teal, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (!bill.isVoided) {
                            IconButton(onClick = { onRequestDeletion(bill) }, modifier = Modifier.align(Alignment.End).size(42.dp)) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = tr("Request bill deletion", "बिल हटाने का अनुरोध"), tint = Color(0xFFB3261E))
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
                        Text(
                            tr("Cloth details haven't synced for this bill yet. Tap Sync bills to try again.", "इस बिल के कपड़ों की जानकारी अभी सिंक नहीं हुई है। फिर से कोशिश करने के लिए बिल सिंक करें।"),
                            color = Muted,
                            fontSize = 14.sp
                        )
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
                                    Text("${formatQuantity(line.quantity)}${if (line.fractionalQuantity) " m" else ""} × ₹${line.unitPrice}${if (line.fractionalQuantity) "/m" else ""} = ₹${line.lineTotal}", color = Teal, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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

@Composable
private fun BillClothThumbnail(
    product: Product,
    imageLoader: suspend (String) -> ByteArray?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(product.sku, product.imageId) {
        mutableStateOf(ProductImageCache.memoryHit(product.sku, product.imageId))
    }
    LaunchedEffect(product.sku, product.imageId) {
        if (product.imageId.isNotBlank()) {
            bitmap = withContext(Dispatchers.IO) { ProductImageCache.load(context, product.sku, product.imageId) }
            if (bitmap == null) {
                val bytes = runCatching { imageLoader(product.imageId) }.getOrNull()
                if (bytes != null) bitmap = withContext(Dispatchers.IO) {
                    ProductImageCache.save(context, product.sku, product.imageId, bytes)
                        ?: BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }
        }
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = SoftBlue) {
        if (bitmap != null) Image(
            bitmap!!.asImageBitmap(), contentDescription = product.name,
            modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop
        ) else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = Muted, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun SectionTitle(number: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = Ink, modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(number, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
        Spacer(Modifier.width(10.dp))
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
    }
}

internal fun createQrBitmap(value: String): Bitmap {
    val hints = mapOf(
        EncodeHintType.MARGIN to 1,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
    )
    val matrix = MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, 600, 600, hints)
    return Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
}
