package com.shop.salesapp

import com.shop.salesapp.ui.model.CartLine
import com.shop.salesapp.ui.model.requiresMeasuredQuantity
import org.json.JSONArray
import org.json.JSONObject
import android.util.Base64
import java.net.HttpURLConnection
import java.net.URL

data class Product(
    val sku: String,
    val name: String,
    val category: String,
    val price: Int,
    val stock: Double,
    val imageId: String = "",
    val available: Boolean = true,
    val fractionalQuantity: Boolean = false,
    val unit: String = if (fractionalQuantity) "METRE" else "PIECE",
    /** Optional price basis for piece-counted items whose per-piece weight/length varies. */
    val pricingUnit: String = "",
    val sizeVariant: String = "",
    val dateAdded: String = "",
    val costPrice: Int = 0
)
data class ShopSession(val email: String, val role: String)
data class DeletionRequest(
    val id: String,
    val date: String,
    val requestedBy: String,
    val type: String,
    val recordId: String,
    val summary: String,
    val reason: String,
    val status: String,
    val reviewedBy: String = "",
    val decisionNote: String = ""
)

data class ShopCustomer(val id: String, val name: String, val phone: String, val email: String, val customerType: String = "Retail")
data class CreatedProduct(val sku: String, val name: String, val price: Int, val stock: Double, val imageId: String, val fractionalQuantity: Boolean = false, val unit: String = if (fractionalQuantity) "METRE" else "PIECE", val pricingUnit: String = "", val sizeVariant: String = "", val dateAdded: String = "", val costPrice: Int = 0)
data class CreatedInvoice(
    val invoiceNumber: String, val total: Int, val pdfUrl: String, val paid: Int, val due: Int,
    val paymentMode: String, val subtotal: Int, val discount: Int, val items: List<InvoiceLine>
)
data class InvoicePdf(val fileName: String, val mimeType: String, val bytes: ByteArray)
data class InvoiceLine(
    val sku: String,
    val name: String,
    val category: String,
    val quantity: Double,
    val unitPrice: Int,
    val discount: Int,
    val lineTotal: Int,
    val voided: Boolean = false,
    val fractionalQuantity: Boolean = false,
    val unit: String = if (fractionalQuantity) "METRE" else "PIECE",
    val measureQuantity: Double = 0.0,
    val pricingUnit: String = ""
)
data class ShopInvoice(
    val number: String,
    val date: String,
    val customerName: String,
    val phone: String,
    val total: Int,
    val paymentMode: String,
    val pdfUrl: String,
    val paid: Int = if (paymentMode.equals("Credit", ignoreCase = true)) 0 else total,
    val due: Int = if (paymentMode.equals("Credit", ignoreCase = true)) total else 0,
    val isVoided: Boolean = false,
    val items: List<InvoiceLine> = emptyList()
)
data class PaymentReceipt(
    val id: String,
    val receiptNumber: String,
    val date: String,
    val invoiceNumber: String,
    val amount: Int,
    val method: String,
    val note: String,
    val syncStatus: String = "SYNCED",
    val requestId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val isVoided: Boolean = false,
    val advanceAmount: Int = 0
)
data class BillPaymentResult(val receipt: PaymentReceipt, val invoiceDue: Int)
data class CustomerPaymentAllocation(val invoiceNumber: String, val amount: Int, val dueAfter: Int)
data class CustomerPaymentResult(val receipt: PaymentReceipt, val allocations: List<CustomerPaymentAllocation>)
data class CustomerFinance(
    val totalPurchases: Int,
    val totalPaid: Int,
    val currentDue: Int,
    val invoiceCount: Int,
    val payments: List<PaymentReceipt>,
    val advanceBalance: Int = 0
)
data class CreatedOrderRequest(val requestNumber: String, val status: String)
data class OrderRequestItem(val name: String, val sku: String, val quantity: Double)
data class CustomerOrderRequest(
    val number: String,
    val date: String,
    val productName: String,
    val quantity: Double,
    val status: String,
    val notes: String,
    val customerName: String = "",
    val customerPhone: String = "",
    val items: List<OrderRequestItem> = emptyList()
)

class ShopApiClient(baseUrl: String) {
    private val root = baseUrl.trim().trimEnd('/')

    val isConfigured: Boolean get() = root.startsWith("https://")

    fun payments(idToken: String): List<PaymentReceipt> {
        val response = request("listPayments", idToken, JSONObject())
        val rows = response.optJSONArray("payments") ?: JSONArray()
        return (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            PaymentReceipt(
                id = row.optString("id"), receiptNumber = row.optString("receiptNumber"),
                date = row.optString("date"), invoiceNumber = row.optString("invoiceNumber"),
                amount = row.optDouble("amount").toInt(), method = row.optString("method"),
                note = row.optString("note"), syncStatus = "SYNCED", requestId = row.optString("requestId"),
                customerName = row.optString("customerName"), customerPhone = row.optString("customerPhone"),
                isVoided = row.optBoolean("voided", false), advanceAmount = row.optInt("advanceAmount")
            )
        }
    }

    fun products(idToken: String, query: String): List<Product> {
        val response = request("products", idToken, JSONObject().put("query", query))
        val rows = response.optJSONArray("products") ?: JSONArray()
        return (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            Product(
                sku = row.optString("sku"),
                name = row.optString("name"),
                category = row.optString("category"),
                price = row.optDouble("price").toInt(),
                stock = row.optDouble("stock"),
                imageId = row.optString("imageId"),
                available = row.optBoolean("available", true),
                fractionalQuantity = row.optBoolean("fractionalQuantity", false),
                unit = row.optString("unit").ifBlank { if (row.optBoolean("fractionalQuantity", false)) "METRE" else "PIECE" },
                pricingUnit = row.optString("pricingUnit"),
                sizeVariant = row.optString("sizeVariant"),
                dateAdded = row.optString("dateAdded"),
                costPrice = row.optDouble("costPrice").toInt()
            )
        }
    }

    fun session(idToken: String): ShopSession {
        val response = request("session", idToken, JSONObject())
        return ShopSession(response.optString("email"), response.optString("role", "SALESPERSON"))
    }

    fun requestDeletion(idToken: String, type: String, recordId: String, reason: String): DeletionRequest {
        val row = request("requestDeletion", idToken, JSONObject()
            .put("type", type).put("recordId", recordId).put("reason", reason)).optJSONObject("request") ?: JSONObject()
        return row.toDeletionRequest()
    }

    fun deletionRequests(idToken: String, type: String): List<DeletionRequest> {
        val rows = request("listDeletionRequests", idToken, JSONObject().put("type", type)).optJSONArray("requests") ?: JSONArray()
        return (0 until rows.length()).mapNotNull { rows.optJSONObject(it)?.toDeletionRequest() }
    }

    fun reviewDeletionRequest(idToken: String, requestId: String, approve: Boolean, note: String = "") {
        request("reviewDeletionRequest", idToken, JSONObject()
            .put("requestId", requestId).put("decision", if (approve) "APPROVE" else "REJECT").put("note", note))
    }

    fun setProductAvailability(idToken: String, sku: String, available: Boolean) {
        request("setProductAvailability", idToken, JSONObject().put("sku", sku).put("available", available))
    }

    fun updateProduct(
        idToken: String,
        sku: String,
        name: String,
        category: String,
        sellingPrice: Int,
        fractionalQuantity: Boolean,
        unit: String,
        sizeVariant: String = ""
    ) {
        request("updateProduct", idToken, JSONObject()
            .put("sku", sku)
            .put("name", name)
            .put("category", category)
            .put("sellingPrice", sellingPrice)
            .put("fractionalQuantity", fractionalQuantity)
            .put("unit", unit)
            .put("sizeVariant", sizeVariant))
    }

    fun addStockAdjustment(idToken: String, sku: String, quantity: Double, reason: String) {
        request("addStockAdjustment", idToken, JSONObject()
            .put("sku", sku)
            .put("quantity", quantity)
            .put("reason", reason))
    }

    private fun JSONObject.toDeletionRequest() = DeletionRequest(
        id = optString("id"), date = optString("date"), requestedBy = optString("requestedBy"),
        type = optString("type"), recordId = optString("recordId"), summary = optString("summary"),
        reason = optString("reason"), status = optString("status"), reviewedBy = optString("reviewedBy"),
        decisionNote = optString("decisionNote")
    )

    fun findCustomer(idToken: String, phone: String): ShopCustomer? {
        val response = request("findCustomer", idToken, JSONObject().put("phone", phone))
        val row = response.optJSONObject("customer") ?: return null
        return ShopCustomer(row.optString("id"), row.optString("name"), row.optString("phone"), row.optString("email"), row.optString("customerType", "Retail"))
    }

    fun searchCustomers(idToken: String, phonePrefix: String): List<ShopCustomer> {
        val response = request("searchCustomers", idToken, JSONObject().put("phone", phonePrefix))
        val rows = response.optJSONArray("customers") ?: JSONArray()
        return (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            ShopCustomer(row.optString("id"), row.optString("name"), row.optString("phone"), row.optString("email"), row.optString("customerType", "Retail"))
        }
    }

    fun customerFinance(idToken: String, phone: String): CustomerFinance {
        val response = request("customerFinance", idToken, JSONObject().put("phone", phone))
        val summary = response.optJSONObject("summary") ?: JSONObject()
        val rows = response.optJSONArray("payments") ?: JSONArray()
        val payments = (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            PaymentReceipt(
                id = row.optString("id"), receiptNumber = row.optString("receiptNumber"), date = row.optString("date"),
                invoiceNumber = row.optString("invoiceNumber"), amount = row.optDouble("amount").toInt(),
                method = row.optString("method"), note = row.optString("note"), requestId = row.optString("requestId"),
                isVoided = row.optBoolean("voided", false), advanceAmount = row.optInt("advanceAmount")
            )
        }
        return CustomerFinance(
            totalPurchases = summary.optDouble("totalPurchases").toInt(),
            totalPaid = summary.optDouble("totalPaid").toInt(),
            currentDue = summary.optDouble("currentDue").toInt(),
            invoiceCount = summary.optInt("invoiceCount"),
            payments = payments,
            advanceBalance = summary.optInt("advanceBalance")
        )
    }

    fun customers(idToken: String): List<ShopCustomer> {
        val response = request("listCustomers", idToken, JSONObject())
        val rows = response.optJSONArray("customers") ?: JSONArray()
        return (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            ShopCustomer(row.optString("id"), row.optString("name"), row.optString("phone"), row.optString("email"), row.optString("customerType", "Retail"))
        }
    }

    fun bills(idToken: String, phone: String = ""): List<ShopInvoice> {
        val response = request("listBills", idToken, JSONObject().put("phone", phone))
        val rows = response.optJSONArray("bills") ?: JSONArray()
        return (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            val itemRows = row.optJSONArray("items") ?: JSONArray()
            val invoiceItems = (0 until itemRows.length()).mapNotNull { itemIndex ->
                val item = itemRows.optJSONObject(itemIndex) ?: return@mapNotNull null
                InvoiceLine(
                    sku = item.optString("sku"),
                    name = item.optString("name"),
                    category = item.optString("category"),
                    quantity = item.optDouble("quantity"),
                    unitPrice = item.optDouble("unitPrice").toInt(),
                    discount = item.optDouble("discount").toInt(),
                    lineTotal = item.optDouble("lineTotal").toInt(),
                    voided = item.optBoolean("voided", false),
                    fractionalQuantity = item.optBoolean("fractionalQuantity", false),
                    unit = item.optString("unit").ifBlank { if (item.optBoolean("fractionalQuantity", false)) "METRE" else "PIECE" },
                    measureQuantity = item.optDouble("measureQuantity", 0.0).takeIf { it.isFinite() } ?: 0.0,
                    pricingUnit = item.optString("pricingUnit")
                )
            }
            val paymentMode = row.optString("paymentMode")
            val total = row.optDouble("total").toInt()
            ShopInvoice(
                number = row.optString("invoiceNumber"),
                date = row.optString("date"),
                customerName = row.optString("customerName"),
                phone = row.optString("phone"),
                total = total,
                paymentMode = paymentMode,
                pdfUrl = row.optString("pdfUrl"),
                paid = row.optDouble("paid", if (paymentMode == "Credit") 0.0 else total.toDouble()).toInt(),
                due = row.optDouble("due", if (paymentMode == "Credit") total.toDouble() else 0.0).toInt(),
                isVoided = row.optBoolean("voided", false),
                items = invoiceItems
            )
        }
    }

    fun invoicePdf(idToken: String, invoiceNumber: String): InvoicePdf {
        val response = try {
            request("invoicePdf", idToken, JSONObject().put("invoiceNumber", invoiceNumber))
        } catch (error: IllegalStateException) {
            if (error.message.orEmpty().contains("Unknown Sales App action", ignoreCase = true)) {
                throw IllegalStateException("Bill sharing is not enabled on the server yet. Update Code.gs in Apps Script and deploy a new version, then try again.", error)
            }
            throw error
        }
        val encoded = response.optString("data")
        check(encoded.isNotBlank()) { "The bill PDF is unavailable. Sync bills and try again." }
        val mimeType = response.optString("mimeType", "application/pdf")
        check(mimeType == "application/pdf") { "The bill file is not a PDF." }
        return InvoicePdf(
            fileName = response.optString("fileName", "$invoiceNumber.pdf"),
            mimeType = mimeType,
            bytes = Base64.decode(encoded, Base64.DEFAULT)
        )
    }

    fun generateInvoicePdf(idToken: String, invoiceNumber: String): String {
        val response = request("generateInvoicePdf", idToken, JSONObject().put("invoiceNumber", invoiceNumber))
        return response.optString("pdfUrl").also { url ->
            check(url.isNotBlank()) { "The bill was saved, but its PDF is still being prepared." }
        }
    }

    fun orderRequests(idToken: String, phone: String): List<CustomerOrderRequest> {
        val response = request("listOrderRequests", idToken, JSONObject().put("phone", phone))
        val rows = response.optJSONArray("requests") ?: JSONArray()
        return (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            val itemRows = row.optJSONArray("items") ?: JSONArray()
            val items = (0 until itemRows.length()).mapNotNull { itemIndex ->
                val item = itemRows.optJSONObject(itemIndex) ?: return@mapNotNull null
                OrderRequestItem(
                    name = item.optString("name"),
                    sku = item.optString("sku"),
                    quantity = item.optDouble("quantity")
                )
            }
            CustomerOrderRequest(
                row.optString("number"), row.optString("date"), row.optString("productName"),
                row.optDouble("quantity"), row.optString("status"), row.optString("notes"),
                row.optString("customerName"), row.optString("customerPhone"), items
            )
        }
    }

    fun createCustomer(idToken: String, name: String, phone: String, email: String, address: String, customerType: String = "Retail"): ShopCustomer {
        val payload = JSONObject().put("name", name).put("phone", phone).put("email", email).put("address", address).put("customerType", customerType)
        val row = request("createCustomer", idToken, payload).optJSONObject("customer") ?: JSONObject()
        return ShopCustomer(row.optString("id"), row.optString("name"), row.optString("phone"), row.optString("email"), row.optString("customerType", customerType))
    }

    fun productImage(idToken: String, imageId: String): ByteArray? {
        if (imageId.isBlank()) return null
        val encoded = request("productImage", idToken, JSONObject().put("imageId", imageId)).optString("data")
        if (encoded.isBlank()) return null
        return Base64.decode(encoded, Base64.DEFAULT)
    }

    fun updateProductImage(idToken: String, sku: String, imageBase64: String): String {
        val payload = JSONObject().put("sku", sku).put("imageBase64", imageBase64)
        return request("updateProductImage", idToken, payload).optString("imageId")
    }

    fun createProduct(
        idToken: String,
        name: String,
        category: String,
        sellingPrice: Double,
        unitCost: Double,
        openingStock: Double,
        imageBase64: String = "",
        fractionalQuantity: Boolean = false,
        unit: String = if (fractionalQuantity) "METRE" else "PIECE",
        pricingUnit: String = "",
        sizeVariant: String = ""
    ): CreatedProduct {
        val body = JSONObject()
            .put("name", name)
            .put("category", category)
            .put("sellingPrice", sellingPrice)
            .put("unitCost", unitCost)
            .put("openingStock", openingStock)
            .put("fractionalQuantity", fractionalQuantity)
            .put("unit", unit)
            .put("pricingUnit", pricingUnit)
            .put("sizeVariant", sizeVariant)
            .put("imageBase64", imageBase64)
        val row = request("createProduct", idToken, body)
        return CreatedProduct(row.optString("sku"), row.optString("name"), row.optDouble("price").toInt(), row.optDouble("stock"), row.optString("imageId"), row.optBoolean("fractionalQuantity", fractionalQuantity), row.optString("unit").ifBlank { unit }, row.optString("pricingUnit", pricingUnit), row.optString("sizeVariant", sizeVariant), row.optString("dateAdded"), row.optDouble("costPrice").toInt())
    }

    fun createInvoice(
        idToken: String,
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        customerAddress: String,
        paymentMode: String,
        items: List<CartLine>,
        amountReceived: Int,
        paymentMethod: String,
        discountValue: Double
    ): CreatedInvoice {
        val lineItems = JSONArray()
        items.forEach { line ->
            lineItems.put(JSONObject().put("sku", line.product.sku).put("quantity", line.quantity).put("measureQuantity", if (line.product.requiresMeasuredQuantity()) line.measureQuantity ?: 0.0 else line.quantity).put("unitPrice", line.unitPrice).put("sellAtCost", line.sellAtCost).put("discount", 0))
        }
        val body = JSONObject()
            .put("customer", JSONObject().put("name", customerName).put("phone", customerPhone).put("email", customerEmail).put("address", customerAddress))
            .put("paymentMode", paymentMode)
            .put("amountReceived", amountReceived)
            .put("paymentMethod", paymentMethod)
            .put("discountValue", discountValue)
            .put("items", lineItems)
        val row = request("createInvoice", idToken, body)
        val createdItems = mutableListOf<InvoiceLine>()
        val responseItems = row.optJSONArray("items") ?: JSONArray()
        for (index in 0 until responseItems.length()) {
            val item = responseItems.optJSONObject(index) ?: continue
            createdItems += InvoiceLine(
                sku = item.optString("sku"), name = item.optString("name"), category = item.optString("category"),
                quantity = item.optDouble("quantity"), unitPrice = item.optDouble("unitPrice").toInt(),
                discount = item.optDouble("discount").toInt(), lineTotal = item.optDouble("lineTotal").toInt(),
                fractionalQuantity = item.optBoolean("fractionalQuantity"),
                unit = item.optString("unit").ifBlank { if (item.optBoolean("fractionalQuantity")) "METRE" else "PIECE" },
                measureQuantity = item.optDouble("measureQuantity", 0.0).takeIf { it.isFinite() } ?: 0.0,
                pricingUnit = item.optString("pricingUnit")
            )
        }
        return CreatedInvoice(
            row.optString("invoiceNumber"), row.optDouble("total").toInt(), row.optString("pdfUrl"),
            row.optDouble("paid").toInt(), row.optDouble("due").toInt(), row.optString("paymentMode"),
            row.optDouble("subtotal").toInt(), row.optDouble("discount").toInt(), createdItems
        )
    }

    fun receivePayment(idToken: String, customerPhone: String, invoiceNumber: String, amount: Int, method: String, note: String, requestId: String): BillPaymentResult {
        val body = JSONObject()
            .put("phone", customerPhone)
            .put("invoiceNumber", invoiceNumber)
            .put("amount", amount)
            .put("method", method)
            .put("note", note)
            .put("requestId", requestId)
        val response = request("receivePayment", idToken, body)
        val row = response.optJSONObject("payment") ?: JSONObject()
        val receipt = PaymentReceipt(
            id = row.optString("id"), receiptNumber = row.optString("receiptNumber"), date = row.optString("date"),
            invoiceNumber = row.optString("invoiceNumber"), amount = row.optDouble("amount").toInt(),
            method = row.optString("method"), note = row.optString("note"), requestId = row.optString("requestId", requestId)
        )
        return BillPaymentResult(receipt, response.optInt("invoiceDue"))
    }

    fun receiveCustomerPayment(idToken: String, customerPhone: String, amount: Int, method: String, note: String, requestId: String): CustomerPaymentResult {
        val body = JSONObject()
            .put("phone", customerPhone)
            .put("amount", amount)
            .put("method", method)
            .put("note", note)
            .put("requestId", requestId)
        val response = request("receiveCustomerPayment", idToken, body)
        val allocationsJson = response.optJSONArray("allocations") ?: JSONArray()
        val allocations = (0 until allocationsJson.length()).mapNotNull { index ->
            val allocation = allocationsJson.optJSONObject(index) ?: return@mapNotNull null
            CustomerPaymentAllocation(
                allocation.optString("invoiceNumber"),
                allocation.optDouble("amount").toInt(),
                allocation.optDouble("dueAfter").toInt()
            )
        }
        val receiptNumber = response.optString("receiptNumber")
        val receipt = PaymentReceipt(
            id = receiptNumber,
            receiptNumber = receiptNumber,
            date = response.optString("date"),
            invoiceNumber = allocations.joinToString(", ") { it.invoiceNumber },
            amount = response.optInt("amount", amount),
            method = response.optString("method", method),
            note = response.optString("note", note),
            requestId = response.optString("requestId", requestId),
            advanceAmount = response.optInt("advanceAmount")
        )
        return CustomerPaymentResult(receipt, allocations)
    }

    fun createOrderRequest(
        idToken: String,
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        customerAddress: String,
        items: List<CartLine>,
        notes: String
    ): CreatedOrderRequest {
        val itemRows = JSONArray().apply {
            items.forEach { line ->
                put(JSONObject()
                    .put("productName", line.product.name)
                    .put("sku", line.product.sku)
                    .put("quantity", line.quantity))
            }
        }
        val body = JSONObject()
            .put("customerName", customerName)
            .put("customerPhone", customerPhone)
            .put("customerEmail", customerEmail)
            .put("customerAddress", customerAddress)
            .put("items", itemRows)
            .put("notes", notes)
        val row = request("createOrderRequest", idToken, body)
        return CreatedOrderRequest(row.optString("requestNumber"), row.optString("status"))
    }

    private fun request(action: String, idToken: String, payload: JSONObject): JSONObject {
        check(isConfigured) { "The Shop API URL is not configured on this build." }
        val body = JSONObject()
            .put("action", action)
            .put("idToken", idToken)
            .put("payload", payload)
        var connection = newConnection(URL(root), "POST")
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            var status = connection.responseCode

            // Apps Script ContentService redirects its response to a one-time
            // script.googleusercontent.com URL. Fetch that response explicitly
            // because HttpURLConnection does not reliably replay POST bodies.
            if (status in 300..399) {
                val location = connection.getHeaderField("Location")
                    ?: throw IllegalStateException("The Shop service returned an invalid redirect.")
                connection.disconnect()
                val redirectedUrl = URL(URL(root), location)
                if (redirectedUrl.protocol != "https" || !redirectedUrl.host.endsWith("googleusercontent.com")) {
                    throw IllegalStateException("The Shop service returned an unexpected redirect.")
                }
                connection = newConnection(redirectedUrl, "GET")
                status = connection.responseCode
            }

            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(response) }.getOrElse {
                throw IllegalStateException("The Shop service returned an unreadable response.")
            }
            if (status !in 200..299 || !json.optBoolean("ok", false)) {
                throw IllegalStateException(json.optString("error").ifBlank { "Shop request failed ($status)." })
            }
            return json
        } finally {
            connection.disconnect()
        }
    }

    private fun newConnection(url: URL, method: String) = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = method
        instanceFollowRedirects = false
        connectTimeout = 12_000
        readTimeout = 30_000
        setRequestProperty("Accept", "application/json")
        if (method == "POST") {
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
    }
}
