package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MaliDao {
    // Wallets
    @Query("SELECT * FROM wallets ORDER BY createdAt ASC")
    fun observeWallets(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets")
    suspend fun getAllWalletsSync(): List<WalletEntity>

    @Query("SELECT * FROM wallets WHERE id = :id LIMIT 1")
    suspend fun getWalletById(id: String): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallets(wallets: List<WalletEntity>)

    @Update
    suspend fun updateWallet(wallet: WalletEntity)

    @Query("DELETE FROM wallets WHERE id = :id")
    suspend fun deleteWalletById(id: String)

    @Query("DELETE FROM wallets")
    suspend fun clearWallets()

    // Categories
    @Query("SELECT * FROM categories ORDER BY type ASC, isDefault DESC, name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesSync(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY dateIso DESC, createdAt DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateIso DESC, createdAt DESC")
    suspend fun getAllTransactionsSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(txs: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    // Budgets
    @Query("SELECT * FROM budgets")
    fun observeBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsSync(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: String)

    @Query("DELETE FROM budgets")
    suspend fun clearBudgets()

    // Debts
    @Query("SELECT * FROM debts ORDER BY dueDateIso ASC")
    fun observeDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts")
    suspend fun getAllDebtsSync(): List<DebtEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebts(debts: List<DebtEntity>)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebtById(id: String)

    @Query("DELETE FROM debts")
    suspend fun clearDebts()

    // Recurring
    @Query("SELECT * FROM recurring ORDER BY nextDateIso ASC")
    fun observeRecurring(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring")
    suspend fun getAllRecurringSync(): List<RecurringEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(item: RecurringEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringList(items: List<RecurringEntity>)

    @Query("DELETE FROM recurring WHERE id = :id")
    suspend fun deleteRecurringById(id: String)

    @Query("DELETE FROM recurring")
    suspend fun clearRecurring()

    // Currency Rates
    @Query("SELECT * FROM currency_rates")
    fun observeRates(): Flow<List<CurrencyRateEntity>>

    @Query("SELECT * FROM currency_rates")
    suspend fun getAllRatesSync(): List<CurrencyRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<CurrencyRateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: CurrencyRateEntity)
}
