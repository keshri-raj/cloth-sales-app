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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contacts
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
import kotlinx.coroutines.launch

@Composable
fun CollectPaymentScreen(
    padding: PaddingValues,
    customers: List<ShopCustomer>,
    searchQuery: String,
    contactName: String,
    contactPhone: String,
    onSearchQuery: (String) -> Unit,
    onPickContact: () -> Unit,
    onBack: () -> Unit,
    onCreateCustomer: suspend (String, String, String, String) -> ShopCustomer,
    onSubmit: (ShopCustomer, Int, String, String) -> Unit
) {
    var customer by remember { mutableStateOf<ShopCustomer?>(null) }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Cash") }
    var note by remember { mutableStateOf("") }
    var showCreateCustomer by remember { mutableStateOf(false) }
    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerPhone by remember { mutableStateOf("") }
    var newCustomerEmail by remember { mutableStateOf("") }
    var newCustomerAddress by remember { mutableStateOf("") }
    var creatingCustomer by remember { mutableStateOf(false) }
    var createCustomerError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val matches = customers.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery.trim(), ignoreCase = true) ||
            CustomerCache.normalizePhone(it.phone).contains(CustomerCache.normalizePhone(searchQuery))
    }.take(60)

    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
        ScreenHeader(tr("Collect", "जमा करें"), onBack)
        if (customer == null) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQuery,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                singleLine = true,
                label = { Text(tr("Search customer by name or phone", "नाम या फ़ोन से ग्राहक खोजें")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                trailingIcon = {
                    IconButton(onClick = onPickContact) {
                        Icon(Icons.Rounded.Contacts, contentDescription = tr("Choose from contacts", "कॉन्टैक्ट से चुनें"), tint = Teal)
                    }
                },
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
                    Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tr("No saved customer matches this search.", "इस खोज से कोई सहेजा ग्राहक नहीं मिला।"), color = Muted)
                        OutlinedButton(
                            onClick = {
                                val enteredDigits = CustomerCache.normalizePhone(searchQuery)
                                newCustomerName = contactName
                                newCustomerPhone = if (enteredDigits.length >= 8) searchQuery.trim() else contactPhone
                                newCustomerEmail = ""
                                newCustomerAddress = ""
                                createCustomerError = null
                                showCreateCustomer = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(tr("Create new customer", "नया ग्राहक बनाएँ"), color = Teal, fontWeight = FontWeight.SemiBold)
                        }
                    }
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
    if (showCreateCustomer) {
        AlertDialog(
            onDismissRequest = { if (!creatingCustomer) showCreateCustomer = false },
            title = { Text(tr("Add customer", "ग्राहक जोड़ें"), color = Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Create this customer, then continue to record their payment.", "ग्राहक बनाएँ, फिर उनका भुगतान दर्ज करें।"), color = Muted, fontSize = 14.sp)
                    OutlinedTextField(
                        value = newCustomerName,
                        onValueChange = { newCustomerName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(tr("Customer name", "ग्राहक का नाम")) },
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = newCustomerPhone,
                        onValueChange = { newCustomerPhone = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(tr("Customer phone", "ग्राहक का फ़ोन")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = newCustomerEmail,
                        onValueChange = { newCustomerEmail = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(tr("Email (optional)", "ईमेल (वैकल्पिक)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = newCustomerAddress,
                        onValueChange = { newCustomerAddress = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(tr("City or address (optional)", "शहर या पता (वैकल्पिक)")) },
                        shape = RoundedCornerShape(14.dp)
                    )
                    createCustomerError?.let { Text(it, color = Color(0xFFB3261E), fontSize = 13.sp) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            creatingCustomer = true
                            createCustomerError = null
                            runCatching {
                                onCreateCustomer(
                                    newCustomerName.trim(), newCustomerPhone.trim(),
                                    newCustomerEmail.trim(), newCustomerAddress.trim()
                                )
                            }.onSuccess { created ->
                                customer = created
                                amount = ""
                                showCreateCustomer = false
                            }.onFailure { error ->
                                createCustomerError = error.message ?: "Could not create customer."
                            }
                            creatingCustomer = false
                        }
                    },
                    enabled = !creatingCustomer && newCustomerName.isNotBlank() && CustomerCache.normalizePhone(newCustomerPhone).length >= 8
                ) { Text(if (creatingCustomer) tr("Saving…", "सहेज रहे हैं…") else tr("Create customer", "ग्राहक बनाएँ")) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCustomer = false }, enabled = !creatingCustomer) {
                    Text(tr("Cancel", "रद्द करें"))
                }
            }
        )
    }
}
