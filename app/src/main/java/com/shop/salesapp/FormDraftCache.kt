package com.shop.salesapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.shop.salesapp.ui.model.CartLine

internal data class OrderFormDraft(
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val customerAddress: String = "",
    val items: List<CartLine> = emptyList(),
    val customerConfirmed: Boolean = false
)

internal data class NewClothFormDraft(
    val name: String = "",
    val sizeVariant: String = "",
    val category: String = "",
    val price: String = "",
    val cost: String = "",
    val stock: String = "",
    val fractionalQuantity: Boolean = false,
    val unit: String = "PIECE",
    val pricingUnit: String = "",
    val photoBase64: String = ""
)

internal data class BillFormDraft(
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val customerAddress: String = "",
    val items: List<CartLine> = emptyList(),
    val customerConfirmed: Boolean = false,
    val paymentMode: String = "",
    val discountValue: String = "",
    val discountEnabled: Boolean = false,
    val creditAmountReceived: String = "0",
    val creditPaymentMethod: String = ""
)

internal object OrderFormDraftCache {
    private const val PREFS = "sales_app_form_drafts"
    private const val KEY_ORDER = "order_request_draft"

    fun load(context: Context): OrderFormDraft? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ORDER, null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            val rows = root.optJSONArray("items") ?: JSONArray()
            val items = (0 until rows.length()).mapNotNull { index ->
                val row = rows.optJSONObject(index) ?: return@mapNotNull null
                val sku = row.optString("sku")
                val name = row.optString("name")
                if (sku.isBlank() || name.isBlank()) null else {
                    val fractional = row.optBoolean("fractionalQuantity", false)
                    val product = Product(
                        sku = sku,
                        name = name,
                        category = row.optString("category"),
                        price = row.optInt("price"),
                        stock = row.optDouble("stock", 0.0),
                        imageId = row.optString("imageId"),
                        available = row.optBoolean("available", true),
                        fractionalQuantity = fractional,
                        unit = row.optString("unit").ifBlank { if (fractional) "METRE" else "PIECE" },
                        pricingUnit = row.optString("pricingUnit"),
                        sizeVariant = row.optString("sizeVariant")
                    )
                    CartLine(product, row.optDouble("quantity", 0.0), row.optDouble("measureQuantity").takeIf { row.has("measureQuantity") })
                }
            }
            OrderFormDraft(
                customerName = root.optString("customerName"),
                customerPhone = root.optString("customerPhone"),
                customerEmail = root.optString("customerEmail"),
                customerAddress = root.optString("customerAddress"),
                items = items,
                customerConfirmed = root.optBoolean("customerConfirmed", false)
            )
        }.getOrNull()
    }

    fun save(context: Context, draft: OrderFormDraft) {
        if (draft.customerName.isBlank() && draft.customerPhone.isBlank() && draft.customerEmail.isBlank() &&
            draft.customerAddress.isBlank() && draft.items.isEmpty()
        ) {
            clear(context)
            return
        }
        val rows = JSONArray()
        draft.items.forEach { line ->
            val product = line.product
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
                    .put("quantity", line.quantity)
                    .put("measureQuantity", line.measureQuantity)
            )
        }
        val root = JSONObject()
            .put("customerName", draft.customerName)
            .put("customerPhone", draft.customerPhone)
            .put("customerEmail", draft.customerEmail)
            .put("customerAddress", draft.customerAddress)
            .put("customerConfirmed", draft.customerConfirmed)
            .put("items", rows)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ORDER, root.toString()).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_ORDER).apply()
    }
}

internal object NewClothFormDraftCache {
    private const val PREFS = "sales_app_form_drafts"
    private const val KEY_CLOTH = "new_cloth_draft"

    fun load(context: Context): NewClothFormDraft? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CLOTH, null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            NewClothFormDraft(
                name = root.optString("name"),
                sizeVariant = root.optString("sizeVariant"),
                category = root.optString("category"),
                price = root.optString("price"),
                cost = root.optString("cost"),
                stock = root.optString("stock"),
                fractionalQuantity = root.optBoolean("fractionalQuantity", false),
                unit = root.optString("unit").ifBlank { "PIECE" },
                pricingUnit = root.optString("pricingUnit"),
                photoBase64 = root.optString("photoBase64")
            )
        }.getOrNull()
    }

    fun save(context: Context, draft: NewClothFormDraft) {
        if (draft.name.isBlank() && draft.sizeVariant.isBlank() && draft.category.isBlank() && draft.price.isBlank() &&
            draft.cost.isBlank() && draft.stock.isBlank() && draft.photoBase64.isBlank() &&
            !draft.fractionalQuantity && draft.unit == "PIECE" && draft.pricingUnit.isBlank()
        ) {
            clear(context)
            return
        }
        val root = JSONObject()
            .put("name", draft.name)
            .put("sizeVariant", draft.sizeVariant)
            .put("category", draft.category)
            .put("price", draft.price)
            .put("cost", draft.cost)
            .put("stock", draft.stock)
            .put("fractionalQuantity", draft.fractionalQuantity)
            .put("unit", draft.unit)
            .put("pricingUnit", draft.pricingUnit)
            .put("photoBase64", draft.photoBase64)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_CLOTH, root.toString()).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_CLOTH).apply()
    }
}

internal object BillFormDraftCache {
    private const val PREFS = "sales_app_form_drafts"
    private const val KEY_BILL = "bill_draft"

    fun load(context: Context): BillFormDraft? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_BILL, null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            val rows = root.optJSONArray("items") ?: JSONArray()
            val items = (0 until rows.length()).mapNotNull { index ->
                val row = rows.optJSONObject(index) ?: return@mapNotNull null
                val sku = row.optString("sku")
                val name = row.optString("name")
                if (sku.isBlank() || name.isBlank()) null else {
                    val fractional = row.optBoolean("fractionalQuantity", false)
                    val product = Product(
                        sku = sku,
                        name = name,
                        category = row.optString("category"),
                        price = row.optInt("price"),
                        stock = row.optDouble("stock", 0.0),
                        imageId = row.optString("imageId"),
                        available = row.optBoolean("available", true),
                        fractionalQuantity = fractional,
                        unit = row.optString("unit").ifBlank { if (fractional) "METRE" else "PIECE" },
                        pricingUnit = row.optString("pricingUnit"),
                        sizeVariant = row.optString("sizeVariant")
                    )
                    CartLine(
                        product = product,
                        quantity = row.optDouble("quantity", 0.0),
                        measureQuantity = row.optDouble("measureQuantity").takeIf { row.has("measureQuantity") },
                        unitPrice = row.optInt("unitPrice", product.price)
                    )
                }
            }
            BillFormDraft(
                customerName = root.optString("customerName"),
                customerPhone = root.optString("customerPhone"),
                customerEmail = root.optString("customerEmail"),
                customerAddress = root.optString("customerAddress"),
                items = items,
                customerConfirmed = root.optBoolean("customerConfirmed", false),
                paymentMode = root.optString("paymentMode"),
                discountValue = root.optString("discountValue"),
                discountEnabled = root.optBoolean("discountEnabled", false),
                creditAmountReceived = root.optString("creditAmountReceived", "0"),
                creditPaymentMethod = root.optString("creditPaymentMethod")
            )
        }.getOrNull()
    }

    fun save(context: Context, draft: BillFormDraft) {
        if (draft.customerName.isBlank() && draft.customerPhone.isBlank() && draft.customerEmail.isBlank() &&
            draft.customerAddress.isBlank() && draft.items.isEmpty() && draft.paymentMode.isBlank() &&
            draft.discountValue.isBlank() && !draft.discountEnabled && draft.creditAmountReceived == "0" &&
            draft.creditPaymentMethod.isBlank()
        ) {
            clear(context)
            return
        }
        val rows = JSONArray()
        draft.items.forEach { line ->
            val product = line.product
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
                    .put("quantity", line.quantity)
                    .put("measureQuantity", line.measureQuantity)
                    .put("unitPrice", line.unitPrice)
            )
        }
        val root = JSONObject()
            .put("customerName", draft.customerName)
            .put("customerPhone", draft.customerPhone)
            .put("customerEmail", draft.customerEmail)
            .put("customerAddress", draft.customerAddress)
            .put("customerConfirmed", draft.customerConfirmed)
            .put("items", rows)
            .put("paymentMode", draft.paymentMode)
            .put("discountValue", draft.discountValue)
            .put("discountEnabled", draft.discountEnabled)
            .put("creditAmountReceived", draft.creditAmountReceived)
            .put("creditPaymentMethod", draft.creditPaymentMethod)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_BILL, root.toString()).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_BILL).apply()
    }
}
