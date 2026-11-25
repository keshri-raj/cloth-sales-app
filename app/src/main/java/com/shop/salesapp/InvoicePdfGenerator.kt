package com.shop.salesapp

import com.shop.salesapp.ui.model.priceUnit
import com.shop.salesapp.ui.model.quantityUnit
import com.shop.salesapp.ui.model.requiresMeasuredQuantity
import com.shop.salesapp.ui.model.unitShort
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.util.Locale

/** Builds a clean, print-friendly invoice PDF from a bill saved on this phone. */
object InvoicePdfGenerator {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val LEFT = 38f
    private const val RIGHT = PAGE_WIDTH - 38f
    private val navy = 0xFF102F50.toInt()
    private val muted = 0xFF586574.toInt()
    private val line = 0xFFD4DCE3.toInt()
    private val paleBlue = 0xFFEAF2F8.toInt()
    private val orange = 0xFFB85B12.toInt()
    private val green = 0xFF26734D.toInt()
    private val paper = 0xFFFFFFFF.toInt()

    fun create(bill: ShopInvoice): ByteArray {
        require(!bill.isVoided) { "This bill has been cancelled." }
        require(bill.items.isNotEmpty()) { "Bill items are not available on this phone. Sync bills and try again." }

        val document = PdfDocument()
        val output = ByteArrayOutputStream()
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var pageNumber = 0
        var y = 0f

        fun startPage() {
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page!!.canvas
            canvas!!.drawColor(paper)
            if (pageNumber == 1) {
                drawText(canvas!!, "AVANI SUTO", LEFT, 48f, 22f, navy, bold = true)
                drawText(canvas!!, "CLOTHING & TEXTILES", LEFT, 65f, 8f, muted, bold = true)
                drawText(canvas!!, "Main Road, Ward No 11 · Simri Bakhtiarpur", LEFT, 81f, 9f, muted)
                drawText(canvas!!, "Saharsa · Bihar · 852127", LEFT, 95f, 9f, muted)
                val box = RectF(399f, 28f, RIGHT, 101f)
                canvas!!.drawRoundRect(box, 8f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = paleBlue })
                drawText(canvas!!, "INVOICE", 414f, 51f, 14f, navy, bold = true)
                drawText(canvas!!, "Bill no.", 414f, 70f, 8f, muted, bold = true)
                drawText(canvas!!, bill.number.takeLast(22), RIGHT - 12f, 70f, 9f, navy, bold = true, align = Paint.Align.RIGHT)
                drawText(canvas!!, "Date  ${bill.date}", 414f, 89f, 9f, muted)
                drawRule(canvas!!, 116f)

                drawText(canvas!!, "BILL TO", LEFT, 139f, 8f, muted, bold = true)
                drawText(canvas!!, bill.customerName.ifBlank { "Customer" }, LEFT, 160f, 14f, navy, bold = true)
                if (bill.phone.isNotBlank()) drawText(canvas!!, bill.phone, LEFT, 177f, 10f, muted)
                drawText(canvas!!, "PAYMENT", 373f, 139f, 8f, muted, bold = true)
                drawText(canvas!!, bill.paymentMode, 373f, 160f, 11f, navy, bold = true)
                drawText(canvas!!, if (bill.due > 0) "Balance due  ${money(bill.due)}" else "Paid in full", 373f, 177f, 10f, if (bill.due > 0) orange else green, bold = true)
                y = 210f
            } else {
                drawText(canvas!!, "AVANI SUTO  ·  ${bill.number}", LEFT, 39f, 11f, navy, bold = true)
                drawText(canvas!!, "Continued", RIGHT, 39f, 9f, muted, align = Paint.Align.RIGHT)
                y = 70f
            }
            drawTableHeader(canvas!!, y)
            y += 30f
        }

        fun finishPage() {
            val active = page ?: return
            drawRule(active.canvas, PAGE_HEIGHT - 43f)
            drawText(active.canvas, "Avani Suto  ·  ${bill.number}", LEFT, PAGE_HEIGHT - 25f, 8f, muted)
            drawText(active.canvas, "Page $pageNumber", RIGHT, PAGE_HEIGHT - 25f, 8f, muted, align = Paint.Align.RIGHT)
            document.finishPage(active)
            page = null
            canvas = null
        }

        try {
            startPage()
            bill.items.forEachIndexed { itemIndex, item ->
                val name = item.name.ifBlank { item.sku }
                val lines = wrap(name, 31).take(2)
                val rowHeight = if (item.sku.isNotBlank()) 43f else 36f
                if (y + rowHeight > PAGE_HEIGHT - 72f) {
                    finishPage()
                    startPage()
                }
                val c = canvas!!
                drawText(c, (itemIndex + 1).toString(), 46f, y, 9f, muted)
                lines.forEachIndexed { index, text -> drawText(c, text, 72f, y + index * 13f, 10f, navy, bold = index == 0) }
                if (item.sku.isNotBlank()) drawText(c, item.sku, 72f, y + 28f, 7.5f, muted)
                val quantityLabel = if (item.requiresMeasuredQuantity() && item.measureQuantity > 0) {
                    "${formatQuantity(item.quantity)} ${unitShort(item.quantityUnit())} · ${formatQuantity(item.measureQuantity)} ${unitShort(item.priceUnit())}"
                } else {
                    "${formatQuantity(item.quantity)} ${unitShort(item.quantityUnit())}"
                }
                drawText(c, quantityLabel, 346f, y + 4f, 9f, muted, align = Paint.Align.RIGHT)
                drawText(c, money(item.unitPrice), 425f, y + 4f, 9f, muted, align = Paint.Align.RIGHT)
                drawText(c, if (item.discount > 0) money(item.discount) else "—", 480f, y + 4f, 9f, muted, align = Paint.Align.RIGHT)
                drawText(c, money(item.lineTotal), RIGHT - 2f, y + 4f, 9.5f, navy, bold = true, align = Paint.Align.RIGHT)
                y += rowHeight
                drawRule(c, y - 5f)
            }

            val needsDiscountRow = bill.items.any { it.discount > 0 }
            val summaryHeight = if (needsDiscountRow) 154f else 132f
            if (y + summaryHeight > PAGE_HEIGHT - 66f) {
                finishPage()
                startPage()
            }
            y += 12f
            val subtotal = bill.items.sumOf { it.lineTotal + it.discount }
            val discount = bill.items.sumOf { it.discount }
            if (needsDiscountRow) {
                summaryRow(canvas!!, "Total amount", money(subtotal), y); y += 21f
                summaryRow(canvas!!, "Discount", "− ${money(discount)}", y, orange); y += 23f
            }
            drawRule(canvas!!, y)
            y += 24f
            summaryRow(canvas!!, "FINAL AMOUNT", money(bill.total), y, navy, bold = true, size = 15f)
            y += 29f
            summaryRow(canvas!!, "Amount received", money(bill.paid), y); y += 20f
            summaryRow(canvas!!, "Balance due", money(bill.due), y, if (bill.due > 0) orange else green, bold = true)
            y += 37f
            drawText(canvas!!, "Thank you for shopping with Avani Suto.", LEFT, y, 10f, navy, bold = true)
            drawText(canvas!!, "Goods once sold are subject to the shop's exchange policy.", LEFT, y + 16f, 8f, muted)
            drawText(canvas!!, "Authorised signature", RIGHT, y + 21f, 8f, muted, align = Paint.Align.RIGHT)
            finishPage()
            document.writeTo(output)
            return output.toByteArray()
        } finally {
            document.close()
        }
    }

    private fun drawTableHeader(canvas: Canvas, y: Float) {
        val rect = RectF(LEFT, y - 17f, RIGHT, y + 12f)
        canvas.drawRoundRect(rect, 4f, 4f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = navy })
        drawText(canvas, "NO.", 46f, y + 1f, 8f, paper, bold = true)
        drawText(canvas, "PRODUCT", 72f, y + 1f, 8f, paper, bold = true)
        drawText(canvas, "QTY", 346f, y + 1f, 8f, paper, bold = true, align = Paint.Align.RIGHT)
        drawText(canvas, "RATE", 425f, y + 1f, 8f, paper, bold = true, align = Paint.Align.RIGHT)
        drawText(canvas, "DISC.", 480f, y + 1f, 8f, paper, bold = true, align = Paint.Align.RIGHT)
        drawText(canvas, "AMOUNT", RIGHT - 2f, y + 1f, 8f, paper, bold = true, align = Paint.Align.RIGHT)
    }

    private fun summaryRow(canvas: Canvas, label: String, value: String, y: Float, color: Int = muted, bold: Boolean = false, size: Float = 10f) {
        drawText(canvas, label, 350f, y, size, color, bold)
        drawText(canvas, value, RIGHT, y, size, color, bold, Paint.Align.RIGHT)
    }

    private fun drawRule(canvas: Canvas, y: Float) {
        canvas.drawLine(LEFT, y, RIGHT, y, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = line; strokeWidth = 0.8f })
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
        val result = mutableListOf<String>()
        var current = ""
        value.split(Regex("\\s+")).filter(String::isNotBlank).forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (candidate.length > limit && current.isNotEmpty()) { result += current; current = word } else current = candidate
        }
        if (current.isNotEmpty()) result += current
        return result.ifEmpty { listOf(value) }
    }

    private fun money(value: Int): String = "₹" + NumberFormat.getNumberInstance(Locale("en", "IN")).format(value)
    private fun formatQuantity(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')
}
