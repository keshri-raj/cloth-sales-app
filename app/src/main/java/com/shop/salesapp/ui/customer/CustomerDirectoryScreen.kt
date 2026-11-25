package com.shop.salesapp.ui.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.salesapp.CustomerCache
import com.shop.salesapp.ShopCustomer
import com.shop.salesapp.ui.home.ActionCard
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.ClothActionBlue
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.Teal
import com.shop.salesapp.ui.theme.tr

@Composable
fun CustomerDirectoryScreen(
    padding: PaddingValues,
    query: String,
    customers: List<ShopCustomer>,
    searching: Boolean,
    syncing: Boolean,
    live: Boolean,
    onQuery: (String) -> Unit,
    onSync: () -> Unit,
    onCreate: () -> Unit,
    onCustomer: (ShopCustomer) -> Unit
) {
    val digits = CustomerCache.normalizePhone(query)
    val results = when {
        query.isBlank() -> customers.take(50)
        digits.isNotBlank() -> customers.filter {
            CustomerCache.normalizePhone(it.phone).contains(digits) || it.name.contains(query, ignoreCase = true)
        }
        else -> customers.filter { it.name.contains(query, ignoreCase = true) || it.phone.contains(query) }
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Customers", "ग्राहक"), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                IconButton(onClick = onSync, enabled = live && !syncing, modifier = Modifier.size(48.dp)) {
                    if (syncing) CircularProgressIndicator(modifier = Modifier.size(21.dp), strokeWidth = 2.dp, color = Teal)
                    else Icon(Icons.Rounded.Refresh, contentDescription = tr("Sync customers", "ग्राहक सिंक करें"), tint = if (live) Teal else Muted)
                }
            }
        }
        item {
            ActionCard(
                title = tr("Create new customer", "नया ग्राहक जोड़ें"),
                subtitle = tr("Save details for faster billing next time", "अगली बार जल्दी बिल बनाने के लिए जानकारी सहेजें"),
                icon = Icons.Rounded.Add,
                fill = ClothActionBlue,
                titleColor = Ink,
                detailColor = Muted,
                onClick = onCreate,
                outline = true
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(tr("Customer name or phone", "ग्राहक का नाम या फ़ोन")) },
                placeholder = { Text(tr("Type to search", "खोजने के लिए लिखें")) },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                trailingIcon = { Icon(Icons.Rounded.Search, contentDescription = tr("Search customers", "ग्राहक खोजें")) },
                shape = RoundedCornerShape(14.dp)
            )
        }
        item {
            Text(
                when {
                    searching -> tr("Checking the Shop sheet…", "Shop शीट में खोज रहे हैं…")
                    syncing -> tr("Syncing customers from Shop…", "Shop से ग्राहक सिंक हो रहे हैं…")
                    live -> tr("Saved customers are searched on this phone first.", "पहले इस फ़ोन में सहेजे ग्राहक खोजे जाते हैं।")
                    else -> tr("Connect to Shop to sync customer details.", "ग्राहक जानकारी सिंक करने के लिए Shop से जुड़ें।")
                },
                color = Muted,
                fontSize = 14.sp
            )
        }
        itemsIndexed(results, key = { index, customer -> "customer:$index:${customer.id}:${customer.phone}" }) { _, customer ->
            Card(onClick = { onCustomer(customer) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE1E8E7))) {
                Column(Modifier.fillMaxWidth().padding(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(customer.name, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                        if (customer.customerType.equals("Shop Owner", ignoreCase = true)) {
                            Surface(shape = RoundedCornerShape(50), color = Color(0xFFFFF0DF)) {
                                Text(tr("Shop Owner", "दुकान मालिक"), color = Color(0xFF9A4D00), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                            }
                        }
                    }
                    Text(customer.phone, fontSize = 16.sp, color = Teal)
                    if (customer.email.isNotBlank()) Text(customer.email, fontSize = 14.sp, color = Muted)
                }
            }
        }
        if (results.isEmpty() && !searching) item {
            Text(
                if (query.isBlank()) tr("No saved customers yet.", "अभी कोई ग्राहक सहेजा नहीं गया।") else tr("No matching customer found.", "कोई मिलता-जुलता ग्राहक नहीं मिला।"),
                color = Muted,
                fontSize = 16.sp,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
