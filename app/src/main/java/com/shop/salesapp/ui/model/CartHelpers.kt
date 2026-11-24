package com.shop.salesapp.ui.model

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

fun formatQuantity(quantity: Double): String = BigDecimal.valueOf(quantity)
    .setScale(3, RoundingMode.HALF_UP)
    .stripTrailingZeros()
    .toPlainString()

fun lineAmount(price: Int, quantity: Double): Int = (price * quantity).roundToInt()

fun cartLineAmount(line: CartLine): Int = lineAmount(line.unitPrice, line.quantity)

fun validCartQuantity(line: CartLine, enforceStock: Boolean = true): Boolean {
    val scaled = line.quantity * 1000
    val hasAtMostThreeDecimals = kotlin.math.abs(scaled - scaled.roundToInt()) < 0.001
    return line.quantity > 0 && (!enforceStock || line.quantity <= line.product.stock) && hasAtMostThreeDecimals &&
        (line.product.fractionalQuantity || line.quantity % 1.0 == 0.0)
}
