package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryCatalog
import com.example.data.model.FinanceFormatters
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.state.CategoryBreakdownItem
import com.example.ui.state.DailySpendPoint
import com.example.ui.state.MonthTrendPoint
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.MonoMediumStyle
import com.example.ui.theme.MonoSmallStyle
import kotlin.math.max

@Composable
fun CategoryDonutChart(
    breakdowns: List<CategoryBreakdownItem>,
    totalExpense: Double,
    currencyCode: String,
    language: AppLanguage = AppLanguage.BN,
    modifier: Modifier = Modifier
) {
    val copy = remember(language) { AppStrings.forLanguage(language) }
    var selectedCategory by remember(breakdowns) { mutableStateOf<String?>(null) }
    val sweepProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 650),
        label = "donutSweep"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeItem = breakdowns.firstOrNull { it.category == selectedCategory }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(184.dp)) {
                val strokePx = 28.dp.toPx()
                val inset = strokePx / 2f
                val arcSize = Size(size.width - strokePx, size.height - strokePx)
                val topLeft = Offset(inset, inset)

                if (breakdowns.isEmpty() || totalExpense <= 0.0) {
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                } else {
                    var startAngle = -90f
                    val gapDegrees = if (breakdowns.size > 1) 2.5f else 0f
                    breakdowns.forEach { item ->
                        val rawSweep = (item.percentage * 360f * sweepProgress)
                        val sweep = (rawSweep - gapDegrees).coerceAtLeast(1.5f)
                        val isHighlighted = selectedCategory == null || selectedCategory == item.category
                        val baseColor = CategoryCatalog.metaFor(item.category).color
                        drawArc(
                            color = if (isHighlighted) baseColor else baseColor.copy(alpha = 0.25f),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = if (selectedCategory == item.category) strokePx * 1.12f else strokePx,
                                cap = StrokeCap.Butt
                            )
                        )
                        startAngle += rawSweep
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val centerTitle = activeItem?.let {
                    CategoryCatalog.metaFor(it.category).displayName(language)
                } ?: copy.totalSpentLabel
                Text(
                    text = centerTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = FinanceFormatters.formatCurrency(
                        activeItem?.total ?: totalExpense,
                        currencyCode,
                        lang = language
                    ),
                    style = MonoMediumStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (activeItem != null) {
                    Text(
                        text = copy.ofMonthLabel((activeItem.percentage * 100).toInt()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            breakdowns.forEach { item ->
                val meta = CategoryCatalog.metaFor(item.category)
                val isSelected = selectedCategory == item.category
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                            else Color.Transparent
                        )
                        .clickable {
                            selectedCategory = if (isSelected) null else item.category
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(meta.color)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${meta.emoji} ${meta.displayName(language)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${FinanceFormatters.formatNumber((item.percentage * 100).toInt(), language)}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = FinanceFormatters.formatCurrency(item.total, currencyCode, lang = language),
                        style = MonoSmallStyle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun IncomeExpenseTrendChart(
    trends: List<MonthTrendPoint>,
    currencyCode: String,
    language: AppLanguage = AppLanguage.BN,
    modifier: Modifier = Modifier
) {
    val copy = remember(language) { AppStrings.forLanguage(language) }
    val financeColors = LocalFinanceColors.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val netLineColor = MaterialTheme.colorScheme.secondary
    var selectedIdx by remember(trends) { mutableStateOf(trends.lastIndex.coerceAtLeast(0)) }
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600),
        label = "trendAnim"
    )

    val selectedPoint = trends.getOrNull(selectedIdx)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selectedPoint != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendPill(copy.income, financeColors.income, FinanceFormatters.formatCurrency(selectedPoint.income, currencyCode, lang = language))
                LegendPill(copy.expense, financeColors.expense, FinanceFormatters.formatCurrency(selectedPoint.expense, currencyCode, lang = language))
                LegendPill(copy.netLabel, netLineColor, FinanceFormatters.formatCurrency(selectedPoint.net, currencyCode, lang = language))
            }
        }

        val maxVal = trends.maxOfOrNull { max(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
        ) {
            val h = size.height
            val w = size.width
            val topPad = 12.dp.toPx()
            val bottomPad = 8.dp.toPx()
            val usableH = (h - topPad - bottomPad).coerceAtLeast(10f)

            for (i in 0..2) {
                val y = topPad + (usableH * i / 2f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (trends.isEmpty()) return@Canvas

            val groupWidth = w / trends.size
            val barWidth = (groupWidth * 0.24f).coerceIn(8.dp.toPx(), 18.dp.toPx())
            val barGap = 4.dp.toPx()
            val netPoints = mutableListOf<Offset>()

            trends.forEachIndexed { idx, pt ->
                val centerX = groupWidth * idx + groupWidth / 2f
                val incomeH = ((pt.income / maxVal).toFloat() * usableH * animProgress).coerceAtLeast(2f)
                val expenseH = ((pt.expense / maxVal).toFloat() * usableH * animProgress).coerceAtLeast(2f)

                val alpha = if (idx == selectedIdx) 1f else 0.68f
                drawRoundRect(
                    color = financeColors.income.copy(alpha = alpha),
                    topLeft = Offset(centerX - barWidth - barGap / 2f, topPad + usableH - incomeH),
                    size = Size(barWidth, incomeH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = financeColors.expense.copy(alpha = alpha),
                    topLeft = Offset(centerX + barGap / 2f, topPad + usableH - expenseH),
                    size = Size(barWidth, expenseH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                val clampedNetRatio = (pt.net.coerceAtLeast(0.0) / maxVal).toFloat().coerceIn(0f, 1f)
                val netY = topPad + usableH - (clampedNetRatio * usableH * animProgress)
                netPoints.add(Offset(centerX, netY))
            }

            if (netPoints.size >= 2) {
                val path = Path().apply {
                    moveTo(netPoints.first().x, netPoints.first().y)
                    for (i in 1 until netPoints.size) {
                        lineTo(netPoints[i].x, netPoints[i].y)
                    }
                }
                drawPath(
                    path = path,
                    color = netLineColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            netPoints.forEachIndexed { idx, offset ->
                drawCircle(
                    color = netLineColor,
                    radius = if (idx == selectedIdx) 5.dp.toPx() else 3.5.dp.toPx(),
                    center = offset
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            trends.forEachIndexed { idx, pt ->
                val selected = idx == selectedIdx
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedIdx = idx }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DailySpendBarChart(
    points: List<DailySpendPoint>,
    currencyCode: String,
    language: AppLanguage = AppLanguage.BN,
    modifier: Modifier = Modifier
) {
    val copy = remember(language) { AppStrings.forLanguage(language) }
    val financeColors = LocalFinanceColors.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val maxExpense = points.maxOfOrNull { it.expense }?.coerceAtLeast(1.0) ?: 1.0
    val peakPoint = points.maxByOrNull { it.expense }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (peakPoint != null && peakPoint.expense > 0.0) {
            Text(
                text = copy.peakDayLabel(
                    peakPoint.dayOfMonth,
                    FinanceFormatters.formatCurrency(peakPoint.expense, currencyCode, lang = language)
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val h = size.height
            val w = size.width
            drawLine(
                color = gridColor,
                start = Offset(0f, h - 2.dp.toPx()),
                end = Offset(w, h - 2.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
            if (points.isEmpty()) return@Canvas

            val slotWidth = w / 31f
            val barWidth = (slotWidth * 0.65f).coerceAtLeast(3.dp.toPx())
            points.forEach { pt ->
                if (pt.expense > 0.0) {
                    val x = ((pt.dayOfMonth - 1).coerceIn(0, 30)) * slotWidth
                    val barH = ((pt.expense / maxExpense).toFloat() * (h - 10.dp.toPx())).coerceAtLeast(4.dp.toPx())
                    drawRoundRect(
                        color = financeColors.expense.copy(alpha = 0.85f),
                        topLeft = Offset(x, h - barH - 2.dp.toPx()),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (language == AppLanguage.BN) "১" else "Day 1",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (language == AppLanguage.BN) "১৫" else "Day 15",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (language == AppLanguage.BN) "৩০" else "Day 30",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegendPill(label: String, dotColor: Color, valueText: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = valueText,
            style = MonoSmallStyle,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
