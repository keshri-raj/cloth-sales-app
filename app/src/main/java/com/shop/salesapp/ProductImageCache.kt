package com.shop.salesapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.File
import java.security.MessageDigest

/** On-device image cache keyed by product SKU and the current Drive image ID. */
object ProductImageCache {
    private val memory = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
    }

    fun memoryHit(sku: String, imageId: String): Bitmap? {
        if (sku.isBlank() || imageId.isBlank()) return null
        return memory.get(cacheKey(sku, imageId))
    }

    fun load(context: Context, sku: String, imageId: String): Bitmap? {
        if (sku.isBlank() || imageId.isBlank()) return null
        val key = cacheKey(sku, imageId)
        memory.get(key)?.let { return it }
        val bitmap = BitmapFactory.decodeFile(imageFile(context, sku, imageId).absolutePath) ?: return null
        memory.put(key, bitmap)
        return bitmap
    }

    fun save(context: Context, sku: String, imageId: String, bytes: ByteArray): Bitmap? {
        if (sku.isBlank() || imageId.isBlank() || bytes.isEmpty()) return null
        val directory = File(context.filesDir, "product-images")
        if (!directory.exists() && !directory.mkdirs()) return null
        val file = imageFile(context, sku, imageId)
        runCatching { file.writeBytes(bytes) }.getOrElse { return null }
        val skuPrefix = digest(sku) + "-"
        directory.listFiles()?.forEach { oldFile ->
            if (oldFile.name.startsWith(skuPrefix) && oldFile != file) oldFile.delete()
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        memory.put(cacheKey(sku, imageId), bitmap)
        return bitmap
    }

    private fun imageFile(context: Context, sku: String, imageId: String): File {
        val prefix = digest(sku)
        return File(File(context.filesDir, "product-images"), "$prefix-${digest(imageId)}.jpg")
    }

    private fun cacheKey(sku: String, imageId: String): String = "$sku|$imageId"

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .take(12)
        .joinToString("") { byte -> "%02x".format(byte) }
}
