package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.CurrencyRateEntity
import com.example.data.local.DebtEntity
import com.example.data.local.WalletEntity
import com.example.ui.components.DatePickerField
import com.example.ui.components.MaliCard
import com.example.ui.components.WalletDropdownField
import com.example.ui.theme.MaliTheme
import com.example.util.AppStrings
import com.example.util.DateHelper
import com.example.util.IconCatalog
import com.example.util.MoneyFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetFormDialog(
    existingBudget: BudgetEntity?,
    expenseCategories: List<CategoryEntity>,
    primaryCurrency: String,
    onDismiss: () -> Unit,
    onSave: (String?, String, Long) -> Unit
) {
    var selectedCatId by remember(existingBudget) {
        mutableStateOf(existingBudget?.categoryId ?: expenseCategories.firstOrNull()?.id ?: "")
    }
    var limitStr by remember(existingBudget) {
        mutableStateOf(
            if (existingBudget != null) MoneyFormat.minorToMajorString(existingBudget.limitMinor)
            else "25000"
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        MaliCard(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("إعداد الميزانية", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "إغلاق") }
                }

                Text("التصنيف", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    expenseCategories.forEach { cat ->
                        val sel = cat.id == selectedCatId
                        Surface(
                            onClick = { selectedCatId = cat.id },
                            shape = CircleShape,
                            color = if (sel) MaterialTheme.colorScheme.primary else MaliTheme.colors.surfaceLow,
                            border = BorderStroke(1.dp, if (sel) MaterialTheme.colorScheme.primary else MaliTheme.colors.outlineVariant)
                        ) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = limitStr,
                    onValueChange = { if (it.matches(Regex("^\\d*([.,]\\d{0,2})?$"))) limitStr = it },
                    label = { Text("الحد الشهري ($primaryCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val minor = MoneyFormat.parseToMinor(limitStr) ?: return@Button
                        if (minor > 0L && selectedCatId.isNotEmpty()) {
                            onSave(existingBudget?.id, selectedCatId, minor)
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("حفظ الميزانية")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DebtFormDialog(
    wallets: List<WalletEntity>,
    primaryCurrency: String,
    strings: AppStrings,
    langCode: String,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, String, String?, String, String) -> Unit
) {
    val activeWallets = remember(wallets) { wallets.filter { !it.archived } }
    var direction by remember { mutableStateOf("owed_to_me") }
    var personName by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(DateHelper.daysOffsetIso(14)) }
    var walletId by remember { mutableStateOf(activeWallets.firstOrNull()?.id ?: "") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        MaliCard(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("إضافة سجل دين", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "إغلاق") }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "owed_to_me" to strings.owedToMe,
                        "i_owe" to strings.iOwe
                    ).forEach { (dir, lbl) ->
                        val sel = direction == dir
                        Surface(
                            onClick = { direction = dir },
                            shape = CircleShape,
                            color = if (sel) MaterialTheme.colorScheme.primary else MaliTheme.colors.surfaceLow,
                            border = BorderStroke(1.dp, if (sel) MaterialTheme.colorScheme.primary else MaliTheme.colors.outlineVariant)
                        ) {
                            Text(
                                lbl,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("اسم الشخص أو الجهة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { if (it.matches(Regex("^\\d*([.,]\\d{0,2})?$"))) amountStr = it },
                    label = { Text("المبلغ ($primaryCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                DatePickerField(
                    label = strings.dueDate,
                    dateIso = dueDate,
                    onDateSelected = { dueDate = it }
                )
                if (activeWallets.isNotEmpty()) {
                    WalletDropdownField(
                        label = strings.wallet,
                        wallets = activeWallets,
                        selectedId = walletId,
                        langCode = langCode,
                        onSelect = { walletId = it }
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val minor = MoneyFormat.parseToMinor(amountStr) ?: return@Button
                        if (personName.isNotBlank() && minor > 0L) {
                            val wCur = activeWallets.find { it.id == walletId }?.currency ?: primaryCurrency
                            onSave(direction, personName.trim(), minor, dueDate, walletId.ifBlank { null }, notes.trim(), wCur)
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("حفظ الدين")
                }
            }
        }
    }
}

@Composable
fun DebtPaymentDialog(
    debt: DebtEntity,
    onDismiss: () -> Unit,
    onConfirm: (Long, Boolean) -> Unit
) {
    val rem = (debt.totalAmountMinor - debt.paidAmountMinor).coerceAtLeast(0L)
    var payStr by remember(debt) { mutableStateOf(MoneyFormat.minorToMajorString(rem)) }
    var updateWallet by remember { mutableStateOf(debt.walletId != null) }

    Dialog(onDismissRequest = onDismiss) {
        MaliCard(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تسجيل دفعة", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "إغلاق") }
                }
                Text(
                    text = "${debt.personName} • المتبقي: ${MoneyFormat.format(rem, debt.currency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = payStr,
                    onValueChange = { if (it.matches(Regex("^\\d*([.,]\\d{0,2})?$"))) payStr = it },
                    label = { Text("مبلغ الدفعة (${debt.currency})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (debt.walletId != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { updateWallet = !updateWallet }
                    ) {
                        Checkbox(checked = updateWallet, onCheckedChange = { updateWallet = it })
                        Text("تحديث رصيد المحفظة تلقائياً", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Button(
                    onClick = {
                        val minor = MoneyFormat.parseToMinor(payStr) ?: return@Button
                        if (minor > 0L) onConfirm(minor, updateWallet)
                    },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("تأكيد الدفعة")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecurringFormDialog(
    wallets: List<WalletEntity>,
    categories: List<CategoryEntity>,
    strings: AppStrings,
    langCode: String,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, String, String, String, String) -> Unit
) {
    val activeWallets = remember(wallets) { wallets.filter { !it.archived } }
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("expense") }
    var amountStr by remember { mutableStateOf("") }
    var walletId by remember { mutableStateOf(activeWallets.firstOrNull()?.id ?: "") }
    val cats = remember(categories, type) { categories.filter { it.type == type } }
    var catId by remember(cats) { mutableStateOf(cats.firstOrNull()?.id ?: "") }
    var frequency by remember { mutableStateOf("monthly") }
    var nextDate by remember { mutableStateOf(DateHelper.daysOffsetIso(30)) }

    Dialog(onDismissRequest = onDismiss) {
        MaliCard(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("إضافة عملية متكررة", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "إغلاق") }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("expense" to strings.expenseTab, "income" to strings.incomeTab).forEach { (k, lbl) ->
                        val sel = type == k
                        Surface(
                            onClick = { type = k },
                            shape = CircleShape,
                            color = if (sel) MaterialTheme.colorScheme.primary else MaliTheme.colors.surfaceLow
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

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("العنوان (مثال: إيجار، اشتراك)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { if (it.matches(Regex("^\\d*([.,]\\d{0,2})?$"))) amountStr = it },
                    label = { Text("المبلغ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (activeWallets.isNotEmpty()) {
                    WalletDropdownField(
                        label = strings.wallet,
                        wallets = activeWallets,
                        selectedId = walletId,
                        langCode = langCode,
                        onSelect = { walletId = it }
                    )
                }

                Text("التكرار", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("daily" to "يومي", "weekly" to "أسبوعي", "monthly" to "شهري", "yearly" to "سنوي").forEach { (f, lbl) ->
                        val sel = frequency == f
                        Surface(
                            onClick = { frequency = f },
                            shape = CircleShape,
                            color = if (sel) MaterialTheme.colorScheme.primary else MaliTheme.colors.surfaceLow
                        ) {
                            Text(
                                lbl,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                DatePickerField(label = "التاريخ القادم", dateIso = nextDate, onDateSelected = { nextDate = it })

                Button(
                    onClick = {
                        val minor = MoneyFormat.parseToMinor(amountStr) ?: return@Button
                        if (title.isNotBlank() && minor > 0L && walletId.isNotEmpty()) {
                            onSave(title.trim(), type, minor, walletId, catId, frequency, nextDate)
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("حفظ")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryFormDialog(
    existing: CategoryEntity?,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, String, String) -> Unit
) {
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var type by remember(existing) { mutableStateOf(existing?.type ?: "expense") }
    var iconName by remember(existing) { mutableStateOf(existing?.iconName ?: "category") }
    var colorKey by remember(existing) { mutableStateOf(existing?.colorKey ?: "emerald") }

    Dialog(onDismissRequest = onDismiss) {
        MaliCard(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (existing == null) "إضافة تصنيف" else "تعديل التصنيف", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "إغلاق") }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم التصنيف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("expense" to strings.expenseTab, "income" to strings.incomeTab).forEach { (k, lbl) ->
                        val sel = type == k
                        Surface(
                            onClick = { type = k },
                            shape = CircleShape,
                            color = if (sel) MaterialTheme.colorScheme.primary else MaliTheme.colors.surfaceLow
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

                Text("الأيقونة", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconCatalog.categoryIconKeys.forEach { key ->
                        val sel = iconName == key
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaliTheme.colors.surfaceLow)
                                .border(1.dp, if (sel) MaterialTheme.colorScheme.primary else MaliTheme.colors.outlineVariant, CircleShape)
                                .clickable { iconName = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                IconCatalog.iconFor(key),
                                contentDescription = null,
                                tint = IconCatalog.colorFor(colorKey),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Text("اللون", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconCatalog.colorKeys.take(6).forEach { c ->
                        val sel = colorKey == c
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(IconCatalog.colorFor(c))
                                .border(if (sel) 2.5.dp else 1.dp, if (sel) Color.White else Color.Transparent, CircleShape)
                                .clickable { colorKey = c }
                        )
                    }
                }

                Button(
                    onClick = {
                        if (name.isNotBlank()) onSave(existing?.id, name.trim(), type, iconName, colorKey)
                    },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("حفظ التصنيف")
                }
            }
        }
    }
}

@Composable
fun ExchangeRatesDialog(
    rates: List<CurrencyRateEntity>,
    onDismiss: () -> Unit,
    onUpdateRate: (String, Double) -> Unit
) {
    val editableCodes = listOf("USD", "EUR", "MAD", "XOF")
    val rateValues = remember(rates) {
        mutableStateOf(
            editableCodes.associateWith { code ->
                rates.find { it.currencyCode == code }?.rateToMru?.toString() ?: "1.0"
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        MaliCard(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("أسعار الصرف مقابل MRU", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "إغلاق") }
                }

                editableCodes.forEach { code ->
                    OutlinedTextField(
                        value = rateValues.value[code] ?: "",
                        onValueChange = { next ->
                            if (next.matches(Regex("^\\d*(\\.\\d{0,4})?$"))) {
                                rateValues.value = rateValues.value + (code to next)
                            }
                        },
                        label = { Text("1 $code = MRU") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = {
                        editableCodes.forEach { code ->
                            val parsed = rateValues.value[code]?.toDoubleOrNull()
                            if (parsed != null && parsed > 0.0) onUpdateRate(code, parsed)
                        }
                        onDismiss()
                    },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("حفظ الأسعار")
                }
            }
        }
    }
}

@Composable
fun SetPinDialog(
    onDismiss: () -> Unit,
    onSetPin: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        MaliCard(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("تعيين رمز قفل من 4 أرقام", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) { pin = it; err = null } },
                    label = { Text("الرمز السري (4 أرقام)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (err != null) {
                    Text(err!!, style = MaterialTheme.typography.labelSmall, color = MaliTheme.colors.expenseCoral)
                }
                Button(
                    onClick = {
                        if (pin.length != 4) err = "أدخل 4 أرقام بالضبط"
                        else onSetPin(pin)
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("تفعيل القفل")
                }
            }
        }
    }
}
