package com.shop.salesapp.ui.common

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.salesapp.DeletionRequest
import com.shop.salesapp.Product
import com.shop.salesapp.ProductImageCache
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.SoftBlue
import com.shop.salesapp.ui.theme.Teal
import com.shop.salesapp.ui.theme.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    onSync: (() -> Unit)? = null,
    syncing: Boolean = false,
    syncEnabled: Boolean = true,
    onShare: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    clearEnabled: Boolean = true
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack, modifier = Modifier.size(52.dp)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Go back", tint = Ink) }
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
        if (onClear != null) {
            TextButton(onClick = onClear, enabled = clearEnabled) {
                Text(tr("Clear", "साफ़ करें"), color = if (clearEnabled) Teal else Muted)
            }
        }
        if (onShare != null) {
            IconButton(onClick = onShare, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Share, contentDescription = tr("Share", "शेयर करें"), tint = Teal)
            }
        }
        if (onSync != null) {
            IconButton(onClick = onSync, enabled = syncEnabled && !syncing, modifier = Modifier.size(48.dp)) {
                if (syncing) CircularProgressIndicator(modifier = Modifier.size(21.dp), strokeWidth = 2.dp, color = Teal)
                else Icon(Icons.Rounded.Refresh, contentDescription = tr("Sync", "सिंक करें"), tint = if (syncEnabled) Teal else Muted)
            }
        }
    }
}

@Composable
fun SectionTitle(number: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = Ink, modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(number, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
        Spacer(Modifier.width(10.dp))
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
    }
}

@Composable
fun DetailLine(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Muted, fontSize = 15.sp, modifier = Modifier.weight(0.42f))
        Text(value.ifBlank { "—" }, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.58f))
    }
}

@Composable
fun BillClothThumbnail(
    product: Product,
    imageLoader: suspend (String) -> ByteArray?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(product.sku, product.imageId) {
        mutableStateOf(ProductImageCache.memoryHit(product.sku, product.imageId))
    }
    LaunchedEffect(product.sku, product.imageId) {
        if (product.imageId.isNotBlank()) {
            bitmap = withContext(Dispatchers.IO) { ProductImageCache.load(context, product.sku, product.imageId) }
            if (bitmap == null) {
                val bytes = runCatching { imageLoader(product.imageId) }.getOrNull()
                if (bytes != null) bitmap = withContext(Dispatchers.IO) {
                    ProductImageCache.save(context, product.sku, product.imageId, bytes)
                        ?: BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }
        }
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = SoftBlue) {
        if (bitmap != null) Image(
            bitmap!!.asImageBitmap(), contentDescription = product.name,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
        ) else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = Muted, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
fun AdminDeletionRequestsSection(
    type: String,
    requests: List<DeletionRequest>,
    onReview: (DeletionRequest, Boolean) -> Unit
) {
    val pending = requests.filter { it.status.equals("PENDING", ignoreCase = true) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            when (type) {
                "PAYMENT" -> tr("Payment correction requests", "भुगतान सुधार अनुरोध")
                "BILL" -> tr("Bill cancellation requests", "बिल रद्द करने के अनुरोध")
                "ORDER" -> tr("Order cancellation requests", "ऑर्डर रद्द करने के अनुरोध")
                else -> tr("Cloth removal requests", "कपड़ा हटाने के अनुरोध")
            },
            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink
        )
        if (pending.isEmpty()) {
            Text(tr("No pending requests in this section.", "इस भाग में कोई लंबित अनुरोध नहीं है।"), color = Muted, fontSize = 14.sp)
        }
        pending.forEach { request ->
            Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E8)), border = BorderStroke(1.dp, Color(0xFFE7D7B2))) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(request.summary, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(request.reason, color = Ink, fontSize = 14.sp)
                    Text("${request.requestedBy} · ${request.date}", color = Muted, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onReview(request, false) }) { Text(tr("Reject", "अस्वीकार"), color = Color(0xFFB3261E)) }
                        Button(onClick = { onReview(request, true) }) { Text(tr("Approve", "स्वीकृत करें")) }
                    }
                }
            }
        }
    }
}
