package com.shop.salesapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shop.salesapp.ui.model.DeletionTarget
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.tr

@Composable
internal fun SalesAppDeletionDialog(
    target: DeletionTarget?,
    submitting: Boolean,
    reason: String,
    onDismiss: () -> Unit,
    onReasonChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    target?.let {
        AlertDialog(
            onDismissRequest = { if (!submitting) onDismiss() },
            title = {
                Text(when (it.type) {
                    "BILL" -> tr("Request bill cancellation", "बिल रद्द करने का अनुरोध")
                    "ORDER" -> tr("Request order cancellation", "ऑर्डर रद्द करने का अनुरोध")
                    else -> tr("Request deletion", "हटाने का अनुरोध")
                })
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(it.summary, color = Ink, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    Text(
                        if (it.type == "BILL" || it.type == "ORDER") tr("The administrator will review this cancellation.", "प्रशासक इस रद्दीकरण की समीक्षा करेंगे।")
                        else tr("The administrator will review this request.", "मैनेजर इस अनुरोध की समीक्षा करेंगे।"),
                        color = Muted
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { onReasonChange(it.take(500)) },
                        label = { Text(tr("Reason", "कारण")) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onSubmit, enabled = !submitting && reason.trim().length >= 4) {
                    if (submitting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(tr("Send request", "अनुरोध भेजें"))
                }
            },
            dismissButton = { TextButton(onClick = onDismiss, enabled = !submitting) { Text(tr("Cancel", "रद्द करें")) } }
        )
    }
}
