package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryCatalog
import com.example.data.model.FinanceFormatters
import com.example.data.model.RecurringPayment
import com.example.data.model.TransactionType
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalizedCopy
import com.example.ui.state.BudgetProgressItem
import com.example.ui.state.DashboardSummary
import com.example.ui.state.UiState
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.MonoMediumStyle
import com.example.ui.theme.MonoSmallStyle
import java.util.concurrent.TimeUnit

@Composable
fun PlanningScreen(
    summaryState: UiState<DashboardSummary>,
    recurringState: UiState<List<RecurringPayment>>,
    selectedMonthKey: String,
    currencyCode: String,
    language: AppLanguage,
    notificationsEnabled: Boolean,
    onOpenBudgetEditor: (String, Double?) -> Unit,
    onDeleteBudget: (String) -> Unit,
    onOpenRecurringEditor: () -> Unit,
    onToggleRecurringActive: (String, Boolean) -> Unit,
    onLogRecurringNow: (RecurringPayment) -> Unit,
    onDeleteRecurring: (String) -> Unit,
    onTestRemindersNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = remember(language) { AppStrings.forLanguage(language) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            modifier = Modifier.testTag("budgets_tab_button")
                        ) {
                            Text(copy.monthlyBudgets)
                        }
                        SegmentedButton(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            modifier = Modifier.testTag("recurring_tab_button")
                        ) {
                            Text(copy.recurringAndAlerts)
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                when (summaryState) {
                    is UiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is UiState.Error -> {
                        item {
                            Text(
                                text = summaryState.message,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    is UiState.Success -> {
                        val summary = summaryState.data
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = FinanceFormatters.formatMonthDisplay(selectedMonthKey, language),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = {
                                        onOpenBudgetEditor(
                                            "OVERALL",
                                            summary.overallBudgetProgress?.budget?.limitAmount
                                        )
                                    },
                                    modifier = Modifier.testTag("add_budget_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(copy.setBudget)
                                }
                            }
                        }

                        if (summary.overallBudgetProgress != null) {
                            item {
                                BudgetCard(
                                    item = summary.overallBudgetProgress,
                                    currencyCode = currencyCode,
                                    language = language,
                                    copy = copy,
                                    isOverall = true,
                                    onEdit = {
                                        onOpenBudgetEditor(
                                            summary.overallBudgetProgress.budget.category,
                                            summary.overallBudgetProgress.budget.limitAmount
                                        )
                                    },
                                    onDelete = { onDeleteBudget(summary.overallBudgetProgress.budget.id) }
                                )
                            }
                        }

                        if (summary.categoryBudgetProgress.isEmpty() && summary.overallBudgetProgress == null) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Savings,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Text(copy.noBudgetsYet, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = copy.noBudgetsDesc,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(summary.categoryBudgetProgress, key = { it.budget.id }) { item ->
                                BudgetCard(
                                    item = item,
                                    currencyCode = currencyCode,
                                    language = language,
                                    copy = copy,
                                    isOverall = false,
                                    onEdit = { onOpenBudgetEditor(item.budget.category, item.budget.limitAmount) },
                                    onDelete = { onDeleteBudget(item.budget.id) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Recurring & Alerts Tab
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (notificationsEnabled) copy.remindersActive else copy.remindersPaused,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = copy.remindersDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            TextButton(
                                onClick = onTestRemindersNow,
                                modifier = Modifier.testTag("check_due_reminders_button")
                            ) {
                                Text(copy.checkDue)
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = copy.scheduledPayments,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onOpenRecurringEditor,
                            modifier = Modifier.testTag("add_recurring_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(copy.addSchedule)
                        }
                    }
                }

                when (recurringState) {
                    is UiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is UiState.Error -> {
                        item {
                            Text(recurringState.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is UiState.Success -> {
                        val list = recurringState.data
                        if (list.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Repeat,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Text(copy.noRecurring, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = copy.noRecurringDesc,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(list, key = { it.id }) { payment ->
                                RecurringPaymentCard(
                                    payment = payment,
                                    currencyCode = currencyCode,
                                    language = language,
                                    copy = copy,
                                    onToggleActive = { onToggleRecurringActive(payment.id, it) },
                                    onLogNow = { onLogRecurringNow(payment) },
                                    onDelete = { onDeleteRecurring(payment.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetCard(
    item: BudgetProgressItem,
    currencyCode: String,
    language: AppLanguage,
    copy: LocalizedCopy,
    isOverall: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val financeColors = LocalFinanceColors.current
    val remaining = item.budget.limitAmount - item.spent
    val barColor = when {
        item.ratio >= 1f -> financeColors.expense
        item.ratio >= 0.75f -> financeColors.warning
        else -> financeColors.income
    }
    val titleText = if (isOverall) {
        copy.overallMonthlyBudget
    } else {
        val meta = CategoryCatalog.metaFor(item.budget.category)
        "${meta.emoji} ${meta.displayName(language)}"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = copy.spentOf(
                            FinanceFormatters.formatCurrency(item.spent, currencyCode, lang = language),
                            FinanceFormatters.formatCurrency(item.budget.limitAmount, currencyCode, lang = language)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onEdit) { Text(copy.edit) }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = copy.delete,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { item.ratio.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = copy.percentUsed((item.ratio * 100).toInt()),
                    style = MaterialTheme.typography.labelSmall,
                    color = barColor
                )
                Text(
                    text = if (remaining >= 0) {
                        copy.leftLabel(FinanceFormatters.formatCurrency(remaining, currencyCode, lang = language))
                    } else {
                        copy.overLimitLabel(FinanceFormatters.formatCurrency(-remaining, currencyCode, lang = language))
                    },
                    style = MonoSmallStyle,
                    color = if (remaining >= 0) MaterialTheme.colorScheme.onSurfaceVariant else financeColors.expense
                )
            }
        }
    }
}

@Composable
private fun RecurringPaymentCard(
    payment: RecurringPayment,
    currencyCode: String,
    language: AppLanguage,
    copy: LocalizedCopy,
    onToggleActive: (Boolean) -> Unit,
    onLogNow: () -> Unit,
    onDelete: () -> Unit
) {
    val financeColors = LocalFinanceColors.current
    val meta = CategoryCatalog.metaFor(payment.category)
    val daysUntilDue = TimeUnit.MILLISECONDS.toDays(payment.nextDueMillis - System.currentTimeMillis())
    val dueBadgeText = when {
        daysUntilDue < 0 -> copy.overdueByDays(-daysUntilDue)
        daysUntilDue == 0L -> copy.dueToday
        else -> copy.dueInDays(daysUntilDue, FinanceFormatters.formatDateShort(payment.nextDueMillis, language))
    }
    val isIncome = payment.parsedType == TransactionType.INCOME

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(meta.color.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(meta.emoji)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(payment.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${payment.parsedFrequency.displayLabel(language)} • ${meta.displayName(language)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = FinanceFormatters.formatCurrency(
                        payment.amount,
                        currencyCode,
                        showSign = true,
                        isPositive = isIncome,
                        lang = language
                    ),
                    style = MonoMediumStyle,
                    color = if (isIncome) financeColors.income else financeColors.expense
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (daysUntilDue <= payment.reminderDaysBefore) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = dueBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (payment.active) copy.alertOn else copy.paused,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = payment.active,
                        onCheckedChange = onToggleActive
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onLogNow,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(copy.logPaymentNow)
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = copy.delete,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
