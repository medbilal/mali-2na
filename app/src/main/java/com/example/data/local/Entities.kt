package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // cash, bank, mobile, savings, custom
    val balanceMinor: Long,
    val currency: String, // MRU, USD, EUR, MAD, XOF
    val iconName: String,
    val colorKey: String,
    val description: String = "",
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // expense, income
    val iconName: String,
    val colorKey: String,
    val isDefault: Boolean = false
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String, // expense, income, transfer
    val amountMinor: Long,
    val walletId: String,
    val toWalletId: String? = null,
    val categoryId: String? = null,
    val dateIso: String, // yyyy-MM-dd
    val note: String = "",
    val attachmentUri: String? = null,
    val currency: String = "MRU",
    val exchangeRate: Double = 1.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val limitMinor: Long
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey val id: String,
    val direction: String, // owed_to_me, i_owe
    val personName: String,
    val totalAmountMinor: Long,
    val paidAmountMinor: Long = 0L,
    val dueDateIso: String,
    val walletId: String? = null,
    val notes: String = "",
    val currency: String = "MRU"
)

@Entity(tableName = "recurring")
data class RecurringEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String, // expense, income
    val amountMinor: Long,
    val walletId: String,
    val categoryId: String,
    val frequency: String, // daily, weekly, monthly, yearly
    val nextDateIso: String,
    val active: Boolean = true
)

@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey val currencyCode: String,
    val rateToMru: Double
)
