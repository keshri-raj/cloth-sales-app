package com.shop.salesapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.FileProvider
import com.shop.salesapp.ui.model.runCatchingCancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

internal data class ProductPhotoActions(
    val uploadingSku: State<String?>,
    val setTargetSku: (String?) -> Unit,
    val choosePhoto: () -> Unit,
    val takePhoto: () -> Unit
)

@Composable
internal fun rememberProductPhotoActions(
    context: Context,
    operationScope: CoroutineScope,
    api: ShopApiClient,
    idToken: String?,
    products: List<Product>,
    onFormImageSelected: (String, Bitmap) -> Unit,
    onProductsUpdated: (List<Product>) -> Unit,
    onCatalogProductUpdated: (Product?) -> Unit,
    onMessage: (String) -> Unit
): ProductPhotoActions {
    val targetSku = remember { mutableStateOf<String?>(null) }
    val pendingPhotoUri = remember { mutableStateOf<Uri?>(null) }
    val uploadingSku = remember { mutableStateOf<String?>(null) }

    fun processPhoto(original: Bitmap) {
        runCatching {
            val scale = minOf(1f, 900f / maxOf(original.width, original.height))
            val resized = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt().coerceAtLeast(1),
                    (original.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else original
            val output = ByteArrayOutputStream()
            var quality = 82
            do {
                output.reset()
                resized.compress(Bitmap.CompressFormat.JPEG, quality, output)
                quality -= 8
            } while (output.size() > 110_000 && quality >= 42)
            Triple(Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP), resized, original)
        }.onSuccess { (imageBase64, resized, originalBitmap) ->
            onFormImageSelected(imageBase64, resized)
            if (resized !== originalBitmap) originalBitmap.recycle()
            val sku = targetSku.value ?: return@onSuccess
            targetSku.value = null
            val token = idToken
            if (token == null || !api.isConfigured) {
                onMessage("Sign in to Shop before saving a product picture.")
            } else if (uploadingSku.value == null) {
                operationScope.launch {
                    uploadingSku.value = sku
                    runCatchingCancellable {
                        withContext(Dispatchers.IO) { api.updateProductImage(token, sku, imageBase64) }
                    }.onSuccess { imageId ->
                        if (imageId.isBlank()) {
                            onMessage("Shop did not save the product picture. Try again.")
                        } else {
                            withContext(Dispatchers.IO) {
                                ProductImageCache.save(context, sku, imageId, Base64.decode(imageBase64, Base64.DEFAULT))
                            }
                            val updated = products.map { product ->
                                if (product.sku == sku) product.copy(imageId = imageId) else product
                            }
                            onProductsUpdated(updated)
                            ProductCache.save(context, updated)
                            onCatalogProductUpdated(updated.firstOrNull { it.sku == sku })
                            onMessage("Product picture saved to Shop.")
                        }
                    }.onFailure { onMessage(it.message ?: "Could not save this product picture.") }
                    uploadingSku.value = null
                }
            }
        }.onFailure {
            targetSku.value = null
            onMessage(it.message ?: "Could not use this picture.")
        }
    }

    fun readBitmap(uri: Uri, failureMessage: String, onBitmap: (Bitmap) -> Unit) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                ?: throw IllegalStateException(failureMessage)
        }.onSuccess(onBitmap).onFailure {
            targetSku.value = null
            onMessage(it.message ?: "Could not use this picture.")
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) targetSku.value = null
        else readBitmap(uri, "Could not open this picture.", ::processPhoto)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingPhotoUri.value
        pendingPhotoUri.value = null
        if (!saved || uri == null) {
            targetSku.value = null
            if (!saved) onMessage("The picture was not captured. Try again.")
        } else readBitmap(uri, "Could not open the captured picture.", ::processPhoto)
    }

    return ProductPhotoActions(
        uploadingSku = uploadingSku,
        setTargetSku = { targetSku.value = it },
        choosePhoto = { photoPicker.launch("image/*") },
        takePhoto = {
            runCatching {
                val directory = File(context.cacheDir, "product-captures")
                if (!directory.exists() && !directory.mkdirs()) throw IllegalStateException("Could not prepare the camera picture.")
                val output = File.createTempFile("product_", ".jpg", directory)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", output)
            }.onSuccess { uri ->
                pendingPhotoUri.value = uri
                camera.launch(uri)
            }.onFailure {
                targetSku.value = null
                onMessage(it.message ?: "Could not open the camera.")
            }
        }
    )
}
