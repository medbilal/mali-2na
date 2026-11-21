package com.example.ui.components

import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.WalletEntity
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.MaliTheme
import com.example.ui.viewmodel.FinanceAlert
import com.example.util.AppStrings
import com.example.util.DateHelper
import com.example.util.IconCatalog
import com.example.util.MoneyFormat

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEntrySheet(
    existingTx: TransactionEntity?,
    initialType: String,
    wallets: List<WalletEntity>,
    categories: List<CategoryEntity>,
    strings: AppStrings,
    langCode: String,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        type: String,
        amountMinor: Long,
        walletId: String,
        toWalletId: String?,
        categoryId: String?,
        dateIso: String,
        note: String,
        attachmentUri: String?
    ) -> Unit
) {
    val activeWallets = remember(wallets) { wallets.filter { !it.archived } }
    var txType by remember(existingTx, initialType) {
        mutableStateOf(existingTx?.type ?: initialType)
    }
    var amountStr by remember(existingTx) {
        mutableStateOf(
            if (existingTx != null) MoneyFormat.minorToMajorString(existingTx.amountMinor) else ""
        )
    }
    var selectedWalletId by remember(existingTx, activeWallets) {
        mutableStateOf(existingTx?.walletId ?: activeWallets.firstOrNull()?.id ?: "")
    }
    var selectedToWalletId by remember(existingTx, activeWallets) {
        mutableStateOf(
            existingTx?.toWalletId
                ?: activeWallets.getOrNull(1)?.id
                ?: activeWallets.firstOrNull()?.id
                ?: ""
        )
    }
    val filteredCategories = remember(categories, txType) {
        categories.filter { it.type == if (txType == "income") "income" else "expense" }
    }
    var selectedCategoryId by remember(existingTx, filteredCategories) {
        mutableStateOf(
            existingTx?.categoryId?.takeIf { id -> filteredCategories.any { it.id == id } }
                ?: filteredCategories.firstOrNull()?.id
        )
    }
    var dateIso by remember(existingTx) {
        mutableStateOf(existingTx?.dateIso ?: DateHelper.todayIso())
    }
    var note by remember(existingTx) { mutableStateOf(existingTx?.note ?: "") }
    var attachmentUri by remember(existingTx) { mutableStateOf(existingTx?.attachmentUri) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) attachmentUri = uri.toString()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaliTheme.colors.cardSurface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingTx == null) strings.newTransaction else strings.editTransaction,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = strings.cancel)
                }
            }

            // 3 Tabs: مصروف / دخل / تحويل
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(MaliTheme.colors.surfaceLow)
                    .border(1.dp, MaliTheme.colors.outlineVariant, CircleShape)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf(
                    Triple("expense", strings.expenseTab, MaliTheme.colors.expenseCoral),
                    Triple("income", strings.incomeTab, MaterialTheme.colorScheme.primary),
                    Triple("transfer", strings.transferTab, MaliTheme.colors.mutedGold)
                )
                tabs.forEach { (key, label, activeBg) ->
                    val selected = txType == key
                    val activeFg = if (key == "income") MaterialTheme.colorScheme.onPrimary
                    else if (key == "transfer") MaliTheme.colors.onMutedGold
                    else Color(0xFF3E0505)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (selected) activeBg else Color.Transparent)
                            .clickable {
                                txType = key
                                errorMsg = null
                                val nextCats = categories.filter { it.type == if (key == "income") "income" else "expense" }
                                selectedCategoryId = nextCats.firstOrNull()?.id
                            }
                            .padding(vertical = 10.dp)
                            .testTag("tx_type_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) activeFg else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Large Centered Amount Box + Quick Chips
            val selectedWallet = activeWallets.find { it.id == selectedWalletId }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaliTheme.colors.surfaceLow)
                    .border(1.dp, MaliTheme.colors.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${strings.amount} (${selectedWallet?.currency ?: "MRU"})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    BasicTextField(
                        value = amountStr,
                        onValueChange = { next ->
                            if (next.matches(Regex("^\\d*([.,]\\d{0,2})?$"))) {
                                amountStr = next
                                errorMsg = null
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            fontFeatureSettings = "tnum"
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().testTag("tx_amount_input"),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.Center) {
                                if (amountStr.isEmpty()) {
                                    Text(
                                        text = "0",
                                        style = TextStyle(
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 32.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("500", "2000", "5000", "20000").forEach { preset ->
                        Surface(
                            onClick = { amountStr = preset; errorMsg = null },
                            shape = CircleShape,
                            color = MaliTheme.colors.surfaceHigh
                        ) {
                            LtrNumberText(
                                text = MoneyFormat.format(preset.toLong() * 100L, "").trim(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Wallet selector(s)
            WalletDropdownField(
                label = if (txType == "transfer") strings.fromWallet else strings.wallet,
                wallets = activeWallets,
                selectedId = selectedWalletId,
                langCode = langCode,
                onSelect = { selectedWalletId = it; errorMsg = null }
            )

            if (txType == "transfer") {
                WalletDropdownField(
                    label = strings.toWallet,
                    wallets = activeWallets,
                    selectedId = selectedToWalletId,
                    langCode = langCode,
                    onSelect = { selectedToWalletId = it; errorMsg = null }
                )
            } else {
                // Category Selector Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = strings.category,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredCategories.forEach { cat ->
                            val isSelected = cat.id == selectedCategoryId
                            Surface(
                                onClick = { selectedCategoryId = cat.id },
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaliTheme.colors.surfaceLow,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaliTheme.colors.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = IconCatalog.iconFor(cat.iconName),
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else IconCatalog.colorFor(cat.colorKey),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Date & Note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DatePickerField(
                    label = strings.date,
                    dateIso = dateIso,
                    onDateSelected = { dateIso = it },
                    modifier = Modifier.weight(1f)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = strings.note,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text(strings.notePlaceholder, style = MaterialTheme.typography.bodyMedium) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaliTheme.colors.surfaceLow,
                            unfocusedContainerColor = MaliTheme.colors.surfaceLow,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaliTheme.colors.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("tx_note_input")
                    )
                }
            }

            // Optional receipt attachment via Android Photo Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.attachment,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaliTheme.colors.outlineVariant)
                ) {
                    Icon(
                        imageVector = if (attachmentUri != null) Icons.Rounded.CheckCircle else Icons.Rounded.AttachFile,
                        contentDescription = null,
                        tint = if (attachmentUri != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (attachmentUri != null) strings.receiptAttached else strings.pickReceipt,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (errorMsg != null) {
                Text(
                    text = errorMsg!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaliTheme.colors.expenseCoral
                )
            }

            Button(
                onClick = {
                    val minor = MoneyFormat.parseToMinor(amountStr)
                    if (minor == null || minor <= 0L) {
                        errorMsg = strings.invalidAmountError
                        return@Button
                    }
                    if (txType == "transfer" && selectedWalletId == selectedToWalletId) {
                        errorMsg = strings.sameWalletError
                        return@Button
                    }
                    onSave(
                        existingTx?.id,
                        txType,
                        minor,
                        selectedWalletId,
                        if (txType == "transfer") selectedToWalletId else null,
                        if (txType == "transfer") null else selectedCategoryId,
                        dateIso,
                        note.trim(),
                        attachmentUri
                    )
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("tx_save_button")
            ) {
                Text(
                    text = if (existingTx == null) strings.saveTransaction else strings.saveChanges,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun WalletDropdownField(
    label: String,
    wallets: List<WalletEntity>,
    selectedId: String,
    langCode: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = wallets.find { it.id == selectedId } ?: wallets.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Box {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(10.dp),
                color = MaliTheme.colors.surfaceLow,
                border = BorderStroke(1.dp, MaliTheme.colors.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = IconCatalog.iconFor(selected?.iconName),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = selected?.let { "${it.name} (${it.currency})" } ?: "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = MaliTheme.colors.cardSurface
            ) {
                wallets.forEach { w ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                "${w.name} • ${walletTypeLabel(w.type, langCode)} (${w.currency})",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onSelect(w.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WalletFormDialog(
    existingWallet: WalletEntity?,
    primaryCurrency: String,
    strings: AppStrings,
    langCode: String,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        name: String,
        type: String,
        balanceMinor: Long,
        currency: String,
        iconName: String,
        colorKey: String,
        description: String
    ) -> Unit
) {
    var name by remember(existingWallet) { mutableStateOf(existingWallet?.name ?: "") }
    var type by remember(existingWallet) { mutableStateOf(existingWallet?.type ?: "bank") }
    var currency by remember(existingWallet) { mutableStateOf(existingWallet?.currency ?: primaryCurrency) }
    var balanceStr by remember(existingWallet) {
        mutableStateOf(
            if (existingWallet != null) MoneyFormat.minorToMajorString(existingWallet.balanceMinor)
            else "0"
        )
    }
    var iconName by remember(existingWallet) {
        mutableStateOf(existingWallet?.iconName ?: IconCatalog.defaultIconForWalletType(type))
    }
    var colorKey by remember(existingWallet) { mutableStateOf(existingWallet?.colorKey ?: "emerald") }
    var description by remember(existingWallet) { mutableStateOf(existingWallet?.description ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        MaliCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingWallet == null) strings.addWallet else strings.editWallet,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.cancel)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("اسم المحفظة") },
                    placeholder = { Text("مثال: BNM أو البنك الشعبي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("wallet_name_input")
                )

                // Type selector chips
                Text("نوع الحساب", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("cash", "bank", "mobile", "savings", "custom").forEach { t ->
                        val sel = type == t
                        Surface(
                            onClick = {
                                type = t
                                if (existingWallet == null) iconName = IconCatalog.defaultIconForWalletType(t)
                            },
                            shape = CircleShape,
                            color = if (sel) MaterialTheme.colorScheme.primary else MaliTheme.colors.surfaceLow,
                            border = BorderStroke(1.dp, if (sel) MaterialTheme.colorScheme.primary else MaliTheme.colors.outlineVariant)
                        ) {
                            Text(
                                text = walletTypeLabel(t, langCode),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Currency selector
                Text("العملة", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("MRU", "USD", "EUR", "MAD", "XOF").forEach { cur ->
                        val sel = currency == cur
                        Surface(
                            onClick = { currency = cur },
                            shape = CircleShape,
                            color = if (sel) MaterialTheme.colorScheme.primary else MaliTheme.colors.surfaceLow,
                            border = BorderStroke(1.dp, if (sel) MaterialTheme.colorScheme.primary else MaliTheme.colors.outlineVariant)
                        ) {
                            Text(
                                text = cur,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = balanceStr,
                    onValueChange = { if (it.matches(Regex("^-?\\d*([.,]\\d{0,2})?$"))) balanceStr = it },
                    label = { Text(if (existingWallet == null) "الرصيد الافتتاحي" else "الرصيد") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("wallet_balance_input")
                )

                // Icon & Color
                Text("الأيقونة واللون", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconCatalog.walletIconKeys.forEach { key ->
                        val sel = iconName == key
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaliTheme.colors.surfaceLow)
                                .border(1.dp, if (sel) MaterialTheme.colorScheme.primary else MaliTheme.colors.outlineVariant, CircleShape)
                                .clickable { iconName = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = IconCatalog.iconFor(key),
                                contentDescription = null,
                                tint = IconCatalog.colorFor(colorKey),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconCatalog.colorKeys.take(5).forEach { cKey ->
                        val sel = colorKey == cKey
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(IconCatalog.colorFor(cKey))
                                .border(
                                    width = if (sel) 2.5.dp else 1.dp,
                                    color = if (sel) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorKey = cKey }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف اختياري") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, style = MaterialTheme.typography.labelSmall, color = MaliTheme.colors.expenseCoral)
                }

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            errorMsg = "يرجى إدخال اسم المحفظة"
                            return@Button
                        }
                        val minor = MoneyFormat.parseToMinor(balanceStr.ifBlank { "0" }) ?: 0L
                        onSave(existingWallet?.id, name.trim(), type, minor, currency, iconName, colorKey, description.trim())
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("wallet_save_button")
                ) {
                    Text("حفظ المحفظة", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun ReconcileBalanceDialog(
    wallet: WalletEntity,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var actualStr by remember(wallet) {
        mutableStateOf(MoneyFormat.minorToMajorString(wallet.balanceMinor))
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
                    Text("تسوية الرصيد", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "إغلاق")
                    }
                }
                Text(
                    text = "أدخل الرصيد الفعلي الحالي وسيتم إنشاء قيد تسوية تلقائي لمطابقة الحساب.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = actualStr,
                    onValueChange = { if (it.matches(Regex("^\\d*([.,]\\d{0,2})?$"))) actualStr = it },
                    label = { Text("الرصيد الفعلي (${wallet.currency})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val minor = MoneyFormat.parseToMinor(actualStr) ?: wallet.balanceMinor
                        onConfirm(minor)
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaliTheme.colors.mutedGold,
                        contentColor = MaliTheme.colors.onMutedGold
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("تأكيد التسوية", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    alerts: List<FinanceAlert>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaliTheme.colors.cardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("التنبيهات المالية", style = MaterialTheme.typography.titleLarge)
            if (alerts.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.NotificationsNone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "كل شيء على ما يرام، لا توجد تنبيهات حالياً.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                alerts.forEach { alert ->
                    MaliCard(
                        backgroundColor = MaliTheme.colors.surfaceLow
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = if (alert.isUrgent) MaliTheme.colors.expenseCoral else MaliTheme.colors.warningAmber
                            )
                            Column {
                                Text(alert.title, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    alert.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinLockScreen(
    biometricEnabled: Boolean,
    onVerifyPin: (String) -> Boolean,
    onBiometricSuccess: () -> Unit
) {
    var entered by remember { mutableStateOf("") }
    var wrongPin by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun tryBiometric() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val prompt = BiometricPrompt.Builder(context)
                    .setTitle("فتح MALI – مالي")
                    .setSubtitle("استخدم البصمة لفتح التطبيق")
                    .setNegativeButton("استخدام الرمز", context.mainExecutor) { _, _ -> }
                    .build()
                prompt.authenticate(
                    CancellationSignal(),
                    context.mainExecutor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            onBiometricSuccess()
                        }
                    }
                )
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text("أدخل رمز الدخول", style = MaterialTheme.typography.headlineMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(4) { idx ->
                    val filled = idx < entered.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (wrongPin) MaliTheme.colors.expenseCoral
                                else if (filled) MaterialTheme.colorScheme.primary
                                else MaliTheme.colors.surfaceHigh
                            )
                    )
                }
            }
            if (wrongPin) {
                Text(
                    "الرمز غير صحيح",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaliTheme.colors.expenseCoral
                )
            }

            // Keypad
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(if (biometricEnabled) "bio" else "", "0", "del")
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { r ->
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        r.forEach { key ->
                            if (key.isEmpty()) {
                                Spacer(Modifier.size(68.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(MaliTheme.colors.cardSurface)
                                        .border(1.dp, MaliTheme.colors.outlineVariant, CircleShape)
                                        .clickable {
                                            when (key) {
                                                "del" -> {
                                                    if (entered.isNotEmpty()) entered = entered.dropLast(1)
                                                    wrongPin = false
                                                }
                                                "bio" -> tryBiometric()
                                                else -> {
                                                    if (entered.length < 4) {
                                                        val next = entered + key
                                                        entered = next
                                                        wrongPin = false
                                                        if (next.length == 4) {
                                                            if (!onVerifyPin(next)) {
                                                                wrongPin = true
                                                                entered = ""
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (key) {
                                        "del" -> Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "مسح")
                                        "bio" -> Icon(
                                            Icons.Rounded.Fingerprint,
                                            contentDescription = "البصمة",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        else -> Text(key, style = MaterialTheme.typography.headlineMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
