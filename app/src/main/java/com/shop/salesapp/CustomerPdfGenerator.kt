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
        val items = resolveItems(request, products)
        val amount = quotationAmount(request, products)
        return buildPdf { document, output ->
            var pageNumber = 0
            var page: PdfDocument.Page? = null
            var canvas: Canvas? = null
            var y = 0f

            fun startPage() {
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page!!.canvas
                canvas!!.drawColor(paper)
                if (pageNumber == 1) {
                    text(canvas!!, "AVANI SUTO", LEFT, 46f, 20f, navy, true)
                    text(canvas!!, "CLOTHING & TEXTILES", LEFT, 61f, 7.5f, gray, true)
                    text(canvas!!, "Main Road, Ward No 11 · Simri Bakhtiarpur", LEFT, 75f, 7.5f, gray)
                    text(canvas!!, "Saharsa · Bihar · 852127", LEFT, 88f, 7.5f, gray)
                    val box = RectF(270f, 23f, RIGHT, 94f)
                    canvas!!.drawRoundRect(box, 7f, 7f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEAF2F8.toInt() })
                    text(canvas!!, "ORDER QUOTATION", 282f, 45f, 11f, navy, true)
                    text(canvas!!, "Request ID", 282f, 63f, 7f, gray, true)
                    text(canvas!!, request.number.takeLast(20), RIGHT - 10f, 63f, 7.5f, navy, true, Paint.Align.RIGHT)
                    text(canvas!!, "Date  ${request.date.ifBlank { "—" }}", 282f, 81f, 8f, gray)
                    rule(canvas!!, 107f)
                    text(canvas!!, "CUSTOMER", LEFT, 128f, 7.5f, gray, true)
                    text(canvas!!, request.customerName.ifBlank { "Customer" }, LEFT, 148f, 12f, navy, true)
                    if (request.customerPhone.isNotBlank()) text(canvas!!, request.customerPhone, LEFT, 163f, 8.5f, gray)
                    text(canvas!!, "STATUS", 280f, 128f, 7.5f, gray, true)
                    text(canvas!!, request.status.ifBlank { "Pending" }, 280f, 148f, 9f, navy, true)
                    y = 193f
                } else {
                    text(canvas!!, "AVANI SUTO  ·  ${request.number}", LEFT, 36f, 9f, navy, true)
                    text(canvas!!, "Continued", RIGHT, 36f, 7.5f, gray, align = Paint.Align.RIGHT)
                    y = 59f
                }
                val header = RectF(LEFT, y - 13f, RIGHT, y + 9f)
                canvas!!.drawRoundRect(header, 3f, 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = navy })
                text(canvas!!, "NO.", LEFT + 5f, y + 1f, 7f, paper, true)
                text(canvas!!, "PRODUCT", LEFT + 25f, y + 1f, 7f, paper, true)
                text(canvas!!, "QTY", 275f, y + 1f, 7f, paper, true, Paint.Align.RIGHT)
                text(canvas!!, "RATE", 330f, y + 1f, 7f, paper, true, Paint.Align.RIGHT)
                text(canvas!!, "AMOUNT", RIGHT - 5f, y + 1f, 7f, paper, true, Paint.Align.RIGHT)
                y += 26f
            }

            fun finishPage() {
                val active = page ?: return
                val footer = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = line; strokeWidth = 0.8f }
                active.canvas.drawLine(LEFT, PAGE_HEIGHT - 37f, RIGHT, PAGE_HEIGHT - 37f, footer)
                text(active.canvas, "AVANI SUTO  /  CUSTOMER QUOTATION", LEFT, PAGE_HEIGHT - 20f, 7f, gray, true)
                text(active.canvas, "$pageNumber", RIGHT, PAGE_HEIGHT - 20f, 7f, wine, true, Paint.Align.RIGHT)
                document.finishPage(active)
                page = null
                canvas = null
            }

            startPage()
            items.forEachIndexed { index, item ->
                val product = products.firstOrNull { it.sku.equals(item.sku, true) }
                val itemName = item.name.ifBlank { product?.name ?: item.sku }
                val nameLines = wrap(itemName, 30).take(2)
                val rowHeight = if (item.sku.isNotBlank()) 34f else 29f
                if (y + rowHeight > PAGE_HEIGHT - 55f) {
                    finishPage()
                    startPage()
                }
                text(canvas!!, (index + 1).toString(), LEFT + 5f, y, 7.5f, gray)
                nameLines.forEachIndexed { lineIndex, value -> text(canvas!!, value, LEFT + 25f, y + lineIndex * 11f, 8f, navy, lineIndex == 0) }
                if (item.sku.isNotBlank()) text(canvas!!, item.sku, LEFT + 25f, y + 22f, 6.5f, gray)
                val quantityUnit = when (product?.unit?.uppercase(Locale.ROOT)) {
                    "KG" -> "kg"
                    "METRE" -> "m"
                    else -> "pc"
                }
                text(canvas!!, "${quantity(item.quantity)} $quantityUnit", 275f, y, 7.5f, gray, align = Paint.Align.RIGHT)
                val pricePending = product == null || product.pricingUnit.isNotBlank()
                text(canvas!!, if (pricePending) "Confirm" else "₹${money(product.price)}", 330f, y, 7.5f, gray, align = Paint.Align.RIGHT)
                val lineAmount = if (!pricePending) product!!.price * item.quantity else null
                text(canvas!!, lineAmount?.let { "₹${money(it.roundToInt())}" } ?: "Confirm", RIGHT - 5f, y, 7.5f, navy, true, Paint.Align.RIGHT)
                y += rowHeight
                rule(canvas!!, y - 4f)
            }
            if (items.isEmpty()) {
                text(canvas!!, request.productName.ifBlank { "Requested items" }, LEFT + 5f, y, 8f, navy, true)
                y += 31f
                rule(canvas!!, y - 4f)
            }

            val summaryHeight = 104f
            if (y + summaryHeight > PAGE_HEIGHT - 55f) {
                finishPage()
                startPage()
            }
            y += 12f
            val summaryBox = RectF(LEFT, y - 2f, RIGHT, y + 82f)
            canvas!!.drawRoundRect(summaryBox, 6f, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEAF2F8.toInt() })
            text(canvas!!, "AMOUNT", LEFT + 12f, y + 21f, 7.5f, gray, true)
            text(canvas!!, amount?.let { "₹${money(it)}" } ?: "To be confirmed", LEFT + 12f, y + 51f, 18f, navy, true)
            text(canvas!!, "Final amount is confirmed by the shop.", LEFT + 12f, y + 69f, 7f, gray)
            y += 104f
            text(canvas!!, "Thank you for choosing Avani Suto.", LEFT, y + 12f, 8.5f, navy, true)
            text(canvas!!, "This is a quotation for the requested items, not a payment receipt.", LEFT, y + 27f, 7f, gray)
            finishPage()
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
        items.forEach { item ->
            val product = products.firstOrNull { it.sku.equals(item.sku, true) } ?: return null
            if (product.pricingUnit.isNotBlank()) return null
            amount += product.price * item.quantity
        }
        return amount.roundToInt()
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
