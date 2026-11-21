package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.TransactionEntity
import com.example.data.local.WalletEntity
import com.example.ui.components.IncomeExpenseBarChart
import com.example.ui.components.LtrNumberText
import com.example.ui.components.MaliCard
import com.example.ui.components.TransactionRowItem
import com.example.ui.components.walletTypeLabel
import com.example.ui.theme.MaliTheme
import com.example.ui.viewmodel.ChartBarPoint
import com.example.ui.viewmodel.MaliUiState
import com.example.util.AppStrings
import com.example.util.DateHelper
import com.example.util.IconCatalog
import com.example.util.MoneyFormat
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    state: MaliUiState,
    strings: AppStrings,
    computeChart: (String) -> List<ChartBarPoint>,
    onToggleBalanceVisibility: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearDemoData: () -> Unit,
    onOpenAddWallet: () -> Unit,
    onOpenWalletDetail: (String) -> Unit,
    onOpenAllTransactions: () -> Unit,
    onOpenReports: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit
) {
    var chartPeriod by remember { mutableStateOf("week") }
    val chartPoints = remember(chartPeriod, state.transactions, state.prefs.primaryCurrency) {
        computeChart(chartPeriod)
    }
    val activeWallets = remember(state.wallets) { state.wallets.filter { !it.archived } }
    val walletMap = remember(state.wallets) { state.wallets.associateBy { it.id } }
    val catMap = remember(state.categories) { state.categories.associateBy { it.id } }
    val recentTxs = remember(state.transactions) { state.transactions.take(5) }
    val masked = state.prefs.hideBalances
    val primaryCur = state.prefs.primaryCurrency

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.welcome,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = DateHelper.formatHeaderDate(state.prefs.languageCode),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaliTheme.colors.cardSurface)
                            .border(1.dp, MaliTheme.colors.outlineVariant, CircleShape)
                            .clickable(onClick = onOpenNotifications)
                            .testTag("notifications_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = strings.notificationsLabel,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        if (state.alerts.isNotEmpty() && state.prefs.notificationsEnabled) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 9.dp, end = 10.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaliTheme.colors.cardSurface)
                            .border(1.dp, MaliTheme.colors.outlineVariant, CircleShape)
                            .clickable(onClick = onOpenSettings)
                            .testTag("settings_shortcut_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = strings.settings,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Demo Banner
        if (state.prefs.isDemoMode) {
            item {
                MaliCard(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp),
                    backgroundColor = MaliTheme.colors.surfaceLow
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaliTheme.colors.mutedGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = strings.demoBanner,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = strings.clearDemo,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaliTheme.colors.mutedGold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onClearDemoData)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("clear_demo_banner_button")
                        )
                    }
                }
            }
        }

        // 3. Main Balance Card
        item {
            MaliCard(
                modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = strings.totalBalance,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            onClick = onToggleBalanceVisibility,
                            shape = CircleShape,
                            color = MaliTheme.colors.surfaceHigh,
                            modifier = Modifier.testTag("toggle_balance_visibility")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (masked) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = strings.hide,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (masked) strings.show else strings.hide,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LtrNumberText(
                            text = MoneyFormat.format(state.totalBalanceMinor, primaryCur, masked),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        val pct = state.monthlyChangePercent
                        val isPositive = (pct ?: 0.0) >= 0.0
                        val badgeColor = if (isPositive) MaliTheme.colors.incomeGreen else MaliTheme.colors.expenseCoral
                        val pctText = if (pct != null) {
                            val rounded = (pct * 10).roundToInt() / 10.0
                            "${if (rounded >= 0) "+" else ""}$rounded% ${strings.thisMonth}"
                        } else {
                            "0% ${strings.thisMonth}"
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaliTheme.colors.surfaceHigh
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPositive) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = pctText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor
                                )
                            }
                        }
                    }

                    if (state.hasUnconvertedCurrencies) {
                        HorizontalDivider(color = MaliTheme.colors.outlineVariant)
                        Text(
                            text = strings.multiCurrencyNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaliTheme.colors.mutedGold
                        )
                    }
                }
            }
        }

        // 4. Financial Summary: 3 Compact Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCompactCard(
                    label = strings.income,
                    amount = MoneyFormat.format(state.monthlyIncomeMinor, primaryCur, masked),
                    accent = MaliTheme.colors.incomeGreen,
                    icon = Icons.Rounded.ArrowDownward,
                    modifier = Modifier.weight(1f)
                )
                SummaryCompactCard(
                    label = strings.expenses,
                    amount = MoneyFormat.format(state.monthlyExpenseMinor, primaryCur, masked),
                    accent = MaliTheme.colors.expenseCoral,
                    icon = Icons.Rounded.ArrowUpward,
                    modifier = Modifier.weight(1f)
                )
                SummaryCompactCard(
                    label = strings.net,
                    amount = MoneyFormat.format(state.monthlyNetMinor, primaryCur, masked),
                    accent = MaliTheme.colors.mutedGold,
                    icon = Icons.Rounded.AccountBalance,
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 5. Wallet Overview Carousel
        item {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.wallets,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        onClick = onOpenAddWallet,
                        shape = CircleShape,
                        color = MaliTheme.colors.surfaceHigh,
                        border = BorderStroke(1.dp, MaliTheme.colors.outlineVariant),
                        modifier = Modifier.testTag("home_add_wallet_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = strings.addWallet,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(activeWallets, key = { it.id }) { wallet ->
                        WalletCarouselCard(
                            wallet = wallet,
                            langCode = state.prefs.languageCode,
                            masked = masked,
                            onClick = { onOpenWalletDetail(wallet.id) }
                        )
                    }
                }
            }
        }

        // 6. Interactive Financial Chart
        item {
            MaliCard(
                modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = strings.incomeVsExpenses,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                LegendDot(strings.income, MaliTheme.colors.incomeGreen)
                                LegendDot(strings.expenses, MaliTheme.colors.expenseCoral)
                            }
                        }

                        TextButton(onClick = onOpenReports) {
                            Text(
                                text = strings.navReports,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Period Pill Switcher
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaliTheme.colors.surfaceLow)
                            .border(1.dp, MaliTheme.colors.outlineVariant, CircleShape)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "week" to strings.week,
                            "month" to strings.month,
                            "year" to strings.year
                        ).forEach { (key, label) ->
                            val selected = chartPeriod == key
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { chartPeriod = key }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .testTag("chart_period_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    IncomeExpenseBarChart(
                        points = chartPoints,
                        currency = primaryCur,
                        masked = masked
                    )
                }
            }
        }

        // 7. Recent Transactions
        item {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.recentTransactions,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.viewAllTransactions,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenAllTransactions)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("view_all_transactions_button")
                    )
                }

                MaliCard(modifier = Modifier.fillMaxWidth()) {
                    if (recentTxs.isEmpty()) {
                        Text(
                            text = "لا توجد عمليات مسجلة بعد. اضغط (+) لإضافة أول عملية.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    } else {
                        recentTxs.forEachIndexed { idx, tx ->
                            TransactionRowItem(
                                tx = tx,
                                wallet = walletMap[tx.walletId],
                                toWallet = tx.toWalletId?.let { walletMap[it] },
                                category = tx.categoryId?.let { catMap[it] },
                                masked = masked,
                                onClick = { onEditTransaction(tx) }
                            )
                            if (idx < recentTxs.lastIndex) {
                                HorizontalDivider(color = MaliTheme.colors.outlineVariant)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun SummaryCompactCard(
    label: String,
    amount: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valueColor: Color = accent,
    modifier: Modifier = Modifier
) {
    MaliCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
            LtrNumberText(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                color = valueColor
            )
        }
    }
}

@Composable
private fun WalletCarouselCard(
    wallet: WalletEntity,
    langCode: String,
    masked: Boolean,
    onClick: () -> Unit
) {
    MaliCard(
        modifier = Modifier.width(172.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = IconCatalog.iconFor(wallet.iconName),
                    contentDescription = wallet.name,
                    tint = IconCatalog.colorFor(wallet.colorKey),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = wallet.currency,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column {
                Text(
                    text = wallet.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = walletTypeLabel(wallet.type, langCode),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            LtrNumberText(
                text = MoneyFormat.format(wallet.balanceMinor, wallet.currency, masked),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
