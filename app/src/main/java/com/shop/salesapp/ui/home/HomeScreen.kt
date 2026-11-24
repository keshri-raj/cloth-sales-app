package com.shop.salesapp.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.ClothActionBlue
import com.shop.salesapp.ui.theme.Muted
import com.shop.salesapp.ui.theme.SoftGreen
import com.shop.salesapp.ui.theme.Teal
import com.shop.salesapp.ui.theme.tr
import com.shop.salesapp.BuildConfig

@Composable
fun HomeScreen(
    padding: PaddingValues,
    apiConfigured: Boolean,
    isSignedIn: Boolean,
    signedInEmail: String?,
    userRole: String,
    isHindi: Boolean,
    onLanguageChange: (Boolean) -> Unit,
    onSignIn: () -> Unit,
    onNewBill: () -> Unit,
    onMakePayment: () -> Unit,
    onCatalog: () -> Unit,
    onAddProduct: () -> Unit,
    onPlaceOrder: () -> Unit
) {
    var userMenuExpanded by remember { mutableStateOf(false) }
    var showAppInfo by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Surface(shape = CircleShape, color = Color.White, border = BorderStroke(1.dp, Color(0xFFD8E1E2))) {
                        IconButton(onClick = { userMenuExpanded = true }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.AccountCircle, contentDescription = if (isHindi) "प्रोफ़ाइल और भाषा" else "Profile and language", tint = Teal, modifier = Modifier.size(32.dp))
                        }
                    }
                    DropdownMenu(expanded = userMenuExpanded, onDismissRequest = { userMenuExpanded = false }) {
                        if (signedInEmail != null) {
                            DropdownMenuItem(
                                text = { Text(signedInEmail, fontWeight = FontWeight.SemiBold) },
                                onClick = { userMenuExpanded = false },
                                enabled = false
                            )
                            DropdownMenuItem(
                                text = { Text(if (userRole == "ADMIN") tr("Administrator", "प्रशासक") else tr("Salesperson", "बिक्री कर्मचारी"), color = Teal) },
                                onClick = { userMenuExpanded = false },
                                enabled = false
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (isHindi) "English" else "English  ✓", color = if (!isHindi) Teal else Ink) },
                            onClick = { onLanguageChange(false); userMenuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isHindi) "हिन्दी  ✓" else "हिन्दी", color = if (isHindi) Teal else Ink) },
                            onClick = { onLanguageChange(true); userMenuExpanded = false }
                        )
                        if (isSignedIn) {
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "साइन इन है" else "Signed in") },
                                onClick = { userMenuExpanded = false },
                                enabled = false
                            )
                        } else if (apiConfigured) {
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "Shop में साइन इन करें" else "Sign in to Shop", color = Teal, fontWeight = FontWeight.Bold) },
                                onClick = { userMenuExpanded = false; onSignIn() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (isHindi) "ऐप की जानकारी" else "App info") },
                            onClick = { userMenuExpanded = false; showAppInfo = true }
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (isHindi) "अवनी सूतो" else "Avani Suto", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSignedIn) SoftGreen else Color(0xFFFFF3D7)
                        ) {
                            Text(
                                when {
                                    isSignedIn -> tr("Connected", "जुड़ा हुआ")
                                    apiConfigured -> tr("Not connected", "कनेक्ट नहीं")
                                    else -> tr("Not set up", "सेटअप नहीं")
                                },
                                color = if (isSignedIn) Color(0xFF2E7D32) else Color(0xFF76551A),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        if (userRole == "ADMIN") tr("Administrator", "प्रशासक") else tr("Salesperson", "बिक्री कर्मचारी"),
                        color = Teal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        item {
            ActionCard(
                title = if (isHindi) "बिल बनाएँ" else "Create a bill",
                subtitle = "",
                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                fill = ClothActionBlue,
                titleColor = Ink,
                detailColor = Muted,
                onClick = onNewBill,
                outline = true,
                compact = true
            )
        }
        item {
            ActionCard(
                title = if (isHindi) "ऑर्डर अनुरोध बनाएँ" else "Create order request",
                subtitle = "",
                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                fill = ClothActionBlue,
                titleColor = Ink,
                detailColor = Muted,
                onClick = onPlaceOrder,
                outline = true,
                compact = true
            )
        }
        item {
            ActionCard(
                title = if (isHindi) "नए कपड़े जोड़ें" else "Add new clothes",
                subtitle = "",
                icon = Icons.Rounded.AddShoppingCart,
                fill = ClothActionBlue,
                titleColor = Ink,
                detailColor = Muted,
                onClick = onAddProduct,
                outline = true,
                compact = true
            )
        }
        item {
            ActionCard(
                title = if (isHindi) "जमा करें" else "Collect",
                subtitle = "",
                icon = Icons.Rounded.AccountBalanceWallet,
                fill = ClothActionBlue,
                titleColor = Ink,
                detailColor = Muted,
                onClick = onMakePayment,
                outline = true,
                compact = true
            )
        }
        item {
            ActionCard(
                title = if (isHindi) "कैटलॉग" else "Catalogue",
                subtitle = "",
                icon = Icons.Rounded.Storefront,
                fill = ClothActionBlue,
                titleColor = Ink,
                detailColor = Muted,
                onClick = onCatalog,
                outline = true,
                compact = true
            )
        }
    }
    if (showAppInfo) {
        AlertDialog(
            onDismissRequest = { showAppInfo = false },
            title = { Text(if (isHindi) "ऐप की जानकारी" else "App info", color = Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Avani Suto", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (isHindi) "ऐप संस्करण ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                        else "App version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        color = Muted,
                        fontSize = 15.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppInfo = false }) {
                    Text(if (isHindi) "बंद करें" else "Close", color = Teal)
                }
            }
        )
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    fill: Color,
    titleColor: Color,
    detailColor: Color,
    onClick: () -> Unit,
    outline: Boolean = false,
    compact: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(if (compact) 84.dp else 90.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = fill),
        border = if (outline) BorderStroke(1.dp, Color(0xFFD8E1E2)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (outline) 0.dp else 3.dp)
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color(0xFFC7DFF3), modifier = Modifier.size(58.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Ink, modifier = Modifier.size(30.dp)) }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(title, fontSize = if (compact) 19.sp else 21.sp, fontWeight = FontWeight.Bold, color = titleColor)
            }
        }
    }
}
