package com.shop.salesapp.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.Teal
import com.shop.salesapp.ui.theme.tr

@Composable
internal fun LoginScreen(
    apiConfigured: Boolean,
    signingIn: Boolean,
    checkingSession: Boolean,
    errorMessage: String?,
    onSignIn: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE1E7EE))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Avani Suto", color = Ink, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                Text(
                    tr("Sign in to continue", "आगे बढ़ने के लिए साइन इन करें"),
                    color = Ink,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    tr("Sign in to access bills, customers, and the cloth catalogue.", "बिल, ग्राहक और कपड़ों का कैटलॉग देखने के लिए साइन इन करें।"),
                    color = Muted,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                if (!apiConfigured) {
                    Text(
                        tr("Shop connection is not configured on this app. Contact the manager.", "इस ऐप में Shop कनेक्शन सेट नहीं है। मैनेजर से संपर्क करें।"),
                        color = Color(0xFFAD352E),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
                if (!errorMessage.isNullOrBlank()) {
                    Text(errorMessage, color = Color(0xFFAD352E), fontSize = 14.sp, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.size(2.dp))
                Button(
                    onClick = onSignIn,
                    enabled = apiConfigured && !signingIn && !checkingSession,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    if (signingIn || checkingSession) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Icon(Icons.Rounded.AccountCircle, contentDescription = null)
                    Spacer(Modifier.width(9.dp))
                    Text(
                        when {
                            signingIn -> tr("Opening sign-in…", "साइन इन खोल रहे हैं…")
                            checkingSession -> tr("Checking sign-in…", "साइन इन जाँच रहे हैं…")
                            else -> tr("Sign in with Google", "Google से साइन इन करें")
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
