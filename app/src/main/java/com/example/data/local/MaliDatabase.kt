package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WalletEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        DebtEntity::class,
        RecurringEntity::class,
        CurrencyRateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MaliDatabase : RoomDatabase() {
    abstract fun maliDao(): MaliDao

    companion object {
        @Volatile
        private var INSTANCE: MaliDatabase? = null

        fun getInstance(context: Context): MaliDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MaliDatabase::class.java,
                    "mali_finance_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
