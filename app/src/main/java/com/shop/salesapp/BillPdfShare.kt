package com.shop.salesapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object BillPdfShare {
    fun share(context: Context, bill: ShopInvoice, bytes: ByteArray) {
        require(bytes.isNotEmpty()) { "The bill PDF is empty. Sync bills and try again." }
        val directory = File(context.cacheDir, "shared-bills")
        if (!directory.exists() && !directory.mkdirs()) throw IllegalStateException("Could not prepare the bill PDF for sharing.")
        val pdf = File(directory, "${safeFileName(bill.number)}.pdf")
        pdf.writeBytes(bytes)
        shareBillToCustomer(context, pdf, bill)
    }

    private fun shareBillToCustomer(context: Context, pdf: File, bill: ShopInvoice) {
        require(pdf.isFile && pdf.length() > 0) { "The PDF could not be created." }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdf)
        val phone = whatsappNumber(bill.phone)
        val text = buildString {
            append("Hello ${bill.customerName.ifBlank { "Customer" }},\n")
            append("Thanks for ordering with us! We appreciate your business. Your bill is attached.\n\n")
            append("Bill: ${bill.number}\n")
            if (bill.date.isNotBlank()) append("Date: ${bill.date}\n")
            append("Total: ₹${bill.total}\n")
            append("Amount paid: ₹${bill.paid}\n")
            append("Amount due: ₹${bill.due}\n")
            append("Payment: ${bill.paymentMode}")
        }

        // WhatsApp currently drops EXTRA_TEXT when a PDF document is shared.
        // Copy the caption before opening WhatsApp so it can be pasted into
        // the chat message field alongside the attached invoice.
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
            ?.setPrimaryClip(ClipData.newPlainText("Bill message", text))
        Toast.makeText(context, "Message copied. Paste it in WhatsApp before sending the bill.", Toast.LENGTH_LONG).show()

        if (phone != null) {
            for (packageName in listOf("com.whatsapp", "com.whatsapp.w4b")) {
                val targeted = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Bill ${bill.number}")
                    putExtra(Intent.EXTRA_TEXT, text)
                    putExtra("jid", "$phone@s.whatsapp.net")
                    clipData = ClipData.newUri(context.contentResolver, pdf.name, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setPackage(packageName)
                }
                if (targeted.resolveActivity(context.packageManager) != null) {
                    context.startActivity(targeted)
                    return
                }
            }
        }

        sharePdfFile(
            context,
            pdf,
            "Send bill to customer",
            "Bill ${bill.number}",
            text,
            preferredPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
        )
    }

    private fun whatsappNumber(phone: String): String? {
        var digits = phone.filter(Char::isDigit)
        if (digits.startsWith('0') && digits.length == 11) digits = digits.drop(1)
        if (digits.length == 10) digits = "91$digits"
        return digits.takeIf { it.length in 8..15 }
    }

    fun sharePdfFile(
        context: Context,
        pdf: File,
        chooserTitle: String,
        subject: String,
        text: String,
        preferredPackages: List<String> = emptyList()
    ) {
        require(pdf.isFile && pdf.length() > 0) { "The PDF could not be created." }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdf)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
            clipData = ClipData.newUri(context.contentResolver, pdf.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        for (packageName in preferredPackages) {
            val targeted = Intent(send).setPackage(packageName)
            if (targeted.resolveActivity(context.packageManager) != null) {
                context.startActivity(targeted)
                return
            }
        }
        context.startActivity(Intent.createChooser(send, chooserTitle))
    }

    private fun safeFileName(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
