package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.CurrencyRateEntity
import com.example.data.local.DebtEntity
import com.example.data.local.MaliDatabase
import com.example.data.local.RecurringEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.WalletEntity
import com.example.data.preferences.PreferencesManager
import com.example.data.preferences.UserPreferences
import com.example.data.repository.MaliRepository
import com.example.util.DateHelper
import com.example.util.MoneyFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChartBarPoint(
    val label: String,
    val incomeMinor: Long,
    val expenseMinor: Long
)

data class FinanceAlert(
    val id: String,
    val title: String,
    val subtitle: String,
    val isUrgent: Boolean
)

data class MaliUiState(
    val isLoading: Boolean = true,
    val wallets: List<WalletEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val recurring: List<RecurringEntity> = emptyList(),
    val rates: List<CurrencyRateEntity> = emptyList(),
    val prefs: UserPreferences = UserPreferences(),
    val totalBalanceMinor: Long = 0L,
    val hasUnconvertedCurrencies: Boolean = false,
    val monthlyIncomeMinor: Long = 0L,
    val monthlyExpenseMinor: Long = 0L,
    val monthlyNetMinor: Long = 0L,
    val monthlyChangePercent: Double? = null,
    val alerts: List<FinanceAlert> = emptyList()
)

class MaliViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MaliRepository(
        db = MaliDatabase.getInstance(application),
        preferences = PreferencesManager(application)
    )

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val uiState: StateFlow<MaliUiState> = combine(
        repository.walletsFlow,
        repository.categoriesFlow,
        repository.transactionsFlow,
        repository.budgetsFlow,
        repository.debtsFlow,
        repository.recurringFlow,
        repository.ratesFlow,
        repository.userPreferencesFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val wallets = values[0] as List<WalletEntity>
        @Suppress("UNCHECKED_CAST")
        val categories = values[1] as List<CategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val transactions = values[2] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val budgets = values[3] as List<BudgetEntity>
        @Suppress("UNCHECKED_CAST")
        val debts = values[4] as List<DebtEntity>
        @Suppress("UNCHECKED_CAST")
        val recurring = values[5] as List<RecurringEntity>
        @Suppress("UNCHECKED_CAST")
        val rates = values[6] as List<CurrencyRateEntity>
        val prefs = values[7] as UserPreferences

        val rateMap = rates.associateBy { it.currencyCode }
        val primaryRate = rateMap[prefs.primaryCurrency]?.rateToMru ?: 1.0

        val activeWallets = wallets.filter { !it.archived }
        val primaryWallets = activeWallets.filter { it.currency == prefs.primaryCurrency }
        val otherWallets = activeWallets.filter { it.currency != prefs.primaryCurrency }

        var totalBalance = primaryWallets.sumOf { it.balanceMinor }
        var hasUnconverted = false
        if (otherWallets.isNotEmpty()) {
            if (prefs.currencyConversion) {
                otherWallets.forEach { w ->
                    val wRate = rateMap[w.currency]?.rateToMru ?: 0.0
                    if (wRate > 0.0) {
                        totalBalance += MoneyFormat.convertMinor(w.balanceMinor, wRate, primaryRate)
                    } else {
                        hasUnconverted = true
                    }
                }
            } else {
                hasUnconverted = true
            }
        }

        val curMonth = DateHelper.currentMonthPrefix()
        val prevMonth = DateHelper.previousMonthPrefix()

        fun txInPrimary(tx: TransactionEntity): Long {
            if (tx.currency == prefs.primaryCurrency) return tx.amountMinor
            val txRate = tx.exchangeRate.takeIf { it > 0.0 }
                ?: rateMap[tx.currency]?.rateToMru ?: 1.0
            return MoneyFormat.convertMinor(tx.amountMinor, txRate, primaryRate)
        }

        val monthTxs = transactions.filter { it.dateIso.startsWith(curMonth) && it.type != "transfer" }
        val mIncome = monthTxs.filter { it.type == "income" }.sumOf { txInPrimary(it) }
        val mExpense = monthTxs.filter { it.type == "expense" }.sumOf { txInPrimary(it) }
        val mNet = mIncome - mExpense

        val prevMonthTxs = transactions.filter { it.dateIso.startsWith(prevMonth) && it.type != "transfer" }
        val prevIncome = prevMonthTxs.filter { it.type == "income" }.sumOf { txInPrimary(it) }
        val prevExpense = prevMonthTxs.filter { it.type == "expense" }.sumOf { txInPrimary(it) }
        val prevNet = prevIncome - prevExpense
        val changePct = if (prevNet != 0L) {
            ((mNet - prevNet).toDouble() / kotlin.math.abs(prevNet).toDouble()) * 100.0
        } else if (mNet > 0L) {
            100.0
        } else {
            null
        }

        // Real alerts derived from budgets, debts, and recurring transactions
        val alerts = buildList {
            val catMap = categories.associateBy { it.id }
            budgets.forEach { b ->
                val spent = monthTxs
                    .filter { it.type == "expense" && it.categoryId == b.categoryId }
                    .sumOf { txInPrimary(it) }
                val pct = if (b.limitMinor > 0) (spent * 100L) / b.limitMinor else 0L
                val catName = catMap[b.categoryId]?.name ?: ""
                if (pct >= 100L) {
                    add(
                        FinanceAlert(
                            id = "alert_b_${b.id}",
                            title = "تجاوز ميزانية $catName ($pct%)",
                            subtitle = "${MoneyFormat.format(spent, prefs.primaryCurrency)} / ${MoneyFormat.format(b.limitMinor, prefs.primaryCurrency)}",
                            isUrgent = true
                        )
                    )
                } else if (pct >= 80L) {
                    add(
                        FinanceAlert(
                            id = "alert_b_${b.id}",
                            title = "اقتراب من حد ميزانية $catName ($pct%)",
                            subtitle = "${MoneyFormat.format(spent, prefs.primaryCurrency)} / ${MoneyFormat.format(b.limitMinor, prefs.primaryCurrency)}",
                            isUrgent = false
                        )
                    )
                }
            }

            val weekAheadIso = DateHelper.daysOffsetIso(7)
            debts.filter { it.paidAmountMinor < it.totalAmountMinor && it.dueDateIso <= weekAheadIso }
                .forEach { d ->
                    val rem = d.totalAmountMinor - d.paidAmountMinor
                    add(
                        FinanceAlert(
                            id = "alert_d_${d.id}",
                            title = "استحقاق دين: ${d.personName}",
                            subtitle = "${MoneyFormat.format(rem, d.currency)} • ${d.dueDateIso}",
                            isUrgent = d.dueDateIso <= DateHelper.todayIso()
                        )
                    )
                }

            recurring.filter { it.active && it.nextDateIso <= weekAheadIso }.forEach { r ->
                add(
                    FinanceAlert(
                        id = "alert_r_${r.id}",
                        title = "موعد قيد متكرر: ${r.title}",
                        subtitle = "${MoneyFormat.format(r.amountMinor, prefs.primaryCurrency)} • ${r.nextDateIso}",
                        isUrgent = false
                    )
                )
            }
        }

        MaliUiState(
            isLoading = false,
            wallets = wallets,
            categories = categories,
            transactions = transactions,
            budgets = budgets,
            debts = debts,
            recurring = recurring,
            rates = rates,
            prefs = prefs,
            totalBalanceMinor = totalBalance,
            hasUnconvertedCurrencies = hasUnconverted,
            monthlyIncomeMinor = mIncome,
            monthlyExpenseMinor = mExpense,
            monthlyNetMinor = mNet,
            monthlyChangePercent = changePct,
            alerts = alerts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MaliUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    fun convertToPrimary(amountMinor: Long, currency: String, storedRate: Double? = null): Long {
        val state = uiState.value
        val primary = state.prefs.primaryCurrency
        if (currency == primary) return amountMinor
        val rateMap = state.rates.associateBy { it.currencyCode }
        val primaryRate = rateMap[primary]?.rateToMru ?: 1.0
        val srcRate = storedRate?.takeIf { it > 0.0 } ?: rateMap[currency]?.rateToMru ?: 1.0
        return MoneyFormat.convertMinor(amountMinor, srcRate, primaryRate)
    }

    fun computeHomeChart(period: String): List<ChartBarPoint> {
        val state = uiState.value
        val txs = state.transactions.filter { it.type != "transfer" }
        return when (period) {
            "week" -> {
                (6 downTo 0).map { daysAgo ->
                    val iso = DateHelper.daysOffsetIso(-daysAgo)
                    val dayTxs = txs.filter { it.dateIso == iso }
                    ChartBarPoint(
                        label = iso.substring(8, 10),
                        incomeMinor = dayTxs.filter { it.type == "income" }
                            .sumOf { convertToPrimary(it.amountMinor, it.currency, it.exchangeRate) },
                        expenseMinor = dayTxs.filter { it.type == "expense" }
                            .sumOf { convertToPrimary(it.amountMinor, it.currency, it.exchangeRate) }
                    )
                }
            }
            "month" -> {
                val monthPrefix = DateHelper.currentMonthPrefix()
                val mTxs = txs.filter { it.dateIso.startsWith(monthPrefix) }
                val buckets = listOf(1..7, 8..14, 15..21, 22..31)
                val labels = listOf("أ1", "أ2", "أ3", "أ4")
                buckets.mapIndexed { idx, range ->
                    val bTxs = mTxs.filter {
                        val day = it.dateIso.substring(8, 10).toIntOrNull() ?: 1
                        day in range
                    }
                    ChartBarPoint(
                        label = labels[idx],
                        incomeMinor = bTxs.filter { it.type == "income" }
                            .sumOf { convertToPrimary(it.amountMinor, it.currency, it.exchangeRate) },
                        expenseMinor = bTxs.filter { it.type == "expense" }
                            .sumOf { convertToPrimary(it.amountMinor, it.currency, it.exchangeRate) }
                    )
                }
            }
            else -> {
                (5 downTo 0).map { mOffset ->
                    val prefix = DateHelper.monthPrefixOffset(mOffset)
                    val mTxs = txs.filter { it.dateIso.startsWith(prefix) }
                    ChartBarPoint(
                        label = DateHelper.formatShortMonthLabel(mOffset, state.prefs.languageCode),
                        incomeMinor = mTxs.filter { it.type == "income" }
                            .sumOf { convertToPrimary(it.amountMinor, it.currency, it.exchangeRate) },
                        expenseMinor = mTxs.filter { it.type == "expense" }
                            .sumOf { convertToPrimary(it.amountMinor, it.currency, it.exchangeRate) }
                    )
                }
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun unlockWithPin(rawPin: String): Boolean {
        val ok = repository.preferences.verifyPin(rawPin, uiState.value.prefs.pinHash)
        if (ok) _isUnlocked.value = true
        return ok
    }

    fun unlockBiometric() {
        _isUnlocked.value = true
    }

    fun toggleHideBalances() {
        viewModelScope.launch {
            repository.preferences.setHideBalances(!uiState.value.prefs.hideBalances)
        }
    }

    fun saveWallet(
        id: String?,
        name: String,
        type: String,
        balanceMinor: Long,
        currency: String,
        iconName: String,
        colorKey: String,
        description: String
    ) {
        viewModelScope.launch {
            repository.saveWallet(id, name, type, balanceMinor, currency, iconName, colorKey, description)
        }
    }

    fun toggleArchiveWallet(walletId: String) {
        viewModelScope.launch { repository.toggleArchiveWallet(walletId) }
    }

    fun reconcileWallet(walletId: String, actualBalanceMinor: Long) {
        viewModelScope.launch { repository.reconcileWallet(walletId, actualBalanceMinor) }
    }

    fun saveTransaction(
        existingId: String?,
        type: String,
        amountMinor: Long,
        walletId: String,
        toWalletId: String?,
        categoryId: String?,
        dateIso: String,
        note: String,
        attachmentUri: String?,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val ok = if (existingId == null) {
                repository.addTransaction(
                    type, amountMinor, walletId, toWalletId, categoryId, dateIso, note, attachmentUri
                )
            } else {
                repository.updateTransaction(
                    existingId, type, amountMinor, walletId, toWalletId, categoryId, dateIso, note, attachmentUri
                )
            }
            onResult(ok)
        }
    }

    fun deleteTransaction(txId: String) {
        viewModelScope.launch { repository.deleteTransaction(txId) }
    }

    fun saveCategory(id: String?, name: String, type: String, iconName: String, colorKey: String) {
        viewModelScope.launch { repository.saveCategory(id, name, type, iconName, colorKey) }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch { repository.deleteCategory(id) }
    }

    fun saveBudget(id: String?, categoryId: String, limitMinor: Long) {
        viewModelScope.launch { repository.saveBudget(id, categoryId, limitMinor) }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch { repository.deleteBudget(id) }
    }

    fun saveDebt(
        id: String?,
        direction: String,
        personName: String,
        totalAmountMinor: Long,
        dueDateIso: String,
        walletId: String?,
        notes: String,
        currency: String
    ) {
        viewModelScope.launch {
            repository.saveDebt(id, direction, personName, totalAmountMinor, dueDateIso, walletId, notes, currency)
        }
    }

    fun recordDebtPayment(debtId: String, paymentMinor: Long, alsoUpdateWallet: Boolean) {
        viewModelScope.launch { repository.recordDebtPayment(debtId, paymentMinor, alsoUpdateWallet) }
    }

    fun deleteDebt(id: String) {
        viewModelScope.launch { repository.deleteDebt(id) }
    }

    fun saveRecurring(
        id: String?,
        title: String,
        type: String,
        amountMinor: Long,
        walletId: String,
        categoryId: String,
        frequency: String,
        nextDateIso: String
    ) {
        viewModelScope.launch {
            repository.saveRecurring(id, title, type, amountMinor, walletId, categoryId, frequency, nextDateIso)
        }
    }

    fun toggleRecurring(id: String) {
        viewModelScope.launch { repository.toggleRecurringActive(id) }
    }

    fun executeRecurringNow(id: String) {
        viewModelScope.launch { repository.executeRecurringNow(id) }
    }

    fun deleteRecurring(id: String) {
        viewModelScope.launch { repository.deleteRecurring(id) }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { repository.preferences.setLanguage(lang) }
    }

    fun setPrimaryCurrency(currency: String) {
        viewModelScope.launch { repository.preferences.setPrimaryCurrency(currency) }
    }

    fun setCurrencyConversion(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setCurrencyConversion(enabled) }
    }

    fun updateCurrencyRate(code: String, rateToMru: Double) {
        viewModelScope.launch { repository.updateCurrencyRate(code, rateToMru) }
    }

    fun setDarkTheme(dark: Boolean) {
        viewModelScope.launch { repository.preferences.setDarkTheme(dark) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setNotificationsEnabled(enabled) }
    }

    fun enablePin(rawPin: String) {
        viewModelScope.launch {
            repository.preferences.enablePinLock(rawPin)
            _isUnlocked.value = true
        }
    }

    fun disablePin() {
        viewModelScope.launch { repository.preferences.disablePinLock() }
    }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setBiometricEnabled(enabled) }
    }

    fun clearDemoData() {
        viewModelScope.launch {
            repository.clearDemoData()
            _statusMessage.value = "تم حذف البيانات التجريبية بنجاح"
        }
    }

    fun restoreDemoData() {
        viewModelScope.launch {
            repository.seedDemoDatabase()
            _statusMessage.value = "تمت استعادة البيانات التجريبية"
        }
    }

    fun exportBackupJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            onReady(repository.exportBackupJson())
        }
    }

    fun exportTransactionsCsv(onReady: (String) -> Unit) {
        viewModelScope.launch {
            onReady(repository.exportTransactionsCsv())
        }
    }

    fun importBackupJson(json: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repository.importBackupJson(json)
            _statusMessage.value = if (ok) "تمت استعادة النسخة الاحتياطية بنجاح" else "تنسيق النسخة الاحتياطية غير صالح"
            onComplete(ok)
        }
    }
}
