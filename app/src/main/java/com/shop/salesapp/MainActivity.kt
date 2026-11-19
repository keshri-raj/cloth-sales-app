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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
private enum class Screen { Home, BillCenter, Bill, Bills, Customer, CustomerDetail, AddCustomer, ProductCenter, Search, Catalog, AddProduct, PlaceOrder }

private val previewProducts = listOf(
    Product("SKU-001", "Wireless Bluetooth Earbuds", "Electronics", 1999, 37),
    Product("SKU-002", "Wireless Optical Mouse", "Peripherals", 799, 58),
    Product("SKU-004", "USB-C Fast Charging Cable", "Accessories", 349, 91)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SalesApp() }
    }
}

@Composable
private fun SalesApp() {
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
    var paymentMode by remember { mutableStateOf("Cash") }
    var productName by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("") }
    var productImageBase64 by remember { mutableStateOf("") }
    var productImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var sellingPrice by remember { mutableStateOf("") }
    var unitCost by remember { mutableStateOf("") }
    var openingStock by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var latestPdfUrl by remember { mutableStateOf("") }
    var createdQrSku by remember { mutableStateOf<String?>(null) }
    var savingInvoice by remember { mutableStateOf(false) }
    var billsPhone by remember { mutableStateOf("") }
    var billsLoading by remember { mutableStateOf(false) }
    var orderCustomerName by remember { mutableStateOf("") }
    var orderCustomerPhone by remember { mutableStateOf("") }
    var orderProductName by remember { mutableStateOf("") }
    var orderProductSku by remember { mutableStateOf("") }
    var orderQuantity by remember { mutableStateOf("1") }
    var orderNotes by remember { mutableStateOf("") }
    var sendingOrderRequest by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<ShopCustomer?>(null) }
    var detailBillsLoading by remember { mutableStateOf(false) }
    var detailBills by remember { mutableStateOf<List<ShopInvoice>>(emptyList()) }
    var detailOrders by remember { mutableStateOf<List<CustomerOrderRequest>>(emptyList()) }
    var detailOrdersLoading by remember { mutableStateOf(false) }
    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerPhone by remember { mutableStateOf("") }
    var newCustomerEmail by remember { mutableStateOf("") }
    var newCustomerAddress by remember { mutableStateOf("") }
    var savingCustomer by remember { mutableStateOf(false) }
    val cart = remember { mutableStateListOf<CartLine>() }
    val bills = remember { mutableStateListOf<ShopInvoice>() }
    val scope = rememberCoroutineScope()
    val api = remember { ShopApiClient(BuildConfig.SHOP_API_BASE_URL) }
    val products = remember(context) {
        mutableStateListOf<Product>().apply { addAll(savedProducts.ifEmpty { previewProducts }) }
    }
    val cachedCustomers = remember(context) {
        mutableStateListOf<ShopCustomer>().apply { addAll(CustomerCache.load(context)) }
    }
    val lastAccount = remember { GoogleSignIn.getLastSignedInAccount(context) }
    var idToken by remember { mutableStateOf(lastAccount?.idToken) }

    fun navigateTo(destination: Screen) {
        if (screen != destination) {
            backStack.add(screen)
            screen = destination
            selectedTab = when (destination) {
                Screen.BillCenter, Screen.Bill, Screen.Bills -> 0
                Screen.Customer, Screen.CustomerDetail, Screen.AddCustomer -> 1
                Screen.ProductCenter, Screen.Search, Screen.Catalog, Screen.AddProduct -> 2
                else -> -1
            }
        }
    }

    fun navigateBack() {
        screen = if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex) else Screen.Home
        selectedTab = when (screen) {
            Screen.BillCenter, Screen.Bill, Screen.Bills -> 0
            Screen.Customer, Screen.CustomerDetail, Screen.AddCustomer -> 1
            Screen.ProductCenter, Screen.Search, Screen.Catalog, Screen.AddProduct -> 2
            else -> -1
        }
    }

    BackHandler(enabled = screen != Screen.Home) { navigateBack() }

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
        if (customerPhone.filter(Char::isDigit).length < 8 || idToken == null || !api.isConfigured) return@LaunchedEffect
        val normalizedPhone = CustomerCache.normalizePhone(customerPhone)
        val localMatch = cachedCustomers.firstOrNull { CustomerCache.normalizePhone(it.phone) == normalizedPhone }
        if (localMatch != null) {
            customerExists = true
            customerName = localMatch.name
            customerEmail = localMatch.email
            customerAddress = ""
            return@LaunchedEffect
        }
        customerSearching = true
        delay(450)
        val token = idToken ?: return@LaunchedEffect
        runCatching { withContext(Dispatchers.IO) { api.findCustomer(token, customerPhone) } }
            .onSuccess { customer ->
                customerExists = customer != null
                if (customer != null) {
                    customerName = customer.name
                    customerEmail = customer.email
                    customerAddress = ""
                    val merged = CustomerCache.merge(cachedCustomers.toList(), listOf(customer))
                    cachedCustomers.clear()
                    cachedCustomers.addAll(merged)
                    CustomerCache.save(context, merged)
                } else {
                    customerName = ""
                    customerEmail = ""
                    customerAddress = ""
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
            return@LaunchedEffect
        }
        detailBillsLoading = true
        detailOrdersLoading = true
        val cached = withContext(Dispatchers.IO) { CustomerHistoryCache.load(context, customer.phone) }
        if (cached != null) {
            detailBills = cached.sales
            detailOrders = cached.orderRequests
            detailBillsLoading = false
            detailOrdersLoading = false
            return@LaunchedEffect
        }
        val token = idToken
        if (!api.isConfigured || token == null) {
            detailBills = emptyList()
            detailOrders = emptyList()
            detailBillsLoading = false
            detailOrdersLoading = false
            return@LaunchedEffect
        }
        val (salesResult, requestsResult) = coroutineScope {
            val sales = async(Dispatchers.IO) { runCatching { api.bills(token, customer.phone) } }
            val requests = async(Dispatchers.IO) { runCatching { api.orderRequests(token, customer.phone) } }
            sales.await() to requests.await()
        }
        salesResult.onSuccess { detailBills = it }
            .onFailure { message = it.message ?: "Could not load this customer's sales." }
        requestsResult.onSuccess { detailOrders = it }
            .onFailure { message = it.message ?: "Could not load this customer's order requests." }
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

    val productPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    ?: throw IllegalStateException("Could not open this picture.")
            }.onSuccess { original ->
                val scale = minOf(1f, 900f / maxOf(original.width, original.height))
                val resized = if (scale < 1f) Bitmap.createScaledBitmap(original, (original.width * scale).toInt().coerceAtLeast(1), (original.height * scale).toInt().coerceAtLeast(1), true) else original
                val output = java.io.ByteArrayOutputStream()
                var quality = 82
                do {
                    output.reset()
                    resized.compress(Bitmap.CompressFormat.JPEG, quality, output)
                    quality -= 8
                } while (output.size() > 110_000 && quality >= 42)
                productImageBase64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                productImageBitmap = resized
                if (resized !== original) original.recycle()
            }.onFailure { message = it.message ?: "Could not use this picture." }
        }
    }

    val scanner = remember { GmsBarcodeScanning.getClient(context) }

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
            bottomBar = {
        if (screen in setOf(Screen.Home, Screen.BillCenter, Screen.Bill, Screen.Bills, Screen.Customer, Screen.CustomerDetail, Screen.AddCustomer, Screen.ProductCenter, Screen.Search, Screen.Catalog, Screen.AddProduct)) {
                    NavigationBar(containerColor = Color.White) {
                        NavigationBarItem(
                            selected = screen == Screen.Home,
                            onClick = { backStack.clear(); selectedTab = -1; screen = Screen.Home },
                            icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                            label = { Text(if (isHindi) "होम" else "Home", fontSize = 15.sp) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { backStack.clear(); selectedTab = 0; screen = Screen.BillCenter },
                            icon = { Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null) },
                            label = { Text(if (isHindi) "बिल" else "Bill", fontSize = 15.sp) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { backStack.clear(); selectedTab = 1; screen = Screen.Customer },
                            icon = { Icon(Icons.Rounded.Contacts, contentDescription = null) },
                            label = { Text(if (isHindi) "ग्राहक" else "Customer", fontSize = 15.sp) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { backStack.clear(); selectedTab = 2; screen = Screen.ProductCenter },
                            icon = { Icon(Icons.Rounded.Inventory2, contentDescription = null) },
                            label = { Text(if (isHindi) "कपड़ा" else "Cloth", fontSize = 15.sp) }
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
                    onNewBill = { navigateTo(Screen.Bill) },
                    onPlaceOrder = {
                        orderCustomerName = ""; orderCustomerPhone = ""; orderProductName = ""; orderProductSku = ""; orderQuantity = "1"; orderNotes = ""
                        navigateTo(Screen.PlaceOrder)
                    },
                    onFind = { selectedTab = 2; navigateTo(Screen.Search) }
                )
                Screen.BillCenter -> BillCenterScreen(
                    padding = insets,
                    onCreateBill = { navigateTo(Screen.Bill) },
                    onFindBill = { billsPhone = ""; navigateTo(Screen.Bills) }
                )
                Screen.ProductCenter -> ProductCenterScreen(
                    padding = insets,
                    onAddProduct = {
                        productName = ""; productCategory = ""; sellingPrice = ""; unitCost = ""; openingStock = ""
                        productImageBase64 = ""; productImageBitmap = null
                        navigateTo(Screen.AddProduct)
                    },
                    onSearch = { navigateTo(Screen.Search) },
                    onCatalog = { navigateTo(Screen.Catalog) }
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
                    onCustomer = { selectedCustomer = it; detailBills = emptyList(); detailOrders = emptyList(); navigateTo(Screen.CustomerDetail) }
                )
                Screen.CustomerDetail -> selectedCustomer?.let { customer ->
                    CustomerDetailScreen(
                        padding = insets, customer = customer, sales = detailBills, orders = detailOrders,
                        loading = detailBillsLoading, ordersLoading = detailOrdersLoading,
                        onBack = { navigateBack() },
                        onOpenPdf = { url -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
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
                        else if (!savingCustomer) scope.launch {
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
                    live = api.isConfigured && idToken != null,
                    refreshing = productSyncing,
                    onQuery = { query = it },
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
                        if (backStack.lastOrNull() == Screen.Bill) backStack.removeAt(backStack.lastIndex)
                        else if (screen != Screen.Bill) backStack.add(screen)
                        selectedTab = 0
                        screen = Screen.Bill
                    }
                )
                Screen.AddProduct -> AddProductScreen(
                    padding = insets,
                    connected = api.isConfigured && idToken != null,
                    name = productName,
                    category = productCategory,
                    price = sellingPrice,
                    cost = unitCost,
                    stock = openingStock,
                    photo = productImageBitmap,
                    onPickPhoto = { productPhotoPicker.launch("image/*") },
                    onName = { productName = it },
                    onCategory = { productCategory = it },
                    onPrice = { sellingPrice = it },
                    onCost = { unitCost = it },
                    onStock = { openingStock = it },
                    onBack = { navigateBack() },
                    onSave = {
                        val token = idToken
                        if (!api.isConfigured || token == null) message = "Connect the Shop API and sign in before saving clothes."
                        else scope.launch {
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
                        }
                    }
                )
                Screen.Bill -> BillScreen(
                    padding = insets,
                    connected = api.isConfigured && idToken != null,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    customerEmail = customerEmail,
                    customerAddress = customerAddress,
                    customerExists = customerExists,
                    customerSearching = customerSearching,
                    saving = savingInvoice,
                    paymentMode = paymentMode,
                    cart = cart,
                    onName = { customerName = it },
                    onPhone = {
                        customerPhone = it
                        customerExists = null
                        customerName = ""
                        customerEmail = ""
                        customerAddress = ""
                    },
                    onCustomerEmail = { customerEmail = it },
                    onCustomerAddress = { customerAddress = it },
                    onPaymentMode = { paymentMode = it },
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
                                    scope.launch {
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
                    onFindProduct = { selectedTab = 2; navigateTo(Screen.Search) },
                    onQuantity = { sku, change ->
                        val index = cart.indexOfFirst { it.product.sku == sku }
                        if (index >= 0) {
                            val newQuantity = cart[index].quantity + change
                            if (newQuantity <= 0) cart.removeAt(index)
                            else cart[index] = cart[index].copy(quantity = newQuantity)
                        }
                    },
                    onSave = {
                        val token = idToken
                        if (!api.isConfigured || token == null) message = "Connect the Shop API and sign in before saving bills."
                        else if (!savingInvoice) {
                            val invoiceCustomer = ShopCustomer("", customerName, customerPhone, customerEmail)
                            val invoiceAddress = customerAddress
                            val invoicePayment = paymentMode
                            val invoiceItems = cart.toList()
                            val isNewCustomer = customerExists == false
                            scope.launch {
                                savingInvoice = true
                                runCatching {
                                    withContext(Dispatchers.IO) {
                                        api.createInvoice(token, invoiceCustomer.name, invoiceCustomer.phone, invoiceCustomer.email, invoiceAddress, invoicePayment, invoiceItems)
                                    }
                                }.onSuccess { created ->
                                    CustomerHistoryCache.invalidate(context, invoiceCustomer.phone)
                                    if (isNewCustomer) {
                                        val merged = CustomerCache.merge(cachedCustomers.toList(), listOf(invoiceCustomer))
                                        cachedCustomers.clear()
                                        cachedCustomers.addAll(merged)
                                        CustomerCache.save(context, merged)
                                    }
                                    latestPdfUrl = created.pdfUrl
                                    message = "Bill ${created.invoiceNumber} saved. Total: ₹${created.total}. The PDF is in Drive."
                                    cart.clear()
                                    customerName = ""
                                    customerPhone = ""
                                    customerEmail = ""
                                    customerAddress = ""
                                    customerExists = null
                                    paymentMode = "Cash"
                                }.onFailure { message = it.message ?: "Could not save the bill." }
                                savingInvoice = false
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
                    onSearch = {
                        val token = idToken
                        if (!api.isConfigured || token == null) message = "Connect to Shop before viewing bills."
                        else scope.launch {
                            billsLoading = true
                            runCatching { withContext(Dispatchers.IO) { api.bills(token, billsPhone) } }
                                .onSuccess { result -> bills.clear(); bills.addAll(result) }
                                .onFailure { message = it.message ?: "Could not load bills." }
                            billsLoading = false
                        }
                    },
                    onOpenPdf = { url -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
                )
                Screen.Catalog -> CatalogScreen(
                    padding = insets, products = products, live = api.isConfigured && idToken != null,
                    imageLoader = { id ->
                        val token = idToken ?: return@CatalogScreen null
                        runCatching { withContext(Dispatchers.IO) { api.productImage(token, id) } }.getOrNull()
                    },
                    onBack = { navigateBack() }
                )
                Screen.PlaceOrder -> PlaceOrderScreen(
                    padding = insets,
                    connected = api.isConfigured && idToken != null,
                    customerName = orderCustomerName,
                    customerPhone = orderCustomerPhone,
                    productName = orderProductName,
                    productSku = orderProductSku,
                    quantity = orderQuantity,
                    notes = orderNotes,
                    sending = sendingOrderRequest,
                    onCustomerName = { orderCustomerName = it },
                    onCustomerPhone = { orderCustomerPhone = it },
                    onProductName = { value ->
                        orderProductName = value
                        orderProductSku = products.firstOrNull {
                            it.sku.equals(value.trim(), ignoreCase = true) || it.name.equals(value.trim(), ignoreCase = true)
                        }?.sku.orEmpty()
                    },
                    onQuantity = { orderQuantity = it },
                    onNotes = { orderNotes = it },
                    onBack = { navigateBack() },
                    onSubmit = {
                        val token = idToken
                        val quantity = orderQuantity.toIntOrNull()
                        if (!api.isConfigured || token == null) message = "Connect to Shop before sending an order request."
                        else if (quantity == null || quantity < 1) message = "Enter a whole-number quantity greater than zero."
                        else if (!sendingOrderRequest) scope.launch {
                            sendingOrderRequest = true
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    api.createOrderRequest(token, orderCustomerName, orderCustomerPhone, orderProductName, orderProductSku, quantity, orderNotes)
                                }
                            }.onSuccess { request ->
                                CustomerHistoryCache.invalidate(context, orderCustomerPhone)
                                message = "Request ${request.requestNumber} sent to the manager. Status: ${request.status}."
                                orderCustomerName = ""; orderCustomerPhone = ""; orderProductName = ""; orderProductSku = ""; orderQuantity = "1"; orderNotes = ""
                                backStack.clear(); selectedTab = -1; screen = Screen.Home
                            }.onFailure { message = it.message ?: "Could not send the order request." }
                            sendingOrderRequest = false
                        }
                    }
                )
            }
        }
        message?.let { text ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { message = null },
                title = { Text(tr("Sales App", "सेल्स ऐप")) },
                text = { Text(text, fontSize = 17.sp) },
                confirmButton = {
                    Row {
                        if (latestPdfUrl.isNotBlank()) {
                            TextButton(onClick = {
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(latestPdfUrl))) }
                                latestPdfUrl = ""
                                message = null
                            }) { Text(tr("Open PDF", "PDF खोलें")) }
                        }
                        TextButton(onClick = { latestPdfUrl = ""; message = null }) { Text(tr("OK", "ठीक है")) }
                    }
                }
            )
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
                    TextButton(onClick = {
                        createdQrSku = null
                        productName = ""; productCategory = ""; sellingPrice = ""; unitCost = ""; openingStock = ""
                        backStack.clear(); selectedTab = 0; screen = Screen.Home
                    }) { Text(tr("Done", "हो गया")) }
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
    onFind: () -> Unit,
    onPlaceOrder: () -> Unit
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
            ActionCard(if (isHindi) "कपड़ा खोजें" else "Find a cloth", if (isHindi) "QR स्कैन करें या नाम से खोजें" else "Scan its QR code or search by name", Icons.Rounded.Search, Color.White, Ink, Muted, onFind, outline = true)
        }
        item {
            ActionCard(if (isHindi) "ऑर्डर भेजें" else "Place an order", if (isHindi) "मैनेजर को ग्राहक का अनुरोध भेजें" else "Send a customer request to the manager", Icons.AutoMirrored.Rounded.ReceiptLong, Color.White, Ink, Muted, onPlaceOrder, outline = true)
        }
        item {
            Spacer(Modifier.height(2.dp))
            Text(if (isHindi) "आसान कदम। साफ़ कीमतें। कोई ड्रॉपडाउन नहीं।" else "Simple steps. Clear prices. No dropdown lists.", color = Muted, fontSize = 15.sp)
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
        ActionCard(tr("Find a bill", "बिल खोजें"), tr("Search past bills by customer phone", "ग्राहक के फ़ोन नंबर से पुराने बिल खोजें"), Icons.Rounded.Search, Color.White, Ink, Muted, onFindBill, outline = true)
    }
}

@Composable
private fun ProductCenterScreen(padding: PaddingValues, onAddProduct: () -> Unit, onSearch: () -> Unit, onCatalog: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(18.dp)); Text(tr("Cloth", "कपड़ा"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
        ActionCard(tr("Add new cloth", "नया कपड़ा जोड़ें"), tr("Save cloth details and create its QR", "कपड़े की जानकारी सहेजें और QR बनाएँ"), Icons.Rounded.AddShoppingCart, Ink, Color.White, Color(0xFFD5E5EA), onAddProduct)
        ActionCard(tr("Search / scan clothes", "कपड़े खोजें / स्कैन करें"), tr("Type a cloth code or scan its QR", "कपड़े का कोड लिखें या QR स्कैन करें"), Icons.Rounded.QrCode2, Color.White, Ink, Muted, onSearch, outline = true)
        ActionCard(tr("Catalog", "कैटलॉग"), tr("Browse cloth photos and prices with a customer", "ग्राहक को कपड़ों की तस्वीरें और कीमतें दिखाएँ"), Icons.Rounded.Storefront, Color.White, Ink, Muted, onCatalog, outline = true)
    }
}

@Composable
private fun CatalogScreen(
    padding: PaddingValues,
    products: List<Product>,
    live: Boolean,
    imageLoader: suspend (String) -> ByteArray?,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(12.dp)); ScreenHeader(tr("Cloth catalog", "कपड़ों का कैटलॉग"), onBack)
        Text(tr("Show these clothes to your customer.", "ये कपड़े ग्राहक को दिखाएँ।"), color = Muted, fontSize = 15.sp, modifier = Modifier.padding(start = 8.dp, bottom = 10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
            items(products, key = { it.sku }) { product ->
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE1E8E7))) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        ProductPhoto(product.imageId, imageLoader, Modifier.fillMaxWidth().height(190.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(product.name, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                        if (product.category.isNotBlank()) Text(product.category, fontSize = 14.sp, color = Muted)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("₹${product.price}", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Teal, modifier = Modifier.weight(1f))
                            Text(if (product.stock <= 0) "Unavailable" else "Available", color = if (product.stock <= 0) Muted else Teal, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            if (products.isEmpty()) item { Text(if (live) "No clothes found." else "Connect to Shop to load the catalog.", color = Muted, fontSize = 17.sp) }
        }
    }
}

@Composable
private fun ProductPhoto(imageId: String, imageLoader: suspend (String) -> ByteArray?, modifier: Modifier = Modifier) {
    var bitmap by remember(imageId) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(imageId) {
        if (imageId.isNotBlank()) {
            val bytes = runCatching { imageLoader(imageId) }.getOrNull()
            if (bytes != null) bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = SoftBlue) {
        if (bitmap != null) Image(bitmap!!.asImageBitmap(), contentDescription = "Cloth photo", modifier = Modifier.fillMaxSize())
        else Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Inventory2, null, tint = Muted, modifier = Modifier.size(48.dp)) }
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
    loading: Boolean, ordersLoading: Boolean, onBack: () -> Unit, onOpenPdf: (String) -> Unit
) {
    val history = buildList {
        sales.forEach { add(CustomerHistoryItem("sale:${it.number}", it.date, invoice = it)) }
        orders.forEach { add(CustomerHistoryItem("request:${it.number}", it.date, orderRequest = it)) }
    }.sortedWith(compareByDescending<CustomerHistoryItem> { it.date }.thenByDescending { it.key })

    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            ScreenHeader(customer.name, onBack)
            Text(customer.phone, color = Teal, fontSize = 18.sp, modifier = Modifier.padding(start = 10.dp))
            if (customer.email.isNotBlank()) Text(customer.email, color = Muted, modifier = Modifier.padding(start = 10.dp))
        }
        item { SectionTitle("", tr("Sales & orders", "बिक्री और ऑर्डर")) }
        if (history.isEmpty() && (loading || ordersLoading)) item { CircularProgressIndicator(modifier = Modifier.padding(12.dp)) }
        else if (history.isEmpty()) item { Text(tr("No sales or order requests for this customer yet.", "इस ग्राहक की अभी कोई बिक्री या ऑर्डर अनुरोध नहीं है।"), color = Muted, modifier = Modifier.padding(8.dp)) }
        else items(history, key = { it.key }) { entry ->
            val bill = entry.invoice
            val order = entry.orderRequest
            val payment = bill?.paymentMode.orEmpty()
            val isCredit = bill != null && payment.equals("credit", ignoreCase = true)
            val tagColor = when {
                order != null -> Color(0xFF2E7D32)
                isCredit -> Color(0xFFC62828)
                else -> Color(0xFF1565C0)
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
                                isCredit -> tr("Credit sale", "उधार बिक्री")
                                payment.equals("cash", ignoreCase = true) -> tr("Cash sale", "नकद बिक्री")
                                else -> tr("Sale · $payment", "बिक्री · $payment")
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
                    } else if (order != null) {
                        Text(order.productName, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("${order.number} · ${order.date} · ${tr("Qty", "मात्रा")} ${order.quantity}", color = Muted, fontSize = 14.sp)
                        if (order.notes.isNotBlank()) Text(order.notes, color = Muted, fontSize = 14.sp)
                    }
                }
            }
        }
        if (history.isNotEmpty() && (loading || ordersLoading)) item { CircularProgressIndicator(modifier = Modifier.padding(12.dp)) }
    }
}

@Composable
private fun PlaceOrderScreen(
    padding: PaddingValues,
    connected: Boolean,
    customerName: String,
    customerPhone: String,
    productName: String,
    productSku: String,
    quantity: String,
    notes: String,
    sending: Boolean,
    onCustomerName: (String) -> Unit,
    onCustomerPhone: (String) -> Unit,
    onProductName: (String) -> Unit,
    onQuantity: (String) -> Unit,
    onNotes: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(tr("Place an order", "ऑर्डर भेजें"), onBack)
            Text(
                tr("Send an item request to the manager. It won’t create a bill or change stock.", "मैनेजर को सामान का अनुरोध भेजें। इससे बिल नहीं बनेगा और स्टॉक नहीं बदलेगा।"),
                color = Muted,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
            )
        }
        item {
            OutlinedTextField(productName, onProductName, Modifier.fillMaxWidth(), label = { Text(tr("Cloth wanted", "माँगा गया कपड़ा")) }, placeholder = { Text(tr("Enter cloth name or SKU", "कपड़े का नाम या SKU लिखें")) }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp), shape = RoundedCornerShape(14.dp))
            Text(
                if (productSku.isNotBlank()) tr("Matched catalogue item · $productSku", "कैटलॉग में मिला कपड़ा · $productSku") else tr("You can request a cloth even if it isn’t in the catalogue.", "कैटलॉग में न होने वाला कपड़ा भी माँग सकते हैं।"),
                color = Muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 6.dp, top = 5.dp)
            )
        }
        item {
            OutlinedTextField(quantity, onQuantity, Modifier.fillMaxWidth(), label = { Text(tr("Quantity wanted", "माँगी गई मात्रा")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp), shape = RoundedCornerShape(14.dp))
        }
        item {
            OutlinedTextField(customerName, onCustomerName, Modifier.fillMaxWidth(), label = { Text(tr("Customer name (optional)", "ग्राहक का नाम (वैकल्पिक)")) }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp), shape = RoundedCornerShape(14.dp))
        }
        item {
            OutlinedTextField(customerPhone, onCustomerPhone, Modifier.fillMaxWidth(), label = { Text(tr("Customer phone (optional)", "ग्राहक का फ़ोन (वैकल्पिक)")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp), shape = RoundedCornerShape(14.dp))
        }
        item {
            OutlinedTextField(notes, onNotes, Modifier.fillMaxWidth(), label = { Text(tr("Note for manager (optional)", "मैनेजर के लिए नोट (वैकल्पिक)")) }, minLines = 2, maxLines = 4, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp), shape = RoundedCornerShape(14.dp))
        }
        item {
            Button(onClick = onSubmit, enabled = connected && productName.isNotBlank() && !sending, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(16.dp)) {
                if (sending) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                else Icon(Icons.AutoMirrored.Rounded.ReceiptLong, null)
                Spacer(Modifier.width(9.dp))
                Text(if (sending) tr("Sending request…", "अनुरोध भेज रहे हैं…") else tr("Send order request", "ऑर्डर अनुरोध भेजें"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            if (!connected) Text(tr("Connect to Shop to send this request.", "अनुरोध भेजने के लिए Shop से जुड़ें।"), color = Muted, fontSize = 14.sp, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
private fun SearchScreen(
    padding: PaddingValues,
    query: String,
    products: List<Product>,
    live: Boolean,
    refreshing: Boolean,
    onQuery: (String) -> Unit,
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
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
            items(results) { product -> ProductCard(product, live, onAdd = { onAddToBill(product) }) }
            if (results.isEmpty()) item { Text(tr("No cloth found. Try another name or code.", "कपड़ा नहीं मिला। दूसरा नाम या कोड आज़माएँ।"), color = Muted, fontSize = 17.sp, modifier = Modifier.padding(8.dp)) }
        }
    }
}

@Composable
private fun ProductCard(product: Product, live: Boolean, onAdd: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE1E8E7))) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(product.name, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(4.dp))
            Text("${product.sku} · ${product.category}", fontSize = 14.sp, color = Muted)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("₹${product.price}", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Teal)
                    Text(if (product.stock <= 0) tr("Out of stock", "स्टॉक में नहीं") else if (live) tr("${product.stock} in stock", "${product.stock} स्टॉक में") else "${product.stock} in stock · preview", fontSize = 14.sp, color = Muted)
                }
                FilledTonalButton(onClick = onAdd, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp)) {
                    Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(5.dp)); Text(tr("Add to bill", "बिल में जोड़ें"), fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun AddProductScreen(
    padding: PaddingValues,
    connected: Boolean,
    name: String,
    category: String,
    price: String,
    cost: String,
    stock: String,
    photo: Bitmap?,
    onPickPhoto: () -> Unit,
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
            OutlinedButton(onClick = onPickPhoto, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                Text(if (photo == null) tr("Add cloth picture (optional)", "कपड़े की तस्वीर जोड़ें (वैकल्पिक)") else tr("Change cloth picture", "तस्वीर बदलें"), fontSize = 16.sp)
            }
            photo?.let { Image(it.asImageBitmap(), contentDescription = "Selected cloth picture", modifier = Modifier.fillMaxWidth().height(190.dp).padding(top = 8.dp)) }
        }
        item { OutlinedTextField(name, onName, Modifier.fillMaxWidth(), label = { Text(tr("Cloth name", "कपड़े का नाम")) }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp), shape = RoundedCornerShape(14.dp)) }
        item { OutlinedTextField(category, onCategory, Modifier.fillMaxWidth(), label = { Text(tr("Category (optional)", "श्रेणी (वैकल्पिक)")) }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp), shape = RoundedCornerShape(14.dp)) }
        item { OutlinedTextField(price, onPrice, Modifier.fillMaxWidth(), label = { Text(tr("Selling price (₹)", "बेचने की कीमत (₹)")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp), shape = RoundedCornerShape(14.dp)) }
        item { OutlinedTextField(cost, onCost, Modifier.fillMaxWidth(), label = { Text(tr("Purchase cost per item (₹)", "एक पीस की खरीद कीमत (₹)")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp), shape = RoundedCornerShape(14.dp)) }
        item { OutlinedTextField(stock, onStock, Modifier.fillMaxWidth(), label = { Text(tr("Starting quantity", "शुरुआती मात्रा")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp), shape = RoundedCornerShape(14.dp)) }
        item {
            Button(onClick = onSave, enabled = connected && name.isNotBlank() && price.isNotBlank() && cost.isNotBlank() && stock.isNotBlank(), modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Rounded.CheckCircle, null); Spacer(Modifier.width(10.dp)); Text(tr("Save cloth", "कपड़ा सहेजें"), fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            Text(if (connected) tr("Saving adds this cloth to Shop and shows its QR code.", "सहेजने पर कपड़ा Shop में जुड़ जाएगा और QR दिखेगा।") else tr("Connect to Shop and sign in before saving.", "सहेजने से पहले Shop से जुड़कर साइन इन करें।"), fontSize = 14.sp, color = Muted)
        }
    }
}

@Composable
private fun BillScreen(
    padding: PaddingValues,
    connected: Boolean,
    customerName: String,
    customerPhone: String,
    customerEmail: String,
    customerAddress: String,
    customerExists: Boolean?,
    customerSearching: Boolean,
    saving: Boolean,
    paymentMode: String,
    cart: List<CartLine>,
    onName: (String) -> Unit,
    onPhone: (String) -> Unit,
    onCustomerEmail: (String) -> Unit,
    onCustomerAddress: (String) -> Unit,
    onPaymentMode: (String) -> Unit,
    onPickContact: () -> Unit,
    onScan: () -> Unit,
    onFindProduct: () -> Unit,
    onQuantity: (String, Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val total = cart.sumOf { it.product.price * it.quantity }
    val phoneFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
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
            ScreenHeader(tr("Create a bill", "बिल बनाएँ"), onBack)
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                shape = RoundedCornerShape(14.dp),
                trailingIcon = { IconButton(onClick = onPickContact) { Icon(Icons.Rounded.Contacts, "Choose contact") } }
            )
            when {
                customerSearching -> Text(tr("Checking saved customers…", "सहेजे ग्राहक खोज रहे हैं…"), color = Muted, fontSize = 14.sp)
                customerExists == true -> Text(tr("Saved customer found", "सहेजा हुआ ग्राहक मिल गया"), color = Teal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                customerExists == false -> Text(tr("New customer — enter their name below", "नया ग्राहक — नीचे नाम लिखें"), color = Muted, fontSize = 14.sp)
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
        item {
            Divider(color = Color(0xFFDDE5E4))
            Spacer(Modifier.height(4.dp))
            SectionTitle("3", tr("Payment", "भुगतान"))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Cash", "UPI", "Card").forEach { option ->
                    if (paymentMode == option) {
                        Button(onClick = { onPaymentMode(option) }, modifier = Modifier.weight(1f).height(54.dp), contentPadding = PaddingValues(4.dp), shape = RoundedCornerShape(14.dp)) {
                            Text(option, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(onClick = { onPaymentMode(option) }, modifier = Modifier.weight(1f).height(54.dp), contentPadding = PaddingValues(4.dp), shape = RoundedCornerShape(14.dp)) {
                            Text(option, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = SoftGreen) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Bill total", "बिल की कुल राशि"), Modifier.weight(1f), fontSize = 18.sp, color = Ink)
                    Text("₹$total", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Teal)
                }
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onSave,
                enabled = !saving && customerPhone.isNotBlank() && customerExists != null && (customerExists || customerName.isNotBlank()) && cart.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(62.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                else Icon(Icons.AutoMirrored.Rounded.ReceiptLong, null)
                Spacer(Modifier.width(10.dp)); Text(if (saving) tr("Creating bill…", "बिल बना रहे हैं…") else tr("Create bill and PDF", "बिल और PDF बनाएँ"), fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                if (connected) tr("This creates the invoice in Shop and saves a PDF in Drive.", "इससे Shop में बिल बनेगा और Drive में PDF सहेजी जाएगी।")
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
private fun BillsScreen(
    padding: PaddingValues,
    phone: String,
    bills: List<ShopInvoice>,
    loading: Boolean,
    onPhone: (String) -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onOpenPdf: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(14.dp))
        ScreenHeader(tr("Find a bill", "बिल खोजें"), onBack)
        Text(tr("Search by customer phone, or leave blank to see recent bills.", "ग्राहक के फ़ोन से खोजें या हाल के बिल देखने के लिए खाली छोड़ें।"), color = Muted, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 14.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = onPhone,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(tr("Customer phone (optional)", "ग्राहक का फ़ोन (वैकल्पिक)")) },
            placeholder = { Text(tr("Leave blank for recent bills", "हाल के बिलों के लिए खाली छोड़ें")) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.height(10.dp))
        Button(onClick = onSearch, enabled = !loading, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp)) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
            else Icon(Icons.Rounded.Search, null)
            Spacer(Modifier.width(8.dp))
            Text(if (loading) tr("Loading bills…", "बिल लोड हो रहे हैं…") else if (phone.isBlank()) tr("Show recent bills", "हाल के बिल दिखाएँ") else tr("Search this customer", "इस ग्राहक को खोजें"), fontSize = 17.sp)
        }
        Spacer(Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
            items(bills, key = { it.number }) { bill ->
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE1E8E7))) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(bill.customerName.ifBlank { tr("Customer", "ग्राहक") }, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                                Text("${bill.phone} · ${bill.date}", fontSize = 14.sp, color = Muted)
                            }
                            Text("₹${bill.total}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Teal)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${bill.number} · ${bill.paymentMode}", Modifier.weight(1f), fontSize = 13.sp, color = Muted)
                            if (bill.pdfUrl.isNotBlank()) TextButton(onClick = { onOpenPdf(bill.pdfUrl) }) { Text(tr("Open PDF", "PDF खोलें")) }
                        }
                    }
                }
            }
            if (bills.isEmpty() && !loading) {
                item { Text(tr("No bills loaded yet. Choose recent bills or search by phone.", "अभी बिल लोड नहीं हुए। हाल के बिल चुनें या फ़ोन से खोजें।"), color = Muted, fontSize = 16.sp, modifier = Modifier.padding(8.dp)) }
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

private fun createQrBitmap(value: String): Bitmap {
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
