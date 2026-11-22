package com.shop.salesapp.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.salesapp.CustomerCache
import com.shop.salesapp.ui.common.ScreenHeader
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.tr

@Composable
fun AddCustomerScreen(
    padding: PaddingValues, name: String, phone: String, email: String, address: String,
    saving: Boolean, connected: Boolean, onName: (String) -> Unit, onPhone: (String) -> Unit,
    onEmail: (String) -> Unit, onAddress: (String) -> Unit, onBack: () -> Unit,
    onPickContact: () -> Unit, onSave: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader(tr("Create customer", "ग्राहक जोड़ें"), onBack) }
        item { OutlinedTextField(name, onName, Modifier.fillMaxWidth(), label = { Text(tr("Customer name", "ग्राहक का नाम")) }, singleLine = true, shape = RoundedCornerShape(14.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp)) }
        item {
            OutlinedTextField(
                phone,
                onPhone,
                Modifier.fillMaxWidth(),
                label = { Text(tr("Phone number", "फ़ोन नंबर")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(14.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                trailingIcon = {
                    IconButton(onClick = onPickContact) {
                        Icon(Icons.Rounded.Contacts, contentDescription = tr("Choose from phone contacts", "फ़ोन संपर्कों से चुनें"))
                    }
                }
            )
        }
        item { OutlinedTextField(email, onEmail, Modifier.fillMaxWidth(), label = { Text(tr("Email (optional)", "ईमेल (वैकल्पिक)")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), shape = RoundedCornerShape(14.dp)) }
        item { OutlinedTextField(address, onAddress, Modifier.fillMaxWidth(), label = { Text(tr("Address (optional)", "पता (वैकल्पिक)")) }, minLines = 2, shape = RoundedCornerShape(14.dp)) }
        item {
            Button(onClick = onSave, enabled = name.isNotBlank() && CustomerCache.normalizePhone(phone).length >= 8 && !saving, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(15.dp)) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                else Icon(Icons.Rounded.CheckCircle, null)
                Spacer(Modifier.width(8.dp)); Text(
                    when {
                        saving -> tr("Saving…", "सहेज रहे हैं…")
                        connected -> tr("Save customer", "ग्राहक सहेजें")
                        else -> tr("Save on this phone", "इस फ़ोन में सहेजें")
                    },
                    fontSize = 18.sp
                )
            }
        }
    }
}
