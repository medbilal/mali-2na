package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.WalletEntity
import com.example.ui.theme.MaliTheme
import com.example.util.AppStrings
import com.example.util.DateHelper
import com.example.util.IconCatalog
import com.example.util.MoneyFormat

object NavRoutes {
    const val HOME = "home"
    const val WALLETS = "wallets"
    const val WALLET_DETAIL = "wallet_detail/{walletId}"
    const val TRANSACTIONS = "transactions"
    const val REPORTS = "reports"
    const val MORE = "more"

    fun walletDetail(walletId: String) = "wallet_detail/$walletId"
}

@Composable
fun MaliCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = MaliTheme.colors.cardSurface,
    borderColor: Color = MaliTheme.colors.outlineVariant,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val mod = modifier
        .clip(shape)
        .background(backgroundColor)
        .border(1.dp, borderColor, shape)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    Column(modifier = mod, content = content)
}

@Composable
fun LtrNumberText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(
            text = text,
            style = style.copy(fontFeatureSettings = "tnum"),
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    }
}

fun walletTypeLabel(type: String, lang: String): String = when (lang) {
    "fr" -> when (type) {
        "cash" -> "Espèces"
        "bank" -> "Compte bancaire"
        "mobile" -> "Mobile Money"
        "savings" -> "Épargne"
        else -> "Personnalisé"
    }
    "en" -> when (type) {
        "cash" -> "Cash"
        "bank" -> "Bank Account"
        "mobile" -> "Mobile Money"
        "savings" -> "Savings"
        else -> "Custom"
    }
    else -> when (type) {
        "cash" -> "نقدي"
        "bank" -> "حساب بنكي"
        "mobile" -> "محفظة هاتف"
        "savings" -> "محفظة ادخار"
        else -> "مخصص"
    }
}

@Composable
fun TransactionRowItem(
    tx: TransactionEntity,
    wallet: WalletEntity?,
    toWallet: WalletEntity?,
    category: CategoryEntity?,
    masked: Boolean,
    showActions: Boolean = false,
    onClick: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val isIncome = tx.type == "income"
    val isTransfer = tx.type == "transfer"
    val amountColor = when {
        isIncome -> MaliTheme.colors.incomeGreen
        isTransfer -> MaliTheme.colors.mutedGold
        else -> MaliTheme.colors.expenseCoral
    }
    val icon = if (isTransfer) IconCatalog.iconFor("swap_horiz") else IconCatalog.iconFor(category?.iconName ?: "receipt")
    val title = tx.note.ifBlank {
        if (isTransfer) "تحويل إلى ${toWallet?.name ?: ""}"
        else category?.name ?: "عملية"
    }
    val sub = if (isTransfer) {
        "${wallet?.name ?: ""} ← ${toWallet?.name ?: ""}"
    } else {
        "${wallet?.name ?: ""} • ${tx.dateIso}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaliTheme.colors.surfaceHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = amountColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        LtrNumberText(
            text = MoneyFormat.formatSigned(tx.amountMinor, tx.type, tx.currency, masked),
            style = MaterialTheme.typography.titleMedium,
            color = amountColor
        )

        if (showActions) {
            if (onEdit != null) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(40.dp).testTag("edit_tx_${tx.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "تعديل",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp).testTag("delete_tx_${tx.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "حذف",
                        tint = MaliTheme.colors.expenseCoral,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    dateIso: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Surface(
            onClick = { showPicker = true },
            shape = RoundedCornerShape(10.dp),
            color = MaliTheme.colors.surfaceLow,
            border = BorderStroke(1.dp, MaliTheme.colors.outlineVariant),
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LtrNumberText(
                    text = dateIso,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = DateHelper.isoToMillis(dateIso)
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            onDateSelected(DateHelper.formatMillisToIso(millis))
                        }
                        showPicker = false
                    }
                ) {
                    Text("موافق")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("إلغاء")
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun MaliBottomNavigationBar(
    currentRoute: String,
    strings: AppStrings,
    onNavigate: (String) -> Unit,
    onAddTransaction: () -> Unit
) {
    Surface(
        color = MaliTheme.colors.cardSurface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = MaliTheme.colors.outlineVariant, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    selected = currentRoute == NavRoutes.HOME,
                    label = strings.navHome,
                    selectedIcon = Icons.Rounded.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    tag = "nav_home",
                    onClick = { onNavigate(NavRoutes.HOME) },
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    selected = currentRoute == NavRoutes.WALLETS || currentRoute.startsWith("wallet_detail"),
                    label = strings.navWallets,
                    selectedIcon = Icons.Rounded.AccountBalanceWallet,
                    unselectedIcon = Icons.Outlined.AccountBalanceWallet,
                    tag = "nav_wallets",
                    onClick = { onNavigate(NavRoutes.WALLETS) },
                    modifier = Modifier.weight(1f)
                )

                // Raised center (+) FAB matching Stitch design
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-12).dp)
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                            .clickable(onClick = onAddTransaction)
                            .testTag("fab_add_transaction"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = strings.addTransaction,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                BottomNavItem(
                    selected = currentRoute == NavRoutes.TRANSACTIONS,
                    label = strings.navTransactions,
                    selectedIcon = Icons.Rounded.ReceiptLong,
                    unselectedIcon = Icons.Outlined.ReceiptLong,
                    tag = "nav_transactions",
                    onClick = { onNavigate(NavRoutes.TRANSACTIONS) },
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    selected = currentRoute == NavRoutes.MORE || currentRoute == NavRoutes.REPORTS,
                    label = strings.navMore,
                    selectedIcon = Icons.Rounded.GridView,
                    unselectedIcon = Icons.Outlined.GridView,
                    tag = "nav_more",
                    onClick = { onNavigate(NavRoutes.MORE) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    selected: Boolean,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .fillMaxHeight()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MaliDesktopSidebar(
    currentRoute: String,
    strings: AppStrings,
    onNavigate: (String) -> Unit,
    onAddTransaction: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(248.dp)
            .fillMaxHeight()
            .background(MaliTheme.colors.cardSurface)
            .border(1.dp, MaliTheme.colors.outlineVariant)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "م",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Column {
                    Text(
                        text = strings.appTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.appSubtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SidebarNavItem(
                    selected = currentRoute == NavRoutes.HOME,
                    label = strings.navHome,
                    icon = if (currentRoute == NavRoutes.HOME) Icons.Rounded.Home else Icons.Outlined.Home,
                    onClick = { onNavigate(NavRoutes.HOME) }
                )
                SidebarNavItem(
                    selected = currentRoute == NavRoutes.WALLETS || currentRoute.startsWith("wallet_detail"),
                    label = strings.navWallets,
                    icon = if (currentRoute == NavRoutes.WALLETS) Icons.Rounded.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                    onClick = { onNavigate(NavRoutes.WALLETS) }
                )
                SidebarNavItem(
                    selected = currentRoute == NavRoutes.TRANSACTIONS,
                    label = strings.navTransactions,
                    icon = if (currentRoute == NavRoutes.TRANSACTIONS) Icons.Rounded.ReceiptLong else Icons.Outlined.ReceiptLong,
                    onClick = { onNavigate(NavRoutes.TRANSACTIONS) }
                )
                SidebarNavItem(
                    selected = currentRoute == NavRoutes.REPORTS,
                    label = strings.navReports,
                    icon = if (currentRoute == NavRoutes.REPORTS) Icons.Rounded.Analytics else Icons.Outlined.Analytics,
                    onClick = { onNavigate(NavRoutes.REPORTS) }
                )
                SidebarNavItem(
                    selected = currentRoute == NavRoutes.MORE,
                    label = strings.navMore,
                    icon = if (currentRoute == NavRoutes.MORE) Icons.Rounded.GridView else Icons.Outlined.GridView,
                    onClick = { onNavigate(NavRoutes.MORE) }
                )
            }
        }

        Button(
            onClick = onAddTransaction,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("sidebar_add_transaction")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(strings.addTransaction, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SidebarNavItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val bg = if (selected) MaliTheme.colors.surfaceHigh else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = fg, modifier = Modifier.size(20.dp))
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}
