package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.CategorySlice
import com.example.ui.components.DatePickerField
import com.example.ui.components.ExpenseDonutChart
import com.example.ui.components.IncomeExpenseLineChart
import com.example.ui.components.LtrNumberText
import com.example.ui.components.MaliCard
import com.example.ui.components.SimpleSpendingBarChart
import com.example.ui.theme.MaliTheme
import com.example.ui.viewmodel.ChartBarPoint
import com.example.ui.viewmodel.MaliUiState
import com.example.util.AppStrings
import com.example.util.DateHelper
import com.example.util.MoneyFormat
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    state: MaliUiState,
    strings: AppStrings,
    convertToPrimary: (Long, String, Double?) -> Long
) {
    var period by remember { mutableStateOf("month") } // today, week, month, year, custom
    var customFrom by remember { mutableStateOf(DateHelper.daysOffsetIso(-30)) }
    var customTo by remember { mutableStateOf(DateHelper.todayIso()) }

    val masked = state.prefs.hideBalances
    val primaryCur = state.prefs.primaryCurrency
    val catMap = remember(state.categories) { state.categories.associateBy { it.id } }

    val filteredTxs = remember(state.transactions, period, customFrom, customTo) {
        val today = DateHelper.todayIso()
        val weekAgo = DateHelper.daysOffsetIso(-7)
        val monthPrefix = DateHelper.currentMonthPrefix()
        val yearPrefix = DateHelper.currentYearPrefix()

        state.transactions.filter { t ->
            if (t.type == "transfer") return@filter false
            when (period) {
                "today" -> t.dateIso == today
                "week" -> t.dateIso >= weekAgo
                "month" -> t.dateIso.startsWith(monthPrefix)
                "year" -> t.dateIso.startsWith(yearPrefix)
                "custom" -> t.dateIso in customFrom..customTo
                else -> true
            }
        }
    }

    val totalIncome = remember(filteredTxs, primaryCur) {
        filteredTxs.filter { it.type == "income" }
            .sumOf { convertToPrimary(it.amountMinor, it.currency, it.exchangeRate) }
    }
    val totalExpense = remember(filteredTxs, primaryCur) {
        filteredTxs.filter { it.type == "expense" }
            .sumOf { convertToPrimary(it.amountMinor, it.currency, it.exchangeRate) }
    }
    val netFlow = totalIncome - totalExpense
    val periodDays = when (period) {
        "today" -> 1L
        "week" -> 7L
        "month" -> 30L
        "year" -> 365L
        else -> {
            val diff = ((DateHelper.isoToMillis(customTo) - DateHelper.isoToMillis(customFrom)) / 86_400_000L) + 1L
            diff.coerceAtLeast(1L)
        }
    }
    val avgDaily = totalExpense / periodDays

    val categorySlices = remember(filteredTxs, catMap, primaryCur) {
        val grouped = mutableMapOf<String, Long>()
        filteredTxs.filter { it.type == "expense" }.forEach { tx ->
            val name = tx.categoryId?.let { catMap[it]?.name } ?: "أخرى"
            grouped[name] = (grouped[name] ?: 0L) + convertToPrimary(tx.amountMinor, tx.currency, tx.exchangeRate)
        }
        grouped.entries
            .sortedByDescending { it.value }
            .map { CategorySlice(it.key, it.value) }
    }
    val topCategoryName = categorySlices.firstOrNull()?.name ?: "—"

    // Real financial insights derived from actual user data
    val insights = remember(filteredTxs, totalIncome, totalExpense, categorySlices, state.transactions) {
        buildList {
            if (categorySlices.isNotEmpty() && totalExpense > 0L) {
                val top = categorySlices.first()
                val pct = ((top.amountMinor * 100L) / totalExpense).toInt()
                add("أعلى فئة إنفاق لديك هي \"${top.name}\" بنسبة $pct% من إجمالي المصروفات.")
            }
            if (totalIncome > totalExpense) {
                val savePct = ((netFlow * 100L) / totalIncome).toInt()
                add("معدل الادخار الصافي في هذه الفترة يبلغ $savePct% (${MoneyFormat.format(netFlow, primaryCur, masked)}).")
            } else if (totalExpense > totalIncome && totalExpense > 0L) {
                add("تنبيه: المصروفات تتجاوز الدخل بمقدار ${MoneyFormat.format(totalExpense - totalIncome, primaryCur, masked)} خلال هذه الفترة.")
            }

            // Month-over-month spending comparison
            val curPrefix = DateHelper.currentMonthPrefix()
            val prevPrefix = DateHelper.previousMonthPrefix()
            val curMonthExp = state.transactions
                .filter { it.type == "expense" && it.dateIso.startsWith(curPrefix) }
                .sumOf { convertToPrimary(it.amountMinor, it.currency, it.exchangeRate) }
            val prevMonthExp = state.transactions
                .filter { it.type == "expense" && it.dateIso.startsWith(prevPrefix) }
                .sumOf { convertToPrimary(it.amountMinor, it.currency, it.exchangeRate) }
            if (prevMonthExp > 0L && curMonthExp > 0L) {
                val diffPct = (((curMonthExp - prevMonthExp).toDouble() / prevMonthExp.toDouble()) * 100.0).roundToInt()
                if (diffPct > 0) {
                    add("مصروفاتك هذا الشهر ارتفعت بنسبة $diffPct% مقارنة بالشهر الماضي.")
                } else if (diffPct < 0) {
                    add("أحسنت! انخفضت مصروفاتك هذا الشهر بنسبة ${kotlin.math.abs(diffPct)}% مقارنة بالشهر الماضي.")
                }
            }

            if (isEmpty()) {
                add("أضف معاملات دخل ومصروفات لعرض مؤشرات مالية دقيقة مبنية على بياناتك.")
            }
        }
    }

    // Monthly trend (last 6 months)
    val monthlyTrendPoints = remember(state.transactions, primaryCur, state.prefs.languageCode) {
        (5 downTo 0).map { offset ->
            val prefix = DateHelper.monthPrefixOffset(offset)
            val mTxs = state.transactions.filter { it.dateIso.startsWith(prefix) && it.type != "transfer" }
            ChartBarPoint(
                label = DateHelper.formatShortMonthLabel(offset, state.prefs.languageCode),
                incomeMinor = mTxs.filter { it.type == "income" }
                    .sumOf { convertToPrimary(it.amountMinor, it.currency, it.exchangeRate) },
                expenseMinor = mTxs.filter { it.type == "expense" }
                    .sumOf { convertToPrimary(it.amountMinor, it.currency, it.exchangeRate) }
            )
        }
    }

    val activeWallets = remember(state.wallets) { state.wallets.filter { !it.archived } }
    val maxWalletBal = remember(activeWallets) {
        (activeWallets.maxOfOrNull { convertToPrimary(it.balanceMinor, it.currency, null) } ?: 0L)
            .coerceAtLeast(100L)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header + Period Selector
        item {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.reportsTitle,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "today" to strings.today,
                        "week" to strings.thisWeek,
                        "month" to strings.month,
                        "year" to strings.year,
                        "custom" to strings.customRange
                    ).forEach { (key, label) ->
                        val selected = period == key
                        Surface(
                            onClick = { period = key },
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaliTheme.colors.cardSurface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaliTheme.colors.outlineVariant
                            ),
                            modifier = Modifier.testTag("report_period_$key")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                if (period == "custom") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DatePickerField(
                            label = "من",
                            dateIso = customFrom,
                            onDateSelected = { customFrom = it },
                            modifier = Modifier.weight(1f)
                        )
                        DatePickerField(
                            label = "إلى",
                            dateIso = customTo,
                            onDateSelected = { customTo = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 5 Key Statistics
        item {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox(
                        label = strings.totalIncome,
                        value = MoneyFormat.format(totalIncome, primaryCur, masked),
                        valueColor = MaliTheme.colors.incomeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = strings.totalExpenses,
                        value = MoneyFormat.format(totalExpense, primaryCur, masked),
                        valueColor = MaliTheme.colors.expenseCoral,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox(
                        label = strings.netCashFlow,
                        value = MoneyFormat.format(netFlow, primaryCur, masked),
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = strings.avgDailySpend,
                        value = MoneyFormat.format(avgDaily, primaryCur, masked),
                        valueColor = MaliTheme.colors.mutedGold,
                        modifier = Modifier.weight(1f)
                    )
                }
                MaliCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.topCategory,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = topCategoryName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Real Data Insights
        item {
            MaliCard(modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = MaliTheme.colors.mutedGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(strings.smartInsights, style = MaterialTheme.typography.titleLarge)
                    }
                    insights.forEach { line ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("•", color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Expense Categories Donut Chart
        item {
            MaliCard(modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(strings.expenseDistribution, style = MaterialTheme.typography.titleLarge)
                    ExpenseDonutChart(
                        slices = categorySlices,
                        totalExpenseMinor = totalExpense,
                        emptyText = "لا توجد مصروفات في هذه الفترة"
                    )
                }
            }
        }

        // Wallet Balance Comparison
        item {
            MaliCard(modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(strings.walletComparison, style = MaterialTheme.typography.titleLarge)
                    activeWallets.forEach { w ->
                        val converted = convertToPrimary(w.balanceMinor, w.currency, null).coerceAtLeast(0L)
                        val ratio = (converted.toFloat() / maxWalletBal.toFloat()).coerceIn(0.04f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(w.name, style = MaterialTheme.typography.labelSmall)
                                LtrNumberText(
                                    text = MoneyFormat.format(w.balanceMinor, w.currency, masked),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(MaliTheme.colors.surfaceLow)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Income vs Expenses Line Chart
        item {
            MaliCard(modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(strings.incomeExpenseTrend, style = MaterialTheme.typography.titleLarge)
                    IncomeExpenseLineChart(points = monthlyTrendPoints)
                }
            }
        }

        // Monthly Spending Bar Chart
        item {
            MaliCard(modifier = Modifier.fillMaxWidth().widthIn(max = 960.dp)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(strings.monthlySpendingChart, style = MaterialTheme.typography.titleLarge)
                    SimpleSpendingBarChart(
                        bars = monthlyTrendPoints.map { it.label to it.expenseMinor },
                        barColor = MaliTheme.colors.expenseCoral
                    )
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    MaliCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LtrNumberText(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = valueColor
            )
        }
    }
}
