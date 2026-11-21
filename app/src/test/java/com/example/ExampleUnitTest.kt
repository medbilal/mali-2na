package com.example

import com.example.util.MoneyFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun moneyFormat_parsesAndFormatsDecimalsSafely() {
        assertEquals(1450050L, MoneyFormat.parseToMinor("14500.50"))
        assertEquals(2000000L, MoneyFormat.parseToMinor("20000"))
        assertNull(MoneyFormat.parseToMinor(""))
        assertEquals("550,000 MRU", MoneyFormat.format(55000000L, "MRU"))
        assertEquals("•••••• MRU", MoneyFormat.format(55000000L, "MRU", masked = true))
        assertEquals("- 14,500 MRU", MoneyFormat.formatSigned(1450000L, "expense", "MRU"))
        assertEquals("+ 280,000 MRU", MoneyFormat.formatSigned(28000000L, "income", "MRU"))
    }

    @Test
    fun currencyConversion_isDecimalAccurate() {
        // 100 USD -> MRU at rate 39.8
        val mruMinor = MoneyFormat.convertMinor(10000L, 39.8, 1.0)
        assertEquals(398000L, mruMinor)
    }
}
