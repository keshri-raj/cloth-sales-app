package com.shop.salesapp.ui.payment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.salesapp.CustomerCache
import com.shop.salesapp.ShopCustomer
import com.shop.salesapp.ui.common.ScreenHeader
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.Teal
import com.shop.salesapp.ui.theme.tr

@Composable
fun CollectPaymentScreen(
    padding: PaddingValues,
    customers: List<ShopCustomer>,
    onBack: () -> Unit,
    onSubmit: (ShopCustomer, Int, String, String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var customer by remember { mutableStateOf<ShopCustomer?>(null) }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Cash") }
    var note by remember { mutableStateOf("") }
    val matches = customers.filter {
        search.isBlank() || it.name.contains(search.trim(), ignoreCase = true) ||
            CustomerCache.normalizePhone(it.phone).contains(CustomerCache.normalizePhone(search))
    }.take(60)

    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
        ScreenHeader(tr("Collect", "जमा करें"), onBack)
        if (customer == null) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                singleLine = true,
                label = { Text(tr("Search customer by name or phone", "नाम या फ़ोन से ग्राहक खोजें")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = RoundedCornerShape(14.dp)
            )
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(matches, key = { CustomerCache.normalizePhone(it.phone) }) { candidate ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { customer = candidate },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(candidate.name.ifBlank { tr("Customer", "ग्राहक") }, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text(candidate.phone, color = Muted, fontSize = 15.sp)
                        }
                    }
                }
                if (matches.isEmpty()) item {
                    Text(tr("No saved customer matches this search.", "इस खोज से कोई सहेजा ग्राहक नहीं मिला।"), color = Muted, modifier = Modifier.padding(8.dp))
                }
            }
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(customer?.name.orEmpty(), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(customer?.phone.orEmpty(), color = Muted, fontSize = 15.sp)
                        }
                        TextButton(onClick = { customer = null }) { Text(tr("Change", "बदलें")) }
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(tr("Amount received · ₹", "मिली राशि · ₹")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Cash", "UPI", "Card").forEach { option ->
                        if (method == option) Button(onClick = { method = option }, modifier = Modifier.weight(1f).height(50.dp), contentPadding = PaddingValues(4.dp)) {
                            Text(option, fontSize = 14.sp)
                        } else OutlinedButton(onClick = { method = option }, modifier = Modifier.weight(1f).height(50.dp), contentPadding = PaddingValues(4.dp)) {
                            Text(option, fontSize = 14.sp)
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(tr("Note (optional)", "नोट (वैकल्पिक)")) },
                    shape = RoundedCornerShape(14.dp)
                )
                Text(
                    tr("Existing dues are settled first; any extra stays as advance credit.", "पहले बकाया चुकता होगा; अतिरिक्त राशि अग्रिम जमा रहेगी।"),
                    color = Muted,
                    fontSize = 14.sp
                )
            }
            Button(
                onClick = {
                    val selected = customer ?: return@Button
                    val received = amount.toIntOrNull() ?: return@Button
                    onSubmit(selected, received, method, note.trim())
                },
                enabled = amount.toIntOrNull()?.let { it > 0 } == true,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp).height(58.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(tr("Save payment", "भुगतान सहेजें"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
