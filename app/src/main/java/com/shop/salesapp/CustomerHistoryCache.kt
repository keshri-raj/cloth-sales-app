package com.shop.salesapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class CustomerHistorySnapshot(
    val sales: List<ShopInvoice>,
    val orderRequests: List<CustomerOrderRequest>
)

/** Keeps each customer's ten newest history entries on-device for up to seven days. */
object CustomerHistoryCache {
    private const val PREFS = "sales_app_customer_history"
    private const val MAX_ENTRIES = 10
    private val maxAgeMillis = TimeUnit.DAYS.toMillis(7)

    fun load(context: Context, phone: String, nowMillis: Long = System.currentTimeMillis()): CustomerHistorySnapshot? {
        val key = cacheKey(phone) ?: return null
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(key, null) ?: return null
        return runCatching {
            val snapshot = JSONObject(raw)
            val savedAt = snapshot.optLong("savedAt", 0L)
            if (savedAt <= 0L || nowMillis < savedAt || nowMillis - savedAt > maxAgeMillis) {
                prefs.edit().remove(key).apply()
                return null
            }
            val sales = mutableListOf<ShopInvoice>()
            val requests = mutableListOf<CustomerOrderRequest>()
            val entries = snapshot.optJSONArray("entries") ?: JSONArray()
            for (index in 0 until entries.length()) {
                val entry = entries.optJSONObject(index) ?: continue
                when (entry.optString("kind")) {
                    "sale" -> sales += ShopInvoice(
                        number = entry.optString("number"),
                        date = entry.optString("date"),
                        customerName = entry.optString("customerName"),
                        phone = entry.optString("phone"),
                        total = entry.optInt("total"),
                        paymentMode = entry.optString("paymentMode"),
                        pdfUrl = entry.optString("pdfUrl"),
                        paid = entry.optInt("paid", if (entry.optString("paymentMode") == "Credit") 0 else entry.optInt("total")),
                        due = entry.optInt("due", if (entry.optString("paymentMode") == "Credit") entry.optInt("total") else 0),
                        items = (0 until (entry.optJSONArray("items")?.length() ?: 0)).mapNotNull { itemIndex ->
                            val item = entry.optJSONArray("items")?.optJSONObject(itemIndex) ?: return@mapNotNull null
                            InvoiceLine(item.optString("sku"), item.optString("name"), item.optString("category"), item.optDouble("quantity"), item.optInt("unitPrice"), item.optInt("discount"), item.optInt("lineTotal"), item.optBoolean("voided", false), item.optBoolean("fractionalQuantity", false))
                        }
                    )
                    "request" -> {
                        val itemRows = entry.optJSONArray("items") ?: JSONArray()
                        val items = (0 until itemRows.length()).mapNotNull { itemIndex ->
                            val item = itemRows.optJSONObject(itemIndex) ?: return@mapNotNull null
                            OrderRequestItem(item.optString("name"), item.optString("sku"), item.optDouble("quantity"))
                        }
                        requests += CustomerOrderRequest(
                            number = entry.optString("number"),
                            date = entry.optString("date"),
                            productName = entry.optString("productName"),
                            quantity = entry.optDouble("quantity"),
                            status = entry.optString("status"),
                            notes = entry.optString("notes"),
                            items = items
                        )
                    }
                }
            }
            CustomerHistorySnapshot(sales, requests)
        }.getOrNull()
    }

    fun save(
        context: Context,
        phone: String,
        sales: List<ShopInvoice>,
        orderRequests: List<CustomerOrderRequest>,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val key = cacheKey(phone) ?: return
        val entries = buildList {
            sales.forEach { sale ->
                add(
                    JSONObject()
                        .put("kind", "sale")
                        .put("number", sale.number)
                        .put("date", sale.date)
                        .put("customerName", sale.customerName)
                        .put("phone", sale.phone)
                        .put("total", sale.total)
                        .put("paymentMode", sale.paymentMode)
                        .put("paid", sale.paid)
                        .put("due", sale.due)
                        .put("items", JSONArray(sale.items.map { item ->
                            JSONObject().put("sku", item.sku).put("name", item.name).put("category", item.category)
                                .put("quantity", item.quantity).put("unitPrice", item.unitPrice).put("discount", item.discount)
                                .put("lineTotal", item.lineTotal).put("voided", item.voided)
                                .put("fractionalQuantity", item.fractionalQuantity)
                        }))
                        .put("pdfUrl", sale.pdfUrl)
                )
            }
            orderRequests.forEach { request ->
                add(
                    JSONObject()
                        .put("kind", "request")
                        .put("number", request.number)
                        .put("date", request.date)
                        .put("productName", request.productName)
                        .put("quantity", request.quantity)
                        .put("status", request.status)
                        .put("notes", request.notes)
                        .put("items", JSONArray(request.items.map { item ->
                            JSONObject().put("name", item.name).put("sku", item.sku).put("quantity", item.quantity)
                        }))
                )
            }
        }.sortedByDescending { it.optString("date") }.take(MAX_ENTRIES)

        val snapshot = JSONObject()
            .put("savedAt", nowMillis)
            .put("entries", JSONArray(entries))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(key, snapshot.toString())
            .apply()
    }

    fun invalidate(context: Context, phone: String) {
        val key = cacheKey(phone) ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(key).apply()
    }

    private fun cacheKey(phone: String): String? {
        val normalizedPhone = CustomerCache.normalizePhone(phone)
        return if (normalizedPhone.length >= 8) "customer_$normalizedPhone" else null
    }
}
