package com.example.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.DarkPrimary
import com.example.ui.theme.DarkTertiary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.MutedGold
import com.example.ui.theme.OnDarkSurfaceVariant
import com.example.ui.theme.WarningAmber

object IconCatalog {
    val walletIconKeys = listOf(
        "payments",
        "account_balance",
        "smartphone",
        "savings",
        "account_balance_wallet"
    )

    val categoryIconKeys = listOf(
        "restaurant",
        "shopping_bag",
        "local_grocery_store",
        "directions_car",
        "receipt",
        "home_work",
        "medical_services",
        "movie",
        "flight",
        "school",
        "work",
        "redeem",
        "payments",
        "laptop_mac",
        "trending_up",
        "replay",
        "category",
        "label"
    )

    val colorKeys = listOf(
        "emerald",
        "gold",
        "coral",
        "mint",
        "amber",
        "peach",
        "slate"
    )

    fun iconFor(key: String?): ImageVector = when (key) {
        "payments" -> Icons.Rounded.Payments
        "account_balance" -> Icons.Rounded.AccountBalance
        "smartphone" -> Icons.Rounded.Smartphone
        "savings" -> Icons.Rounded.Savings
        "account_balance_wallet" -> Icons.Rounded.AccountBalanceWallet
        "restaurant" -> Icons.Rounded.Restaurant
        "shopping_bag" -> Icons.Rounded.ShoppingBag
        "local_grocery_store" -> Icons.Rounded.LocalGroceryStore
        "directions_car" -> Icons.Rounded.DirectionsCar
        "receipt" -> Icons.Rounded.ReceiptLong
        "home_work" -> Icons.Rounded.HomeWork
        "medical_services" -> Icons.Rounded.MedicalServices
        "movie" -> Icons.Rounded.Movie
        "flight" -> Icons.Rounded.Flight
        "school" -> Icons.Rounded.School
        "work" -> Icons.Rounded.Work
        "redeem" -> Icons.Rounded.Redeem
        "laptop_mac" -> Icons.Rounded.LaptopMac
        "trending_up" -> Icons.Rounded.TrendingUp
        "replay" -> Icons.Rounded.Replay
        "add_circle" -> Icons.Rounded.AddCircle
        "swap_horiz" -> Icons.Rounded.SwapHoriz
        "label" -> Icons.Rounded.Label
        else -> Icons.Rounded.Category
    }

    fun colorFor(key: String?): Color = when (key) {
        "emerald", "emerald-primary", "income-green" -> EmeraldPrimary
        "gold", "muted-gold", "dark-secondary" -> MutedGold
        "coral", "expense-coral" -> ExpenseCoral
        "mint", "dark-primary" -> DarkPrimary
        "amber", "warning-amber" -> WarningAmber
        "peach", "dark-tertiary" -> DarkTertiary
        else -> OnDarkSurfaceVariant
    }

    fun defaultIconForWalletType(type: String): String = when (type) {
        "cash" -> "payments"
        "bank" -> "account_balance"
        "mobile" -> "smartphone"
        "savings" -> "savings"
        else -> "account_balance_wallet"
    }
}
