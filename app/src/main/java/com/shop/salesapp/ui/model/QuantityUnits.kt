package com.shop.salesapp.ui.model

import com.shop.salesapp.InvoiceLine
import com.shop.salesapp.Product

fun normalizedUnit(unit: String, fractionalQuantity: Boolean = false): String = when (unit.trim().uppercase()) {
    "KG", "KGS", "KILOGRAM", "KILOGRAMS" -> "KG"
    "M", "METRE", "METRES", "METER", "METERS" -> "METRE"
    "PIECE", "PIECES", "PCS", "PC" -> "PIECE"
    else -> if (fractionalQuantity) "METRE" else "PIECE"
}

fun Product.quantityUnit(): String = normalizedUnit(unit, fractionalQuantity)
fun InvoiceLine.quantityUnit(): String = normalizedUnit(unit, fractionalQuantity)

fun unitShort(unit: String): String = when (normalizedUnit(unit)) {
    "KG" -> "kg"
    "METRE" -> "m"
    else -> "pcs"
}

fun unitEnglish(unit: String): String = when (normalizedUnit(unit)) {
    "KG" -> "kg"
    "METRE" -> "metres"
    else -> "pieces"
}

fun unitHindi(unit: String): String = when (normalizedUnit(unit)) {
    "KG" -> "किलो"
    "METRE" -> "मीटर"
    else -> "पीस"
}

fun unitPriceEnglish(unit: String): String = when (normalizedUnit(unit)) {
    "KG" -> "per kg"
    "METRE" -> "per metre"
    else -> "per piece"
}

fun unitPriceHindi(unit: String): String = when (normalizedUnit(unit)) {
    "KG" -> "प्रति किलो"
    "METRE" -> "प्रति मीटर"
    else -> "प्रति पीस"
}
