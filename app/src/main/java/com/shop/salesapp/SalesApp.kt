package com.shop.salesapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.shop.salesapp.ui.bill.BillCenterScreen
import com.shop.salesapp.ui.bill.BillScreen
import com.shop.salesapp.ui.bill.BillsScreen
import com.shop.salesapp.ui.bill.CartQuantityControl
import com.shop.salesapp.ui.bill.WhatsAppShareIcon
import com.shop.salesapp.ui.common.AdminDeletionRequestsSection
import com.shop.salesapp.ui.common.DetailLine
import com.shop.salesapp.ui.common.ScreenHeader
import com.shop.salesapp.ui.auth.LoginScreen
import com.shop.salesapp.ui.customer.AddCustomerScreen
import com.shop.salesapp.ui.customer.CustomerDetailScreen
import com.shop.salesapp.ui.customer.CustomerDirectoryScreen
import com.shop.salesapp.ui.home.HomeScreen
import com.shop.salesapp.ui.model.CartLine
import com.shop.salesapp.ui.model.DeletionTarget
import com.shop.salesapp.ui.model.Screen
import com.shop.salesapp.ui.model.addOptimisticPayment
import com.shop.salesapp.ui.model.applyPaymentAllocations
import com.shop.salesapp.ui.model.confirmOptimisticPayment
import com.shop.salesapp.ui.model.formatQuantity
import com.shop.salesapp.ui.model.lineAmount
import com.shop.salesapp.ui.model.mergeLocalPaymentLog
import com.shop.salesapp.ui.model.mergePaymentHistory
import com.shop.salesapp.ui.model.newLocalPayment
import com.shop.salesapp.ui.model.previewProducts
import com.shop.salesapp.ui.model.replaceLocalPayment
import com.shop.salesapp.ui.model.runCatchingCancellable
import com.shop.salesapp.ui.payment.PaymentsScreen
import com.shop.salesapp.ui.payment.CollectPaymentScreen
import com.shop.salesapp.ui.product.AddProductScreen
import com.shop.salesapp.ui.product.CatalogScreen
import com.shop.salesapp.ui.product.OrderRequestDetailScreen
import com.shop.salesapp.ui.product.OrderRequestsScreen
import com.shop.salesapp.ui.product.ProductCenterScreen
import com.shop.salesapp.ui.product.ProductDetailScreen
import com.shop.salesapp.ui.product.SearchScreen
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.ClothActionBlue
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.Paper
import com.shop.salesapp.ui.theme.SoftBlue
import com.shop.salesapp.ui.theme.Teal
import com.shop.salesapp.ui.theme.localizedAppMessage
import com.shop.salesapp.ui.theme.salesAppHindi
import com.shop.salesapp.ui.theme.tr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.CancellationException
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt
import com.shop.salesapp.ui.common.*
import com.shop.salesapp.ui.home.ActionCard
import com.shop.salesapp.ui.model.*
import com.shop.salesapp.ui.product.*
import com.shop.salesapp.ui.theme.*


@Composable
internal fun SalesApp(operationScope: CoroutineScope) {
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
    var billShouldScrollToDiscount by remember { mutableStateOf(false) }
    var catalogQuery by remember { mutableStateOf("") }
    var selectedCatalogProduct by remember { mutableStateOf<Product?>(null) }
    var productSyncing by remember { mutableStateOf(false) }
    var customerDirectoryQuery by remember { mutableStateOf("") }
    var customerDirectoryLoading by remember { mutableStateOf(false) }
    var customerSyncing by remember { mutableStateOf(false) }
    var customerUploadsRunning by remember { mutableStateOf(false) }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerEmail by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var customerExists by remember { mutableStateOf<Boolean?>(null) }
    var customerSearching by remember { mutableStateOf(false) }
    var customerMatches by remember { mutableStateOf<List<ShopCustomer>>(emptyList()) }
    var paymentMode by remember { mutableStateOf("") }
    var creditAmountReceived by remember { mutableStateOf("0") }
    var creditPaymentMethod by remember { mutableStateOf("") }
    var billDiscountValue by remember { mutableStateOf("") }
    var billDiscountEnabled by remember { mutableStateOf(false) }
    var productName by remember { mutableStateOf("") }
    var productSizeVariant by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("") }
    var productImageBase64 by remember { mutableStateOf("") }
    var productImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var sellingPrice by remember { mutableStateOf("") }
    var unitCost by remember { mutableStateOf("") }
    var openingStock by remember { mutableStateOf("") }
    var fractionalQuantity by remember { mutableStateOf(false) }
    var productUnit by remember { mutableStateOf("PIECE") }
    var productPricingUnit by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var userRole by remember { mutableStateOf("SALESPERSON") }
    var isAuthenticated by remember { mutableStateOf(false) }
    var checkingSession by remember { mutableStateOf(false) }
    var signInError by remember { mutableStateOf<String?>(null) }
    var pendingDeletionTarget by remember { mutableStateOf<DeletionTarget?>(null) }
    var deletionReason by remember { mutableStateOf("Mistake") }
    var submittingDeletion by remember { mutableStateOf(false) }
    val paymentDeletionRequests = remember { mutableStateListOf<DeletionRequest>() }
    val billDeletionRequests = remember { mutableStateListOf<DeletionRequest>() }
    val productDeletionRequests = remember { mutableStateListOf<DeletionRequest>() }
    val orderCancellationRequests = remember { mutableStateListOf<DeletionRequest>() }
    val isAdministrator = userRole == "ADMIN"
    var latestPdfUrl by remember { mutableStateOf("") }
    var latestInvoice by remember { mutableStateOf<ShopInvoice?>(null) }
    var invoicePdfPreparingNumbers by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sharingBillNumber by remember { mutableStateOf<String?>(null) }
    var sharingCustomerDocument by remember { mutableStateOf(false) }
    var createdQrSku by remember { mutableStateOf<String?>(null) }
    var signInReturnScreen by remember { mutableStateOf<Screen?>(null) }
    var qrPreviewProduct by remember { mutableStateOf<Product?>(null) }
    var savingInvoice by remember { mutableStateOf(false) }
    var savingCloth by remember { mutableStateOf(false) }
    var billsPhone by remember { mutableStateOf("") }
    var billsLoading by remember { mutableStateOf(false) }
    var paymentDirectoryQuery by remember { mutableStateOf("") }
    var collectCustomerSearch by remember { mutableStateOf("") }
    var collectContactName by remember { mutableStateOf("") }
    var collectContactPhone by remember { mutableStateOf("") }
    var orderMode by remember { mutableStateOf(false) }
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
    val pendingPaymentSyncCount = madePayments.count { !it.isVoided && it.syncStatus != "SYNCED" }
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
    var printedQrSkus by remember(context) { mutableStateOf(QrLabelStatusStore.loadPrintedSkus(context)) }
    val cachedCustomers = remember(context) {
        mutableStateListOf<ShopCustomer>().apply { addAll(CustomerCache.load(context)) }
    }
    val lastAccount = remember { GoogleSignIn.getLastSignedInAccount(context) }
    var idToken by remember { mutableStateOf(lastAccount?.idToken) }

    fun cacheCustomerImmediately(name: String, phone: String, email: String, address: String): ShopCustomer {
        val pending = PendingCustomer(name.trim(), phone.trim(), email.trim(), address.trim())
        val local = ShopCustomer(
            id = "LOCAL-${CustomerCache.normalizePhone(pending.phone)}",
            name = pending.name,
            phone = pending.phone,
            email = pending.email
        )
        CustomerCache.enqueue(context, pending)
        val merged = CustomerCache.merge(cachedCustomers.toList(), listOf(local))
        cachedCustomers.clear()
        cachedCustomers.addAll(merged)
        CustomerCache.save(context, merged)
        return local
    }

    fun setQrLabelsPrinted(selectedProducts: List<Product>, printed: Boolean) {
        val updated = printedQrSkus.toMutableSet()
        selectedProducts.forEach { product ->
            if (printed) updated.add(product.sku) else updated.remove(product.sku)
        }
        printedQrSkus = updated
        QrLabelStatusStore.savePrintedSkus(context, updated)
    }

    suspend fun syncPendingCustomerUploads(token: String) {
        if (customerUploadsRunning || !api.isConfigured) return
        customerUploadsRunning = true
        var syncedCount = 0
        try {
            CustomerCache.loadPending(context).forEach { pending ->
                val synced = withContext(Dispatchers.IO) {
                    api.findCustomer(token, pending.phone)
                        ?: api.createCustomer(token, pending.name, pending.phone, pending.email, pending.address)
                }
                val merged = CustomerCache.merge(cachedCustomers.toList(), listOf(synced))
                cachedCustomers.clear()
                cachedCustomers.addAll(merged)
                CustomerCache.save(context, merged)
                CustomerCache.removePending(context, pending.phone)
                syncedCount += 1
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            message = error.message ?: "Customer is saved on this phone and will sync when connected."
        } finally {
            customerUploadsRunning = false
        }
        if (syncedCount > 0) message = if (syncedCount == 1) "Customer synced to Shop." else "$syncedCount customers synced to Shop."
    }

    fun dismissKeyboard() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }

    fun saveOrderDraft() {
        OrderFormDraftCache.save(
            context,
            OrderFormDraft(
                customerName = customerName,
                customerPhone = customerPhone,
                customerEmail = customerEmail,
                customerAddress = customerAddress,
                items = cart.toList(),
                customerConfirmed = customerExists == true
            )
        )
    }

    fun saveBillDraft() {
        BillFormDraftCache.save(
            context,
            BillFormDraft(
                customerName = customerName,
                customerPhone = customerPhone,
                customerEmail = customerEmail,
                customerAddress = customerAddress,
                items = cart.toList(),
                customerConfirmed = customerExists == true,
                paymentMode = paymentMode,
                discountValue = billDiscountValue,
                discountEnabled = billDiscountEnabled,
                creditAmountReceived = creditAmountReceived,
                creditPaymentMethod = creditPaymentMethod
            )
        )
    }

    fun saveNewClothDraft() {
        NewClothFormDraftCache.save(
            context,
            NewClothFormDraft(
                name = productName,
                sizeVariant = productSizeVariant,
                category = productCategory,
                price = sellingPrice,
                cost = unitCost,
                stock = openingStock,
                fractionalQuantity = fractionalQuantity,
                unit = productUnit,
                pricingUnit = productPricingUnit,
                photoBase64 = productImageBase64
            )
        )
    }

    fun persistCurrentDraft() {
        when (screen) {
            Screen.PlaceOrder -> saveOrderDraft()
            Screen.Bill -> saveBillDraft()
            Screen.AddProduct -> if (createdQrSku == null) saveNewClothDraft()
            else -> Unit
        }
    }

    fun restoreOrderDraft() {
        val draft = OrderFormDraftCache.load(context)
        customerPhone = draft?.customerPhone.orEmpty()
        val normalizedDraftPhone = CustomerCache.normalizePhone(customerPhone)
        val matchedCustomer = cachedCustomers.firstOrNull {
            CustomerCache.normalizePhone(it.phone) == normalizedDraftPhone && normalizedDraftPhone.length >= 8
        }
        customerName = matchedCustomer?.name ?: draft?.customerName.orEmpty()
        customerEmail = matchedCustomer?.email ?: draft?.customerEmail.orEmpty()
        customerAddress = draft?.customerAddress.orEmpty()
        cart.clear()
        draft?.items?.let(cart::addAll)
        customerExists = (matchedCustomer != null || draft?.customerConfirmed == true) && customerName.isNotBlank() &&
            CustomerCache.normalizePhone(customerPhone).length >= 8
        customerMatches = emptyList()
        orderMode = true
    }

    fun restoreBillDraft() {
        val draft = BillFormDraftCache.load(context)
        customerPhone = draft?.customerPhone.orEmpty()
        val normalizedDraftPhone = CustomerCache.normalizePhone(customerPhone)
        val matchedCustomer = cachedCustomers.firstOrNull {
            CustomerCache.normalizePhone(it.phone) == normalizedDraftPhone && normalizedDraftPhone.length >= 8
        }
        customerName = matchedCustomer?.name ?: draft?.customerName.orEmpty()
        customerEmail = matchedCustomer?.email ?: draft?.customerEmail.orEmpty()
        customerAddress = draft?.customerAddress.orEmpty()
        cart.clear()
        draft?.items?.let(cart::addAll)
        customerExists = (matchedCustomer != null || draft?.customerConfirmed == true) && customerName.isNotBlank() && normalizedDraftPhone.length >= 8
        customerMatches = emptyList()
        paymentMode = draft?.paymentMode.orEmpty()
        billDiscountValue = draft?.discountValue.orEmpty()
        billDiscountEnabled = draft?.discountEnabled ?: false
        creditAmountReceived = draft?.creditAmountReceived ?: "0"
        creditPaymentMethod = draft?.creditPaymentMethod.orEmpty()
        orderMode = false
    }

    fun restoreNewClothDraft() {
        val draft = NewClothFormDraftCache.load(context)
        productName = draft?.name.orEmpty()
        productSizeVariant = draft?.sizeVariant.orEmpty()
        productCategory = draft?.category.orEmpty()
        sellingPrice = draft?.price.orEmpty()
        unitCost = draft?.cost.orEmpty()
        openingStock = draft?.stock.orEmpty()
        fractionalQuantity = draft?.fractionalQuantity ?: false
        productUnit = draft?.unit ?: "PIECE"
        productPricingUnit = draft?.pricingUnit.orEmpty()
        productImageBase64 = draft?.photoBase64.orEmpty()
        productImageBitmap = runCatching {
            if (productImageBase64.isBlank()) null else {
                val bytes = Base64.decode(productImageBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }.getOrNull()
    }

    LaunchedEffect(
        screen, customerName, customerPhone, customerEmail, customerAddress, customerExists, cart.toList(),
        paymentMode, billDiscountValue, billDiscountEnabled, creditAmountReceived, creditPaymentMethod,
        productName, productSizeVariant, productCategory, sellingPrice, unitCost, openingStock,
        fractionalQuantity, productUnit, productPricingUnit, productImageBase64, createdQrSku
    ) {
        when (screen) {
            Screen.PlaceOrder -> {
                delay(300)
                saveOrderDraft()
            }
            Screen.Bill -> {
                delay(300)
                saveBillDraft()
            }
            Screen.AddProduct -> if (createdQrSku == null) {
                delay(300)
                saveNewClothDraft()
            } else NewClothFormDraftCache.clear(context)
            else -> Unit
        }
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
            persistCurrentDraft()
            dismissKeyboard()
            backStack.add(screen)
            screen = destination
            selectedTab = when (destination) {
                Screen.BillCenter, Screen.Bill, Screen.Bills -> 0
                Screen.Customer, Screen.CustomerDetail, Screen.AddCustomer -> 1
                Screen.ProductCenter, Screen.Search, Screen.Catalog, Screen.ProductDetail, Screen.AddProduct, Screen.PlaceOrder, Screen.OrderRequests, Screen.OrderRequestDetail -> 2
                Screen.Payments, Screen.CollectPayment -> 3
                else -> -1
            }
        }
    }

    fun navigateBack() {
        persistCurrentDraft()
        dismissKeyboard()
        screen = if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex) else Screen.Home
        selectedTab = when (screen) {
            Screen.BillCenter, Screen.Bill, Screen.Bills -> 0
            Screen.Customer, Screen.CustomerDetail, Screen.AddCustomer -> 1
            Screen.ProductCenter, Screen.Search, Screen.Catalog, Screen.ProductDetail, Screen.AddProduct, Screen.PlaceOrder, Screen.OrderRequests, Screen.OrderRequestDetail -> 2
            Screen.Payments, Screen.CollectPayment -> 3
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
            if (account.idToken.isNullOrBlank()) {
                signInError = "Google sign-in did not return a Shop token. Check the app's sign-in setup."
                signInReturnScreen = null
            } else {
                signInError = null
                isAuthenticated = false
                signInReturnScreen?.let { destination ->
                    signInReturnScreen = null
                    backStack.clear()
                    backStack.add(Screen.ProductCenter)
                    screen = destination
                    selectedTab = 2
                }
            }
        }.onFailure {
            signInError = "Could not sign in. Try again or ask the manager to check access."
            signInReturnScreen = null
        }
    }

    fun beginSignIn(returnTo: Screen? = null) {
        if (signingIn) return
        if (returnTo == Screen.AddProduct) saveNewClothDraft()
        signInError = null
        isAuthenticated = false
        checkingSession = false
        signInReturnScreen = returnTo
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
                syncPendingCustomerUploads(token)
            }
    }

    LaunchedEffect(customerPhone, idToken, api.isConfigured) {
        customerSearching = false
        val digits = CustomerCache.normalizePhone(customerPhone)
        if (customerExists == true && customerName.isNotBlank() && digits.length >= 8) {
            customerMatches = emptyList()
            return@LaunchedEffect
        }
        customerExists = null
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
                if (customerExists == true && customerName.isNotBlank()) return@onSuccess
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
            .onFailure {
                if (customerExists != true || customerName.isBlank()) message = it.message ?: "Could not search customers."
            }
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
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (phoneIndex >= 0 && cursor.moveToFirst()) {
                    (if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else "") to cursor.getString(phoneIndex).orEmpty()
                } else "" to ""
            } ?: ("" to "")
        }.onSuccess { (name, phone) ->
            if (phone.isBlank()) message = "That contact has no phone number. You can type it instead."
            else if (screen == Screen.AddCustomer) {
                newCustomerName = name
                newCustomerPhone = phone
            } else if (screen == Screen.Payments) {
                paymentDirectoryQuery = phone
            } else if (screen == Screen.CollectPayment) {
                collectCustomerSearch = phone
                collectContactName = name
                collectContactPhone = phone
            } else customerPhone = phone
        }.onFailure { message = "Could not read the selected phone number. You can type it instead." }
    }

    val productPhotoActions = rememberProductPhotoActions(
        context = context,
        operationScope = operationScope,
        api = api,
        idToken = idToken,
        products = products,
        onFormImageSelected = { base64, bitmap ->
            productImageBase64 = base64
            productImageBitmap = bitmap
        },
        onProductsUpdated = { updated ->
            products.clear()
            products.addAll(updated)
        },
        onCatalogProductUpdated = { updated ->
            if (updated != null) selectedCatalogProduct = updated
        },
        onMessage = { message = it }
    )

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
                    val merged = CustomerCache.merge(cachedCustomers.toList(), freshCustomers)
                    cachedCustomers.clear()
                    cachedCustomers.addAll(merged)
                    CustomerCache.save(context, merged)
                    message = "Customers synced from Shop."
                    syncPendingCustomerUploads(token)
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
                        "ORDER" -> orderCancellationRequests
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
                message = if (target.type == "BILL" || target.type == "ORDER") {
                    "Cancellation request sent to the administrator."
                } else {
                    "Deletion request sent to the administrator."
                }
                if (isAdministrator) refreshDeletionRequests(target.type)
                if (target.type == "BILL" || target.type == "ORDER") {
                    selectedCustomer?.let { syncCustomerDetails(it) }
                    if (target.type == "ORDER") {
                        idToken?.let { cancellationToken -> operationScope.launch {
                            runCatchingCancellable { withContext(Dispatchers.IO) { api.orderRequests(cancellationToken, "") } }
                                .onSuccess { orderRequests = it }
                        } }
                    } else idToken?.let { syncBillsAndPayments(it) }
                }
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
                if (approve && type == "ORDER") {
                    val freshOrders = withContext(Dispatchers.IO) { api.orderRequests(token, "") }
                    orderRequests = freshOrders
                    selectedCustomer?.let { customer ->
                        val customerOrders = withContext(Dispatchers.IO) { api.orderRequests(token, customer.phone) }
                        detailOrders = customerOrders
                        withContext(Dispatchers.IO) {
                            CustomerHistoryCache.save(context, customer.phone, detailBills, customerOrders)
                        }
                    }
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
        sizeVariant: String,
        category: String,
        price: Int,
        fractionalQuantity: Boolean,
        unit: String,
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
                    api.updateProduct(token, product.sku, name, category, price, fractionalQuantity, unit, sizeVariant)
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
        isAuthenticated = false
        userRole = "SALESPERSON"
        val token = idToken
        if (token == null || !api.isConfigured) {
            checkingSession = false
            return@LaunchedEffect
        }
        checkingSession = true
        runCatchingCancellable { withContext(Dispatchers.IO) { api.session(token) } }
            .onSuccess {
                if (idToken == token) {
                    userRole = it.role
                    isAuthenticated = true
                    signInError = null
                }
            }
            .onFailure {
                if (idToken == token) {
                    idToken = null
                    signInReturnScreen = null
                    signInError = "Could not verify your Shop sign-in. Please sign in again."
                }
            }
        checkingSession = false
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
        if (sharingBillNumber != null || bill.number in invoicePdfPreparingNumbers) return
        operationScope.launch {
            sharingBillNumber = bill.number
            runCatchingCancellable {
                val bytes = withContext(Dispatchers.IO) { InvoicePdfGenerator.create(bill) }
                BillPdfShare.share(context, bill, bytes)
            }.onFailure { message = it.message ?: "Could not prepare this bill for sharing." }
            sharingBillNumber = null
        }
    }

    fun openBillPdf(bill: ShopInvoice) {
        if (bill.isVoided || bill.number in invoicePdfPreparingNumbers || sharingBillNumber == bill.number) return
        invoicePdfPreparingNumbers = invoicePdfPreparingNumbers + bill.number
        operationScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { InvoicePdfGenerator.create(bill) }
                BillPdfShare.open(context, bill, bytes)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                message = error.message ?: "Could not create the bill PDF."
                Log.w("SalesApp", "Could not create invoice PDF ${bill.number} on the phone.", error)
            } finally {
                invoicePdfPreparingNumbers = invoicePdfPreparingNumbers - bill.number
            }
        }
    }

    fun shareOrderQuotation(request: CustomerOrderRequest) {
        if (sharingCustomerDocument) return
        sharingCustomerDocument = true
        operationScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { CustomerPdfGenerator.quotation(request, products) }
                BillPdfShare.shareCustomerPdf(
                    context = context,
                    fileName = "Quotation-${request.number}",
                    bytes = bytes,
                    customerName = request.customerName,
                    phone = request.customerPhone,
                    chooserTitle = "Send quotation to customer",
                    subject = "Quotation ${request.number}",
                    text = "Your requested product details and estimated quotation are attached. Items with measurement-dependent pricing are marked for confirmation."
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                message = error.message ?: "Could not prepare the quotation PDF."
                Log.w("SalesApp", "Could not create quotation PDF ${request.number}.", error)
            } finally {
                sharingCustomerDocument = false
            }
        }
    }

    fun shareCatalogPdf() {
        if (sharingCustomerDocument) return
        sharingCustomerDocument = true
        operationScope.launch {
            try {
                val availableProducts = products.filter { it.available }
                val token = idToken
                val bytes = withContext(Dispatchers.IO) {
                    val imageGate = Semaphore(3)
                    val photos = coroutineScope {
                        availableProducts.map { product -> async {
                            val bitmap = imageGate.withPermit {
                                ProductImageCache.load(context, product.sku, product.imageId)
                                    ?: if (token != null && product.imageId.isNotBlank()) {
                                        runCatching {
                                            api.productImage(token, product.imageId)?.let { imageBytes ->
                                                ProductImageCache.save(context, product.sku, product.imageId, imageBytes)
                                            }
                                        }.getOrNull()
                                    } else null
                            }
                            product.sku to bitmap
                        } }.awaitAll()
                    }.mapNotNull { (sku, bitmap) -> bitmap?.let { sku to it } }.toMap()
                    CustomerPdfGenerator.catalog(availableProducts, photos)
                }
                val directory = File(context.cacheDir, "shared-customer-documents")
                if (!directory.exists() && !directory.mkdirs()) throw IllegalStateException("Could not prepare the catalogue PDF.")
                val pdf = File(directory, "Avani-Suto-Catalogue.pdf").apply { writeBytes(bytes) }
                BillPdfShare.sharePdfFile(
                    context = context,
                    pdf = pdf,
                    chooserTitle = "Share Avani Suto catalogue",
                    subject = "Avani Suto Cloth Catalogue",
                    text = "Explore the available Avani Suto collection and customer prices."
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                message = error.message ?: "Could not prepare the catalogue PDF."
                Log.w("SalesApp", "Could not create customer catalogue PDF.", error)
            } finally {
                sharingCustomerDocument = false
            }
        }
    }

    fun shareQrLabelPdf(selectedProducts: List<Product>) {
        runCatching {
            val pdf = QrLabelPrinter.createLabelPdf(context, selectedProducts)
            val downloadsPath = runCatching { QrLabelPrinter.saveToDownloads(context, pdf) }.getOrNull()
            val printerApp = context.packageManager.getLaunchIntentForPackage("com.caysn.shreyanseasylabel")
            if (downloadsPath != null && printerApp != null) {
                android.widget.Toast.makeText(
                    context,
                    "Saved to $downloadsPath. In Shreyans, choose Print Documents → PDF Printing → Label printer → Custom 48×26 mm → Choose PDF.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                context.startActivity(printerApp)
            } else {
                BillPdfShare.sharePdfFile(
                    context,
                    pdf,
                    "Print QR labels",
                    "Cloth QR labels",
                    "${selectedProducts.size} cloth QR label(s), sized 48 × 26 mm for the PSF-20 label roll."
                )
            }
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
                    api.createOrderRequest(token, requestCustomer.name, requestCustomer.phone, requestCustomer.email, customerAddress, requestItems, "")
                }
            }.onSuccess { request ->
                CustomerHistoryCache.invalidate(context, requestCustomer.phone)
                OrderFormDraftCache.clear(context)
                if (isNewCustomer) {
                    val merged = CustomerCache.merge(cachedCustomers.toList(), listOf(requestCustomer))
                    cachedCustomers.clear(); cachedCustomers.addAll(merged); CustomerCache.save(context, merged)
                }
                message = "Order ${request.requestNumber} sent to the manager. Status: ${request.status}."
                orderMode = false
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
                .onSuccess { requests ->
                    orderRequests = requests
                    if (isAdministrator) refreshDeletionRequests("ORDER")
                }
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
        if (!isAuthenticated) {
            LoginScreen(
                apiConfigured = api.isConfigured,
                signingIn = signingIn,
                checkingSession = checkingSession,
                errorMessage = signInError,
                onSignIn = { beginSignIn() }
            )
        } else {
        Scaffold(
            containerColor = Paper,
            topBar = {
                if (savingCloth || receivingPayment || savingCustomer || sendingOrderRequest) {
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
                SalesAppNavigationBar(
                    screen = screen,
                    selectedTab = selectedTab,
                    isHindi = isHindi,
                    pendingPaymentSyncCount = pendingPaymentSyncCount,
                    onSelect = { destination, tab ->
                        persistCurrentDraft()
                        backStack.clear()
                        selectedTab = tab
                        screen = destination
                    }
                )
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
                    onNewBill = {
                        restoreBillDraft()
                        navigateTo(Screen.Bill)
                    },
                    onMakePayment = { navigateTo(Screen.CollectPayment) },
                    onCatalog = { selectedTab = 2; navigateTo(Screen.Catalog) },
                    onAddProduct = {
                        restoreNewClothDraft()
                        navigateTo(Screen.AddProduct)
                    },
                    onPlaceOrder = {
                        restoreOrderDraft()
                        navigateTo(Screen.PlaceOrder)
                    }
                )
                Screen.BillCenter -> BillCenterScreen(
                    padding = insets,
                    onCreateBill = {
                        restoreBillDraft()
                        navigateTo(Screen.Bill)
                    },
                    onFindBill = { billsPhone = ""; navigateTo(Screen.Bills) }
                )
                Screen.ProductCenter -> ProductCenterScreen(
                    padding = insets,
                    onAddProduct = {
                        restoreNewClothDraft()
                        navigateTo(Screen.AddProduct)
                    },
                    onCatalog = { navigateTo(Screen.Catalog) },
                    onViewOrderRequests = { navigateTo(Screen.OrderRequests); syncOrderRequests() },
                    onPlaceOrder = {
                        restoreOrderDraft()
                        navigateTo(Screen.PlaceOrder)
                    }
                )
                Screen.OrderRequests -> OrderRequestsScreen(
                    padding = insets,
                    requests = orderRequests,
                    customers = cachedCustomers,
                    products = products,
                    loading = orderRequestsLoading,
                    connected = api.isConfigured && idToken != null,
                    isAdministrator = isAdministrator,
                    cancellationRequests = orderCancellationRequests,
                    onReviewCancellation = { request, approve -> reviewEntryDeletionRequest("ORDER", request, approve) },
                    onBack = { navigateBack() },
                    onSync = { syncOrderRequests() },
                    onRequestClick = { request -> selectedOrderRequest = request; navigateTo(Screen.OrderRequestDetail) },
                    onShareQuotation = { request -> shareOrderQuotation(request) },
                    sharing = sharingCustomerDocument
                )
                Screen.OrderRequestDetail -> OrderRequestDetailScreen(
                    padding = insets,
                    request = selectedOrderRequest,
                    products = products,
                    onShareQuotation = { request -> shareOrderQuotation(request) },
                    sharing = sharingCustomerDocument,
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
                        onOpenPdf = { bill -> openBillPdf(bill) },
                        onShareBill = { bill -> shareBillPdf(bill) },
                        onOpenOrderRequest = { request -> selectedOrderRequest = request; navigateTo(Screen.OrderRequestDetail) },
                        onRequestBillCancellation = { bill -> requestEntryDeletion("BILL", bill.number, "${bill.number} · ${bill.customerName} · ₹${bill.total}") },
                        onRequestOrderCancellation = { request -> requestEntryDeletion("ORDER", request.number, "${request.number} · ${request.customerName}") },
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
                    onPickContact = {
                        contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
                    },
                    onSave = {
                        val token = idToken
                        if (!savingCustomer) {
                            val local = cacheCustomerImmediately(newCustomerName, newCustomerPhone, newCustomerEmail, newCustomerAddress)
                            customerDirectoryQuery = ""
                            savingCustomer = false
                            message = "${local.name} saved on this phone${if (token != null && api.isConfigured) "; syncing to Shop." else "; it will sync when connected."}"
                            navigateBack()
                            if (token != null && api.isConfigured) operationScope.launch { syncPendingCustomerUploads(token) }
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
                        if (index >= 0) cart[index] = cart[index].copy(quantity = if (orderMode) cart[index].quantity + step else (cart[index].quantity + step).coerceAtMost(product.stock), measureQuantity = if (product.pricingUnit.isNotBlank()) null else cart[index].measureQuantity)
                        else cart.add(CartLine(product, if (orderMode) step else minOf(step, product.stock)))
                        if (!orderMode) billShouldScrollToDiscount = true
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
                    sizeVariant = productSizeVariant,
                    category = productCategory,
                    price = sellingPrice,
                    cost = unitCost,
                    stock = openingStock,
                    fractionalQuantity = fractionalQuantity,
                    unit = productUnit,
                    pricingUnit = productPricingUnit,
                    photo = productImageBitmap,
                    onTakePhoto = { productPhotoActions.takePhoto() },
                    onChoosePhoto = { productPhotoActions.choosePhoto() },
                    onName = { productName = it },
                    onSizeVariant = { productSizeVariant = it },
                    onCategory = { productCategory = it },
                    onPrice = { sellingPrice = it },
                    onCost = { unitCost = it },
                    onStock = { openingStock = it },
                    onFractionalQuantity = { fractionalQuantity = it },
                    onPricingUnit = { productPricingUnit = it },
                    onUnit = { unit ->
                        productUnit = unit
                        fractionalQuantity = unit != "PIECE"
                        if (unit != "PIECE") productPricingUnit = ""
                    },
                    onBack = { navigateBack() },
                    onClear = {
                        productName = ""
                        productSizeVariant = ""
                        productCategory = ""
                        sellingPrice = ""
                        unitCost = ""
                        openingStock = ""
                        fractionalQuantity = false
                        productUnit = "PIECE"
                        productPricingUnit = ""
                        productImageBase64 = ""
                        productImageBitmap = null
                        NewClothFormDraftCache.clear(context)
                    },
                    onSave = {
                        val token = idToken
                        if (!api.isConfigured) message = "Connect the Shop API before saving clothes."
                        else if (productCategory.isBlank()) message = "Choose a category before saving this cloth."
                        else if (token == null) beginSignIn(returnTo = Screen.AddProduct)
                        else if (!savingCloth) operationScope.launch {
                            savingCloth = true
                            try {
                                runCatchingCancellable {
                                    withContext(Dispatchers.IO) {
                                        api.createProduct(token, productName, productCategory, sellingPrice.toDouble(), unitCost.toDouble(), openingStock.toDouble(), productImageBase64, fractionalQuantity, productUnit, productPricingUnit, productSizeVariant)
                                    }
                                }.onSuccess { created ->
                                    NewClothFormDraftCache.clear(context)
                                    val newProduct = Product(created.sku, created.name, productCategory, created.price, created.stock, created.imageId, true, created.fractionalQuantity, created.unit, created.pricingUnit, created.sizeVariant)
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
                    shouldScrollToDiscount = billShouldScrollToDiscount,
                    onDiscountScrollComplete = { billShouldScrollToDiscount = false },
                    connected = api.isConfigured && idToken != null,
                    orderMode = screen == Screen.PlaceOrder,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    customerEmail = customerEmail,
                    customerAddress = customerAddress,
                    customerExists = customerExists,
                    customerSearching = customerSearching,
                    customerMatches = customerMatches,
                    customers = cachedCustomers,
                    saving = if (screen == Screen.PlaceOrder) sendingOrderRequest else savingInvoice,
                    savingCustomer = savingCustomer,
                    paymentMode = paymentMode,
                    discountValue = billDiscountValue,
                    discountEnabled = billDiscountEnabled,
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
                        if (!savingCustomer) {
                            val nameToSave = customerName.trim()
                            val phoneToSave = customerPhone.trim()
                            val emailToSave = customerEmail.trim()
                            val addressToSave = customerAddress.trim()
                            val local = cacheCustomerImmediately(nameToSave, phoneToSave, emailToSave, addressToSave)
                            customerName = local.name
                            customerPhone = local.phone
                            customerEmail = local.email
                            customerExists = true
                            customerMatches = listOf(local)
                            message = "${local.name} saved on this phone${if (token != null && api.isConfigured) "; syncing to Shop." else "; it will sync when connected."}"
                            if (token != null && api.isConfigured) operationScope.launch { syncPendingCustomerUploads(token) }
                        }
                    },
                    onPaymentMode = { mode ->
                        if (mode == "Credit" && paymentMode != "Credit") {
                            creditAmountReceived = "0"
                            creditPaymentMethod = ""
                        }
                        paymentMode = mode
                    },
                    onDiscountValue = { billDiscountValue = it },
                    onDiscountEnabled = { enabled ->
                        billDiscountEnabled = enabled
                        if (!enabled) billDiscountValue = ""
                    },
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
                                    else if (cached.stock <= 0 && !orderMode) message = "This cloth is out of stock."
                                    else {
                                        val index = cart.indexOfFirst { it.product.sku == cached.sku }
                                        val step = if (cached.fractionalQuantity) 0.1 else 1.0
                                        if (index >= 0) cart[index] = cart[index].copy(quantity = if (orderMode) cart[index].quantity + step else (cart[index].quantity + step).coerceAtMost(cached.stock), measureQuantity = if (cached.pricingUnit.isNotBlank()) null else cart[index].measureQuantity)
                                        else cart.add(CartLine(cached, if (orderMode) step else minOf(step, cached.stock)))
                                        query = scannedCode
                                    }
                                } else if (token != null && api.isConfigured) {
                                    operationScope.launch {
                                        runCatchingCancellable { withContext(Dispatchers.IO) { api.products(token, scannedCode).firstOrNull { it.sku == scannedCode } } }
                                            .onSuccess { product ->
                                                if (product == null) message = "Cloth not found in Shop. Check the QR label."
                                                else if (!product.available) message = "This cloth has been marked unavailable."
                                                else if (product.stock <= 0 && !orderMode) message = "This cloth is out of stock."
                                                else {
                                                    val merged = ProductCache.merge(products.toList(), listOf(product))
                                                    products.clear()
                                                    products.addAll(merged)
                                                    hasLiveProducts = true
                                                    ProductCache.save(context, merged)
                                                    val index = cart.indexOfFirst { it.product.sku == product.sku }
                                                    val step = if (product.fractionalQuantity) 0.1 else 1.0
                                                    if (index >= 0) cart[index] = cart[index].copy(quantity = if (orderMode) cart[index].quantity + step else (cart[index].quantity + step).coerceAtMost(product.stock), measureQuantity = if (product.pricingUnit.isNotBlank()) null else cart[index].measureQuantity)
                                                    else cart.add(CartLine(product, if (orderMode) step else minOf(step, product.stock)))
                                                }
                                            }
                                            .onFailure { message = it.message ?: "Could not look up this cloth." }
                                    }
                                } else {
                                    val product = previewProducts.firstOrNull { it.sku == scannedCode }
                                    if (product != null) cart.add(CartLine(product, if (orderMode) 1.0 else minOf(1.0, product.stock)))
                                    else message = "Cloth not found in preview catalog. Search by name or code."
                                }
                        }
                    },
                    onFindProduct = {
                        orderMode = screen == Screen.PlaceOrder
                        query = ""
                        selectedTab = 2
                        navigateTo(Screen.Search)
                    },
                    onQuantity = { sku, change ->
                        val index = cart.indexOfFirst { it.product.sku == sku }
                        if (index >= 0) {
                            val line = cart[index]
                            val newQuantity = if (orderMode) line.quantity + change else (line.quantity + change).coerceAtMost(line.product.stock)
                            if (newQuantity <= 0) cart.removeAt(index)
                            else cart[index] = line.copy(quantity = newQuantity, measureQuantity = if (line.product.pricingUnit.isNotBlank()) null else line.measureQuantity)
                        }
                    },
                    onSetQuantity = { sku, newQuantity ->
                        val index = cart.indexOfFirst { it.product.sku == sku }
                        if (index >= 0 && newQuantity >= 0 && (orderMode || newQuantity <= cart[index].product.stock)) {
                            cart[index] = cart[index].copy(quantity = newQuantity, measureQuantity = if (cart[index].product.pricingUnit.isNotBlank()) null else cart[index].measureQuantity)
                        }
                    },
                    onSetMeasureQuantity = { sku, measure ->
                        val index = cart.indexOfFirst { it.product.sku == sku }
                        if (index >= 0) cart[index] = cart[index].copy(measureQuantity = measure)
                    },
                    onSetUnitPrice = { sku, price ->
                        val index = cart.indexOfFirst { it.product.sku == sku }
                        if (index >= 0 && price > 0) cart[index] = cart[index].copy(unitPrice = price)
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
                                        val subtotal = invoiceItems.sumOf { cartLineAmount(it) }
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
                                                unitPrice = line.unitPrice,
                                                discount = 0,
                                                lineTotal = cartLineAmount(line),
                                                fractionalQuantity = line.product.fractionalQuantity,
                                                unit = line.product.unit,
                                                measureQuantity = line.measureQuantity ?: 0.0,
                                                pricingUnit = line.product.pricingUnit
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
                                    latestPdfUrl = ""
                                    latestInvoice = newBill
                                    message = "Bill saved successfully."
                                    BillFormDraftCache.clear(context)
                                    cart.clear()
                                    customerName = ""
                                    customerPhone = ""
                                    customerEmail = ""
                                    customerAddress = ""
                                    customerExists = null
                                    customerMatches = emptyList()
                                    paymentMode = ""
                                    creditAmountReceived = "0"
                                    creditPaymentMethod = ""
                                    billDiscountValue = ""
                                    billDiscountEnabled = false
                                }.onFailure { message = it.message ?: "Could not save the bill." }
                                savingInvoice = false
                            }
                        }
                        }
                    },
                    onBack = { navigateBack() },
                    onClear = {
                        if (screen == Screen.PlaceOrder) OrderFormDraftCache.clear(context)
                        else BillFormDraftCache.clear(context)
                        cart.clear()
                        customerName = ""
                        customerPhone = ""
                        customerEmail = ""
                        customerAddress = ""
                        customerExists = null
                        customerMatches = emptyList()
                        paymentMode = ""
                        creditAmountReceived = "0"
                        creditPaymentMethod = ""
                        billDiscountValue = ""
                        billDiscountEnabled = false
                        query = ""
                        catalogQuery = ""
                        newCustomerName = ""
                        newCustomerPhone = ""
                        newCustomerEmail = ""
                        newCustomerAddress = ""
                    }
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
                        onOpenPdf = { bill -> openBillPdf(bill) },
                    onShareBill = { bill -> shareBillPdf(bill) },
                    sharingBillNumber = sharingBillNumber
                )
                Screen.Payments -> PaymentsScreen(
                    padding = insets,
                    onBack = { navigateBack() },
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
                    onPickPhoneContact = {
                        contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
                    },
                    onSync = {
                        val token = idToken
                        if (!api.isConfigured || token == null) message = "Sign in to Shop before syncing balances. Saved balances are still available on this phone."
                        else if (!billsLoading) operationScope.launch {
                            LocalPaymentLog.loadAll(context).filter { !it.isVoided && it.syncStatus != "SYNCED" }.forEach { pending ->
                                val owner = cachedCustomers.firstOrNull { CustomerCache.normalizePhone(it.phone) == CustomerCache.normalizePhone(pending.customerPhone) }
                                    ?: ShopCustomer("", pending.customerName, pending.customerPhone, "")
                                syncPayment(owner, pending)
                            }
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
                    },
                    onStartCollect = {
                        collectCustomerSearch = ""
                        collectContactName = ""
                        collectContactPhone = ""
                        navigateTo(Screen.CollectPayment)
                    }
                )
                Screen.CollectPayment -> CollectPaymentScreen(
                    padding = insets,
                    customers = cachedCustomers,
                    searchQuery = collectCustomerSearch,
                    contactName = collectContactName,
                    contactPhone = collectContactPhone,
                    onSearchQuery = { query ->
                        collectCustomerSearch = query
                        collectContactName = ""
                        collectContactPhone = ""
                    },
                    onPickContact = { contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)) },
                    onBack = { navigateBack() },
                    onCreateCustomer = { name, phone, email, address ->
                        val normalizedPhone = CustomerCache.normalizePhone(phone)
                        val cachedMatch = cachedCustomers.firstOrNull {
                            CustomerCache.normalizePhone(it.phone) == normalizedPhone
                        }
                        if (cachedMatch != null) {
                            cachedMatch
                        } else {
                            val token = idToken
                            val savedToShop = token != null && api.isConfigured
                            val customer = if (savedToShop) {
                                val signedInToken = requireNotNull(token)
                                withContext(Dispatchers.IO) {
                                    api.findCustomer(signedInToken, phone) ?: api.createCustomer(signedInToken, name, phone, email, address)
                                }
                            } else {
                                cacheCustomerImmediately(name, phone, email, address)
                            }
                            val merged = CustomerCache.merge(cachedCustomers.toList(), listOf(customer))
                            cachedCustomers.clear()
                            cachedCustomers.addAll(merged)
                            CustomerCache.save(context, merged)
                            if (savedToShop) CustomerCache.removePending(context, phone)
                            customer
                        }
                    },
                    onSubmit = { customer, amount, method, note ->
                        val local = newLocalPayment("", amount, method, note, customer)
                        LocalPaymentLog.save(context, customer.phone, local)
                        madePayments = mergePaymentHistory(madePayments, LocalPaymentLog.loadAll(context))
                        syncPayment(customer, local)
                        navigateBack()
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
                    printedQrSkus = printedQrSkus,
                    onQrPrintedChange = ::setQrLabelsPrinted,
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
                    onPrintLabels = ::shareQrLabelPdf,
                    onShareCatalog = { shareCatalogPdf() },
                    onBack = { navigateBack() }
                )
                Screen.ProductDetail -> selectedCatalogProduct?.let { product ->
                    ProductDetailScreen(
                        padding = insets,
                        product = product,
                        isAdministrator = isAdministrator,
                        connected = api.isConfigured && idToken != null,
                        uploading = productPhotoActions.uploadingSku.value == product.sku,
                        imageLoader = { id ->
                            val token = idToken ?: return@ProductDetailScreen null
                            runCatchingCancellable { withContext(Dispatchers.IO) { api.productImage(token, id) } }.getOrNull()
                        },
                        onTakePhoto = {
                            if (!api.isConfigured || idToken == null) message = "Sign in to Shop before saving a product picture."
                            else { productPhotoActions.setTargetSku(product.sku); productPhotoActions.takePhoto() }
                        },
                        onChoosePhoto = {
                            if (!api.isConfigured || idToken == null) message = "Sign in to Shop before saving a product picture."
                            else { productPhotoActions.setTargetSku(product.sku); productPhotoActions.choosePhoto() }
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
                        onEdit = { name, sizeVariant, category, price, fractional, unit, complete ->
                            updateClothDetails(product, name, sizeVariant, category, price, fractional, unit, complete)
                        },
                        onBack = { navigateBack() }
                    )
                }
            }
        }
        if (savingInvoice) SalesAppSavingDialog()
        SalesAppMessageDialog(
            message = message,
            isHindi = isHindi,
            latestInvoice = latestInvoice,
            invoicePdfPreparingNumbers = invoicePdfPreparingNumbers,
            sharingBillNumber = sharingBillNumber,
            onDismiss = { latestPdfUrl = ""; latestInvoice = null; message = null },
            onDone = {
                latestPdfUrl = ""
                latestInvoice = null
                message = null
                backStack.clear()
                screen = Screen.Home
                selectedTab = -1
            },
            onShareBillPdf = ::shareBillPdf,
            onRequestBillCancellation = { bill ->
                latestPdfUrl = ""
                latestInvoice = null
                message = null
                requestEntryDeletion("BILL", bill.number, "${bill.number} · ${bill.customerName} · ₹${bill.total}")
            }
        )
        SalesAppDeletionDialog(
            target = pendingDeletionTarget,
            submitting = submittingDeletion,
            reason = deletionReason,
            onDismiss = { pendingDeletionTarget = null },
            onReasonChange = { deletionReason = it },
            onSubmit = ::submitEntryDeletionRequest
        )
        SalesAppQrDialogs(
            createdQrSku = createdQrSku,
            previewProduct = qrPreviewProduct,
            products = products,
            onFinishProductCreation = {
                NewClothFormDraftCache.clear(context)
                createdQrSku = null
                productName = ""; productSizeVariant = ""; productCategory = ""; sellingPrice = ""; unitCost = ""; openingStock = ""; fractionalQuantity = false; productUnit = "PIECE"; productPricingUnit = ""
                backStack.clear(); selectedTab = 0; screen = Screen.Home
            },
            onCreatedQrProductMissing = {
                message = "Cloth saved, but it is not in the local list yet. Use Find a cloth to print its QR label."
            },
            onPrintQrLabel = { product ->
                createdQrSku = null
                qrPreviewProduct = null
                shareQrLabelPdf(listOf(product))
            },
            onClosePreview = { qrPreviewProduct = null }
        )
        }
        }
    }
}
