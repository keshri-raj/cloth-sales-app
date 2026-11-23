package com.shop.salesapp

import android.content.Context
import android.content.ContentValues
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import kotlin.math.roundToInt

object QrLabelPrinter {
    /** Creates one 48 × 26 mm page per cloth for the PSF-20 label roll. */
    fun createLabelPdf(context: Context, products: List<Product>): File {
        require(products.isNotEmpty()) { "No clothes are available to export." }
        val directory = File(context.cacheDir, "qr-labels")
        if (!directory.exists() && !directory.mkdirs()) throw IllegalStateException("Could not prepare the QR label PDF.")
        val output = File(directory, "Avani-Suto-QR-Labels.pdf")
        val pointsPerMm = 72f / 25.4f
        val pageWidth = (48f * pointsPerMm).roundToInt()
        val pageHeight = (26f * pointsPerMm).roundToInt()
        val document = PdfDocument()
        try {
            products.distinctBy { it.sku }.forEachIndexed { index, product ->
                val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create())
                drawProductLabel(page.canvas, pageWidth.toFloat(), pageHeight.toFloat(), product)
                document.finishPage(page)
            }
            output.outputStream().use(document::writeTo)
        } finally {
            document.close()
        }
        return output
    }

    /** Copies a generated PDF into the public Downloads collection for the printer app's file picker. */
    fun saveToDownloads(context: Context, pdf: File): String {
        require(pdf.isFile && pdf.length() > 0) { "The QR label PDF is empty." }
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "Open the QR label PDF from Share on this Android version."
        }
        val name = pdf.name
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Avani Suto")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        deletePreviousLabelPdfs(context)
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Could not save the QR label PDF to Downloads.")
        try {
            resolver.openOutputStream(uri, "w")?.use { output -> pdf.inputStream().use { it.copyTo(output) } }
                ?: throw IllegalStateException("Could not write the QR label PDF to Downloads.")
            val ready = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            resolver.update(uri, ready, null, null)
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
        return "Downloads/Avani Suto/$name"
    }

    /** Removes only this app's older QR label PDFs, leaving all other Downloads files alone. */
    private fun deletePreviousLabelPdfs(context: Context) {
        val resolver = context.contentResolver
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/Avani Suto/"
        val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME)
        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
            arrayOf(relativePath),
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
            val oldUris = buildList {
                while (cursor.moveToNext()) {
                    val oldName = cursor.getString(nameColumn) ?: continue
                    val isGeneratedLabel = oldName.equals("Avani-Suto-QR-Labels.pdf", ignoreCase = true) ||
                        (oldName.startsWith("avani_suto_qr_labels_", ignoreCase = true) && oldName.endsWith(".pdf", ignoreCase = true))
                    if (isGeneratedLabel) {
                        add(ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cursor.getLong(idColumn)))
                    }
                }
            }
            oldUris.forEach { resolver.delete(it, null, null) }
        }
    }

    private fun drawProductLabel(canvas: Canvas, width: Float, height: Float, product: Product) {
        val padding = 4f
        val qrSize = height - padding * 2
        val qr = createQrBitmap(product.sku)
        canvas.drawBitmap(qr, null, RectF(padding, padding, padding + qrSize, padding + qrSize), null)
        qr.recycle()

        val textX = padding + qrSize + 5f
        val textWidth = width - textX - padding
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 7.4f
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
        val code = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF222222.toInt()
            textSize = 5.6f
            typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL)
        }
        val price = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 7.2f
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
        drawLabelText(canvas, product.name, title, textX, height * 0.28f, textWidth, 2)
        canvas.drawText("₹${product.price}", textX, height * 0.60f, price)
        drawLabelText(canvas, product.sku, code, textX, height * 0.86f, textWidth, 1)
    }

}

private fun drawLabelText(canvas: Canvas, value: String, paint: Paint, x: Float, firstBaseline: Float, width: Float, maxLines: Int) {
    val words = value.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    var line = ""
    var lineIndex = 0
    for (word in words) {
        val candidate = if (line.isBlank()) word else "$line $word"
        if (paint.measureText(candidate) > width && line.isNotBlank()) {
            canvas.drawText(line, x, firstBaseline + lineIndex * paint.textSize * 1.15f, paint)
            line = word
            lineIndex++
            if (lineIndex >= maxLines) return
        } else line = candidate
    }
    if (line.isNotBlank() && lineIndex < maxLines) canvas.drawText(line, x, firstBaseline + lineIndex * paint.textSize * 1.15f, paint)
}
