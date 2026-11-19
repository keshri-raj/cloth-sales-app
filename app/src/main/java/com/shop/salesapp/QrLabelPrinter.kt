package com.shop.salesapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import kotlin.math.roundToInt

object QrLabelPrinter {
    /** Creates one 50 × 25 mm page per cloth for the PSF20's 2-inch label media. */
    fun createLabelPdf(context: Context, products: List<Product>): File {
        require(products.isNotEmpty()) { "No clothes are available to export." }
        val directory = File(context.cacheDir, "qr-labels")
        if (!directory.exists() && !directory.mkdirs()) throw IllegalStateException("Could not prepare the QR label PDF.")
        val output = File(directory, "cloth_qr_labels_${System.currentTimeMillis()}.pdf")
        val pointsPerMm = 72f / 25.4f
        val pageWidth = (50f * pointsPerMm).roundToInt()
        val pageHeight = (25f * pointsPerMm).roundToInt()
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
            textSize = 6f
            typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL)
        }
        drawLabelText(canvas, product.name, title, textX, height * 0.40f, textWidth, 2)
        drawLabelText(canvas, product.sku, code, textX, height * 0.78f, textWidth, 2)
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
