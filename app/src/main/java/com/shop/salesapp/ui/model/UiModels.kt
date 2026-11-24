package com.shop.salesapp.ui.model

import com.shop.salesapp.CustomerOrderRequest
import com.shop.salesapp.Product
import com.shop.salesapp.ShopInvoice

data class DeletionTarget(val type: String, val recordId: String, val summary: String)

data class CartLine(
    val product: Product,
    val quantity: Double,
    val measureQuantity: Double? = null,
    val unitPrice: Int = product.price,
    val sellAtCost: Boolean = false
)

data class CustomerHistoryItem(
    val key: String,
    val date: String,
    val invoice: ShopInvoice? = null,
    val orderRequest: CustomerOrderRequest? = null
)
