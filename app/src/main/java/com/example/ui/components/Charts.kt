package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.MaliTheme
import com.example.ui.theme.MutedGold
import com.example.ui.theme.OnDarkSurfaceVariant
import com.example.ui.viewmodel.ChartBarPoint
import com.example.util.MoneyFormat

@Composable
fun IncomeExpenseBarChart(
    points: List<ChartBarPoint>,
    currency: String,
    masked: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedIdx by remember(points) { mutableIntStateOf(-1) }
    val maxVal = (points.maxOfOrNull { maxOf(it.incomeMinor, it.expenseMinor) } ?: 0L)
        .coerceAtLeast(100L)
    val incomeColor = MaliTheme.colors.incomeGreen
    val expenseColor = MaliTheme.colors.expenseCoral
    val gridColor = MaliTheme.colors.outlineVariant

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (selectedIdx in points.indices) {
            val pt = points[selectedIdx]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaliTheme.colors.surfaceLow)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LtrNumberText(
                        text = "+${MoneyFormat.format(pt.incomeMinor, currency, masked)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = incomeColor
                    )
                    LtrNumberText(
                        text = "-${MoneyFormat.format(pt.expenseMinor, currency, masked)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = expenseColor
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(148.dp)) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val lines = 3
                for (i in 0..lines) {
                    val y = size.height * (i.toFloat() / lines)
                    drawLine(
                        color = gridColor.copy(alpha = 0.55f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            Row(
                modifier = Modifier.matchParentSize().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                points.forEachIndexed { idx, pt ->
                    val incRatio = (pt.incomeMinor.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                    val expRatio = (pt.expenseMinor.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedIdx = if (selectedIdx == idx) -1 else idx }
                            .padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Row(
                            modifier = Modifier.height(112.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val incHeight = (incRatio * 104f).coerceAtLeast(if (pt.incomeMinor > 0) 6f else 3f)
                            val expHeight = (expRatio * 104f).coerceAtLeast(if (pt.expenseMinor > 0) 6f else 3f)
                            Box(
                                modifier = Modifier
                                    .width(9.dp)
                                    .height(incHeight.dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (selectedIdx == idx) incomeColor else incomeColor.copy(alpha = 0.88f))
                            )
                            Box(
                                modifier = Modifier
                                    .width(9.dp)
                                    .height(expHeight.dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (selectedIdx == idx) expenseColor else expenseColor.copy(alpha = 0.88f))
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = pt.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedIdx == idx) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

data class CategorySlice(
    val name: String,
    val amountMinor: Long
)

@Composable
fun ExpenseDonutChart(
    slices: List<CategorySlice>,
    totalExpenseMinor: Long,
    emptyText: String,
    modifier: Modifier = Modifier
) {
    val palette = listOf(
        EmeraldPrimary,
        MutedGold,
        ExpenseCoral,
        DarkPrimary,
        OnDarkSurfaceVariant
    )
    val trackColor = MaliTheme.colors.outlineVariant

    if (totalExpenseMinor <= 0L || slices.isEmpty()) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Canvas(modifier = Modifier.size(112.dp)) {
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Butt)
                )
            }
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        val topSlices = slices.take(5)
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Canvas(modifier = Modifier.size(116.dp)) {
                var startAngle = -90f
                val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Butt)
                topSlices.forEachIndexed { idx, slice ->
                    val sweep = (slice.amountMinor.toFloat() / totalExpenseMinor.toFloat()) * 360f
                    drawArc(
                        color = palette[idx % palette.size],
                        startAngle = startAngle,
                        sweepAngle = sweep.coerceAtLeast(2f),
                        useCenter = false,
                        style = stroke
                    )
                    startAngle += sweep
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                topSlices.forEachIndexed { idx, slice ->
                    val pct = ((slice.amountMinor * 100L) / totalExpenseMinor).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(palette[idx % palette.size])
                            )
                            Text(
                                text = slice.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                        LtrNumberText(
                            text = "$pct%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeExpenseLineChart(
    points: List<ChartBarPoint>,
    modifier: Modifier = Modifier
) {
    val incomeColor = MaliTheme.colors.incomeGreen
    val expenseColor = MaliTheme.colors.expenseCoral
    val gridColor = MaliTheme.colors.outlineVariant

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val h = size.height - 12.dp.toPx()
            val w = size.width
            // Grid lines
            for (i in 0..3) {
                val y = h * (i / 3f)
                drawLine(
                    color = gridColor.copy(alpha = 0.6f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (points.size >= 2) {
                val maxV = (points.maxOfOrNull { maxOf(it.incomeMinor, it.expenseMinor) } ?: 0L)
                    .coerceAtLeast(100L)
                val stepX = w / (points.size - 1).coerceAtLeast(1)

                val incPath = Path()
                val incFill = Path()
                val expPath = Path()

                points.forEachIndexed { i, pt ->
                    val x = i * stepX
                    val yInc = h - ((pt.incomeMinor.toFloat() / maxV.toFloat()) * (h * 0.88f))
                    val yExp = h - ((pt.expenseMinor.toFloat() / maxV.toFloat()) * (h * 0.88f))
                    if (i == 0) {
                        incPath.moveTo(x, yInc)
                        incFill.moveTo(x, h)
                        incFill.lineTo(x, yInc)
                        expPath.moveTo(x, yExp)
                    } else {
                        incPath.lineTo(x, yInc)
                        incFill.lineTo(x, yInc)
                        expPath.lineTo(x, yExp)
                    }
                    if (i == points.lastIndex) {
                        incFill.lineTo(x, h)
                        incFill.close()
                    }
                }

                drawPath(
                    path = incFill,
                    brush = Brush.verticalGradient(
                        listOf(incomeColor.copy(alpha = 0.18f), Color.Transparent)
                    )
                )
                drawPath(
                    path = incPath,
                    color = incomeColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = expPath,
                    color = expenseColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                points.forEachIndexed { i, pt ->
                    val x = i * stepX
                    val yInc = h - ((pt.incomeMinor.toFloat() / maxV.toFloat()) * (h * 0.88f))
                    val yExp = h - ((pt.expenseMinor.toFloat() / maxV.toFloat()) * (h * 0.88f))
                    drawCircle(color = incomeColor, radius = 4.dp.toPx(), center = Offset(x, yInc))
                    drawCircle(color = expenseColor, radius = 4.dp.toPx(), center = Offset(x, yExp))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SimpleSpendingBarChart(
    bars: List<Pair<String, Long>>,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val maxV = (bars.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(100L)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(128.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { (label, value) ->
            val ratio = (value.toFloat() / maxV.toFloat()).coerceIn(0f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Canvas(modifier = Modifier.width(24.dp).height(96.dp)) {
                    val barH = (size.height * ratio).coerceAtLeast(if (value > 0) 6.dp.toPx() else 3.dp.toPx())
                    drawRoundRect(
                        color = barColor.copy(alpha = 0.85f),
                        topLeft = Offset(0f, size.height - barH),
                        size = Size(size.width, barH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
