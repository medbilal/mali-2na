package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.DebtEntity
import com.example.ui.components.LtrNumberText
import com.example.ui.components.MaliCard
import com.example.ui.theme.MaliTheme
import com.example.ui.viewmodel.MaliUiState
import com.example.util.AppStrings
import com.example.util.DateHelper
import com.example.util.IconCatalog
import com.example.util.MoneyFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoreHubScreen(
    state: MaliUiState,
    strings: AppStrings,
    initialSection: String = "budgets",
    statusMessage: String?,
    convertToPrimary: (Long, String, Double?) -> Long,
    onOpenReports: () -> Unit,
    onOpenWallets: () -> Unit,
    onSaveBudget: (String?, String, Long) -> Unit,
    onDeleteBudget: (String) -> Unit,
    onSaveDebt: (String, String, Long, String, String?, String, String) -> Unit,
    onRecordDebtPayment: (String, Long, Boolean) -> Unit,
    onDeleteDebt: (String) -> Unit,
    onSaveRecurring: (String, String, Long, String, String, String, String) -> Unit,
    onToggleRecurring: (String) -> Unit,
    onExecuteRecurringNow: (String) -> Unit,
    onDeleteRecurring: (String) -> Unit,
    onSaveCategory: (String?, String, String, String, String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetPrimaryCurrency: (String) -> Unit,
    onSetCurrencyConversion: (Boolean) -> Unit,
    onUpdateCurrencyRate: (String, Double) -> Unit,
    onSetDarkTheme: (Boolean) -> Unit,
    onSetNotifications: (Boolean) -> Unit,
    onEnablePin: (String) -> Unit,
    onDisablePin: () -> Unit,
    onSetBiometric: (Boolean) -> Unit,
    onClearDemoData: () -> Unit,
    onRestoreDemoData: () -> Unit,
    onExportJson: ((String) -> Unit) -> Unit,
    onExportCsv: ((String) -> Unit) -> Unit,
    onImportJson: (String) -> Unit,
    onClearStatusMessage: () -> Unit
) {
    var activeSection by remember(initialSection) { mutableStateOf(initialSection) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }
    var showDebtDialog by remember { mutableStateOf(false) }
    var payingDebt by remember { mutableStateOf<DebtEntity?>(null) }
    var showRecurringDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showRatesDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) onImportJson(text)
            } catch (_: Exception) {
            }
        }
    }

    fun shareText(title: String, content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    val catMap = remember(state.categories) { state.categories.associateBy { it.id } }
    val masked = state.prefs.hideBalances
    val primaryCur = state.prefs.primaryCurrency
    val curMonth = DateHelper.currentMonthPrefix()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header + Reports shortcut
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.moreTitle, style = MaterialTheme.typography.headlineLarge)
                        Text(
                            strings.moreSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        onClick = onOpenReports,
                        shape = CircleShape,
                        color = MaliTheme.colors.surfaceHigh,
                        border = BorderStroke(1.dp, MaliTheme.colors.outlineVariant),
                        modifier = Modifier.testTag("more_open_reports")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Analytics,
                                contentDescription = strings.navReports,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                strings.navReports,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Section Switcher
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val tabs = listOf(
                        "budgets" to strings.budgets,
                        "debts" to strings.debts,
                        "recurring" to strings.recurring,
                        "categories" to strings.categories,
                        "settings" to strings.settings
                    )
                    items(tabs) { (key, label) ->
                        val selected = activeSection == key
                        Surface(
                            onClick = {
                                activeSection = key
                                onClearStatusMessage()
                            },
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaliTheme.colors.cardSurface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaliTheme.colors.outlineVariant
                            ),
                            modifier = Modifier.testTag("more_tab_$key")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }
        }

        // 1. BUDGETS
        if (activeSection == "budgets") {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.monthlyBudgets, style = MaterialTheme.typography.titleLarge)
                    Button(
                        onClick = { editingBudget = null; showBudgetDialog = true },
                        shape = CircleShape,
                        modifier = Modifier.testTag("add_budget_button")
                    ) {
                        Text(strings.addBudget, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (state.budgets.isEmpty()) {
                item {
                    MaliCard(modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)) {
                        Text(
                            "لا توجد ميزانيات محددة بعد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(state.budgets, key = { it.id }) { budget ->
                    val cat = catMap[budget.categoryId]
                    val spent = state.transactions
                        .filter { it.type == "expense" && it.categoryId == budget.categoryId && it.dateIso.startsWith(curMonth) }
                        .sumOf { convertToPrimary(it.amountMinor, it.currency, it.exchangeRate) }
                    val pct = if (budget.limitMinor > 0L) ((spent * 100L) / budget.limitMinor).toInt() else 0
                    val rem = budget.limitMinor - spent
                    val statusColor = when {
                        pct >= 100 -> MaliTheme.colors.expenseCoral
                        pct >= 80 -> MaliTheme.colors.warningAmber
                        else -> MaterialTheme.colorScheme.primary
                    }
                    val statusLabel = when {
                        pct >= 100 -> strings.budgetExceeded
                        pct >= 80 -> strings.budgetWarning
                        else -> strings.budgetNormal
                    }

                    MaliCard(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp),
                        onClick = { editingBudget = budget; showBudgetDialog = true }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat?.name ?: "تصنيف", style = MaterialTheme.typography.titleMedium)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("$statusLabel ($pct%)", style = MaterialTheme.typography.labelSmall, color = statusColor)
                                    IconButton(onClick = { onDeleteBudget(budget.id) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(MaliTheme.colors.surfaceLow)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((pct / 100f).coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                LtrNumberText(
                                    text = "${strings.spent}: ${MoneyFormat.format(spent, primaryCur, masked)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                LtrNumberText(
                                    text = "${strings.remaining}: ${MoneyFormat.format(rem, primaryCur, masked)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. DEBTS
        if (activeSection == "debts") {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.debtsManagement, style = MaterialTheme.typography.titleLarge)
                    Button(
                        onClick = { showDebtDialog = true },
                        shape = CircleShape,
                        modifier = Modifier.testTag("add_debt_button")
                    ) {
                        Text(strings.addDebt, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            val owedToMe = state.debts.filter { it.direction == "owed_to_me" }
            val iOwe = state.debts.filter { it.direction == "i_owe" }

            item {
                Text(
                    strings.owedToMe,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaliTheme.colors.incomeGreen,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)
                )
            }
            if (owedToMe.isEmpty()) {
                item {
                    Text(
                        "لا توجد سجلات",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)
                    )
                }
            } else {
                items(owedToMe, key = { it.id }) { debt ->
                    DebtRowCard(
                        debt = debt,
                        strings = strings,
                        masked = masked,
                        onPay = { payingDebt = debt },
                        onDelete = { onDeleteDebt(debt.id) },
                        modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)
                    )
                }
            }

            item {
                Text(
                    strings.iOwe,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaliTheme.colors.expenseCoral,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp).padding(top = 6.dp)
                )
            }
            if (iOwe.isEmpty()) {
                item {
                    Text(
                        "لا توجد سجلات",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)
                    )
                }
            } else {
                items(iOwe, key = { it.id }) { debt ->
                    DebtRowCard(
                        debt = debt,
                        strings = strings,
                        masked = masked,
                        onPay = { payingDebt = debt },
                        onDelete = { onDeleteDebt(debt.id) },
                        modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)
                    )
                }
            }
        }

        // 3. RECURRING
        if (activeSection == "recurring") {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.recurringTitle, style = MaterialTheme.typography.titleLarge)
                    Button(
                        onClick = { showRecurringDialog = true },
                        shape = CircleShape,
                        modifier = Modifier.testTag("add_recurring_button")
                    ) {
                        Text(strings.addRecurring, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            items(state.recurring, key = { it.id }) { item ->
                val freqLabel = when (item.frequency) {
                    "daily" -> "يومي"
                    "weekly" -> "أسبوعي"
                    "yearly" -> "سنوي"
                    else -> "شهري"
                }
                val color = if (item.type == "income") MaliTheme.colors.incomeGreen else MaliTheme.colors.expenseCoral
                MaliCard(modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "$freqLabel • القادم: ${item.nextDateIso}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LtrNumberText(
                                text = MoneyFormat.format(item.amountMinor, primaryCur, masked),
                                style = MaterialTheme.typography.titleMedium,
                                color = color
                            )
                            IconButton(
                                onClick = { onExecuteRecurringNow(item.id) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = strings.executeNow,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                onClick = { onToggleRecurring(item.id) },
                                shape = CircleShape,
                                color = MaliTheme.colors.surfaceHigh
                            ) {
                                Text(
                                    text = if (item.active) strings.pause else strings.activate,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteRecurring(item.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "حذف",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. CATEGORIES
        if (activeSection == "categories") {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.categoriesTitle, style = MaterialTheme.typography.titleLarge)
                    Button(
                        onClick = { editingCategory = null; showCategoryDialog = true },
                        shape = CircleShape,
                        modifier = Modifier.testTag("add_category_button")
                    ) {
                        Text(strings.addCategory, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            items(state.categories, key = { it.id }) { cat ->
                MaliCard(modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaliTheme.colors.surfaceHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    IconCatalog.iconFor(cat.iconName),
                                    contentDescription = cat.name,
                                    tint = IconCatalog.colorFor(cat.colorKey),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(cat.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if (cat.type == "income") strings.incomeTab else strings.expenseTab,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = { editingCategory = cat; showCategoryDialog = true }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "تعديل", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onDeleteCategory(cat.id) }) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "حذف",
                                    tint = MaliTheme.colors.expenseCoral,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. SETTINGS
        if (activeSection == "settings") {
            item {
                MaliCard(modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)) {
                    Column {
                        // Language
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(strings.primaryLanguage, style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("ar" to "العربية", "fr" to "Français", "en" to "English").forEach { (code, lbl) ->
                                    val sel = state.prefs.languageCode == code
                                    Surface(
                                        onClick = { onSetLanguage(code) },
                                        shape = CircleShape,
                                        color = if (sel) MaterialTheme.colorScheme.primary else MaliTheme.colors.surfaceLow,
                                        border = BorderStroke(1.dp, if (sel) MaterialTheme.colorScheme.primary else MaliTheme.colors.outlineVariant)
                                    ) {
                                        Text(
                                            lbl,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaliTheme.colors.outlineVariant)

                        // Primary Currency
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.primaryCurrency, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    strings.exchangeRates,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { showRatesDialog = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("MRU", "USD", "EUR", "MAD", "XOF").forEach { cur ->
                                    val sel = state.prefs.primaryCurrency == cur
                                    Surface(
                                        onClick = { onSetPrimaryCurrency(cur) },
                                        shape = CircleShape,
                                        color = if (sel) MaterialTheme.colorScheme.primary else MaliTheme.colors.surfaceLow,
                                        border = BorderStroke(1.dp, if (sel) MaterialTheme.colorScheme.primary else MaliTheme.colors.outlineVariant)
                                    ) {
                                        Text(
                                            cur,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaliTheme.colors.outlineVariant)

                        SettingsToggleRow(
                            title = strings.autoConversion,
                            subtitle = strings.autoConversionDesc,
                            checked = state.prefs.currencyConversion,
                            onCheckedChange = onSetCurrencyConversion
                        )
                        HorizontalDivider(color = MaliTheme.colors.outlineVariant)

                        SettingsToggleRow(
                            title = strings.darkTheme,
                            subtitle = strings.darkThemeDesc,
                            checked = state.prefs.darkTheme,
                            onCheckedChange = onSetDarkTheme
                        )
                        HorizontalDivider(color = MaliTheme.colors.outlineVariant)

                        SettingsToggleRow(
                            title = strings.notificationsLabel,
                            subtitle = strings.notificationsDesc,
                            checked = state.prefs.notificationsEnabled,
                            onCheckedChange = onSetNotifications
                        )
                        HorizontalDivider(color = MaliTheme.colors.outlineVariant)

                        SettingsToggleRow(
                            title = strings.pinLock,
                            subtitle = strings.pinLockDesc,
                            checked = state.prefs.pinLockEnabled,
                            onCheckedChange = { enable ->
                                if (enable) showPinDialog = true else onDisablePin()
                            }
                        )

                        if (state.prefs.pinLockEnabled) {
                            HorizontalDivider(color = MaliTheme.colors.outlineVariant)
                            SettingsToggleRow(
                                title = strings.biometricLock,
                                subtitle = strings.biometricLockDesc,
                                checked = state.prefs.biometricEnabled,
                                onCheckedChange = onSetBiometric
                            )
                        }
                    }
                }
            }

            // Backup, Export & Demo Data
            item {
                MaliCard(modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(strings.backupData, style = MaterialTheme.typography.titleMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onExportJson { json -> shareText("MALI Backup JSON", json) } },
                                shape = CircleShape
                            ) {
                                Text(strings.exportJson, style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { onExportCsv { csv -> shareText("MALI Transactions CSV", csv) } },
                                shape = CircleShape
                            ) {
                                Text(strings.exportCsv, style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                                shape = CircleShape
                            ) {
                                Text(strings.importJson, style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = onOpenWallets,
                                shape = CircleShape
                            ) {
                                Text(strings.wallets, style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = onClearDemoData,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Text(strings.clearDemoData, style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = onRestoreDemoData,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaliTheme.colors.surfaceHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(strings.restoreDemoData, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        if (!statusMessage.isNullOrBlank()) {
                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaliTheme.colors.incomeGreen
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = strings.aboutFooter,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }

    if (showBudgetDialog) {
        BudgetFormDialog(
            existingBudget = editingBudget,
            expenseCategories = state.categories.filter { it.type == "expense" },
            primaryCurrency = primaryCur,
            onDismiss = { showBudgetDialog = false },
            onSave = { id, catId, limit ->
                onSaveBudget(id, catId, limit)
                showBudgetDialog = false
            }
        )
    }

    if (showDebtDialog) {
        DebtFormDialog(
            wallets = state.wallets,
            primaryCurrency = primaryCur,
            strings = strings,
            langCode = state.prefs.languageCode,
            onDismiss = { showDebtDialog = false },
            onSave = { dir, person, amt, due, wId, notes, cur ->
                onSaveDebt(dir, person, amt, due, wId, notes, cur)
                showDebtDialog = false
            }
        )
    }

    if (payingDebt != null) {
        DebtPaymentDialog(
            debt = payingDebt!!,
            onDismiss = { payingDebt = null },
            onConfirm = { amt, updateW ->
                onRecordDebtPayment(payingDebt!!.id, amt, updateW)
                payingDebt = null
            }
        )
    }

    if (showRecurringDialog) {
        RecurringFormDialog(
            wallets = state.wallets,
            categories = state.categories,
            strings = strings,
            langCode = state.prefs.languageCode,
            onDismiss = { showRecurringDialog = false },
            onSave = { title, type, amt, wId, catId, freq, nextDate ->
                onSaveRecurring(title, type, amt, wId, catId, freq, nextDate)
                showRecurringDialog = false
            }
        )
    }

    if (showCategoryDialog) {
        CategoryFormDialog(
            existing = editingCategory,
            strings = strings,
            onDismiss = { showCategoryDialog = false },
            onSave = { id, name, type, icon, color ->
                onSaveCategory(id, name, type, icon, color)
                showCategoryDialog = false
            }
        )
    }

    if (showRatesDialog) {
        ExchangeRatesDialog(
            rates = state.rates,
            onDismiss = { showRatesDialog = false },
            onUpdateRate = onUpdateCurrencyRate
        )
    }

    if (showPinDialog) {
        SetPinDialog(
            onDismiss = { showPinDialog = false },
            onSetPin = { rawPin ->
                onEnablePin(rawPin)
                showPinDialog = false
            }
        )
    }
}

@Composable
private fun DebtRowCard(
    debt: DebtEntity,
    strings: AppStrings,
    masked: Boolean,
    onPay: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rem = (debt.totalAmountMinor - debt.paidAmountMinor).coerceAtLeast(0L)
    val done = rem == 0L

    MaliCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(debt.personName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${strings.remaining}: ${MoneyFormat.format(rem, debt.currency, masked)} / ${MoneyFormat.format(debt.totalAmountMinor, debt.currency, masked)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${strings.dueDate}: ${debt.dueDateIso}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (done) {
                    Surface(shape = CircleShape, color = MaliTheme.colors.surfaceHigh) {
                        Text(
                            strings.paidOff,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaliTheme.colors.incomeGreen,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Surface(
                        onClick = onPay,
                        shape = CircleShape,
                        color = MaliTheme.colors.surfaceHigh,
                        border = BorderStroke(1.dp, MaliTheme.colors.outlineVariant)
                    ) {
                        Text(
                            strings.payDebt,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
