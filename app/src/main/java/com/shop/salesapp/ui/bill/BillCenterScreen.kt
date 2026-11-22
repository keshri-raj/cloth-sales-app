package com.shop.salesapp.ui.bill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
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
fun BillCenterScreen(padding: PaddingValues, onCreateBill: () -> Unit, onFindBill: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(14.dp)); Text(tr("Bills", "बिल"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
        ActionCard(tr("Create a bill", "बिल बनाएँ"), tr("Add customer and clothes", "ग्राहक और कपड़े जोड़ें"), Icons.AutoMirrored.Rounded.ReceiptLong, ClothActionBlue, Ink, Muted, onCreateBill, outline = true)
        ActionCard(tr("Search bills", "बिल खोजें"), tr("Search by bill number, customer name, or phone", "बिल नंबर, ग्राहक के नाम या फ़ोन से खोजें"), Icons.Rounded.Search, ClothActionBlue, Ink, Muted, onFindBill, outline = true)
    }
}
