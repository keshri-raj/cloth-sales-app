package com.shop.salesapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local receipt log for optimistic payment feedback and retrying uncertain API responses. */
object LocalPaymentLog {
    private const val PREFS = "sales_app_local_payments"
    private const val MAX_ENTRIES = 100

    fun load(context: Context, phone: String): List<PaymentReceipt> {
        val key = cacheKey(phone) ?: return emptyList()
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, null) ?: return emptyList()
        return runCatching {
            val rows = JSONArray(raw)
            buildList {
                for (index in 0 until rows.length()) {
                    val row = rows.optJSONObject(index) ?: continue
                    add(
                        PaymentReceipt(
                            id = row.optString("id"),
                            receiptNumber = row.optString("receiptNumber"),
                            date = row.optString("date"),
                            invoiceNumber = row.optString("invoiceNumber"),
                            amount = row.optInt("amount"),
                            method = row.optString("method"),
                            note = row.optString("note"),
                            syncStatus = row.optString("syncStatus", "PENDING"),
                            requestId = row.optString("requestId"),
                            customerName = row.optString("customerName"),
                            customerPhone = row.optString("customerPhone", phone)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun loadAll(context: Context): List<PaymentReceipt> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.all.keys.flatMap { key ->
            val phone = key.removePrefix("customer_")
            load(context, phone).map { if (it.customerPhone.isBlank()) it.copy(customerPhone = phone) else it }
        }.sortedByDescending { it.date }.take(MAX_ENTRIES)
    }

    fun save(context: Context, phone: String, receipt: PaymentReceipt) {
        val key = cacheKey(phone) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val updated = (load(context, phone).filterNot { it.requestId == receipt.requestId } + receipt)
            .takeLast(MAX_ENTRIES)
        val rows = JSONArray()
        updated.forEach { payment ->
            rows.put(
                JSONObject()
                    .put("id", payment.id)
                    .put("receiptNumber", payment.receiptNumber)
                    .put("date", payment.date)
                    .put("invoiceNumber", payment.invoiceNumber)
                    .put("amount", payment.amount)
                    .put("method", payment.method)
                    .put("note", payment.note)
                    .put("syncStatus", payment.syncStatus)
                    .put("requestId", payment.requestId)
                    .put("customerName", payment.customerName)
                    .put("customerPhone", payment.customerPhone.ifBlank { phone })
            )
        }
        prefs.edit().putString(key, rows.toString()).apply()
    }

    private fun cacheKey(phone: String): String? {
        val normalizedPhone = CustomerCache.normalizePhone(phone)
        return if (normalizedPhone.length >= 8) "customer_$normalizedPhone" else null
    }
}
