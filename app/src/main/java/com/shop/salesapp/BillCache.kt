package com.shop.salesapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** On-device copy of bills previously synced from Shop or created in this app. */
object BillCache {
    private const val PREFS = "sales_app_bill_cache"
    private const val KEY_BILLS = "bills_json"
    private const val MAX_BILLS = 1000

    fun load(context: Context): List<ShopInvoice> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_BILLS, null) ?: return emptyList()
        return runCatching {
            val rows = JSONArray(raw)
            (0 until rows.length()).mapNotNull { index ->
                val row = rows.optJSONObject(index) ?: return@mapNotNull null
                val number = row.optString("number")
                if (number.isBlank()) null
                else {
                    val itemRows = row.optJSONArray("items") ?: JSONArray()
                    val items = (0 until itemRows.length()).mapNotNull { itemIndex ->
                        val item = itemRows.optJSONObject(itemIndex) ?: return@mapNotNull null
                        InvoiceLine(
                            sku = item.optString("sku"),
                            name = item.optString("name"),
                            category = item.optString("category"),
                            quantity = item.optDouble("quantity"),
                            unitPrice = item.optInt("unitPrice"),
                            discount = item.optInt("discount"),
                            lineTotal = item.optInt("lineTotal"),
                            voided = item.optBoolean("voided", false),
                            fractionalQuantity = item.optBoolean("fractionalQuantity", false)
                        )
                    }
                    ShopInvoice(
                    number = number,
                    date = row.optString("date"),
                    customerName = row.optString("customerName"),
                    phone = row.optString("phone"),
                    total = row.optInt("total"),
                    paymentMode = row.optString("paymentMode"),
                    pdfUrl = row.optString("pdfUrl"),
                    paid = row.optInt("paid", if (row.optString("paymentMode") == "Credit") 0 else row.optInt("total")),
                    due = row.optInt("due", if (row.optString("paymentMode") == "Credit") row.optInt("total") else 0),
                    isVoided = row.optBoolean("isVoided", false),
                    items = items
                    ).takeIf { it.isComplete() }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, bills: List<ShopInvoice>) {
        val rows = JSONArray()
        bills.asSequence().filter { it.isComplete() }.take(MAX_BILLS).forEach { bill ->
            val itemRows = JSONArray(bill.items.map { item ->
                JSONObject()
                    .put("sku", item.sku)
                    .put("name", item.name)
                    .put("category", item.category)
                    .put("quantity", item.quantity)
                    .put("unitPrice", item.unitPrice)
                    .put("discount", item.discount)
                    .put("lineTotal", item.lineTotal)
                    .put("voided", item.voided)
                    .put("fractionalQuantity", item.fractionalQuantity)
            })
            rows.put(
                JSONObject()
                    .put("number", bill.number)
                    .put("date", bill.date)
                    .put("customerName", bill.customerName)
                    .put("phone", bill.phone)
                    .put("total", bill.total)
                    .put("paymentMode", bill.paymentMode)
                    .put("paid", bill.paid)
                    .put("due", bill.due)
                    .put("isVoided", bill.isVoided)
                    .put("pdfUrl", bill.pdfUrl)
                    .put("items", itemRows)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BILLS, rows.toString())
            .apply()
    }

    fun merge(existing: List<ShopInvoice>, updates: List<ShopInvoice>): List<ShopInvoice> {
        val merged = LinkedHashMap<String, ShopInvoice>()
        existing.filter { it.isComplete() }.forEach { merged[it.number] = it }
        updates.filter { it.isComplete() }.forEach { merged[it.number] = it }
        return merged.values
            .sortedWith(compareByDescending<ShopInvoice> { it.date }.thenByDescending { it.number })
            .take(MAX_BILLS)
    }

    private fun ShopInvoice.isComplete(): Boolean =
        number.isNotBlank() &&
            CustomerCache.normalizePhone(phone).length >= 8 &&
            total > 0 &&
            paymentMode.trim() in setOf("Cash", "UPI", "Card", "Credit")
}
