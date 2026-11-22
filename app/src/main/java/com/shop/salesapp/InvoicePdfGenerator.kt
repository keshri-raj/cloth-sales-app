package com.shop.salesapp

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.util.Locale

/** Builds an invoice PDF on demand from the bill already stored on this phone. */
object InvoicePdfGenerator {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val LEFT = 42f
    private const val RIGHT = PAGE_WIDTH - 42f
    private val navy = 0xFF102F50.toInt()
    private val gray = 0xFF586574.toInt()
    private val line = 0xFFD9DEE5.toInt()
    private val orange = 0xFFB85B12.toInt()

    fun create(bill: ShopInvoice): ByteArray {
        require(!bill.isVoided) { "This bill has been cancelled." }
        require(bill.items.isNotEmpty()) { "Bill items are not available on this phone. Sync bills and try again." }

        val document = PdfDocument()
        val output = ByteArrayOutputStream()
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = 0f

        fun startPage() {
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page!!.canvas
            y = 48f
            if (pageNumber == 1) {
                drawText(canvas!!, "AVANI SUTO", LEFT, y, 21f, navy, bold = true)
                drawText(canvas!!, "INVOICE", RIGHT, y, 22f, navy, bold = true, align = Paint.Align.RIGHT)
                y += 28f
                drawText(canvas!!, "Invoice no.  ${bill.number}", LEFT, y, 11f, gray)
                drawText(canvas!!, "Date  ${bill.date}", RIGHT, y, 11f, gray, align = Paint.Align.RIGHT)
                y += 25f
                drawRule(canvas!!, y)
                y += 24f
                drawText(canvas!!, "BILL TO", LEFT, y, 9f, gray, bold = true)
                y += 19f
                drawText(canvas!!, bill.customerName.ifBlank { "Customer" }, LEFT, y, 15f, navy, bold = true)
                y += 18f
                drawText(canvas!!, bill.phone, LEFT, y, 11f, gray)
                y += 28f
                drawRule(canvas!!, y)
                y += 22f
                drawText(canvas!!, "ITEM", LEFT, y, 9f, gray, bold = true)
                drawText(canvas!!, "QTY", 365f, y, 9f, gray, bold = true, align = Paint.Align.RIGHT)
                drawText(canvas!!, "PRICE", 455f, y, 9f, gray, bold = true, align = Paint.Align.RIGHT)
                drawText(canvas!!, "AMOUNT", RIGHT, y, 9f, gray, bold = true, align = Paint.Align.RIGHT)
                y += 12f
                drawRule(canvas!!, y)
                y += 22f
            } else {
                drawText(canvas!!, "AVANI SUTO  ·  ${bill.number}", LEFT, y, 12f, navy, bold = true)
                y += 26f
            }
        }

        fun finishPage() {
            val activePage = page ?: return
            drawText(activePage.canvas, "Avani Suto  ·  ${bill.number}  ·  Page $pageNumber", LEFT, PAGE_HEIGHT - 28f, 9f, gray)
            document.finishPage(activePage)
            page = null
            canvas = null
        }

        try {
            startPage()
            bill.items.forEach { item ->
                val itemName = item.name.ifBlank { item.sku }
                val nameLines = wrap(itemName, 31)
                val rowHeight = maxOf(36f, nameLines.size * 14f + 8f)
                if (y + rowHeight > PAGE_HEIGHT - 58f) {
                    finishPage()
                    startPage()
                }
                nameLines.forEachIndexed { index, part ->
                    drawText(canvas!!, part, LEFT, y + index * 14f, 11f, 0xFF1D2936.toInt())
                }
                val quantity = if (item.pricingUnit.isNotBlank() && item.measureQuantity > 0) item.measureQuantity else item.quantity
                val unit = when (item.pricingUnit.uppercase(Locale.ROOT)) {
                    "METRE" -> "m"
                    "KG" -> "kg"
                    else -> when (item.unit.uppercase(Locale.ROOT)) {
                        "METRE" -> "m"
                        "KG" -> "kg"
                        else -> "pc"
                    }
                }
                drawText(canvas!!, "${formatQuantity(quantity)} $unit", 365f, y, 10f, gray, align = Paint.Align.RIGHT)
                drawText(canvas!!, money(item.unitPrice), 455f, y, 10f, gray, align = Paint.Align.RIGHT)
                drawText(canvas!!, money(item.lineTotal), RIGHT, y, 10f, navy, bold = true, align = Paint.Align.RIGHT)
                y += rowHeight
                drawRule(canvas!!, y - 5f)
            }

            y += 17f
            if (y + 155f > PAGE_HEIGHT - 45f) {
                finishPage()
                startPage()
            }
            val subtotal = bill.items.sumOf { it.lineTotal + it.discount }
            val discount = bill.items.sumOf { it.discount }
            if (discount > 0) {
                summaryRow(canvas!!, "Subtotal", money(subtotal), y); y += 22f
                summaryRow(canvas!!, "Discount", "− ${money(discount)}", y, orange); y += 22f
            }
            drawRule(canvas!!, y)
            y += 23f
            summaryRow(canvas!!, "Total", money(bill.total), y, navy, bold = true, size = 16f)
            y += 30f
            summaryRow(canvas!!, "Payment method", bill.paymentMode, y); y += 21f
            summaryRow(canvas!!, "Amount paid", money(bill.paid), y); y += 21f
            summaryRow(canvas!!, "Balance due", money(bill.due), y, if (bill.due > 0) orange else navy, bold = true)
            y += 40f
            drawText(canvas!!, "Thank you for ordering with us!", LEFT, y, 12f, navy, bold = true)
            finishPage()
            document.writeTo(output)
            return output.toByteArray()
        } finally {
            document.close()
        }
    }

    private fun summaryRow(canvas: Canvas, label: String, value: String, y: Float, color: Int = gray, bold: Boolean = false, size: Float = 11f) {
        drawText(canvas, label, 330f, y, size, color, bold)
        drawText(canvas, value, RIGHT, y, size, color, bold, Paint.Align.RIGHT)
    }

    private fun drawRule(canvas: Canvas, y: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = line; strokeWidth = 1f }
        canvas.drawLine(LEFT, y, RIGHT, y, paint)
    }

    private fun drawText(canvas: Canvas, value: String, x: Float, baseline: Float, size: Float, color: Int, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            textAlign = align
            typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
        }
        canvas.drawText(value, x, baseline, paint)
    }

    private fun wrap(value: String, limit: Int): List<String> {
        val words = value.split(Regex("\\s+")).filter(String::isNotBlank)
        val result = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (candidate.length > limit && current.isNotEmpty()) {
                result += current
                current = word
            } else current = candidate
        }
        if (current.isNotEmpty()) result += current
        return result.ifEmpty { listOf(value) }
    }

    private fun money(value: Int): String = "₹" + NumberFormat.getNumberInstance(Locale("en", "IN")).format(value)
    private fun formatQuantity(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')
}
