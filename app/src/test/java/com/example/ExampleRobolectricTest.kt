package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MaliDatabase
import com.example.data.preferences.PreferencesManager
import com.example.data.repository.MaliRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: MaliDatabase
    private lateinit var repo: MaliRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MaliDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = MaliRepository(db, PreferencesManager(context))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun appName_isMaliArabic() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MALI – مالي", appName)
    }

    @Test
    fun transferAndReversal_preserveWalletBalancesAtomically() = runTest {
        repo.seedDemoDatabase()
        val initialCash = db.maliDao().getWalletById("w_cash")!!.balanceMinor
        val initialBmci = db.maliDao().getWalletById("w_bmci")!!.balanceMinor

        // Prevent same-wallet transfer
        assertFalse(
            repo.addTransaction("transfer", 100000L, "w_cash", "w_cash", null, "2026-11-21", "", null)
        )

        // Valid transfer of 20,000 MRU from Cash to BMCI
        assertTrue(
            repo.addTransaction("transfer", 2000000L, "w_cash", "w_bmci", null, "2026-11-21", "Test transfer", null)
        )
        assertEquals(initialCash - 2000000L, db.maliDao().getWalletById("w_cash")!!.balanceMinor)
        assertEquals(initialBmci + 2000000L, db.maliDao().getWalletById("w_bmci")!!.balanceMinor)

        // Delete the transfer and verify exact reversal
        val latestTx = repo.transactionsFlow.first().first { it.note == "Test transfer" }
        repo.deleteTransaction(latestTx.id)
        assertEquals(initialCash, db.maliDao().getWalletById("w_cash")!!.balanceMinor)
        assertEquals(initialBmci, db.maliDao().getWalletById("w_bmci")!!.balanceMinor)
    }

    @Test
    fun editTransaction_reversesOldEffectBeforeApplyingNew() = runTest {
        repo.seedDemoDatabase()
        val initialCash = db.maliDao().getWalletById("w_cash")!!.balanceMinor

        repo.addTransaction("expense", 500000L, "w_cash", null, "c_food", "2026-11-21", "Lunch", null)
        assertEquals(initialCash - 500000L, db.maliDao().getWalletById("w_cash")!!.balanceMinor)

        val tx = repo.transactionsFlow.first().first { it.note == "Lunch" }
        repo.updateTransaction(tx.id, "expense", 800000L, "w_cash", null, "c_food", "2026-11-21", "Lunch updated", null)
        assertEquals(initialCash - 800000L, db.maliDao().getWalletById("w_cash")!!.balanceMinor)
    }
}
