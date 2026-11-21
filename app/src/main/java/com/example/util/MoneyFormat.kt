package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object MoneyFormat {
    private val usSymbols = DecimalFormatSymbols(Locale.US)
    private val wholeFormat = DecimalFormat("#,##0", usSymbols)
    private val decimalFormat = DecimalFormat("#,##0.##", usSymbols)

    fun parseToMinor(input: String): Long? {
        val cleaned = input.trim().replace(",", ".")
        if (cleaned.isEmpty()) return null
        return try {
            val bd = BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP)
            bd.multiply(BigDecimal(100)).longValueExact()
        } catch (_: Exception) {
            null
        }
    }

    fun minorToMajorString(minor: Long): String {
        val bd = BigDecimal(minor).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        val stripped = bd.stripTrailingZeros()
        return if (stripped.scale() <= 0) {
            bd.toBigInteger().toString()
        } else {
            bd.toPlainString()
        }
    }

    fun format(amountMinor: Long, currency: String = "MRU", masked: Boolean = false): String {
        if (masked) return "•••••• $currency"
        val isNegative = amountMinor < 0
        val absMinor = kotlin.math.abs(amountMinor)
        val major = absMinor / 100.0
        val numStr = if (absMinor % 100L == 0L) {
            wholeFormat.format(absMinor / 100L)
        } else {
            decimalFormat.format(major)
        }
        return if (isNegative) "-$numStr $currency" else "$numStr $currency"
    }

    fun formatSigned(
        amountMinor: Long,
        type: String,
        currency: String = "MRU",
        masked: Boolean = false
    ): String {
        val base = format(amountMinor, currency, masked)
        return when (type) {
            "income" -> "+ $base"
            "transfer" -> "⇄ $base"
            else -> "- $base"
        }
    }

    fun convertMinor(
        amountMinor: Long,
        fromRateToMru: Double,
        toRateToMru: Double
    ): Long {
        if (fromRateToMru <= 0.0 || toRateToMru <= 0.0) return amountMinor
        val factor = BigDecimal.valueOf(fromRateToMru)
            .divide(BigDecimal.valueOf(toRateToMru), 8, RoundingMode.HALF_UP)
        return BigDecimal(amountMinor)
            .multiply(factor)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }
}

object DateHelper {
    private fun isoFormatter() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun todayIso(): String = isoFormatter().format(Date())

    fun daysOffsetIso(offsetDays: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        return isoFormatter().format(cal.time)
    }

    fun currentMonthPrefix(): String = todayIso().substring(0, 7)

    fun currentYearPrefix(): String = todayIso().substring(0, 4)

    fun previousMonthPrefix(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        return isoFormatter().format(cal.time).substring(0, 7)
    }

    fun formatHeaderDate(langCode: String): String {
        val locale = when (langCode) {
            "fr" -> Locale.FRENCH
            "en" -> Locale.ENGLISH
            else -> Locale("ar", "MA")
        }
        val sdf = SimpleDateFormat("EEEE، d MMMM yyyy", locale)
        return sdf.format(Date())
    }

    fun formatShortMonthLabel(monthOffset: Int, langCode: String): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -monthOffset)
        val locale = when (langCode) {
            "fr" -> Locale.FRENCH
            "en" -> Locale.ENGLISH
            else -> Locale("ar", "MA")
        }
        return SimpleDateFormat("MMM", locale).format(cal.time)
    }

    fun monthPrefixOffset(monthOffset: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -monthOffset)
        return isoFormatter().format(cal.time).substring(0, 7)
    }

    fun formatMillisToIso(millis: Long): String = isoFormatter().format(Date(millis))

    fun isoToMillis(iso: String): Long {
        return try {
            isoFormatter().parse(iso)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    fun advanceDateIso(currentIso: String, frequency: String): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = isoToMillis(currentIso)
        when (frequency) {
            "daily" -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            "yearly" -> cal.add(Calendar.YEAR, 1)
            else -> cal.add(Calendar.MONTH, 1)
        }
        return isoFormatter().format(cal.time)
    }
}
