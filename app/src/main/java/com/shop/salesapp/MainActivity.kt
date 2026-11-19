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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.rounded.Contacts
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val Ink = Color(0xFF153447)
private val Teal = Color(0xFF147D78)
private val Paper = Color(0xFFF5F7F5)
private val SoftBlue = Color(0xFFE8F1F5)
private val SoftGreen = Color(0xFFE5F3EF)
private val Muted = Color(0xFF60717A)
private val salesAppHindi = mutableStateOf(false)

@Composable
private fun tr(english: String, hindi: String): String = if (salesAppHindi.value) hindi else english

data class CartLine(val product: Product, val quantity: Int)
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
    local.forEach { payment ->
        val index = merged.indexOfFirst {
            (payment.requestId.isNotBlank() && it.requestId == payment.requestId) ||
                (payment.receiptNumber.isNotBlank() && it.receiptNumber == payment.receiptNumber)
        }
        if (index >= 0) merged[index] = payment else merged.add(payment)
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

private enum class Screen { Home, BillCenter, Bill, Bills, Customer, CustomerDetail, AddCustomer, ProductCenter, Search, Catalog, AddProduct, PlaceOrder, Payments }

private val previewProducts = listOf(
    Product("SKU-001", "Wireless Bluetooth Earbuds", "Electronics", 1999, 37),
    Product("SKU-002", "Wireless Optical Mouse", "Peripherals", 799, 58),
    Product("SKU-004", "USB-C Fast Charging Cable", "Accessories", 349, 91)
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
    var isHindi by remember(context) {
        mutableStateOf(context.getSharedPreferences("sales_app_preferences", android.content.Context.MODE_PRIVATE).getBoolean("use_hindi", false))
    }
    LaunchedEffect(isHindi) { salesAppHindi.value = isHindi }
    val savedProducts = remember(context) { ProductCache.load(context) }
    var hasLiveProducts by remember(context) { mutableStateOf(savedProducts.isNotEmpty()) }
    var selectedTab by remember { mutableIntStateOf(-1) }
    var query by remember { mutableStateOf("") }
    var productSyncing by remember { mutableStateOf(false) }
    var customerDirectoryQuery by remember { mutableStateOf("") }
    var customerDirectoryLoading by remember { mutableStateOf(false) }
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
    var message by remember { mutableStateOf<String?>(null) }
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

    LaunchedEffect(screen) {
        if (screen != Screen.Payments) return@LaunchedEffect
        while (true) {
            madePayments = mergePaymentHistory(madePayments, LocalPaymentLog.loadAll(context))
            delay(1_500)
        }
    }

    fun navigateTo(destination: Screen) {
        if (screen != destination) {
            backStack.add(screen)
            screen = destination
            selectedTab = when (destination) {
                Screen.BillCenter, Screen.Bill, Screen.Bills -> 0
                Screen.Customer, Screen.CustomerDetail, Screen.AddCustomer -> 1
                Screen.ProductCenter, Screen.Search, Screen.Catalog, Screen.AddProduct, Screen.PlaceOrder -> 2
                Screen.Payments -> 3
                else -> -1
            }
        }
    }

    fun navigateBack() {
        screen = if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex) else Screen.Home
        selectedTab = when (screen) {
            Screen.BillCenter, Screen.Bill, Screen.Bills -> 0
            Screen.Customer, Screen.CustomerDetail, Screen.AddCustomer -> 1
            Screen.ProductCenter, Screen.Search, Screen.Catalog, Screen.AddProduct, Screen.PlaceOrder -> 2
            Screen.Payments -> 3
            else -> -1
        }
    }

    BackHandler(enabled = screen != Screen.Home) { navigateBack() }

    LaunchedEffect(idToken, api.isConfigured) {
        val token = idToken ?: return@LaunchedEffect
        if (!api.isConfigured) return@LaunchedEffect
        runCatching {
            withContext(Dispatchers.IO) { api.bills(token, "") to api.payments(token) }
        }
            .onSuccess { (fresh, remotePayments) ->
                val merged = BillCache.merge(BillCache.load(context), fresh)
                bills.clear(); bills.addAll(merged)
                BillCache.save(context, merged)
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

    LaunchedEffect(query, idToken) {
        productSyncing = false
        val token = idToken ?: return@LaunchedEffect
        if (!api.isConfigured) return@LaunchedEffect
        productSyncing = true
        delay(if (query.isBlank()) 0 else 250)
        runCatching { withContext(Dispatchers.IO) { api.products(token, query) } }
            .onSuccess { result ->
                val existing = if (hasLiveProducts) products.toList() else emptyList()
                val merged = ProductCache.merge(existing, result)
                products.clear()
                products.addAll(merged)
                hasLiveProducts = true
                ProductCache.save(context, merged)
            }
            .onFailure { message = it.message ?: "Could not load clothes from Shop." }
        productSyncing = false
    }

    LaunchedEffect(idToken, api.isConfigured) {
        val token = idToken ?: return@LaunchedEffect
        if (!api.isConfigured) return@LaunchedEffect
        runCatching { withContext(Dispatchers.IO) { api.customers(token) } }
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
        runCatching { withContext(Dispatchers.IO) { api.searchCustomers(token, customerPhone) } }
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
        runCatching { withContext(Dispatchers.IO) { api.findCustomer(token, customerDirectoryQuery) } }
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
            val sales = async(Dispatchers.IO) { runCatching { api.bills(token, customer.phone) } }
            val requests = async(Dispatchers.IO) { runCatching { api.orderRequests(token, customer.phone) } }
            val finance = async(Dispatchers.IO) { runCatching { api.customerFinance(token, customer.phone) } }
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
                        runCatching {
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

    val scanner = remember { GmsBarcodeScanning.getClient(context) }

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

    fun shareBillPdf(bill: ShopInvoice) {
        val token = idToken
        if (!api.isConfigured || token == null) {
            message = "Sign in to Shop before sharing this bill."
            return
        }
        if (sharingBillNumber != null) return
        operationScope.launch {
            sharingBillNumber = bill.number
            runCatching {
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
            runCatching {
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
        if (screen in setOf(Screen.Home, Screen.BillCenter, Screen.Bill, Screen.Bills, Screen.Customer, Screen.CustomerDetail, Screen.AddCustomer, Screen.ProductCenter, Screen.Search, Screen.Catalog, Screen.AddProduct, Screen.Payments)) {
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
                    isHindi = isHindi,
                    onLanguageChange = { hindi ->
                        isHindi = hindi
                        context.getSharedPreferences("sales_app_preferences", android.content.Context.MODE_PRIVATE)
                            .edit().putBoolean("use_hindi", hindi).apply()
                    },
                    onSignIn = { beginSignIn() },
                    onNewBill = { orderMode = false; navigateTo(Screen.Bill) },
                    onMakePayment = { paymentListTab = "DUE"; navigateTo(Screen.Payments) },
                    onCatalog = { selectedTab = 2; navigateTo(Screen.Catalog) }
                )
                Screen.BillCenter -> BillCenterScreen(
                    padding = insets,
                    onCreateBill = { orderMode = false; navigateTo(Screen.Bill) },
                    onFindBill = { billsPhone = ""; navigateTo(Screen.Bills) }
                )
                Screen.ProductCenter -> ProductCenterScreen(
                    padding = insets,
                    onAddProduct = {
                        productName = ""; productCategory = ""; sellingPrice = ""; unitCost = ""; openingStock = ""
                        productImageBase64 = ""; productImageBitmap = null
                        navigateTo(Screen.AddProduct)
                    },
                    onSearch = { orderMode = false; navigateTo(Screen.Search) },
                    onCatalog = { navigateTo(Screen.Catalog) },
                    onPlaceOrder = {
                        orderMode = true
                        orderNotes = ""
                        cart.clear()
                        customerPhone = ""; customerName = ""; customerEmail = ""; customerAddress = ""
                        customerExists = null; customerMatches = emptyList()
                        navigateTo(Screen.PlaceOrder)
                    }
                )
                Screen.Customer -> CustomerDirectoryScreen(
                    padding = insets,
                    query = customerDirectoryQuery,
                    customers = cachedCustomers,
                    searching = customerDirectoryLoading,
                    live = idToken != null && api.isConfigured,
                    onQuery = { customerDirectoryQuery = it },
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
                        onOpenPdf = { url -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } },
                        onShareBill = { bill -> shareBillPdf(bill) },
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
                            runCatching { withContext(Dispatchers.IO) { api.createCustomer(token, newCustomerName, newCustomerPhone, newCustomerEmail, newCustomerAddress) } }
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
                    products = products,
                    orderMode = orderMode,
                    live = api.isConfigured && idToken != null,
                    refreshing = productSyncing,
                    onQuery = { query = it },
                    onShareQrLabels = { labels -> shareQrLabelPdf(labels) },
                    onProductQr = { qrPreviewProduct = it },
                    onScan = {
                        scanner.startScan()
                            .addOnSuccessListener { barcode ->
                                query = barcode.rawValue.orEmpty()
                                message = "Scanned ${barcode.rawValue.orEmpty()}"
                            }
                            .addOnFailureListener { message = "Could not scan. Type the cloth name or code." }
                    },
                    onAddToBill = { product ->
                        val index = cart.indexOfFirst { it.product.sku == product.sku }
                        if (index >= 0) cart[index] = cart[index].copy(quantity = cart[index].quantity + 1)
                        else cart.add(CartLine(product, 1))
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
                    photo = productImageBitmap,
                    onTakePhoto = { takeProductPhoto() },
                    onChoosePhoto = { productPhotoPicker.launch("image/*") },
                    onName = { productName = it },
                    onCategory = { productCategory = it },
                    onPrice = { sellingPrice = it },
                    onCost = { unitCost = it },
                    onStock = { openingStock = it },
                    onBack = { navigateBack() },
                    onSave = {
                        val token = idToken
                        if (!api.isConfigured || token == null) message = "Connect the Shop API and sign in before saving clothes."
                        else if (!savingCloth) operationScope.launch {
                            savingCloth = true
                            try {
                                runCatching {
                                    withContext(Dispatchers.IO) {
                                        api.createProduct(token, productName, productCategory, sellingPrice.toDouble(), unitCost.toDouble(), openingStock.toInt(), productImageBase64)
                                    }
                                }.onSuccess { created ->
                                    val newProduct = Product(created.sku, created.name, productCategory, created.price, created.stock, created.imageId)
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
                    paymentMode = paymentMode,
                    creditAmountReceived = creditAmountReceived,
                    creditPaymentMethod = creditPaymentMethod,
                    cart = cart,
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
                    onPaymentMode = { paymentMode = it },
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
                        scanner.startScan()
                            .addOnSuccessListener { barcode ->
                                val scannedCode = barcode.rawValue.orEmpty()
                                val token = idToken
                                val cached = products.firstOrNull { it.sku == scannedCode }
                                if (cached != null) {
                                    val index = cart.indexOfFirst { it.product.sku == cached.sku }
                                    if (index >= 0) cart[index] = cart[index].copy(quantity = cart[index].quantity + 1)
                                    else cart.add(CartLine(cached, 1))
                                    query = scannedCode
                                } else if (token != null && api.isConfigured) {
                                    operationScope.launch {
                                        runCatching { withContext(Dispatchers.IO) { api.products(token, scannedCode).firstOrNull { it.sku == scannedCode } } }
                                            .onSuccess { product ->
                                                if (product == null) message = "Cloth not found in Shop. Check the QR label."
                                                else {
                                                    val merged = ProductCache.merge(products.toList(), listOf(product))
                                                    products.clear()
                                                    products.addAll(merged)
                                                    hasLiveProducts = true
                                                    ProductCache.save(context, merged)
                                                    val index = cart.indexOfFirst { it.product.sku == product.sku }
                                                    if (index >= 0) cart[index] = cart[index].copy(quantity = cart[index].quantity + 1)
                                                    else cart.add(CartLine(product, 1))
                                                }
                                            }
                                            .onFailure { message = it.message ?: "Could not look up this cloth." }
                                    }
                                } else {
                                    val product = previewProducts.firstOrNull { it.sku == scannedCode }
                                    if (product != null) cart.add(CartLine(product, 1))
                                    else message = "Cloth not found in preview catalog. Search by name or code."
                                }
                            }
                            .addOnFailureListener { message = "Could not scan. You can search by cloth name." }
                    },
                    onFindProduct = {
                        orderMode = screen == Screen.PlaceOrder
                        selectedTab = 2
                        navigateTo(Screen.Search)
                    },
                    onQuantity = { sku, change ->
                        val index = cart.indexOfFirst { it.product.sku == sku }
                        if (index >= 0) {
                            val newQuantity = cart[index].quantity + change
                            if (newQuantity <= 0) cart.removeAt(index)
                            else cart[index] = cart[index].copy(quantity = newQuantity)
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
                                runCatching {
                                    withContext(Dispatchers.IO) {
                                        val total = invoiceItems.sumOf { it.product.price * it.quantity }
                                        val amountReceived = if (invoicePayment == "Credit") invoiceCreditAmount else total
                                        api.createInvoice(token, invoiceCustomer.name, invoiceCustomer.phone, invoiceCustomer.email, invoiceAddress, invoicePayment, invoiceItems, amountReceived, invoicePaymentMethod)
                                    }
                                }.onSuccess { created ->
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
                                        due = created.due
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
                    onPhone = { billsPhone = it },
                    onBack = { navigateBack() },
                    onSync = {
                        val token = idToken
                        if (!api.isConfigured || token == null) message = "Sign in to Shop before syncing bills. Your saved bills are still available on this phone."
                        else operationScope.launch {
                            billsLoading = true
                            runCatching { withContext(Dispatchers.IO) { api.bills(token, "") } }
                                .onSuccess { result ->
                                    val merged = BillCache.merge(BillCache.load(context), result)
                                    BillCache.save(context, merged)
                                    bills.clear(); bills.addAll(merged)
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
                    onQuery = { paymentDirectoryQuery = it },
                    onListTab = { paymentListTab = it },
                    onSync = {
                        val token = idToken
                        if (!api.isConfigured || token == null) message = "Sign in to Shop before syncing balances. Saved balances are still available on this phone."
                        else if (!billsLoading) operationScope.launch {
                            billsLoading = true
                            runCatching { withContext(Dispatchers.IO) { api.bills(token, "") } }
                                .onSuccess { fresh ->
                                    val merged = BillCache.merge(BillCache.load(context), fresh)
                                    bills.clear(); bills.addAll(merged)
                                    BillCache.save(context, merged)
                                }
                                .onFailure { message = it.message ?: "Could not sync outstanding balances." }
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
                    padding = insets, products = products, live = api.isConfigured && idToken != null,
                    uploadingPhotoSku = uploadingProductPhotoSku,
                    onTakePhoto = { product ->
                        if (!api.isConfigured || idToken == null) message = "Sign in to Shop before saving a product picture."
                        else {
                            productPhotoTargetSku = product.sku
                            takeProductPhoto()
                        }
                    },
                    onChoosePhoto = { product ->
                        if (!api.isConfigured || idToken == null) message = "Sign in to Shop before saving a product picture."
                        else {
                            productPhotoTargetSku = product.sku
                            productPhotoPicker.launch("image/*")
                        }
                    },
                    imageLoader = { id ->
                        val token = idToken ?: return@CatalogScreen null
                        runCatching { withContext(Dispatchers.IO) { api.productImage(token, id) } }.getOrNull()
                    },
                    onProductQr = { qrPreviewProduct = it },
                    onBack = { navigateBack() }
                )
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
                    title = { Text(tr("Sales App", "सेल्स ऐप")) },
                    text = { Text(text, fontSize = 17.sp) },
                    confirmButton = { TextButton(onClick = { message = null }) { Text(tr("OK", "ठीक है")) } }
                )
            }
        }
        createdQrSku?.let { sku ->
            val qrBitmap = remember(sku) { createQrBitmap(sku).asImageBitmap() }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = {
                    createdQrSku = null
                    productName = ""; productCategory = ""; sellingPrice = ""; unitCost = ""; openingStock = ""
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
                            productName = ""; productCategory = ""; sellingPrice = ""; unitCost = ""; openingStock = ""
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

@Composable
private fun HomeScreen(
    padding: PaddingValues,
    apiConfigured: Boolean,
    isSignedIn: Boolean,
    signedInEmail: String?,
    isHindi: Boolean,
    onLanguageChange: (Boolean) -> Unit,
    onSignIn: () -> Unit,
    onNewBill: () -> Unit,
    onMakePayment: () -> Unit,
    onCatalog: () -> Unit
) {
    var userMenuExpanded by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 22.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 24.dp),
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
                Text(if (isHindi) "सेल्स ऐप" else "SALES APP", color = Teal, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(if (isHindi) "आज क्या करना\nहै?" else "What do you need\nto do?", fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = if (isSignedIn) SoftGreen else Color(0xFFFFF3D7)) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(
                        when {
                            !apiConfigured -> if (isHindi) "पूर्वावलोकन मोड · Shop कनेक्शन सेट नहीं है" else "Preview mode · Shop connection not set up"
                            isSignedIn -> if (isHindi) "Shop से जुड़ा हुआ" else "Connected to Shop"
                            else -> if (isHindi) "Shop कनेक्शन तैयार · साइन इन के लिए ऊपर प्रोफ़ाइल आइकन दबाएँ" else "Shop connection ready · Tap the profile icon above to sign in"
                        },
                        fontSize = 14.sp,
                        color = if (isSignedIn) Ink else Color(0xFF654A13)
                    )
                }
            }
        }
        item {
            ActionCard(
                title = if (isHindi) "बिल बनाएँ" else "Create a bill",
                subtitle = if (isHindi) "ग्राहक चुनें और सामान जोड़ें" else "Choose a customer and add items",
                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                fill = Ink,
                titleColor = Color.White,
                detailColor = Color(0xFFD5E5EA),
                onClick = onNewBill
            )
        }
        item {
            ActionCard(
                title = if (isHindi) "भुगतान लें" else "Make a payment",
                subtitle = if (isHindi) "ग्राहक का बकाया देखें और भुगतान दर्ज करें" else "See what a customer owes and record a payment",
                icon = Icons.Rounded.AccountBalanceWallet,
                fill = Color(0xFFE7F2F7),
                titleColor = Ink,
                detailColor = Muted,
                onClick = onMakePayment
            )
        }
        item {
            ActionCard(
                title = if (isHindi) "कैटलॉग" else "Catalogue",
                subtitle = if (isHindi) "ग्राहक को कपड़ों की तस्वीरें और कीमतें दिखाएँ" else "Show cloth photos and prices to a customer",
                icon = Icons.Rounded.Storefront,
                fill = Color.White,
                titleColor = Ink,
                detailColor = Muted,
                onClick = onCatalog,
                outline = true
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
    outline: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(112.dp),
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
                Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = titleColor)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 15.sp, color = detailColor, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack, modifier = Modifier.size(52.dp)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Go back", tint = Ink) }
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
    }
}

@Composable
private fun BillCenterScreen(padding: PaddingValues, onCreateBill: () -> Unit, onFindBill: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(18.dp)); Text(tr("Bills", "बिल"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
        ActionCard(tr("Create a bill", "बिल बनाएँ"), tr("Add customer and clothes", "ग्राहक और कपड़े जोड़ें"), Icons.AutoMirrored.Rounded.ReceiptLong, Ink, Color.White, Color(0xFFD5E5EA), onCreateBill)
        ActionCard(tr("Search bills", "बिल खोजें"), tr("Search by bill number, customer name, or phone", "बिल नंबर, ग्राहक के नाम या फ़ोन से खोजें"), Icons.Rounded.Search, Color.White, Ink, Muted, onFindBill, outline = true)
    }
}

@Composable
private fun ProductCenterScreen(padding: PaddingValues, onAddProduct: () -> Unit, onSearch: () -> Unit, onCatalog: () -> Unit, onPlaceOrder: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(18.dp)); Text(tr("Cloth", "कपड़ा"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
        ActionCard(tr("Add new cloth", "नया कपड़ा जोड़ें"), tr("Save cloth details and create its QR", "कपड़े की जानकारी सहेजें और QR बनाएँ"), Icons.Rounded.AddShoppingCart, Ink, Color.White, Color(0xFFD5E5EA), onAddProduct)
        ActionCard(tr("Search / scan clothes", "कपड़े खोजें / स्कैन करें"), tr("Type a cloth code or scan its QR", "कपड़े का कोड लिखें या QR स्कैन करें"), Icons.Rounded.QrCode2, Color.White, Ink, Muted, onSearch, outline = true)
        ActionCard(tr("Catalog", "कैटलॉग"), tr("Browse cloth photos and prices with a customer", "ग्राहक को कपड़ों की तस्वीरें और कीमतें दिखाएँ"), Icons.Rounded.Storefront, Color.White, Ink, Muted, onCatalog, outline = true)
        ActionCard(tr("Place an order", "ऑर्डर भेजें"), tr("Send a customer request to the manager", "ग्राहक का अनुरोध मैनेजर को भेजें"), Icons.AutoMirrored.Rounded.ReceiptLong, Color.White, Ink, Muted, onPlaceOrder, outline = true)
    }
}

@Composable
private fun CatalogScreen(
    padding: PaddingValues,
    products: List<Product>,
    live: Boolean,
    uploadingPhotoSku: String?,
    onTakePhoto: (Product) -> Unit,
    onChoosePhoto: (Product) -> Unit,
    imageLoader: suspend (String) -> ByteArray?,
    onProductQr: (Product) -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(12.dp)); ScreenHeader(tr("Cloth catalog", "कपड़ों का कैटलॉग"), onBack)
        Text(tr("Show these clothes to your customer.", "ये कपड़े ग्राहक को दिखाएँ।"), color = Muted, fontSize = 15.sp, modifier = Modifier.padding(start = 8.dp, bottom = 10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
            items(products, key = { it.sku }) { product ->
                val qrBitmap = remember(product.sku) { createQrBitmap(product.sku).asImageBitmap() }
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE1E8E7))) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        ProductPhoto(
                            sku = product.sku,
                            imageId = product.imageId,
                            imageLoader = imageLoader,
                            modifier = Modifier.fillMaxWidth().height(190.dp),
                            onTakePhoto = { onTakePhoto(product) },
                            onChoosePhoto = { onChoosePhoto(product) },
                            uploading = uploadingPhotoSku == product.sku
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(product.name, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                        if (product.category.isNotBlank()) Text(product.category, fontSize = 14.sp, color = Muted)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("₹${product.price}", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Teal, modifier = Modifier.weight(1f))
                            Text(if (product.stock <= 0) "Unavailable" else "Available", color = if (product.stock <= 0) Muted else Teal, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                qrBitmap,
                                contentDescription = "QR code for ${product.name}",
                                modifier = Modifier.size(92.dp).clickable { onProductQr(product) }
                            )
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(tr("Cloth QR", "कपड़े का QR"), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text(tr("Scan to identify this cloth", "कपड़े की पहचान के लिए स्कैन करें"), color = Muted, fontSize = 14.sp)
                                Text(product.sku, color = Teal, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
            if (products.isEmpty()) item { Text(if (live) "No clothes found." else "Connect to Shop to load the catalog.", color = Muted, fontSize = 17.sp) }
        }
    }
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
    live: Boolean,
    onQuery: (String) -> Unit,
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
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(14.dp))
        Text(tr("Customers", "ग्राहक"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text(tr("Create new customer", "नया ग्राहक जोड़ें"), fontSize = 17.sp)
        }
        Text(tr("Search by phone number or name.", "फ़ोन नंबर या नाम से खोजें।"), color = Muted, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 12.dp))
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
        Spacer(Modifier.height(10.dp))
        Text(
            when {
                searching -> tr("Checking the Shop sheet…", "Shop शीट में खोज रहे हैं…")
                live -> tr("Saved customers are searched on this phone first.", "पहले इस फ़ोन में सहेजे ग्राहक खोजे जाते हैं।")
                else -> tr("Connect to Shop to sync customer details.", "ग्राहक जानकारी सिंक करने के लिए Shop से जुड़ें।")
            },
            color = Muted,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(results, key = { it.id.ifBlank { it.phone } }) { customer ->
                Card(onClick = { onCustomer(customer) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE1E8E7))) {
                    Column(Modifier.fillMaxWidth().padding(15.dp)) {
                        Text(customer.name, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text(customer.phone, fontSize = 16.sp, color = Teal)
                        if (customer.email.isNotBlank()) Text(customer.email, fontSize = 14.sp, color = Muted)
                    }
                }
            }
            if (results.isEmpty() && !searching) {
                item {
                    Text(
                        if (query.isBlank()) tr("No saved customers yet.", "अभी कोई ग्राहक सहेजा नहीं गया।") else tr("No matching customer found.", "कोई मिलता-जुलता ग्राहक नहीं मिला।"),
                        color = Muted,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCustomerScreen(
    padding: PaddingValues, name: String, phone: String, email: String, address: String,
    saving: Boolean, connected: Boolean, onName: (String) -> Unit, onPhone: (String) -> Unit,
    onEmail: (String) -> Unit, onAddress: (String) -> Unit, onBack: () -> Unit, onSave: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
    loading: Boolean, ordersLoading: Boolean, onBack: () -> Unit, onOpenPdf: (String) -> Unit,
    onShareBill: (ShopInvoice) -> Unit, sharingBillNumber: String?,
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
    val history = buildList {
        sales.forEach { add(CustomerHistoryItem("sale:${it.number}", it.date, invoice = it)) }
        orders.forEach { add(CustomerHistoryItem("request:${it.number}", it.date, orderRequest = it)) }
    }.sortedWith(compareByDescending<CustomerHistoryItem> { it.date }.thenByDescending { it.key })
    val dueBills = sales.filter { it.due > 0 }.sortedByDescending { it.date }
    val recentPayments = finance?.payments.orEmpty()

    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            ScreenHeader(customer.name, onBack)
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
            else items(history, key = { it.key }) { entry ->
            val bill = entry.invoice
            val order = entry.orderRequest
            val payment = bill?.paymentMode.orEmpty()
            val isCredit = bill != null && bill.due > 0
            val tagColor = when {
                order != null -> Color(0xFF2E7D32)
                isCredit -> Color(0xFFC62828)
                else -> Teal
            }
            Card(
                onClick = { bill?.pdfUrl?.takeIf(String::isNotBlank)?.let(onOpenPdf) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = tagColor.copy(alpha = 0.07f)),
                border = BorderStroke(1.dp, tagColor.copy(alpha = 0.35f))
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when {
                                order != null -> tr("Order request", "ऑर्डर अनुरोध")
                                isCredit -> tr("DUE", "बकाया")
                                else -> tr("PAID · $payment", "चुका · $payment")
                            },
                            modifier = Modifier.weight(1f), color = tagColor, fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (bill != null) "₹${bill.total}" else order?.status.orEmpty(),
                            color = tagColor, fontSize = 17.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    if (bill != null) {
                        Text(bill.number, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(bill.date, color = Muted, fontSize = 14.sp)
                        Text(
                            tr("Paid ₹${bill.paid} · Due ₹${bill.due}", "मिला ₹${bill.paid} · बकाया ₹${bill.due}"),
                            color = if (bill.due > 0) Color(0xFFC62828) else Teal,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        WhatsAppShareIconButton(
                            onClick = { onShareBill(bill) },
                            enabled = sharingBillNumber == null,
                            sharing = sharingBillNumber == bill.number
                        )
                    } else if (order != null) {
                        Text(order.productName, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("${order.number} · ${order.date} · ${tr("Qty", "मात्रा")} ${order.quantity}", color = Muted, fontSize = 14.sp)
                        if (order.notes.isNotBlank()) Text(order.notes, color = Muted, fontSize = 14.sp)
                    }
                }
            }
            }
            if (history.isNotEmpty() && (loading || ordersLoading)) item { CircularProgressIndicator(modifier = Modifier.padding(12.dp)) }
        } else {
            item { SectionTitle("", tr("Bills to settle", "भुगतान बाकी बिल")) }
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
            else items(dueBills, key = { "due:${it.number}" }) { bill ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3F1)), border = BorderStroke(1.dp, Color(0xFFC62828).copy(alpha = 0.28f))) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            item { SectionTitle("", tr("Payment history", "भुगतान इतिहास")) }
            if (recentPayments.isEmpty()) item {
                Text(
                    if (financeLoading) tr("Loading payment history…", "भुगतान इतिहास लोड हो रहा है…") else tr("No payments have been recorded yet.", "अभी तक कोई भुगतान दर्ज नहीं है।"),
                    color = Muted, modifier = Modifier.padding(8.dp)
                )
            } else items(recentPayments, key = { "payment:${it.id}" }) { payment ->
                val paymentColor = if (payment.syncStatus == "SYNCED") Teal else Color(0xFF9A6200)
                Surface(shape = RoundedCornerShape(12.dp), color = paymentColor.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("₹${payment.amount} · ${payment.method}", color = paymentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            if (payment.syncStatus == "SYNCED") "${payment.receiptNumber} · ${payment.date}"
                            else tr("Local receipt · ${payment.date}", "स्थानीय रसीद · ${payment.date}"),
                            color = Muted, fontSize = 13.sp
                        )
                        if (payment.invoiceNumber.isNotBlank()) Text(tr("Applied to ${payment.invoiceNumber}", "इन बिलों में जोड़ा: ${payment.invoiceNumber}"), color = Muted, fontSize = 13.sp)
                        if (payment.note.isNotBlank()) Text(payment.note, color = Muted, fontSize = 13.sp)
                        if (payment.syncStatus != "SYNCED") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (payment.syncStatus == "SYNCING") tr("Syncing to Shop…", "Shop में सिंक हो रहा है…") else tr("Waiting to sync · not yet confirmed", "सिंक बाकी · अभी पुष्टि नहीं हुई"),
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
    onQuery: (String) -> Unit,
    onShareQrLabels: (List<Product>) -> Unit,
    onProductQr: (Product) -> Unit,
    onScan: () -> Unit,
    onAddToBill: (Product) -> Unit
) {
    val results = if (query.isBlank()) products else products.filter {
            it.name.contains(query, ignoreCase = true) || it.sku.contains(query, ignoreCase = true)
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(22.dp))
        Text(tr("Find a cloth", "कपड़ा खोजें"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 19.sp),
            placeholder = { Text(tr("Type cloth name or code", "कपड़े का नाम या कोड लिखें"), fontSize = 17.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onScan, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Rounded.QrCode2, null); Spacer(Modifier.width(10.dp)); Text(tr("Scan cloth QR", "कपड़े का QR स्कैन करें"), fontSize = 18.sp)
        }
        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(12.dp), color = SoftBlue) {
            Text(
                when {
                    !live -> tr("Sample clothes · live stock appears after connecting Shop", "नमूना कपड़े · Shop से जुड़ने पर लाइव स्टॉक दिखेगा")
                    refreshing -> tr("Showing saved clothes · checking latest price and stock…", "सहेजे कपड़े दिख रहे हैं · नई कीमत और स्टॉक जाँच रहे हैं…")
                    else -> tr("Prices and availability synced from Shop", "कीमत और उपलब्धता Shop से सिंक है")
                },
                Modifier.fillMaxWidth().padding(12.dp), fontSize = 14.sp, color = Ink
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { onShareQrLabels(products) },
            enabled = products.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Rounded.Print, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(tr("Create & share QR PDF (${products.size})", "QR PDF बनाएँ और साझा करें (${products.size})"), fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
            items(results) { product -> ProductCard(product, live, orderMode, onAdd = { onAddToBill(product) }, onQr = { onProductQr(product) }) }
            if (results.isEmpty()) item { Text(tr("No cloth found. Try another name or code.", "कपड़ा नहीं मिला। दूसरा नाम या कोड आज़माएँ।"), color = Muted, fontSize = 17.sp, modifier = Modifier.padding(8.dp)) }
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
                    Text(if (product.stock <= 0) tr("Out of stock", "स्टॉक में नहीं") else if (live) tr("${product.stock} in stock", "${product.stock} स्टॉक में") else "${product.stock} in stock · preview", fontSize = 14.sp, color = Muted)
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
    photo: Bitmap?,
    onTakePhoto: () -> Unit,
    onChoosePhoto: () -> Unit,
    onName: (String) -> Unit,
    onCategory: (String) -> Unit,
    onPrice: (String) -> Unit,
    onCost: (String) -> Unit,
    onStock: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader(tr("Add a cloth", "कपड़ा जोड़ें"), onBack); Text(tr("Enter the details. Add a picture if you have one.", "जानकारी भरें। चाहें तो तस्वीर भी जोड़ें।"), color = Muted, fontSize = 15.sp) }
        item {
            ProductPhoto(
                sku = "new-product-preview",
                imageId = "",
                imageLoader = { null },
                modifier = Modifier.fillMaxWidth().height(190.dp),
                selectedPhoto = photo,
                onTakePhoto = onTakePhoto,
                onChoosePhoto = onChoosePhoto
            )
        }
        item { OutlinedTextField(name, onName, Modifier.fillMaxWidth(), label = { Text(tr("Cloth name", "कपड़े का नाम")) }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp), shape = RoundedCornerShape(14.dp)) }
        item { OutlinedTextField(category, onCategory, Modifier.fillMaxWidth(), label = { Text(tr("Category (optional)", "श्रेणी (वैकल्पिक)")) }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp), shape = RoundedCornerShape(14.dp)) }
        item { OutlinedTextField(price, onPrice, Modifier.fillMaxWidth(), label = { Text(tr("Selling price (₹)", "बेचने की कीमत (₹)")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp), shape = RoundedCornerShape(14.dp)) }
        item { OutlinedTextField(cost, onCost, Modifier.fillMaxWidth(), label = { Text(tr("Purchase cost per item (₹)", "एक पीस की खरीद कीमत (₹)")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp), shape = RoundedCornerShape(14.dp)) }
        item { OutlinedTextField(stock, onStock, Modifier.fillMaxWidth(), label = { Text(tr("Starting quantity", "शुरुआती मात्रा")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp), shape = RoundedCornerShape(14.dp)) }
        item {
            Button(onClick = onSave, enabled = connected && !saving && name.isNotBlank() && price.isNotBlank() && cost.isNotBlank() && stock.isNotBlank(), modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(16.dp)) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                else Icon(Icons.Rounded.CheckCircle, null)
                Spacer(Modifier.width(10.dp)); Text(if (saving) tr("Saving cloth…", "कपड़ा सहेज रहे हैं…") else tr("Save cloth", "कपड़ा सहेजें"), fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            Text(if (connected) tr("Saving adds this cloth to Shop and shows its QR code.", "सहेजने पर कपड़ा Shop में जुड़ जाएगा और QR दिखेगा।") else tr("Connect to Shop and sign in before saving.", "सहेजने से पहले Shop से जुड़कर साइन इन करें।"), fontSize = 14.sp, color = Muted)
        }
    }
}

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
    paymentMode: String,
    creditAmountReceived: String,
    creditPaymentMethod: String,
    cart: List<CartLine>,
    onName: (String) -> Unit,
    onPhone: (String) -> Unit,
    onCustomerEmail: (String) -> Unit,
    onCustomerAddress: (String) -> Unit,
    onPaymentMode: (String) -> Unit,
    onCreditAmountReceived: (String) -> Unit,
    onCreditPaymentMethod: (String) -> Unit,
    onSelectCustomer: (ShopCustomer) -> Unit,
    onPickContact: () -> Unit,
    onScan: () -> Unit,
    onFindProduct: () -> Unit,
    onQuantity: (String, Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val total = cart.sumOf { it.product.price * it.quantity }
    val paidNow = if (paymentMode == "Credit") creditAmountReceived.toIntOrNull() else total
    val validCreditAmount = paymentMode != "Credit" || (paidNow != null && paidNow in 0..total)
    val phoneFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        delay(250)
        phoneFocus.requestFocus()
        keyboard?.show()
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
            OutlinedTextField(
                value = customerPhone,
                onValueChange = onPhone,
                modifier = Modifier.fillMaxWidth().focusRequester(phoneFocus),
                label = { Text(tr("Phone number", "फ़ोन नंबर")) },
                placeholder = { Text(tr("Type number", "नंबर लिखें")) },
                singleLine = true,
                readOnly = customerExists == true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
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
            when {
                customerSearching -> Text(tr("Checking saved customers…", "सहेजे ग्राहक खोज रहे हैं…"), color = Muted, fontSize = 14.sp)
                customerExists == true -> Text(tr("Saved customer found", "सहेजा हुआ ग्राहक मिल गया"), color = Teal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                customerExists == false -> Text(tr("New customer — enter their name below", "नया ग्राहक — नीचे नाम लिखें"), color = Muted, fontSize = 14.sp)
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
            if (customerExists == true) {
                Surface(shape = RoundedCornerShape(14.dp), color = SoftGreen, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(tr("Saved customer", "सहेजा हुआ ग्राहक"), color = Muted, fontSize = 14.sp)
                        Text(customerName, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (customerExists == false) {
                Spacer(Modifier.height(4.dp))
                Text(tr("New customer details", "नए ग्राहक की जानकारी"), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(customerName, onName, Modifier.fillMaxWidth(), label = { Text(tr("Customer name", "ग्राहक का नाम")) }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp), shape = RoundedCornerShape(14.dp))
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(customerEmail, onCustomerEmail, Modifier.fillMaxWidth(), label = { Text(tr("Email (optional)", "ईमेल (वैकल्पिक)")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp), shape = RoundedCornerShape(14.dp))
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(customerAddress, onCustomerAddress, Modifier.fillMaxWidth(), label = { Text(tr("City or address (optional)", "शहर या पता (वैकल्पिक)")) }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp), shape = RoundedCornerShape(14.dp))
            }
        }
        item {
            Divider(color = Color(0xFFDDE5E4))
            Spacer(Modifier.height(4.dp))
            SectionTitle("2", tr("Add items", "सामान जोड़ें"))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onScan, modifier = Modifier.weight(1f).height(58.dp), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Rounded.QrCode2, null); Spacer(Modifier.width(7.dp)); Text(tr("Scan QR", "QR स्कैन करें"), fontSize = 16.sp)
                }
                OutlinedButton(onClick = onFindProduct, modifier = Modifier.weight(1f).height(58.dp), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Rounded.Search, null); Spacer(Modifier.width(7.dp)); Text(tr("Search", "खोजें"), fontSize = 16.sp)
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
            items(cart, key = { it.product.sku }) { line ->
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(line.product.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink)
                            Text("₹${line.product.price} each", color = Muted, fontSize = 14.sp)
                        }
                        IconButton(onClick = { onQuantity(line.product.sku, -1) }) { Icon(Icons.Rounded.Remove, "Remove one") }
                        Text(line.quantity.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onQuantity(line.product.sku, 1) }) { Icon(Icons.Rounded.Add, "Add one") }
                    }
                }
            }
        }
        if (!orderMode) item {
            Divider(color = Color(0xFFDDE5E4))
            Spacer(Modifier.height(4.dp))
            SectionTitle("3", tr("Payment", "भुगतान"))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Cash", "UPI", "Card", "Credit").forEach { option ->
                    if (paymentMode == option) {
                        Button(onClick = { onPaymentMode(option) }, modifier = Modifier.weight(1f).height(54.dp), contentPadding = PaddingValues(2.dp), shape = RoundedCornerShape(14.dp)) {
                            Text(tr(option, if (option == "Credit") "उधार" else option), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(onClick = { onPaymentMode(option) }, modifier = Modifier.weight(1f).height(54.dp), contentPadding = PaddingValues(2.dp), shape = RoundedCornerShape(14.dp)) {
                            Text(tr(option, if (option == "Credit") "उधार" else option), fontSize = 14.sp)
                        }
                    }
                }
            }
            if (paymentMode == "Credit") {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = creditAmountReceived,
                    onValueChange = onCreditAmountReceived,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Paid now (₹)", "अभी मिली राशि (₹)")) },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                    shape = RoundedCornerShape(14.dp)
                )
                Text(tr("Received by", "राशि मिली via"), color = Muted, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Cash", "UPI", "Card").forEach { method ->
                        if (creditPaymentMethod == method) Button(onClick = { onCreditPaymentMethod(method) }, modifier = Modifier.weight(1f).height(46.dp), contentPadding = PaddingValues(2.dp)) { Text(method, fontSize = 14.sp) }
                        else OutlinedButton(onClick = { onCreditPaymentMethod(method) }, modifier = Modifier.weight(1f).height(46.dp), contentPadding = PaddingValues(2.dp)) { Text(method, fontSize = 14.sp) }
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
            Surface(shape = RoundedCornerShape(18.dp), color = SoftGreen) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (orderMode) tr("Estimated total", "अनुमानित कुल") else tr("Bill total", "बिल की कुल राशि"), Modifier.weight(1f), fontSize = 18.sp, color = Ink)
                    Text("₹$total", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Teal)
                }
            }
            if (orderMode) Text(
                tr("This is an order request only. It will not create a bill or change stock.", "यह केवल ऑर्डर अनुरोध है। इससे बिल नहीं बनेगा और स्टॉक नहीं बदलेगा।"),
                color = Muted, fontSize = 14.sp, lineHeight = 19.sp
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onSave,
                enabled = !saving && customerPhone.isNotBlank() && customerExists != null && (customerExists || customerName.isNotBlank()) && cart.isNotEmpty() && (orderMode || validCreditAmount),
                modifier = Modifier.fillMaxWidth().height(62.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                else Icon(Icons.AutoMirrored.Rounded.ReceiptLong, null)
                Spacer(Modifier.width(10.dp)); Text(
                    when {
                        saving && orderMode -> tr("Sending order…", "ऑर्डर भेज रहे हैं…")
                        saving -> tr("Creating bill…", "बिल बना रहे हैं…")
                        orderMode -> tr("Send order request", "ऑर्डर अनुरोध भेजें")
                        else -> tr("Create bill and PDF", "बिल और PDF बनाएँ")
                    },
                    fontSize = 19.sp, fontWeight = FontWeight.Bold
                )
            }
            Text(
                if (orderMode) tr("The manager will see this request in Order Requests.", "मैनेजर यह अनुरोध Order Requests में देखेंगे।")
                else if (connected) tr("This creates the invoice in Shop and saves a PDF in Drive.", "इससे Shop में बिल बनेगा और Drive में PDF सहेजी जाएगी।")
                else tr("Preview only — connect to Shop and sign in before saving.", "यह केवल पूर्वावलोकन है — सहेजने के लिए Shop से जुड़कर साइन इन करें।"),
                Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = Muted
            )
        }
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
    onQuery: (String) -> Unit,
    onListTab: (String) -> Unit,
    onSync: () -> Unit,
    onCustomer: (ShopCustomer) -> Unit,
    onRetryPayment: (PaymentReceipt) -> Unit
) {
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
    val filteredPayments = paymentsMade.filter { payment ->
        query.isBlank() || payment.customerName.contains(query.trim(), ignoreCase = true) ||
            CustomerCache.normalizePhone(payment.customerPhone).contains(CustomerCache.normalizePhone(query)) ||
            payment.invoiceNumber.contains(query.trim(), ignoreCase = true)
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(tr("Payments", "भुगतान"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(
                if (connected) tr("Settle customer balances and record receipts.", "ग्राहक का बकाया चुकता करें और भुगतान दर्ज करें।")
                else tr("Showing saved balances from this phone. Sign in to sync.", "इस फ़ोन में सहेजे बकाया दिख रहे हैं। सिंक करने के लिए साइन इन करें।"),
                color = Muted, fontSize = 15.sp
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (listTab == "MADE") Button(onClick = { onListTab("MADE") }, modifier = Modifier.weight(1f).height(50.dp), contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text(tr("Payments made", "मिले भुगतान"), fontSize = 14.sp)
                } else OutlinedButton(onClick = { onListTab("MADE") }, modifier = Modifier.weight(1f).height(50.dp), contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text(tr("Payments made", "मिले भुगतान"), fontSize = 14.sp)
                }
                if (listTab == "DUE") Button(onClick = { onListTab("DUE") }, modifier = Modifier.weight(1f).height(50.dp), contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text(tr("Settle due payment", "बकाया लें"), fontSize = 14.sp)
                } else OutlinedButton(onClick = { onListTab("DUE") }, modifier = Modifier.weight(1f).height(50.dp), contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text(tr("Settle due payment", "बकाया लें"), fontSize = 14.sp)
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
                        Text("${paymentsMade.size}", color = Teal, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(tr("₹${paymentsMade.sumOf { it.amount }} received", "₹${paymentsMade.sumOf { it.amount }} मिले"), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
        if (listTab == "DUE") item {
            Button(onClick = onSync, enabled = !loading && connected, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                else Icon(Icons.Rounded.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text(if (loading) tr("Syncing balances…", "बकाया सिंक हो रहा है…") else tr("Sync balances", "बकाया सिंक करें"), fontSize = 17.sp)
            }
        }
        if (listTab == "MADE" && filteredPayments.isEmpty()) item {
            Text(
                if (loading) tr("Loading payments…", "भुगतान लोड हो रहा है…")
                else if (paymentsMade.isEmpty()) tr("No payments recorded yet.", "अभी तक कोई भुगतान दर्ज नहीं है।")
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
        if (listTab == "MADE") items(filteredPayments, key = { "payment:${it.requestId.ifBlank { it.receiptNumber }}" }) { payment ->
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFDCE7E7))) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(payment.customerName.ifBlank { payment.customerPhone }, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("${payment.date}  ·  ${payment.method}", color = Muted, fontSize = 13.sp)
                        if (payment.invoiceNumber.isNotBlank()) Text(tr("Bill ${payment.invoiceNumber}", "बिल ${payment.invoiceNumber}"), color = Muted, fontSize = 13.sp)
                        Text(
                            when (payment.syncStatus) {
                                "SYNCED" -> tr("Saved in Shop", "Shop में सहेजा गया")
                                "SYNCING" -> tr("Syncing in background…", "बैकग्राउंड में सिंक हो रहा है…")
                                else -> tr("Waiting to sync", "सिंक होना बाकी है")
                            },
                            color = when (payment.syncStatus) { "SYNCED" -> Color(0xFF2E7D32); "SYNCING" -> Teal; else -> Color(0xFFC62828) },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("₹${payment.amount}", color = Color(0xFF1565C0), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        if (payment.syncStatus != "SYNCED") TextButton(onClick = { onRetryPayment(payment) }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                            Text(tr("Sync now", "अभी सिंक करें"), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        if (listTab == "DUE") items(grouped, key = { CustomerCache.normalizePhone(it.first.phone) }) { (customer, due, count) ->
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
    val searchedBills = if (searchText.isBlank()) bills else bills.filter {
        it.number.contains(searchText, ignoreCase = true) ||
            it.customerName.contains(searchText, ignoreCase = true) ||
            (searchDigits.isNotBlank() && CustomerCache.normalizePhone(it.phone).contains(searchDigits))
    }
    val visibleBills = when (statusFilter) {
        "Paid" -> searchedBills.filter { it.due <= 0 }
        "Due" -> searchedBills.filter { it.due > 0 }
        else -> searchedBills
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = tr("Back", "वापस"), tint = Ink)
            }
            Text(tr("Bills", "बिल"), modifier = Modifier.weight(1f), fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Ink)
            OutlinedButton(onClick = onSync, enabled = !loading, shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(19.dp), strokeWidth = 2.dp, color = Teal)
                else Icon(Icons.Rounded.Refresh, null, tint = Teal)
                Spacer(Modifier.width(6.dp))
                Text(if (loading) tr("Syncing", "सिंक हो रहा है") else tr("Sync", "सिंक"), color = Teal, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
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
            listOf("All", "Paid", "Due").forEach { filter ->
                val selected = statusFilter == filter
                Surface(
                    modifier = Modifier.height(46.dp).clickable { statusFilter = filter },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) Teal else Color.Transparent,
                    border = if (selected) null else BorderStroke(1.dp, Color(0xFFB9C1CC))
                ) {
                    Box(Modifier.padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
                        Text(tr(filter, when (filter) { "Paid" -> "चुका हुआ"; "Due" -> "बाकी"; else -> "सभी" }), color = if (selected) Color.White else Ink, fontSize = 16.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                    }
                }
            }
        }
        Text(tr("Recent bills", "हाल के बिल"), modifier = Modifier.padding(top = 18.dp, bottom = 10.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
            items(visibleBills, key = { it.number }) { bill ->
                val paid = bill.due <= 0
                val stampColor = if (paid) Color(0xFF32845F) else Color(0xFFB76B2E)
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFD9DEE5))) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f).padding(end = 6.dp)) {
                                Text(bill.customerName.ifBlank { tr("Customer", "ग्राहक") }, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
                                Text(bill.date, modifier = Modifier.padding(top = 5.dp), fontSize = 15.sp, color = Muted)
                                Text(bill.number, modifier = Modifier.padding(top = 5.dp), fontSize = 14.sp, color = Muted, maxLines = 1)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${bill.total}", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Teal)
                                Box(Modifier.padding(top = 4.dp, end = 2.dp).rotate(-11f).border(2.dp, stampColor, RoundedCornerShape(7.dp)).padding(horizontal = 9.dp, vertical = 2.dp)) {
                                    Text(if (paid) tr("PAID", "चुका") else tr("DUE", "बाकी"), color = stampColor, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                }
                                if (!paid) Text(tr("₹${bill.due} due", "₹${bill.due} बाकी"), modifier = Modifier.padding(top = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFB76B2E))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Divider(color = Color(0xFFE0E5EB))
                        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { if (bill.pdfUrl.isNotBlank()) onOpenPdf(bill.pdfUrl) }, enabled = bill.pdfUrl.isNotBlank(), modifier = Modifier.weight(1f)) {
                                Icon(Icons.AutoMirrored.Rounded.ReceiptLong, null, tint = Teal, modifier = Modifier.size(21.dp))
                                Spacer(Modifier.width(7.dp))
                                Text(tr("View PDF", "PDF देखें"), color = Teal, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Divider(modifier = Modifier.height(28.dp).width(1.dp), color = Color(0xFFBCC5D0))
                            Row(Modifier.weight(1f).clickable(enabled = sharingBillNumber == null) { onShareBill(bill) }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                WhatsAppShareIcon(sharing = sharingBillNumber == bill.number)
                                Text(tr("WhatsApp", "WhatsApp"), color = Teal, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
