package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.TransactionEntity
import com.example.ui.components.LtrNumberText
import com.example.ui.components.MaliCard
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.MaliTheme
import com.example.ui.viewmodel.MaliUiState
import com.example.util.AppStrings
import com.example.util.DateHelper
import com.example.util.MoneyFormat

@Composable
fun TransactionsScreen(
    state: MaliUiState,
    strings: AppStrings,
    convertToPrimary: (Long, String, Double?) -> Long,
    onAddTransaction: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("all") }
    var filterWalletId by remember { mutableStateOf("all") }
    var filterCategoryId by remember { mutableStateOf("all") }
    var filterDateMode by remember { mutableStateOf("all") } // all, today, week, month
    var sortOrder by remember { mutableStateOf("newest") }
    var pendingDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    val walletMap = remember(state.wallets) { state.wallets.associateBy { it.id } }
    val catMap = remember(state.categories) { state.categories.associateBy { it.id } }
    val masked = state.prefs.hideBalances
    val primaryCur = state.prefs.primaryCurrency

    val filtered = remember(
        state.transactions, searchQuery, filterType, filterWalletId,
        filterCategoryId, filterDateMode, sortOrder
    ) {
        val q = searchQuery.trim().lowercase()
        val today = DateHelper.todayIso()
        val weekAgo = DateHelper.daysOffsetIso(-7)
        val monthPrefix = DateHelper.currentMonthPrefix()

        val list = state.transactions.filter { tx ->
            if (filterType != "all" && tx.type != filterType) return@filter false
            if (filterWalletId != "all" && tx.walletId != filterWalletId && tx.toWalletId != filterWalletId) return@filter false
            if (filterCategoryId != "all" && tx.categoryId != filterCategoryId) return@filter false
            when (filterDateMode) {
                "today" -> if (tx.dateIso != today) return@filter false
                "week" -> if (tx.dateIso < weekAgo) return@filter false
                "month" -> if (!tx.dateIso.startsWith(monthPrefix)) return@filter false
            }
            if (q.isNotEmpty()) {
                val catName = tx.categoryId?.let { catMap[it]?.name } ?: ""
                val wName = walletMap[tx.walletId]?.name ?: ""
                val hay = "${tx.note} $catName $wName".lowercase()
                if (!hay.contains(q)) return@filter false
            }
            true
        }
        if (sortOrder == "newest") {
            list.sortedWith(compareByDescending<TransactionEntity> { it.dateIso }.thenByDescending { it.createdAt })
        } else {
            list.sortedWith(compareBy<TransactionEntity> { it.dateIso }.thenBy { it.createdAt })
        }
    }

    val groupedByDate = remember(filtered) {
        filtered.groupBy { it.dateIso }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.transactionsHistory,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.transactionsSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAddTransaction,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("tx_screen_add_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("جديد", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Search & Filters Card
        item {
            MaliCard(modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(strings.searchPlaceholder) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaliTheme.colors.surfaceLow,
                            unfocusedContainerColor = MaliTheme.colors.surfaceLow,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaliTheme.colors.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("tx_search_input")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterDropdownChip(
                                label = when (filterType) {
                                    "expense" -> strings.expenses
                                    "income" -> strings.income
                                    "transfer" -> strings.transferTab
                                    else -> strings.allTypes
                                },
                                options = listOf(
                                    "all" to strings.allTypes,
                                    "expense" to strings.expenses,
                                    "income" to strings.income,
                                    "transfer" to strings.transferTab
                                ),
                                onSelect = { filterType = it }
                            )
                        }
                        item {
                            FilterDropdownChip(
                                label = if (filterWalletId == "all") strings.allWallets
                                else walletMap[filterWalletId]?.name ?: strings.allWallets,
                                options = listOf("all" to strings.allWallets) +
                                    state.wallets.map { it.id to it.name },
                                onSelect = { filterWalletId = it }
                            )
                        }
                        item {
                            FilterDropdownChip(
                                label = if (filterCategoryId == "all") strings.allCategories
                                else catMap[filterCategoryId]?.name ?: strings.allCategories,
                                options = listOf("all" to strings.allCategories) +
                                    state.categories.map { it.id to it.name },
                                onSelect = { filterCategoryId = it }
                            )
                        }
                        item {
                            FilterDropdownChip(
                                label = when (filterDateMode) {
                                    "today" -> strings.today
                                    "week" -> strings.thisWeek
                                    "month" -> strings.month
                                    else -> strings.date
                                },
                                options = listOf(
                                    "all" to "كل التواريخ",
                                    "today" to strings.today,
                                    "week" to strings.thisWeek,
                                    "month" to strings.month
                                ),
                                onSelect = { filterDateMode = it }
                            )
                        }
                        item {
                            FilterDropdownChip(
                                label = if (sortOrder == "newest") strings.newestFirst else strings.oldestFirst,
                                options = listOf(
                                    "newest" to strings.newestFirst,
                                    "oldest" to strings.oldestFirst
                                ),
                                onSelect = { sortOrder = it }
                            )
                        }
                    }
                }
            }
        }

        if (groupedByDate.isEmpty()) {
            item {
                MaliCard(modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)) {
                    Text(
                        text = "لا توجد عمليات مطابقة للبحث.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(28.dp)
                    )
                }
            }
        } else {
            groupedByDate.forEach { (dateIso, dayItems) ->
                item(key = "header_$dateIso") {
                    val dayNet = dayItems.sumOf { t ->
                        val amt = convertToPrimary(t.amountMinor, t.currency, t.exchangeRate)
                        when (t.type) {
                            "income" -> amt
                            "expense" -> -amt
                            else -> 0L
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateIso,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LtrNumberText(
                                text = (if (dayNet >= 0) "+" else "") +
                                    MoneyFormat.format(dayNet, primaryCur, masked),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (dayNet >= 0) MaliTheme.colors.incomeGreen else MaliTheme.colors.expenseCoral
                            )
                        }
                        MaliCard(modifier = Modifier.fillMaxWidth()) {
                            dayItems.forEachIndexed { idx, tx ->
                                TransactionRowItem(
                                    tx = tx,
                                    wallet = walletMap[tx.walletId],
                                    toWallet = tx.toWalletId?.let { walletMap[it] },
                                    category = tx.categoryId?.let { catMap[it] },
                                    masked = masked,
                                    showActions = true,
                                    onClick = { onEditTransaction(tx) },
                                    onEdit = { onEditTransaction(tx) },
                                    onDelete = { pendingDelete = tx }
                                )
                                if (idx < dayItems.lastIndex) {
                                    HorizontalDivider(color = MaliTheme.colors.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = MaliTheme.colors.cardSurface,
            title = { Text(strings.deleteTxTitle, style = MaterialTheme.typography.titleLarge) },
            text = { Text(strings.deleteTxBody, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTransaction(pendingDelete!!.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text(strings.deleteConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun FilterDropdownChip(
    label: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(10.dp),
            color = MaliTheme.colors.surfaceLow,
            border = BorderStroke(1.dp, MaliTheme.colors.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaliTheme.colors.cardSurface
        ) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text, color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
