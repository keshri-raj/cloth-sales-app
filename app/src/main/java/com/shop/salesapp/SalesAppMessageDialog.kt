package com.shop.salesapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.shop.salesapp.ui.bill.WhatsAppShareIcon
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.Teal
import com.shop.salesapp.ui.theme.localizedAppMessage
import com.shop.salesapp.ui.theme.tr

@Composable
internal fun SalesAppMessageDialog(
    message: String?,
    isHindi: Boolean,
    latestInvoice: ShopInvoice?,
    invoicePdfPreparingNumbers: Set<String>,
    sharingBillNumber: String?,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
    onShareBillPdf: (ShopInvoice) -> Unit,
    onRequestBillCancellation: (ShopInvoice) -> Unit
) {
    message?.let { text ->
        val savedBill = latestInvoice
        if (savedBill != null) {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(tr("Bill saved", "बिल सेव हो गया"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink) },
                text = {
                    Surface(color = Color(0xFFEAF3EF), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(savedBill.number, modifier = Modifier.weight(1f), fontSize = 13.sp, color = Ink, maxLines = 1)
                            Text("₹${savedBill.total}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                            }
                    }
                },
                confirmButton = {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onShareBillPdf(savedBill) },
                            enabled = sharingBillNumber == null && savedBill.number !in invoicePdfPreparingNumbers,
                            modifier = Modifier.size(48.dp)
                        ) {
                            WhatsAppShareIcon(
                                sharing = sharingBillNumber == savedBill.number,
                                contentDescription = tr("Share bill on WhatsApp", "बिल WhatsApp पर साझा करें")
                            )
                        }
                        OutlinedButton(
                            onClick = { onRequestBillCancellation(savedBill) },
                            enabled = sharingBillNumber != savedBill.number,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 7.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFB3261E))
                        ) {
                            Icon(Icons.Rounded.Cancel, contentDescription = null, tint = Color(0xFFB3261E), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(tr("Cancel bill", "बिल रद्द करें"), color = Color(0xFFB3261E), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(onClick = onDone, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 7.dp)) {
                            Text(tr("Done", "हो गया"), color = Teal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(26.dp)
            )
        } else {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(tr("Avani Suto", "अवनि सूतो")) },
                text = { Text(localizedAppMessage(text, isHindi), fontSize = 17.sp) },
                confirmButton = { TextButton(onClick = onDismiss) { Text(tr("OK", "ठीक है")) } }
            )
        }
    }
}
