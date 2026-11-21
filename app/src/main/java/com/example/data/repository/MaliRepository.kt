package com.example.data.repository

import androidx.room.withTransaction
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
import com.example.util.DateHelper
import com.example.util.IconCatalog
import com.example.util.MoneyFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class MaliRepository(
    private val db: MaliDatabase,
    val preferences: PreferencesManager
) {
    private val dao = db.maliDao()

    val walletsFlow: Flow<List<WalletEntity>> = dao.observeWallets()
    val categoriesFlow: Flow<List<CategoryEntity>> = dao.observeCategories()
    val transactionsFlow: Flow<List<TransactionEntity>> = dao.observeTransactions()
    val budgetsFlow: Flow<List<BudgetEntity>> = dao.observeBudgets()
    val debtsFlow: Flow<List<DebtEntity>> = dao.observeDebts()
    val recurringFlow: Flow<List<RecurringEntity>> = dao.observeRecurring()
    val ratesFlow: Flow<List<CurrencyRateEntity>> = dao.observeRates()
    val userPreferencesFlow: Flow<UserPreferences> = preferences.preferencesFlow

    suspend fun ensureSeeded() {
        val prefs = preferences.preferencesFlow.first()
        if (!prefs.hasSeededInitial) {
            seedDemoDatabase()
            preferences.markSeeded(isDemo = true)
        }
    }

    suspend fun seedDemoDatabase() {
        db.withTransaction {
            dao.clearTransactions()
            dao.clearBudgets()
            dao.clearDebts()
            dao.clearRecurring()
            dao.clearWallets()
            dao.clearCategories()

            dao.insertRates(defaultRates())
            dao.insertCategories(defaultCategories())

            val demoWallets = listOf(
                WalletEntity(
                    id = "w_cash",
                    name = "النقدي",
                    type = "cash",
                    balanceMinor = 12000000L, // 120,000 MRU
                    currency = "MRU",
                    iconName = "payments",
                    colorKey = "emerald",
                    description = "محفظة النقد اليومي",
                    createdAt = 1000L
                ),
                WalletEntity(
                    id = "w_bmci",
                    name = "BMCI",
                    type = "bank",
                    balanceMinor = 35000000L, // 350,000 MRU
                    currency = "MRU",
                    iconName = "account_balance",
                    colorKey = "gold",
                    description = "الحساب البنكي الرئيسي",
                    createdAt = 2000L
                ),
                WalletEntity(
                    id = "w_bp",
                    name = "البنك الشعبي",
                    type = "bank",
                    balanceMinor = 8000000L, // 80,000 MRU
                    currency = "MRU",
                    iconName = "account_balance",
                    colorKey = "mint",
                    description = "حساب المعاملات والفواتير",
                    createdAt = 3000L
                )
            )
            dao.insertWallets(demoWallets)

            val demoTransactions = listOf(
                TransactionEntity(
                    id = "t_1",
                    type = "income",
                    amountMinor = 28000000L,
                    walletId = "w_bmci",
                    categoryId = "c_salary",
                    dateIso = DateHelper.daysOffsetIso(-1),
                    note = "الراتب الشهري",
                    currency = "MRU",
                    exchangeRate = 1.0,
                    createdAt = System.currentTimeMillis() - 86400_000L
                ),
                TransactionEntity(
                    id = "t_2",
                    type = "expense",
                    amountMinor = 1450000L,
                    walletId = "w_cash",
                    categoryId = "c_food",
                    dateIso = DateHelper.daysOffsetIso(-1),
                    note = "سوبرماركت التيسير",
                    currency = "MRU",
                    exchangeRate = 1.0,
                    createdAt = System.currentTimeMillis() - 90000_000L
                ),
                TransactionEntity(
                    id = "t_3",
                    type = "transfer",
                    amountMinor = 2000000L,
                    walletId = "w_cash",
                    toWalletId = "w_bmci",
                    categoryId = null,
                    dateIso = DateHelper.daysOffsetIso(-2),
                    note = "إيداع نقدي في BMCI",
                    currency = "MRU",
                    exchangeRate = 1.0,
                    createdAt = System.currentTimeMillis() - 172800_000L
                ),
                TransactionEntity(
                    id = "t_4",
                    type = "expense",
                    amountMinor = 850000L,
                    walletId = "w_bp",
                    categoryId = "c_bills",
                    dateIso = DateHelper.daysOffsetIso(-3),
                    note = "فاتورة الكهرباء سوملك",
                    currency = "MRU",
                    exchangeRate = 1.0,
                    createdAt = System.currentTimeMillis() - 259200_000L
                ),
                TransactionEntity(
                    id = "t_5",
                    type = "expense",
                    amountMinor = 620000L,
                    walletId = "w_cash",
                    categoryId = "c_trans",
                    dateIso = DateHelper.daysOffsetIso(-4),
                    note = "وقود السيارة",
                    currency = "MRU",
                    exchangeRate = 1.0,
                    createdAt = System.currentTimeMillis() - 345600_000L
                ),
                TransactionEntity(
                    id = "t_6",
                    type = "income",
                    amountMinor = 4500000L,
                    walletId = "w_bp",
                    categoryId = "c_free",
                    dateIso = DateHelper.daysOffsetIso(-6),
                    note = "تصميم هوية تجارية",
                    currency = "MRU",
                    exchangeRate = 1.0,
                    createdAt = System.currentTimeMillis() - 518400_000L
                ),
                TransactionEntity(
                    id = "t_7",
                    type = "expense",
                    amountMinor = 1100000L,
                    walletId = "w_bmci",
                    categoryId = "c_shop",
                    dateIso = DateHelper.daysOffsetIso(-11),
                    note = "مستلزمات منزلية",
                    currency = "MRU",
                    exchangeRate = 1.0,
                    createdAt = System.currentTimeMillis() - 950400_000L
                ),
                TransactionEntity(
                    id = "t_8",
                    type = "expense",
                    amountMinor = 3200000L,
                    walletId = "w_bmci",
                    categoryId = "c_rent",
                    dateIso = DateHelper.monthPrefixOffset(1) + "-05",
                    note = "إيجار الشهر الماضي",
                    currency = "MRU",
                    exchangeRate = 1.0,
                    createdAt = System.currentTimeMillis() - 2600000_000L
                ),
                TransactionEntity(
                    id = "t_9",
                    type = "income",
                    amountMinor = 26000000L,
                    walletId = "w_bmci",
                    categoryId = "c_salary",
                    dateIso = DateHelper.monthPrefixOffset(1) + "-01",
                    note = "راتب الشهر الماضي",
                    currency = "MRU",
                    exchangeRate = 1.0,
                    createdAt = System.currentTimeMillis() - 2900000_000L
                )
            )
            dao.insertTransactions(demoTransactions)

            dao.insertBudgets(
                listOf(
                    BudgetEntity("b_1", "c_food", 4500000L),
                    BudgetEntity("b_2", "c_trans", 1500000L),
                    BudgetEntity("b_3", "c_bills", 1000000L)
                )
            )

            dao.insertDebts(
                listOf(
                    DebtEntity(
                        id = "d_1",
                        direction = "owed_to_me",
                        personName = "أحمد ولد سيدي",
                        totalAmountMinor = 3000000L,
                        paidAmountMinor = 1000000L,
                        dueDateIso = DateHelper.daysOffsetIso(10),
                        walletId = "w_cash",
                        notes = "سلفة شخصية"
                    ),
                    DebtEntity(
                        id = "d_2",
                        direction = "i_owe",
                        personName = "مكتبة المعرفة",
                        totalAmountMinor = 1200000L,
                        paidAmountMinor = 400000L,
                        dueDateIso = DateHelper.daysOffsetIso(5),
                        walletId = "w_bp",
                        notes = "مستلزمات مكتبية"
                    )
                )
            )

            dao.insertRecurringList(
                listOf(
                    RecurringEntity(
                        id = "r_1",
                        title = "إيجار الشقة",
                        type = "expense",
                        amountMinor = 6500000L,
                        walletId = "w_bmci",
                        categoryId = "c_rent",
                        frequency = "monthly",
                        nextDateIso = DateHelper.daysOffsetIso(9),
                        active = true
                    ),
                    RecurringEntity(
                        id = "r_2",
                        title = "الراتب الشهري",
                        type = "income",
                        amountMinor = 28000000L,
                        walletId = "w_bmci",
                        categoryId = "c_salary",
                        frequency = "monthly",
                        nextDateIso = DateHelper.daysOffsetIso(29),
                        active = true
                    )
                )
            )
        }
        preferences.setDemoMode(true)
    }

    suspend fun clearDemoData() {
        val prefs = preferences.preferencesFlow.first()
        db.withTransaction {
            dao.clearTransactions()
            dao.clearBudgets()
            dao.clearDebts()
            dao.clearRecurring()
            dao.clearWallets()
            dao.insertWallet(
                WalletEntity(
                    id = "w_cash",
                    name = "النقدي",
                    type = "cash",
                    balanceMinor = 0L,
                    currency = prefs.primaryCurrency,
                    iconName = "payments",
                    colorKey = "emerald",
                    description = ""
                )
            )
            if (dao.getAllCategoriesSync().isEmpty()) {
                dao.insertCategories(defaultCategories())
            }
            if (dao.getAllRatesSync().isEmpty()) {
                dao.insertRates(defaultRates())
            }
        }
        preferences.setDemoMode(false)
    }

    // Wallet Operations
    suspend fun saveWallet(
        id: String?,
        name: String,
        type: String,
        balanceMinor: Long,
        currency: String,
        iconName: String,
        colorKey: String,
        description: String
    ) {
        val existing = if (id != null) dao.getWalletById(id) else null
        val wallet = if (existing != null) {
            existing.copy(
                name = name,
                type = type,
                balanceMinor = balanceMinor,
                currency = currency,
                iconName = iconName,
                colorKey = colorKey,
                description = description
            )
        } else {
            WalletEntity(
                id = "w_${System.currentTimeMillis()}",
                name = name,
                type = type,
                balanceMinor = balanceMinor,
                currency = currency,
                iconName = iconName,
                colorKey = colorKey,
                description = description,
                archived = false
            )
        }
        dao.insertWallet(wallet)
    }

    suspend fun toggleArchiveWallet(walletId: String) {
        val w = dao.getWalletById(walletId) ?: return
        dao.updateWallet(w.copy(archived = !w.archived))
    }

    suspend fun reconcileWallet(walletId: String, actualBalanceMinor: Long) {
        db.withTransaction {
            val w = dao.getWalletById(walletId) ?: return@withTransaction
            val diff = actualBalanceMinor - w.balanceMinor
            if (diff == 0L) return@withTransaction
            val rate = dao.getAllRatesSync().find { it.currencyCode == w.currency }?.rateToMru ?: 1.0
            dao.updateWallet(w.copy(balanceMinor = actualBalanceMinor))
            dao.insertTransaction(
                TransactionEntity(
                    id = "t_${System.currentTimeMillis()}",
                    type = if (diff > 0L) "income" else "expense",
                    amountMinor = kotlin.math.abs(diff),
                    walletId = w.id,
                    categoryId = null,
                    dateIso = DateHelper.todayIso(),
                    note = "تسوية رصيد المحفظة",
                    currency = w.currency,
                    exchangeRate = rate
                )
            )
        }
    }

    // Atomic Transaction Operations
    suspend fun addTransaction(
        type: String,
        amountMinor: Long,
        walletId: String,
        toWalletId: String?,
        categoryId: String?,
        dateIso: String,
        note: String,
        attachmentUri: String?
    ): Boolean {
        if (amountMinor <= 0L) return false
        if (type == "transfer" && (toWalletId == null || walletId == toWalletId)) return false

        db.withTransaction {
            val w = dao.getWalletById(walletId) ?: return@withTransaction
            val rates = dao.getAllRatesSync().associateBy { it.currencyCode }
            val srcRate = rates[w.currency]?.rateToMru ?: 1.0
            val tx = TransactionEntity(
                id = "t_${System.currentTimeMillis()}",
                type = type,
                amountMinor = amountMinor,
                walletId = walletId,
                toWalletId = if (type == "transfer") toWalletId else null,
                categoryId = if (type == "transfer") null else categoryId,
                dateIso = dateIso,
                note = note,
                attachmentUri = attachmentUri,
                currency = w.currency,
                exchangeRate = srcRate
            )
            dao.insertTransaction(tx)
            applyTxEffect(tx, sign = 1L, ratesMap = rates)
        }
        return true
    }

    suspend fun updateTransaction(
        existingId: String,
        type: String,
        amountMinor: Long,
        walletId: String,
        toWalletId: String?,
        categoryId: String?,
        dateIso: String,
        note: String,
        attachmentUri: String?
    ): Boolean {
        if (amountMinor <= 0L) return false
        if (type == "transfer" && (toWalletId == null || walletId == toWalletId)) return false

        db.withTransaction {
            val oldTx = dao.getTransactionById(existingId) ?: return@withTransaction
            val rates = dao.getAllRatesSync().associateBy { it.currencyCode }
            // Reverse old impact first
            applyTxEffect(oldTx, sign = -1L, ratesMap = rates)

            val w = dao.getWalletById(walletId) ?: return@withTransaction
            val srcRate = rates[w.currency]?.rateToMru ?: 1.0
            val updated = oldTx.copy(
                type = type,
                amountMinor = amountMinor,
                walletId = walletId,
                toWalletId = if (type == "transfer") toWalletId else null,
                categoryId = if (type == "transfer") null else categoryId,
                dateIso = dateIso,
                note = note,
                attachmentUri = attachmentUri,
                currency = w.currency,
                exchangeRate = srcRate
            )
            dao.insertTransaction(updated)
            applyTxEffect(updated, sign = 1L, ratesMap = rates)
        }
        return true
    }

    suspend fun deleteTransaction(txId: String) {
        db.withTransaction {
            val oldTx = dao.getTransactionById(txId) ?: return@withTransaction
            val rates = dao.getAllRatesSync().associateBy { it.currencyCode }
            applyTxEffect(oldTx, sign = -1L, ratesMap = rates)
            dao.deleteTransactionById(txId)
        }
    }

    private suspend fun applyTxEffect(
        tx: TransactionEntity,
        sign: Long,
        ratesMap: Map<String, CurrencyRateEntity>
    ) {
        val srcWallet = dao.getWalletById(tx.walletId) ?: return
        val delta = tx.amountMinor * sign
        when (tx.type) {
            "expense" -> {
                dao.updateWallet(srcWallet.copy(balanceMinor = srcWallet.balanceMinor - delta))
            }
            "income" -> {
                dao.updateWallet(srcWallet.copy(balanceMinor = srcWallet.balanceMinor + delta))
            }
            "transfer" -> {
                dao.updateWallet(srcWallet.copy(balanceMinor = srcWallet.balanceMinor - delta))
                val dstWallet = tx.toWalletId?.let { dao.getWalletById(it) }
                if (dstWallet != null) {
                    val creditedAmount = if (srcWallet.currency == dstWallet.currency) {
                        tx.amountMinor
                    } else {
                        val srcRate = tx.exchangeRate.takeIf { it > 0.0 }
                            ?: ratesMap[srcWallet.currency]?.rateToMru ?: 1.0
                        val dstRate = ratesMap[dstWallet.currency]?.rateToMru ?: 1.0
                        MoneyFormat.convertMinor(tx.amountMinor, srcRate, dstRate)
                    }
                    dao.updateWallet(
                        dstWallet.copy(balanceMinor = dstWallet.balanceMinor + (creditedAmount * sign))
                    )
                }
            }
        }
    }

    // Categories
    suspend fun saveCategory(
        id: String?,
        name: String,
        type: String,
        iconName: String,
        colorKey: String
    ) {
        val cat = CategoryEntity(
            id = id ?: "c_${System.currentTimeMillis()}",
            name = name,
            type = type,
            iconName = iconName,
            colorKey = colorKey,
            isDefault = false
        )
        dao.insertCategory(cat)
    }

    suspend fun deleteCategory(id: String) {
        dao.deleteCategoryById(id)
    }

    // Budgets
    suspend fun saveBudget(id: String?, categoryId: String, limitMinor: Long) {
        if (limitMinor <= 0L) return
        val budget = BudgetEntity(
            id = id ?: "b_${System.currentTimeMillis()}",
            categoryId = categoryId,
            limitMinor = limitMinor
        )
        dao.insertBudget(budget)
    }

    suspend fun deleteBudget(id: String) {
        dao.deleteBudgetById(id)
    }

    // Debts
    suspend fun saveDebt(
        id: String?,
        direction: String,
        personName: String,
        totalAmountMinor: Long,
        dueDateIso: String,
        walletId: String?,
        notes: String,
        currency: String
    ) {
        if (totalAmountMinor <= 0L) return
        val debt = DebtEntity(
            id = id ?: "d_${System.currentTimeMillis()}",
            direction = direction,
            personName = personName,
            totalAmountMinor = totalAmountMinor,
            paidAmountMinor = 0L,
            dueDateIso = dueDateIso,
            walletId = walletId,
            notes = notes,
            currency = currency
        )
        dao.insertDebt(debt)
    }

    suspend fun recordDebtPayment(
        debtId: String,
        paymentMinor: Long,
        alsoUpdateWallet: Boolean
    ) {
        if (paymentMinor <= 0L) return
        db.withTransaction {
            val debt = dao.getAllDebtsSync().find { it.id == debtId } ?: return@withTransaction
            val remaining = (debt.totalAmountMinor - debt.paidAmountMinor).coerceAtLeast(0L)
            val actualPay = minOf(remaining, paymentMinor)
            if (actualPay <= 0L) return@withTransaction

            dao.insertDebt(debt.copy(paidAmountMinor = debt.paidAmountMinor + actualPay))
            if (alsoUpdateWallet && debt.walletId != null) {
                val w = dao.getWalletById(debt.walletId)
                if (w != null) {
                    val txType = if (debt.direction == "owed_to_me") "income" else "expense"
                    val rate = dao.getAllRatesSync().find { it.currencyCode == w.currency }?.rateToMru ?: 1.0
                    val tx = TransactionEntity(
                        id = "t_${System.currentTimeMillis()}",
                        type = txType,
                        amountMinor = actualPay,
                        walletId = w.id,
                        categoryId = null,
                        dateIso = DateHelper.todayIso(),
                        note = if (debt.direction == "owed_to_me") "تحصيل دين: ${debt.personName}" else "سداد دين: ${debt.personName}",
                        currency = w.currency,
                        exchangeRate = rate
                    )
                    dao.insertTransaction(tx)
                    val delta = if (txType == "income") actualPay else -actualPay
                    dao.updateWallet(w.copy(balanceMinor = w.balanceMinor + delta))
                }
            }
        }
    }

    suspend fun deleteDebt(id: String) {
        dao.deleteDebtById(id)
    }

    // Recurring
    suspend fun saveRecurring(
        id: String?,
        title: String,
        type: String,
        amountMinor: Long,
        walletId: String,
        categoryId: String,
        frequency: String,
        nextDateIso: String
    ) {
        if (amountMinor <= 0L) return
        dao.insertRecurring(
            RecurringEntity(
                id = id ?: "r_${System.currentTimeMillis()}",
                title = title,
                type = type,
                amountMinor = amountMinor,
                walletId = walletId,
                categoryId = categoryId,
                frequency = frequency,
                nextDateIso = nextDateIso,
                active = true
            )
        )
    }

    suspend fun toggleRecurringActive(id: String) {
        val item = dao.getAllRecurringSync().find { it.id == id } ?: return
        dao.insertRecurring(item.copy(active = !item.active))
    }

    suspend fun executeRecurringNow(id: String) {
        db.withTransaction {
            val item = dao.getAllRecurringSync().find { it.id == id } ?: return@withTransaction
            val w = dao.getWalletById(item.walletId) ?: return@withTransaction
            val rates = dao.getAllRatesSync().associateBy { it.currencyCode }
            val rate = rates[w.currency]?.rateToMru ?: 1.0
            val tx = TransactionEntity(
                id = "t_${System.currentTimeMillis()}",
                type = item.type,
                amountMinor = item.amountMinor,
                walletId = item.walletId,
                categoryId = item.categoryId,
                dateIso = DateHelper.todayIso(),
                note = item.title,
                currency = w.currency,
                exchangeRate = rate
            )
            dao.insertTransaction(tx)
            applyTxEffect(tx, sign = 1L, ratesMap = rates)
            dao.insertRecurring(
                item.copy(nextDateIso = DateHelper.advanceDateIso(item.nextDateIso, item.frequency))
            )
        }
    }

    suspend fun deleteRecurring(id: String) {
        dao.deleteRecurringById(id)
    }

    // Currency Rates
    suspend fun updateCurrencyRate(code: String, rateToMru: Double) {
        if (rateToMru <= 0.0) return
        dao.insertRate(CurrencyRateEntity(code, rateToMru))
    }

    // Backup Export / Import & CSV Export
    suspend fun exportBackupJson(): String {
        val root = JSONObject()
        val wallets = dao.getAllWalletsSync()
        val categories = dao.getAllCategoriesSync()
        val transactions = dao.getAllTransactionsSync()
        val budgets = dao.getAllBudgetsSync()
        val debts = dao.getAllDebtsSync()
        val recurring = dao.getAllRecurringSync()
        val rates = dao.getAllRatesSync()

        root.put("version", 1)
        root.put("exportedAt", DateHelper.todayIso())
        root.put("wallets", JSONArray().apply {
            wallets.forEach { w ->
                put(JSONObject().apply {
                    put("id", w.id); put("name", w.name); put("type", w.type)
                    put("balanceMinor", w.balanceMinor); put("currency", w.currency)
                    put("iconName", w.iconName); put("colorKey", w.colorKey)
                    put("description", w.description); put("archived", w.archived)
                })
            }
        })
        root.put("categories", JSONArray().apply {
            categories.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id); put("name", c.name); put("type", c.type)
                    put("iconName", c.iconName); put("colorKey", c.colorKey); put("isDefault", c.isDefault)
                })
            }
        })
        root.put("transactions", JSONArray().apply {
            transactions.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id); put("type", t.type); put("amountMinor", t.amountMinor)
                    put("walletId", t.walletId); put("toWalletId", t.toWalletId)
                    put("categoryId", t.categoryId); put("dateIso", t.dateIso)
                    put("note", t.note); put("currency", t.currency); put("exchangeRate", t.exchangeRate)
                })
            }
        })
        root.put("budgets", JSONArray().apply {
            budgets.forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id); put("categoryId", b.categoryId); put("limitMinor", b.limitMinor)
                })
            }
        })
        root.put("debts", JSONArray().apply {
            debts.forEach { d ->
                put(JSONObject().apply {
                    put("id", d.id); put("direction", d.direction); put("personName", d.personName)
                    put("totalAmountMinor", d.totalAmountMinor); put("paidAmountMinor", d.paidAmountMinor)
                    put("dueDateIso", d.dueDateIso); put("walletId", d.walletId); put("notes", d.notes)
                    put("currency", d.currency)
                })
            }
        })
        root.put("recurring", JSONArray().apply {
            recurring.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id); put("title", r.title); put("type", r.type)
                    put("amountMinor", r.amountMinor); put("walletId", r.walletId)
                    put("categoryId", r.categoryId); put("frequency", r.frequency)
                    put("nextDateIso", r.nextDateIso); put("active", r.active)
                })
            }
        })
        root.put("rates", JSONArray().apply {
            rates.forEach { r ->
                put(JSONObject().apply {
                    put("currencyCode", r.currencyCode); put("rateToMru", r.rateToMru)
                })
            }
        })
        return root.toString(2)
    }

    suspend fun importBackupJson(rawJson: String): Boolean {
        return try {
            val root = JSONObject(rawJson)
            val walletsArr = root.optJSONArray("wallets") ?: return false
            val categoriesArr = root.optJSONArray("categories") ?: JSONArray()
            val txArr = root.optJSONArray("transactions") ?: JSONArray()
            val budgetsArr = root.optJSONArray("budgets") ?: JSONArray()
            val debtsArr = root.optJSONArray("debts") ?: JSONArray()
            val recArr = root.optJSONArray("recurring") ?: JSONArray()
            val ratesArr = root.optJSONArray("rates") ?: JSONArray()

            val wallets = buildList {
                for (i in 0 until walletsArr.length()) {
                    val o = walletsArr.getJSONObject(i)
                    add(
                        WalletEntity(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            type = o.optString("type", "cash"),
                            balanceMinor = o.getLong("balanceMinor"),
                            currency = o.optString("currency", "MRU"),
                            iconName = o.optString("iconName", IconCatalog.defaultIconForWalletType(o.optString("type", "cash"))),
                            colorKey = o.optString("colorKey", "emerald"),
                            description = o.optString("description", ""),
                            archived = o.optBoolean("archived", false)
                        )
                    )
                }
            }
            val categories = buildList {
                for (i in 0 until categoriesArr.length()) {
                    val o = categoriesArr.getJSONObject(i)
                    add(
                        CategoryEntity(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            type = o.optString("type", "expense"),
                            iconName = o.optString("iconName", "category"),
                            colorKey = o.optString("colorKey", "emerald"),
                            isDefault = o.optBoolean("isDefault", false)
                        )
                    )
                }
            }
            val txs = buildList {
                for (i in 0 until txArr.length()) {
                    val o = txArr.getJSONObject(i)
                    add(
                        TransactionEntity(
                            id = o.getString("id"),
                            type = o.getString("type"),
                            amountMinor = o.getLong("amountMinor"),
                            walletId = o.getString("walletId"),
                            toWalletId = o.optString("toWalletId", "").ifBlank { null },
                            categoryId = o.optString("categoryId", "").ifBlank { null },
                            dateIso = o.getString("dateIso"),
                            note = o.optString("note", ""),
                            currency = o.optString("currency", "MRU"),
                            exchangeRate = o.optDouble("exchangeRate", 1.0)
                        )
                    )
                }
            }
            val budgets = buildList {
                for (i in 0 until budgetsArr.length()) {
                    val o = budgetsArr.getJSONObject(i)
                    add(BudgetEntity(o.getString("id"), o.getString("categoryId"), o.getLong("limitMinor")))
                }
            }
            val debts = buildList {
                for (i in 0 until debtsArr.length()) {
                    val o = debtsArr.getJSONObject(i)
                    add(
                        DebtEntity(
                            id = o.getString("id"),
                            direction = o.getString("direction"),
                            personName = o.getString("personName"),
                            totalAmountMinor = o.getLong("totalAmountMinor"),
                            paidAmountMinor = o.optLong("paidAmountMinor", 0L),
                            dueDateIso = o.getString("dueDateIso"),
                            walletId = o.optString("walletId", "").ifBlank { null },
                            notes = o.optString("notes", ""),
                            currency = o.optString("currency", "MRU")
                        )
                    )
                }
            }
            val recurring = buildList {
                for (i in 0 until recArr.length()) {
                    val o = recArr.getJSONObject(i)
                    add(
                        RecurringEntity(
                            id = o.getString("id"),
                            title = o.getString("title"),
                            type = o.getString("type"),
                            amountMinor = o.getLong("amountMinor"),
                            walletId = o.getString("walletId"),
                            categoryId = o.getString("categoryId"),
                            frequency = o.optString("frequency", "monthly"),
                            nextDateIso = o.getString("nextDateIso"),
                            active = o.optBoolean("active", true)
                        )
                    )
                }
            }
            val rates = buildList {
                for (i in 0 until ratesArr.length()) {
                    val o = ratesArr.getJSONObject(i)
                    add(CurrencyRateEntity(o.getString("currencyCode"), o.getDouble("rateToMru")))
                }
            }

            db.withTransaction {
                dao.clearTransactions()
                dao.clearBudgets()
                dao.clearDebts()
                dao.clearRecurring()
                dao.clearWallets()
                if (categories.isNotEmpty()) {
                    dao.clearCategories()
                    dao.insertCategories(categories)
                }
                if (rates.isNotEmpty()) dao.insertRates(rates)
                dao.insertWallets(wallets)
                dao.insertTransactions(txs)
                dao.insertBudgets(budgets)
                dao.insertDebts(debts)
                dao.insertRecurringList(recurring)
            }
            preferences.setDemoMode(false)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun exportTransactionsCsv(): String {
        val wallets = dao.getAllWalletsSync().associateBy { it.id }
        val cats = dao.getAllCategoriesSync().associateBy { it.id }
        val txs = dao.getAllTransactionsSync()
        val sb = StringBuilder()
        sb.appendLine("Date,Type,Amount,Currency,Wallet,ToWallet,Category,Note")
        txs.forEach { t ->
            val wName = wallets[t.walletId]?.name ?: ""
            val toName = t.toWalletId?.let { wallets[it]?.name } ?: ""
            val cName = t.categoryId?.let { cats[it]?.name } ?: ""
            val amt = MoneyFormat.minorToMajorString(t.amountMinor)
            val safeNote = t.note.replace("\"", "\"\"")
            sb.appendLine("${t.dateIso},${t.type},$amt,${t.currency},\"$wName\",\"$toName\",\"$cName\",\"$safeNote\"")
        }
        return sb.toString()
    }

    private fun defaultRates() = listOf(
        CurrencyRateEntity("MRU", 1.0),
        CurrencyRateEntity("USD", 39.8),
        CurrencyRateEntity("EUR", 42.9),
        CurrencyRateEntity("MAD", 3.95),
        CurrencyRateEntity("XOF", 0.065)
    )

    private fun defaultCategories() = listOf(
        // 12 Expense categories per prompt.txt
        CategoryEntity("c_food", "طعام ومطاعم", "expense", "restaurant", "coral", true),
        CategoryEntity("c_shop", "تسوق", "expense", "shopping_bag", "gold", true),
        CategoryEntity("c_trans", "نقل ومواصلات", "expense", "directions_car", "peach", true),
        CategoryEntity("c_bills", "فواتير", "expense", "receipt", "amber", true),
        CategoryEntity("c_rent", "إيجار", "expense", "home_work", "coral", true),
        CategoryEntity("c_health", "صحة", "expense", "medical_services", "emerald", true),
        CategoryEntity("c_ent", "ترفيه", "expense", "movie", "gold", true),
        CategoryEntity("c_travel", "سفر", "expense", "flight", "mint", true),
        CategoryEntity("c_edu", "تعليم", "expense", "school", "amber", true),
        CategoryEntity("c_biz", "أعمال", "expense", "work", "gold", true),
        CategoryEntity("c_gifts", "هدايا", "expense", "redeem", "peach", true),
        CategoryEntity("c_other_e", "أخرى", "expense", "category", "slate", true),
        // 7 Income categories per prompt.txt
        CategoryEntity("c_salary", "راتب", "income", "payments", "emerald", true),
        CategoryEntity("c_biz_i", "تجارة وأعمال", "income", "work", "gold", true),
        CategoryEntity("c_free", "عمل حر", "income", "laptop_mac", "mint", true),
        CategoryEntity("c_inv", "استثمار", "income", "trending_up", "gold", true),
        CategoryEntity("c_gift_i", "هدايا", "income", "redeem", "peach", true),
        CategoryEntity("c_refund", "استرداد", "income", "replay", "mint", true),
        CategoryEntity("c_other_i", "أخرى", "income", "add_circle", "emerald", true)
    )
}
