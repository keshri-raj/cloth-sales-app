package com.shop.salesapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PendingCustomer(
    val name: String,
    val phone: String,
    val email: String,
    val address: String
)

/** Small on-device index of customer details for fast phone-prefix suggestions. */
object CustomerCache {
    private const val PREFS = "sales_app_customer_cache"
    private const val KEY_CUSTOMERS = "customers_json"
    private const val KEY_PENDING = "pending_customers_json"

    fun load(context: Context): List<ShopCustomer> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOMERS, null) ?: return emptyList()
        return runCatching {
            val rows = JSONArray(raw)
            (0 until rows.length()).mapNotNull { index ->
                val row = rows.optJSONObject(index) ?: return@mapNotNull null
                val phone = row.optString("phone")
                val name = row.optString("name")
                if (phone.isBlank() || name.isBlank()) null
                else ShopCustomer(row.optString("id"), name, phone, row.optString("email"))
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, customers: List<ShopCustomer>) {
        val rows = JSONArray()
        customers.forEach { customer ->
            rows.put(
                JSONObject()
                    .put("id", customer.id)
                    .put("name", customer.name)
                    .put("phone", customer.phone)
                    .put("email", customer.email)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOMERS, rows.toString())
            .apply()
    }

    fun loadPending(context: Context): List<PendingCustomer> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PENDING, null) ?: return emptyList()
        return runCatching {
            val rows = JSONArray(raw)
            (0 until rows.length()).mapNotNull { index ->
                val row = rows.optJSONObject(index) ?: return@mapNotNull null
                val name = row.optString("name").trim()
                val phone = row.optString("phone").trim()
                if (name.isBlank() || normalizePhone(phone).length < 8) null
                else PendingCustomer(name, phone, row.optString("email"), row.optString("address"))
            }
        }.getOrDefault(emptyList())
    }

    fun enqueue(context: Context, customer: PendingCustomer) {
        val pending = loadPending(context).filterNot {
            normalizePhone(it.phone) == normalizePhone(customer.phone)
        } + customer
        val rows = JSONArray()
        pending.forEach {
            rows.put(JSONObject()
                .put("name", it.name)
                .put("phone", it.phone)
                .put("email", it.email)
                .put("address", it.address))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PENDING, rows.toString())
            .apply()
    }

    fun removePending(context: Context, phone: String) {
        val remaining = loadPending(context).filterNot {
            normalizePhone(it.phone) == normalizePhone(phone)
        }
        val rows = JSONArray()
        remaining.forEach {
            rows.put(JSONObject()
                .put("name", it.name)
                .put("phone", it.phone)
                .put("email", it.email)
                .put("address", it.address))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PENDING, rows.toString())
            .apply()
    }

    fun merge(existing: List<ShopCustomer>, updates: List<ShopCustomer>): List<ShopCustomer> {
        val merged = LinkedHashMap<String, ShopCustomer>()
        existing.forEach { merged[normalizePhone(it.phone)] = it }
        updates.forEach { merged[normalizePhone(it.phone)] = it }
        return merged.values.filter { normalizePhone(it.phone).isNotBlank() }
    }

    fun normalizePhone(phone: String): String {
        val digits = phone.filter(Char::isDigit)
        return if (digits.length == 12 && digits.startsWith("91")) digits.takeLast(10) else digits
    }
}
