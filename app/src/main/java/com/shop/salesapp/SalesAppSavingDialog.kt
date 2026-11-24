package com.shop.salesapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.Teal
import com.shop.salesapp.ui.theme.tr

@Composable
internal fun SalesAppSavingDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.width(280.dp),
            shape = RoundedCornerShape(24.dp),
            color = androidx.compose.ui.graphics.Color.White,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(52.dp),
                    color = Teal,
                    strokeWidth = 4.dp
                )
                Text(
                    tr("Creating bill", "बिल बनाया जा रहा है"),
                    color = Ink,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    tr("Please wait. Keep this screen open.", "कृपया प्रतीक्षा करें। यह स्क्रीन खुली रखें।"),
                    color = Muted,
                    fontSize = 14.sp
                )
            }
        }
    }
}
