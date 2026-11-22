package com.shop.salesapp

import org.json.JSONArray
import org.json.JSONObject

internal const val CURRENT_INVOICE_CACHE_SCHEMA = 2

/** One-time upgrade: every cached invoice line explicitly carries its measurement fields. */
internal fun migrateInvoiceLineSchema(items: JSONArray): Boolean {
    var changed = false
    for (index in 0 until items.length()) {
        val item = items.optJSONObject(index) ?: continue
        val pricingUnit = item.optString("pricingUnit", "")
        if (!item.has("pricingUnit") || item.isNull("pricingUnit")) {
            item.put("pricingUnit", pricingUnit)
            changed = true
        }
        val existingMeasure = if (item.has("measureQuantity")) item.optDouble("measureQuantity", Double.NaN) else Double.NaN
        if (!existingMeasure.isFinite()) {
            val legacyQuantity = item.optDouble("quantity", 0.0).takeIf { it.isFinite() } ?: 0.0
            item.put("measureQuantity", if (pricingUnit.isNotBlank()) legacyQuantity else 0.0)
            changed = true
        }
    }
    return changed
}

internal fun migrateBillCacheSchema(rows: JSONArray): Boolean {
    var changed = false
    for (index in 0 until rows.length()) {
        val bill = rows.optJSONObject(index) ?: continue
        val items = bill.optJSONArray("items") ?: continue
        changed = migrateInvoiceLineSchema(items) || changed
    }
    return changed
}

internal fun JSONObject.migrateHistoryCacheSchema(): Boolean {
    val schemaVersion = optInt("schemaVersion", 0)
    val entries = optJSONArray("entries") ?: return false
    var changed = false
    for (index in 0 until entries.length()) {
        val entry = entries.optJSONObject(index) ?: continue
        if (entry.optString("kind") == "sale") {
            changed = migrateInvoiceLineSchema(entry.optJSONArray("items") ?: JSONArray()) || changed
        }
    }
    if (schemaVersion < CURRENT_INVOICE_CACHE_SCHEMA) {
        put("schemaVersion", CURRENT_INVOICE_CACHE_SCHEMA)
        return true
    }
    return changed
}
