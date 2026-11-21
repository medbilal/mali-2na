package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.TransactionEntity
import com.example.data.local.WalletEntity
import com.example.ui.components.LtrNumberText
import com.example.ui.components.MaliCard
import com.example.ui.components.ReconcileBalanceDialog
import com.example.ui.components.SimpleSpendingBarChart
import com.example.ui.components.TransactionRowItem
import com.example.ui.components.WalletFormDialog
import com.example.ui.components.walletTypeLabel
import com.example.ui.theme.MaliTheme
import com.example.ui.viewmodel.MaliUiState
import com.example.util.AppStrings
import com.example.util.DateHelper
import com.example.util.IconCatalog
import com.example.util.MoneyFormat

@Composable
fun WalletsScreen(
    state: MaliUiState,
    strings: AppStrings,
    onOpenWalletDetail: (String) -> Unit,
    onSaveWallet: (String?, String, String, Long, String, String, String, String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    val activeWallets = remember(state.wallets) { state.wallets.filter { !it.archived } }
    val archivedWallets = remember(state.wallets) { state.wallets.filter { it.archived } }
    val masked = state.prefs.hideBalances

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.wallets,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.walletsSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showCreateDialog = true },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("new_wallet_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.newWallet, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        items(activeWallets.size) { index ->
            val w = activeWallets[index]
            WalletFullCard(
                wallet = w,
                langCode = state.prefs.languageCode,
                masked = masked,
                onClick = { onOpenWalletDetail(w.id) },
                modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)
            )
        }

        if (archivedWallets.isNotEmpty()) {
            item {
                Text(
                    text = strings.archivedWallets,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp).padding(top = 8.dp)
                )
            }
            items(archivedWallets.size) { index ->
                val w = archivedWallets[index]
                MaliCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 960.dp)
                        .alpha(0.75f),
                    backgroundColor = MaliTheme.colors.surfaceLow,
                    onClick = { onOpenWalletDetail(w.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(w.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${walletTypeLabel(w.type, state.prefs.languageCode)} • مؤرشفة",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LtrNumberText(
                            text = MoneyFormat.format(w.balanceMinor, w.currency, masked),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }

    if (showCreateDialog) {
        WalletFormDialog(
            existingWallet = null,
            primaryCurrency = state.prefs.primaryCurrency,
            strings = strings,
            langCode = state.prefs.languageCode,
            onDismiss = { showCreateDialog = false },
            onSave = { id, name, type, balanceMinor, currency, iconName, colorKey, desc ->
                onSaveWallet(id, name, type, balanceMinor, currency, iconName, colorKey, desc)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun WalletFullCard(
    wallet: WalletEntity,
    langCode: String,
    masked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MaliCard(
        modifier = modifier.testTag("wallet_card_${wallet.id}"),
        onClick = onClick
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = wallet.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${walletTypeLabel(wallet.type, langCode)} • ${wallet.currency}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (wallet.description.isNotBlank()) {
                        Text(
                            text = wallet.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaliTheme.colors.surfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = IconCatalog.iconFor(wallet.iconName),
                        contentDescription = wallet.name,
                        tint = IconCatalog.colorFor(wallet.colorKey),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            LtrNumberText(
                text = MoneyFormat.format(wallet.balanceMinor, wallet.currency, masked),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun WalletDetailScreen(
    walletId: String,
    state: MaliUiState,
    strings: AppStrings,
    onBack: () -> Unit,
    onSaveWallet: (String?, String, String, Long, String, String, String, String) -> Unit,
    onToggleArchive: (String) -> Unit,
    onReconcile: (String, Long) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit
) {
    BackHandler(onBack = onBack)

    val wallet = state.wallets.find { it.id == walletId }
    if (wallet == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("المحفظة غير موجودة", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showReconcileDialog by remember { mutableStateOf(false) }

    val walletTxs = remember(state.transactions, walletId) {
        state.transactions.filter { it.walletId == walletId || it.toWalletId == walletId }
    }
    val walletMap = remember(state.wallets) { state.wallets.associateBy { it.id } }
    val catMap = remember(state.categories) { state.categories.associateBy { it.id } }
    val masked = state.prefs.hideBalances

    val totalIncome = remember(walletTxs) {
        walletTxs.filter { it.type == "income" && it.walletId == walletId }.sumOf { it.amountMinor }
    }
    val totalExpense = remember(walletTxs) {
        walletTxs.filter { it.type == "expense" && it.walletId == walletId }.sumOf { it.amountMinor }
    }

    val weeklyBars = remember(walletTxs) {
        val monthPrefix = DateHelper.currentMonthPrefix()
        val mTxs = walletTxs.filter { it.dateIso.startsWith(monthPrefix) }
        val ranges = listOf(1..7, 8..14, 15..21, 22..31)
        listOf("أسبوع 1", "أسبوع 2", "أسبوع 3", "أسبوع 4").mapIndexed { idx, lbl ->
            val sum = mTxs.filter {
                val day = it.dateIso.substring(8, 10).toIntOrNull() ?: 1
                day in ranges[idx]
            }.sumOf { it.amountMinor }
            lbl to sum
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaliTheme.colors.cardSurface)
                            .border(1.dp, MaliTheme.colors.outlineVariant, CircleShape)
                            .clickable(onClick = onBack)
                            .testTag("wallet_detail_back"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "العودة للمحافظ",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Text(wallet.name, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "${walletTypeLabel(wallet.type, state.prefs.languageCode)} • ${wallet.currency}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        onClick = { showEditDialog = true },
                        shape = CircleShape,
                        color = MaliTheme.colors.cardSurface,
                        border = BorderStroke(1.dp, MaliTheme.colors.outlineVariant)
                    ) {
                        Text(
                            strings.editWallet,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    Surface(
                        onClick = { showReconcileDialog = true },
                        shape = CircleShape,
                        color = MaliTheme.colors.surfaceHigh,
                        border = BorderStroke(1.dp, MaliTheme.colors.outlineVariant)
                    ) {
                        Text(
                            strings.reconcileBalance,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaliTheme.colors.mutedGold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Balance & Stats
        item {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MaliCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            strings.currentBalance,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LtrNumberText(
                            text = MoneyFormat.format(wallet.balanceMinor, wallet.currency, masked),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MaliCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                strings.totalIncome,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LtrNumberText(
                                text = MoneyFormat.format(totalIncome, wallet.currency, masked),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaliTheme.colors.incomeGreen
                            )
                        }
                    }
                    MaliCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                strings.totalExpenses,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LtrNumberText(
                                text = MoneyFormat.format(totalExpense, wallet.currency, masked),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaliTheme.colors.expenseCoral
                            )
                        }
                    }
                }
            }
        }

        // Monthly Mini Chart
        item {
            MaliCard(modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(strings.walletActivity, style = MaterialTheme.typography.titleLarge)
                    SimpleSpendingBarChart(
                        bars = weeklyBars,
                        barColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Transactions & Archive Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.walletTransactions, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = if (wallet.archived) strings.unarchiveWallet else strings.archiveWallet,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaliTheme.colors.expenseCoral,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            onToggleArchive(wallet.id)
                            onBack()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        item {
            MaliCard(modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)) {
                if (walletTxs.isEmpty()) {
                    Text(
                        text = "لا توجد عمليات في هذه المحفظة بعد.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                } else {
                    walletTxs.forEachIndexed { idx, tx ->
                        TransactionRowItem(
                            tx = tx,
                            wallet = walletMap[tx.walletId],
                            toWallet = tx.toWalletId?.let { walletMap[it] },
                            category = tx.categoryId?.let { catMap[it] },
                            masked = masked,
                            onClick = { onEditTransaction(tx) }
                        )
                        if (idx < walletTxs.lastIndex) {
                            HorizontalDivider(color = MaliTheme.colors.outlineVariant)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }

    if (showEditDialog) {
        WalletFormDialog(
            existingWallet = wallet,
            primaryCurrency = state.prefs.primaryCurrency,
            strings = strings,
            langCode = state.prefs.languageCode,
            onDismiss = { showEditDialog = false },
            onSave = { id, name, type, balanceMinor, currency, iconName, colorKey, desc ->
                onSaveWallet(id, name, type, balanceMinor, currency, iconName, colorKey, desc)
                showEditDialog = false
            }
        )
    }

    if (showReconcileDialog) {
        ReconcileBalanceDialog(
            wallet = wallet,
            onDismiss = { showReconcileDialog = false },
            onConfirm = { actualMinor ->
                onReconcile(wallet.id, actualMinor)
                showReconcileDialog = false
            }
        )
    }
}
