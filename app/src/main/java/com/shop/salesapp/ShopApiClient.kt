package com.shop.salesapp

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
    val stock: Int,
    val imageId: String = ""
)

data class ShopCustomer(val id: String, val name: String, val phone: String, val email: String)
data class CreatedProduct(val sku: String, val name: String, val price: Int, val stock: Int, val imageId: String)
data class CreatedInvoice(val invoiceNumber: String, val total: Int, val pdfUrl: String)
data class ShopInvoice(val number: String, val date: String, val customerName: String, val phone: String, val total: Int, val paymentMode: String, val pdfUrl: String)
data class CreatedOrderRequest(val requestNumber: String, val status: String)
data class CustomerOrderRequest(val number: String, val date: String, val productName: String, val quantity: Int, val status: String, val notes: String)

class ShopApiClient(baseUrl: String) {
    private val root = baseUrl.trim().trimEnd('/')

    val isConfigured: Boolean get() = root.startsWith("https://")

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
                stock = row.optDouble("stock").toInt(),
                imageId = row.optString("imageId")
            )
        }
    }

    fun findCustomer(idToken: String, phone: String): ShopCustomer? {
        val response = request("findCustomer", idToken, JSONObject().put("phone", phone))
        val row = response.optJSONObject("customer") ?: return null
        return ShopCustomer(row.optString("id"), row.optString("name"), row.optString("phone"), row.optString("email"))
    }

    fun customers(idToken: String): List<ShopCustomer> {
        val response = request("listCustomers", idToken, JSONObject())
        val rows = response.optJSONArray("customers") ?: JSONArray()
        return (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            ShopCustomer(row.optString("id"), row.optString("name"), row.optString("phone"), row.optString("email"))
        }
    }

    fun bills(idToken: String, phone: String = ""): List<ShopInvoice> {
        val response = request("listBills", idToken, JSONObject().put("phone", phone))
        val rows = response.optJSONArray("bills") ?: JSONArray()
        return (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            ShopInvoice(
                number = row.optString("invoiceNumber"),
                date = row.optString("date"),
                customerName = row.optString("customerName"),
                phone = row.optString("phone"),
                total = row.optDouble("total").toInt(),
                paymentMode = row.optString("paymentMode"),
                pdfUrl = row.optString("pdfUrl")
            )
        }
    }

    fun orderRequests(idToken: String, phone: String): List<CustomerOrderRequest> {
        val response = request("listOrderRequests", idToken, JSONObject().put("phone", phone))
        val rows = response.optJSONArray("requests") ?: JSONArray()
        return (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            CustomerOrderRequest(row.optString("number"), row.optString("date"), row.optString("productName"), row.optInt("quantity"), row.optString("status"), row.optString("notes"))
        }
    }

    fun createCustomer(idToken: String, name: String, phone: String, email: String, address: String): ShopCustomer {
        val payload = JSONObject().put("name", name).put("phone", phone).put("email", email).put("address", address)
        val row = request("createCustomer", idToken, payload).optJSONObject("customer") ?: JSONObject()
        return ShopCustomer(row.optString("id"), row.optString("name"), row.optString("phone"), row.optString("email"))
    }

    fun productImage(idToken: String, imageId: String): ByteArray? {
        if (imageId.isBlank()) return null
        val encoded = request("productImage", idToken, JSONObject().put("imageId", imageId)).optString("data")
        if (encoded.isBlank()) return null
        return Base64.decode(encoded, Base64.DEFAULT)
    }

    fun createProduct(
        idToken: String,
        name: String,
        category: String,
        sellingPrice: Double,
        unitCost: Double,
        openingStock: Int,
        imageBase64: String = ""
    ): CreatedProduct {
        val body = JSONObject()
            .put("name", name)
            .put("category", category)
            .put("sellingPrice", sellingPrice)
            .put("unitCost", unitCost)
            .put("openingStock", openingStock)
            .put("imageBase64", imageBase64)
        val row = request("createProduct", idToken, body)
        return CreatedProduct(row.optString("sku"), row.optString("name"), row.optDouble("price").toInt(), row.optDouble("stock").toInt(), row.optString("imageId"))
    }

    fun createInvoice(
        idToken: String,
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        customerAddress: String,
        paymentMode: String,
        items: List<CartLine>
    ): CreatedInvoice {
        val lineItems = JSONArray()
        items.forEach { line ->
            lineItems.put(JSONObject().put("sku", line.product.sku).put("quantity", line.quantity).put("discount", 0))
        }
        val body = JSONObject()
            .put("customer", JSONObject().put("name", customerName).put("phone", customerPhone).put("email", customerEmail).put("address", customerAddress))
            .put("paymentMode", paymentMode)
            .put("items", lineItems)
        val row = request("createInvoice", idToken, body)
        return CreatedInvoice(row.optString("invoiceNumber"), row.optDouble("total").toInt(), row.optString("pdfUrl"))
    }

    fun createOrderRequest(
        idToken: String,
        customerName: String,
        customerPhone: String,
        productName: String,
        sku: String,
        quantity: Int,
        notes: String
    ): CreatedOrderRequest {
        val body = JSONObject()
            .put("customerName", customerName)
            .put("customerPhone", customerPhone)
            .put("productName", productName)
            .put("sku", sku)
            .put("quantity", quantity)
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
