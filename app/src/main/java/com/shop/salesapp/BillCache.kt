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
                else ShopInvoice(
                    number = number,
                    date = row.optString("date"),
                    customerName = row.optString("customerName"),
                    phone = row.optString("phone"),
                    total = row.optInt("total"),
                    paymentMode = row.optString("paymentMode"),
                    pdfUrl = row.optString("pdfUrl")
                ).takeIf { it.isComplete() }
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, bills: List<ShopInvoice>) {
        val rows = JSONArray()
        bills.asSequence().filter { it.isComplete() }.take(MAX_BILLS).forEach { bill ->
            rows.put(
                JSONObject()
                    .put("number", bill.number)
                    .put("date", bill.date)
                    .put("customerName", bill.customerName)
                    .put("phone", bill.phone)
                    .put("total", bill.total)
                    .put("paymentMode", bill.paymentMode)
                    .put("pdfUrl", bill.pdfUrl)
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
