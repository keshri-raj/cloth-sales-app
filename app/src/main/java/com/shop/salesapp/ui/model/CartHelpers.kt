package com.shop.salesapp.ui.model

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

fun formatQuantity(quantity: Double): String = BigDecimal.valueOf(quantity)
    .setScale(3, RoundingMode.HALF_UP)
    .stripTrailingZeros()
    .toPlainString()

fun lineAmount(price: Int, quantity: Double): Int = (price * quantity).roundToInt()

fun cartLineAmount(line: CartLine): Int = if (line.product.pricingUnit.isNotBlank()) {
    lineAmount(line.product.price, line.measureQuantity ?: 0.0)
} else lineAmount(line.product.price, line.quantity)

fun validCartQuantity(line: CartLine, enforceStock: Boolean = true, requirePriceMeasure: Boolean = true): Boolean {
    val scaled = line.quantity * 1000
    val hasAtMostThreeDecimals = kotlin.math.abs(scaled - scaled.roundToInt()) < 0.001
    val validSaleMeasure = if (line.product.pricingUnit.isBlank() || !requirePriceMeasure) true else {
        val measure = line.measureQuantity ?: 0.0
        val scaledMeasure = measure * 1000
        measure > 0 && kotlin.math.abs(scaledMeasure - scaledMeasure.roundToInt()) < 0.001
    }
    return line.quantity > 0 && (!enforceStock || line.quantity <= line.product.stock) && hasAtMostThreeDecimals &&
        (line.product.fractionalQuantity || line.quantity % 1.0 == 0.0) && validSaleMeasure
}
