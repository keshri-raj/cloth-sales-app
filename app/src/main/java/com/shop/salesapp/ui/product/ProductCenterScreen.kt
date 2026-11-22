package com.shop.salesapp.ui.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.salesapp.ui.home.ActionCard
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.ClothActionBlue
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.tr

@Composable
fun ProductCenterScreen(padding: PaddingValues, onAddProduct: () -> Unit, onCatalog: () -> Unit, onPlaceOrder: () -> Unit, onViewOrderRequests: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(14.dp)); Text(tr("Cloth", "कपड़ा"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
        ActionCard(tr("Add new clothes", "नए कपड़े जोड़ें"), tr("Save cloth details and create its QR", "कपड़े की जानकारी सहेजें और QR बनाएँ"), Icons.Rounded.AddShoppingCart, ClothActionBlue, Ink, Muted, onAddProduct, outline = true)
        ActionCard(tr("Create order request", "ऑर्डर अनुरोध बनाएँ"), tr("Request cloth for a customer — no bill or payment", "ग्राहक के लिए कपड़ा मँगाएँ — बिल या भुगतान नहीं"), Icons.AutoMirrored.Rounded.ReceiptLong, ClothActionBlue, Ink, Muted, onPlaceOrder, outline = true)
        ActionCard(tr("Order requests", "ऑर्डर अनुरोध"), tr("Tap to see the full items and customer details", "पूरे सामान और ग्राहक की जानकारी देखने के लिए टैप करें"), Icons.Rounded.Inventory2, ClothActionBlue, Ink, Muted, onViewOrderRequests, outline = true)
        ActionCard(tr("Cloth catalogue", "कपड़ों का कैटलॉग"), tr("Search, scan, and view cloth QR codes", "कपड़े खोजें, स्कैन करें और QR देखें"), Icons.Rounded.Storefront, ClothActionBlue, Ink, Muted, onCatalog, outline = true)
    }
}
