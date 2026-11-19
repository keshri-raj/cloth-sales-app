package com.shop.salesapp

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Durable payment upload: WorkManager keeps it alive across screens and app restarts. */
class PaymentSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val requestId = inputData.getString(KEY_REQUEST_ID).orEmpty()
        if (requestId.isBlank()) return Result.failure()
        val payment = LocalPaymentLog.loadAll(applicationContext).firstOrNull { it.requestId == requestId }
            ?: return Result.failure()
        val phone = payment.customerPhone
        if (phone.isBlank()) return Result.failure()

        return try {
            LocalPaymentLog.save(applicationContext, phone, payment.copy(syncStatus = "SYNCING"))
            val token = freshIdToken() ?: throw IllegalStateException("Sign in to Google to sync this payment.")
            val api = ShopApiClient(BuildConfig.SHOP_API_BASE_URL)
            if (!api.isConfigured) throw IllegalStateException("Sales App is not connected to Shop.")
            val result = withContext(Dispatchers.IO) {
                if (payment.invoiceNumber.isBlank()) {
                    api.receiveCustomerPayment(token, phone, payment.amount, payment.method, payment.note, payment.requestId)
                } else {
                    val billResult = api.receivePayment(
                        token, phone, payment.invoiceNumber, payment.amount, payment.method, payment.note, payment.requestId
                    )
                    CustomerPaymentResult(
                        billResult.receipt,
                        listOf(CustomerPaymentAllocation(payment.invoiceNumber, payment.amount, billResult.invoiceDue))
                    )
                }
            }
            val synced = result.receipt.copy(
                syncStatus = "SYNCED", customerName = payment.customerName, customerPhone = phone
            )
            LocalPaymentLog.save(applicationContext, phone, synced)
            updateCachedBills(result.allocations, synced.method)
            Result.success()
        } catch (error: Exception) {
            LocalPaymentLog.save(applicationContext, phone, payment.copy(syncStatus = "PENDING"))
            Result.retry()
        }
    }

    private suspend fun freshIdToken(): String? {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .apply { if (BuildConfig.GOOGLE_SERVER_CLIENT_ID.isNotBlank()) requestIdToken(BuildConfig.GOOGLE_SERVER_CLIENT_ID) }
            .build()
        val client = GoogleSignIn.getClient(applicationContext, options)
        return try {
            val account = suspendCancellableCoroutine { continuation ->
                client.silentSignIn().addOnCompleteListener { task ->
                    if (task.isSuccessful) continuation.resume(task.result)
                    else continuation.resumeWithException(task.exception ?: IllegalStateException("Could not refresh Google sign-in."))
                }
            }
            account.idToken
        } catch (_: Exception) {
            null
        }
    }

    private fun updateCachedBills(allocations: List<CustomerPaymentAllocation>, method: String) {
        val byInvoice = allocations.associateBy { it.invoiceNumber }
        val updated = BillCache.load(applicationContext).map { invoice ->
            val allocation = byInvoice[invoice.number] ?: return@map invoice
            val due = allocation.dueAfter.coerceAtLeast(0)
            invoice.copy(
                paid = (invoice.total - due).coerceAtLeast(0),
                due = due,
                paymentMode = if (due == 0) method else "Credit"
            )
        }
        BillCache.save(applicationContext, updated)
    }

    companion object {
        private const val KEY_REQUEST_ID = "request_id"

        fun enqueue(context: Context, requestId: String) {
            val request = OneTimeWorkRequestBuilder<PaymentSyncWorker>()
                .setInputData(Data.Builder().putString(KEY_REQUEST_ID, requestId).build())
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("payment_sync")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "payment_sync_$requestId", ExistingWorkPolicy.REPLACE, request
            )
        }
    }
}
