package com.shop.salesapp.ui.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun isWithinBillCancellationWindow(date: String, today: LocalDate = LocalDate.now()): Boolean {
    val created = runCatching { LocalDate.parse(date.take(10)) }.getOrNull() ?: return false
    val ageInDays = ChronoUnit.DAYS.between(created, today)
    return ageInDays in 0..7
}

fun isOrderRequestCancellable(status: String): Boolean = status.equals("Pending", ignoreCase = true)
