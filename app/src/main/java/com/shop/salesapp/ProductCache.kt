package com.shop.salesapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local product catalogue so search and QR lookups are immediate. */
object ProductCache {
    private const val PREFS = "sales_app_product_cache"
    private const val KEY_PRODUCTS = "products_json"

    fun load(context: Context): List<Product> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PRODUCTS, null) ?: return emptyList()
        return runCatching {
            val rows = JSONArray(raw)
            (0 until rows.length()).mapNotNull { index ->
                val row = rows.optJSONObject(index) ?: return@mapNotNull null
                val sku = row.optString("sku")
                val name = row.optString("name")
                if (sku.isBlank() || name.isBlank()) null
                else Product(
                    sku = sku,
                    name = name,
                    category = row.optString("category"),
                    price = row.optDouble("price").toInt(),
                    stock = row.optDouble("stock"),
                    imageId = row.optString("imageId"),
                    available = row.optBoolean("available", true),
                    fractionalQuantity = row.optBoolean("fractionalQuantity", false),
                    unit = row.optString("unit").ifBlank { if (row.optBoolean("fractionalQuantity", false)) "METRE" else "PIECE" },
                    pricingUnit = row.optString("pricingUnit"),
                    sizeVariant = row.optString("sizeVariant"),
                    dateAdded = row.optString("dateAdded")
                )
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, products: List<Product>) {
        val rows = JSONArray()
        products.forEach { product ->
            rows.put(
                JSONObject()
                    .put("sku", product.sku)
                    .put("name", product.name)
                    .put("category", product.category)
                    .put("price", product.price)
                    .put("stock", product.stock)
                    .put("imageId", product.imageId)
                    .put("available", product.available)
                    .put("fractionalQuantity", product.fractionalQuantity)
                    .put("unit", product.unit)
                    .put("pricingUnit", product.pricingUnit)
                    .put("sizeVariant", product.sizeVariant)
                    .put("dateAdded", product.dateAdded)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PRODUCTS, rows.toString())
            .apply()
    }

    fun merge(existing: List<Product>, updates: List<Product>): List<Product> {
        val merged = LinkedHashMap<String, Product>()
        existing.forEach { merged[it.sku] = it }
        updates.forEach { product ->
            val existing = merged[product.sku]
            merged[product.sku] = if (product.dateAdded.isBlank() && !existing?.dateAdded.isNullOrBlank()) {
                product.copy(dateAdded = existing!!.dateAdded)
            } else product
        }
        return merged.values.toList()
    }
}
