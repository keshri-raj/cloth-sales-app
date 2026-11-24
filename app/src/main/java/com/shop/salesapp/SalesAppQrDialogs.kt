package com.shop.salesapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.tr

@Composable
internal fun SalesAppQrDialogs(
    createdQrSku: String?,
    previewProduct: Product?,
    products: List<Product>,
    onFinishProductCreation: () -> Unit,
    onCreatedQrProductMissing: () -> Unit,
    onPrintQrLabel: (Product) -> Unit,
    onClosePreview: () -> Unit
) {
    createdQrSku?.let { sku ->
        val qrBitmap = remember(sku) { createQrBitmap(sku).asImageBitmap() }
        val product = products.firstOrNull { it.sku == sku }
        AlertDialog(
            onDismissRequest = onFinishProductCreation,
            title = { Text(tr("Cloth saved successfully", "कपड़ा सफलतापूर्वक सेव हुआ")) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    product?.let { Text(it.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink) }
                    Image(qrBitmap, contentDescription = "QR code for $sku", modifier = Modifier.size(190.dp))
                    Text(sku, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                    Text(
                        tr(
                            "Its QR is ready. Print the product label now, or continue and print it later from the catalogue.",
                            "इसका QR तैयार है। अभी उत्पाद का लेबल प्रिंट करें, या आगे बढ़ें और बाद में कैटलॉग से प्रिंट करें।"
                        ),
                        fontSize = 15.sp,
                        color = Muted
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        val product = products.firstOrNull { it.sku == sku }
                        if (product == null) onCreatedQrProductMissing() else onPrintQrLabel(product)
                    }) { Text(tr("Print label", "लेबल प्रिंट करें")) }
                    TextButton(onClick = onFinishProductCreation) { Text(tr("Continue", "आगे बढ़ें")) }
                }
            }
        )
    }

    previewProduct?.let { product ->
        val qrBitmap = remember(product.sku) { createQrBitmap(product.sku).asImageBitmap() }
        AlertDialog(
            onDismissRequest = onClosePreview,
            title = { Text(tr("Cloth QR label", "कपड़े का QR लेबल")) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(qrBitmap, contentDescription = "QR code for ${product.name}", modifier = Modifier.size(190.dp))
                    Text(product.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(product.sku, color = Muted, fontSize = 15.sp)
                    Text(tr("Print on PSF-20 label roll (48 × 26 mm). Label shows Avani Suto, cloth name, category, SKU and added date—no price.", "PSF-20 लेबल रोल (48 × 26 मिमी) पर प्रिंट करें। लेबल पर Avani Suto, कपड़े का नाम, श्रेणी, SKU और जोड़ने की तारीख होगी—कीमत नहीं।"), color = Muted, fontSize = 14.sp)
                }
            },
            confirmButton = { TextButton(onClick = { onPrintQrLabel(product) }) { Text(tr("Print QR label", "QR लेबल प्रिंट करें")) } },
            dismissButton = { TextButton(onClick = onClosePreview) { Text(tr("Close", "बंद करें")) } }
        )
    }
}
