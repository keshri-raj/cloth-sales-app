package com.shop.salesapp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.shop.salesapp.ui.model.Screen
import com.shop.salesapp.ui.theme.ClothActionBlue
import com.shop.salesapp.ui.theme.Ink
import com.shop.salesapp.ui.theme.Muted

@Composable
internal fun SalesAppNavigationBar(
    screen: Screen,
    selectedTab: Int,
    isHindi: Boolean,
    pendingPaymentSyncCount: Int,
    onSelect: (Screen, Int) -> Unit
) {
    if (screen !in setOf(Screen.Home, Screen.BillCenter, Screen.Customer, Screen.ProductCenter)) return

    NavigationBar(containerColor = Color.White) {
        val navColors = NavigationBarItemDefaults.colors(
            selectedIconColor = Ink,
            selectedTextColor = Ink,
            indicatorColor = ClothActionBlue,
            unselectedIconColor = Muted,
            unselectedTextColor = Muted
        )

        NavigationBarItem(
            selected = screen == Screen.Home,
            onClick = { onSelect(Screen.Home, -1) },
            icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
            label = { Text(if (isHindi) "होम" else "Home", fontSize = 13.sp) },
            colors = navColors
        )
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onSelect(Screen.BillCenter, 0) },
            icon = { Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null) },
            label = { Text(if (isHindi) "बिल" else "Bill", fontSize = 13.sp) },
            colors = navColors
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onSelect(Screen.Customer, 1) },
            icon = { Icon(Icons.Rounded.Contacts, contentDescription = null) },
            label = { Text(if (isHindi) "ग्राहक" else "Customer", fontSize = 13.sp) },
            colors = navColors
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onSelect(Screen.ProductCenter, 2) },
            icon = { Icon(Icons.Rounded.Inventory2, contentDescription = null) },
            label = { Text(if (isHindi) "कपड़ा" else "Cloth", fontSize = 13.sp) },
            colors = navColors
        )
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onSelect(Screen.Payments, 3) },
            icon = {
                BadgedBox(badge = {
                    if (pendingPaymentSyncCount > 0) {
                        Badge { Text(if (pendingPaymentSyncCount > 9) "9+" else pendingPaymentSyncCount.toString()) }
                    }
                }) { Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null) }
            },
            label = { Text(if (isHindi) "खाता" else "Khata", fontSize = 13.sp) },
            colors = navColors
        )
    }
}
