package com.shop.salesapp

import android.graphics.Canvas
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/** Creates customer-facing quotation and catalogue PDFs on demand. */
object CustomerPdfGenerator {
    private const val PAGE_WIDTH = 430
    private const val PAGE_HEIGHT = 748
    private const val LEFT = 28f
    private const val RIGHT = PAGE_WIDTH - 28f
    private val navy = 0xFF102F50.toInt()
    private val gray = 0xFF625B55.toInt()
    private val line = 0xFFD9CABC.toInt()
    private val orange = 0xFFB85B12.toInt()
    private val wine = 0xFF682C3B.toInt()
    private val paper = 0xFFFAF4EA.toInt()
    private val cardPaper = 0xFFF0E5D7.toInt()

    fun quotation(request: CustomerOrderRequest, products: List<Product>): ByteArray {
        val amount = quotationAmount(request, products)
        return buildPdf { document, output ->
            val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
            val canvas = page.canvas
            canvas.drawColor(paper)
            text(canvas, "CLOTHING & TEXTILES", LEFT, 56f, 9f, wine, true)
            text(canvas, "Avani Suto", LEFT, 122f, 43f, wine, serif = true)
            text(canvas, "ORDER REQUEST", RIGHT, 122f, 12f, wine, true, Paint.Align.RIGHT)
            rule(canvas, 151f)
            text(canvas, "CUSTOMER", LEFT, 189f, 9f, gray, true)
            text(canvas, request.customerName.ifBlank { "Customer" }, LEFT, 216f, 17f, navy, true)
            if (request.customerPhone.isNotBlank()) text(canvas, request.customerPhone, LEFT, 240f, 11f, gray)
            text(canvas, "REQUEST ID", LEFT, 293f, 9f, gray, true)
            text(canvas, request.number, LEFT, 318f, 14f, navy, true)
            text(canvas, "DATE", LEFT, 360f, 9f, gray, true)
            text(canvas, request.date.ifBlank { "—" }, LEFT, 385f, 12f, navy)
            val amountBox = RectF(LEFT, 444f, RIGHT, 555f)
            canvas.drawRoundRect(amountBox, 8f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = wine })
            text(canvas, "AMOUNT", LEFT + 18f, 481f, 10f, paper, true)
            text(canvas, amount?.let { "₹${money(it)}" } ?: "To be confirmed", LEFT + 18f, 526f, 24f, paper, true)
            text(canvas, "Please contact the shop to confirm the final amount.", LEFT, 593f, 10f, gray)
            text(canvas, "Thank you for choosing Avani Suto.", LEFT, 617f, 11f, wine, true)
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = line; strokeWidth = 0.8f }
            canvas.drawLine(LEFT, PAGE_HEIGHT - 40f, RIGHT, PAGE_HEIGHT - 40f, footerPaint)
            text(canvas, "AVANI SUTO  /  CUSTOMER QUOTATION", LEFT, PAGE_HEIGHT - 21f, 7.5f, gray, true)
            document.finishPage(page)
            document.writeTo(output)
            output.toByteArray()
        }
    }

    fun catalog(products: List<Product>, photos: Map<String, Bitmap> = emptyMap()): ByteArray {
        val available = products.filter { it.available }.sortedBy { it.name.lowercase(Locale.ROOT) }
        require(available.isNotEmpty()) { "There are no available products to share in the catalogue." }
        return buildPdf { document, output ->
            val groups = available.groupBy { categoryTitle(it.category) }
            val productPages = groups.values.sumOf { (it.size + 1) / 2 }
            val totalPages = 1 + productPages
            fun beginPage(number: Int): PdfDocument.Page {
                val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, number).create())
                page.canvas.drawColor(paper)
                return page
            }
            fun footer(canvas: Canvas, number: Int) {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = line; strokeWidth = 0.8f }
                canvas.drawLine(LEFT, PAGE_HEIGHT - 40f, RIGHT, PAGE_HEIGHT - 40f, paint)
                text(canvas, "AVANI SUTO  /  CLOTHING & TEXTILES", LEFT, PAGE_HEIGHT - 21f, 7.5f, gray, true)
                text(canvas, "%02d / %02d".format(number, totalPages), RIGHT, PAGE_HEIGHT - 21f, 8f, wine, true, Paint.Align.RIGHT)
            }
            fun image(canvas: Canvas, product: Product, box: RectF) {
                val bitmap = photos[product.sku]
                if (bitmap == null || bitmap.isRecycled) {
                    val paints = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardPaper }
                    canvas.drawRoundRect(box, 4f, 4f, paints)
                    text(canvas, product.name.take(1).uppercase(Locale.ROOT), box.centerX(), box.centerY() + 15f, 48f, wine, true, Paint.Align.CENTER)
                    return
                }
                val bitmapRatio = bitmap.width.toFloat() / bitmap.height
                val boxRatio = box.width() / box.height()
                val source = if (bitmapRatio > boxRatio) {
                    val width = (bitmap.height * boxRatio).toInt().coerceAtLeast(1)
                    val left = (bitmap.width - width) / 2
                    Rect(left, 0, left + width, bitmap.height)
                } else {
                    val height = (bitmap.width / boxRatio).toInt().coerceAtLeast(1)
                    val top = (bitmap.height - height) / 2
                    Rect(0, top, bitmap.width, top + height)
                }
                canvas.drawBitmap(bitmap, source, box, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            }
            val coverPage = beginPage(1)
            val cover = coverPage.canvas
            text(cover, "CLOTHING & TEXTILES", LEFT, 48f, 9f, wine, true)
            text(cover, "Avani Suto", LEFT, 116f, 45f, wine, serif = true)
            text(cover, "A collection chosen for you", LEFT, 141f, 12f, gray)
            val first = available[0]
            val second = available.getOrNull(1) ?: first
            val third = available.getOrNull(2) ?: second
            image(cover, first, RectF(LEFT, 165f, RIGHT, 354f))
            image(cover, second, RectF(LEFT, 363f, 210f, 472f))
            image(cover, third, RectF(219f, 363f, RIGHT, 472f))
            val browse = RectF(LEFT, 488f, RIGHT, 671f)
            cover.drawRect(browse, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = wine })
            text(cover, "Browse the collection", LEFT + 16f, 551f, 17f, paper, serif = true)
            groups.keys.take(4).forEachIndexed { index, category ->
                val baseline = 578f + index * 21f
                text(cover, category.take(34), LEFT + 16f, baseline, 10f, paper)
                val page = 2 + groups.keys.take(index).sumOf { key -> ((groups[key]?.size ?: 0) + 1) / 2 }
                text(cover, "%02d".format(page), RIGHT - 14f, baseline, 10f, paper, true, Paint.Align.RIGHT)
                if (index < minOf(groups.size, 4) - 1) {
                    cover.drawLine(LEFT + 16f, baseline + 8f, RIGHT - 14f, baseline + 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66FAF4EA })
                }
            }
            footer(cover, 1)
            document.finishPage(coverPage)

            var pageNo = 1
            groups.forEach { (category, categoryProducts) ->
                categoryProducts.chunked(2).forEachIndexed { batchIndex, batch ->
                    pageNo++
                    val productPage = beginPage(pageNo)
                    val canvas = productPage.canvas
                    text(canvas, "AVANI SUTO", LEFT, 35f, 8.5f, wine, true)
                    text(canvas, category, LEFT, 80f, 29f, wine, serif = true)
                    text(canvas, "Selected pieces from our collection", LEFT, 103f, 10f, gray)
                    batch.forEachIndexed { index, product ->
                        val top = 130f + index * 247f
                        val bottom = top + 231f
                        val imageBox = RectF(LEFT, top, RIGHT, top + 174f)
                        image(canvas, product, imageBox)
                        val details = RectF(LEFT, top + 174f, RIGHT, bottom)
                        canvas.drawRect(details, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardPaper })
                        text(canvas, product.sku, LEFT + 12f, top + 192f, 8f, wine, true)
                        text(canvas, product.name.take(32), LEFT + 12f, top + 217f, 16f, 0xFF26231F.toInt(), serif = true)
                        val priceUnit = product.pricingUnit.ifBlank { product.unit }
                        val suffix = if (priceUnit.equals("PIECE", true) || priceUnit.isBlank()) "" else " / ${priceUnit.lowercase(Locale.ROOT)}"
                        text(canvas, "₹${money(product.price)}$suffix", RIGHT - 12f, top + 217f, 12f, wine, true, Paint.Align.RIGHT)
                    }
                    val footerTop = 630f
                    val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = line; strokeWidth = 0.8f }
                    canvas.drawLine(LEFT, footerTop, RIGHT, footerTop, divider)
                    text(canvas, "Ask about price & availability", LEFT, footerTop + 24f, 12f, wine, true)
                    text(canvas, "Send the item code when you message the shop.", LEFT, footerTop + 43f, 9f, gray)
                    footer(canvas, pageNo)
                    document.finishPage(productPage)
                }
            }
            document.writeTo(output); output.toByteArray()
        }
    }

    private fun resolveItems(request: CustomerOrderRequest, products: List<Product>): List<OrderRequestItem> = request.items.ifEmpty {
        request.productName.split(", ").mapNotNull { summary ->
            val match = Regex("^(.*?) ×([0-9]+(?:\\.[0-9]+)?)$").matchEntire(summary.trim()) ?: return@mapNotNull null
            val name = match.groupValues[1].trim()
            val quantity = match.groupValues[2].toDoubleOrNull() ?: return@mapNotNull null
            val product = products.firstOrNull { it.name.equals(name, true) }
            OrderRequestItem(name, product?.sku.orEmpty(), quantity)
        }
    }

    private fun quotationAmount(request: CustomerOrderRequest, products: List<Product>): Int? {
        val items = resolveItems(request, products)
        if (items.isEmpty()) return null
        var amount = 0.0
        var hasPricedItem = false
        items.forEach { item ->
            val product = products.firstOrNull { it.sku.equals(item.sku, true) } ?: return@forEach
            if (product.pricingUnit.isNotBlank()) return@forEach
            amount += product.price * item.quantity
            hasPricedItem = true
        }
        return amount.roundToInt().takeIf { hasPricedItem }
    }

    private fun categoryTitle(value: String): String = value.substringBefore(" · ").trim().ifBlank { "Collection" }

    private fun buildPdf(write: (PdfDocument, ByteArrayOutputStream) -> ByteArray): ByteArray {
        val document = PdfDocument()
        return try { write(document, ByteArrayOutputStream()) } finally { document.close() }
    }

    private fun rule(canvas: Canvas, y: Float) {
        canvas.drawLine(LEFT, y, RIGHT, y, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = line; strokeWidth = 1f })
    }

    private fun text(canvas: Canvas, value: String, x: Float, baseline: Float, size: Float, color: Int, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT, serif: Boolean = false) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; this.color = color; textAlign = align
            typeface = when {
                serif && bold -> Typeface.create("serif", Typeface.BOLD)
                serif -> Typeface.create("serif", Typeface.NORMAL)
                bold -> Typeface.create("sans-serif", Typeface.BOLD)
                else -> Typeface.create("sans-serif", Typeface.NORMAL)
            }
        }
        canvas.drawText(value, x, baseline, paint)
    }

    private fun wrap(value: String, limit: Int): List<String> {
        val result = mutableListOf<String>(); var current = ""
        value.split(Regex("\\s+")).filter(String::isNotBlank).forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (candidate.length > limit && current.isNotEmpty()) { result += current; current = word } else current = candidate
        }
        if (current.isNotEmpty()) result += current
        return result.ifEmpty { listOf(value) }
    }

    private fun money(value: Int): String = NumberFormat.getNumberInstance(Locale("en", "IN")).format(value)
    private fun quantity(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.ROOT, "%.3f", value).trimEnd('0').trimEnd('.')
}
