package com.shop.salesapp.ui.model

import com.shop.salesapp.CustomerFinance
import com.shop.salesapp.CustomerPaymentAllocation
import com.shop.salesapp.PaymentReceipt
import com.shop.salesapp.ShopCustomer
import com.shop.salesapp.ShopInvoice

fun addOptimisticPayment(finance: CustomerFinance, payment: PaymentReceipt): CustomerFinance {
    if (finance.payments.any { it.requestId == payment.requestId || it.receiptNumber == payment.receiptNumber }) return finance
    return finance.copy(
        totalPaid = finance.totalPaid + payment.amount,
        currentDue = (finance.currentDue - payment.amount).coerceAtLeast(0),
        payments = (listOf(payment) + finance.payments).take(50)
    )
}

fun mergeLocalPaymentLog(finance: CustomerFinance, localPayments: List<PaymentReceipt>): CustomerFinance {
    val unmatched = localPayments.filter { local ->
        local.syncStatus != "SYNCED" &&
        finance.payments.none { remote ->
            (local.requestId.isNotBlank() && remote.requestId == local.requestId) ||
                (local.receiptNumber.isNotBlank() && remote.receiptNumber == local.receiptNumber)
        }
    }
    val pendingAmount = unmatched.sumOf { it.amount }
    return finance.copy(
        totalPaid = finance.totalPaid + pendingAmount,
        currentDue = (finance.currentDue - pendingAmount).coerceAtLeast(0),
        payments = (unmatched + finance.payments).sortedByDescending { it.date }.take(50)
    )
}

fun confirmOptimisticPayment(finance: CustomerFinance, requestId: String, payment: PaymentReceipt): CustomerFinance {
    val represented = finance.payments.any {
        (requestId.isNotBlank() && it.requestId == requestId) || it.receiptNumber == payment.receiptNumber
    }
    val withoutPending = finance.payments.filterNot {
        (requestId.isNotBlank() && it.requestId == requestId) || it.receiptNumber == payment.receiptNumber
    }
    val updated = finance.copy(payments = (listOf(payment) + withoutPending).take(50))
    return if (represented) updated else addOptimisticPayment(updated, payment)
}

fun replaceLocalPayment(finance: CustomerFinance, requestId: String, payment: PaymentReceipt): CustomerFinance =
    finance.copy(payments = (listOf(payment) + finance.payments.filterNot { it.requestId == requestId }).take(50))

fun mergePaymentHistory(remote: List<PaymentReceipt>, local: List<PaymentReceipt>): List<PaymentReceipt> {
    val merged = remote.toMutableList()
    local.filter { it.syncStatus != "SYNCED" }.forEach { payment ->
        val alreadyPresent = merged.any {
            (payment.requestId.isNotBlank() && it.requestId == payment.requestId) ||
                (payment.receiptNumber.isNotBlank() && it.receiptNumber == payment.receiptNumber)
        }
        if (!alreadyPresent) merged.add(payment)
    }
    return merged.sortedByDescending { it.date }.take(200)
}

fun newLocalPayment(
    invoiceNumber: String,
    amount: Int,
    method: String,
    note: String,
    customer: ShopCustomer
): PaymentReceipt {
    val requestId = java.util.UUID.randomUUID().toString()
    val timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    val localId = "LOCAL-$requestId"
    return PaymentReceipt(
        id = localId,
        receiptNumber = localId,
        date = timestamp,
        invoiceNumber = invoiceNumber,
        amount = amount,
        method = method,
        note = note,
        syncStatus = "SYNCING",
        requestId = requestId,
        customerName = customer.name,
        customerPhone = customer.phone
    )
}

fun applyPaymentAllocations(
    invoices: List<ShopInvoice>,
    allocations: List<CustomerPaymentAllocation>,
    method: String
): List<ShopInvoice> {
    val byInvoice = allocations.associateBy { it.invoiceNumber }
    return invoices.map { invoice ->
        val allocation = byInvoice[invoice.number] ?: return@map invoice
        val due = allocation.dueAfter.coerceAtLeast(0)
        invoice.copy(paid = (invoice.total - due).coerceAtLeast(0), due = due, paymentMode = if (due == 0) method else "Credit")
    }
}
