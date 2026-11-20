package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryCatalog
import com.example.data.model.CurrencyOption
import com.example.data.model.FinanceFormatters
import com.example.data.model.RecurringFrequency
import com.example.data.model.TransactionType
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetEditorSheet(
    monthKey: String,
    currencyCode: String,
    language: AppLanguage = AppLanguage.BN,
    initialCategory: String = "OVERALL",
    initialLimit: Double? = null,
    onDismiss: () -> Unit,
    onSave: (category: String, limitAmount: Double) -> Unit
) {
    val copy = remember(language) { AppStrings.forLanguage(language) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val categories = remember {
        listOf("OVERALL") + CategoryCatalog.expenseCategories.map { it.id }
    }
    var selectedCategory by remember(initialCategory) {
        mutableStateOf(
            if (initialCategory == "OVERALL") "OVERALL"
            else CategoryCatalog.metaFor(initialCategory).id
        )
    }
    var limitText by remember(initialLimit) {
        mutableStateOf(initialLimit?.let { if (it > 0) it.toString() else "" } ?: "")
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val symbol = CurrencyOption.fromCode(currencyCode).symbol

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "${copy.setBudget} (${FinanceFormatters.formatMonthDisplay(monthKey, language)})",
                style = MaterialTheme.typography.headlineSmall
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = copy.categoryLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { catId ->
                        val label = if (catId == "OVERALL") {
                            copy.overallMonthlyBudget
                        } else {
                            val meta = CategoryCatalog.metaFor(catId)
                            "${meta.emoji} ${meta.displayName(language)}"
                        }
                        FilterChip(
                            selected = selectedCategory == catId,
                            onClick = { selectedCategory = catId },
                            label = { Text(label) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = limitText,
                onValueChange = { raw ->
                    val clean = raw.filter { it.isDigit() || it == '.' }
                    if (clean.count { it == '.' } <= 1) limitText = clean
                },
                label = { Text("${copy.monthlyLimitLabel} ($symbol)") },
                placeholder = { Text("5000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budget_limit_input"),
                shape = RoundedCornerShape(14.dp)
            )

            if (errorMsg != null) {
                Text(
                    text = errorMsg.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(copy.cancel)
                }
                Button(
                    onClick = {
                        val parsed = limitText.toDoubleOrNull()
                        if (parsed == null || parsed <= 0.0) {
                            errorMsg = copy.enterValidAmount
                        } else {
                            onSave(selectedCategory, parsed)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("save_budget_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(copy.save)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecurringEditorSheet(
    currencyCode: String,
    language: AppLanguage = AppLanguage.BN,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        frequency: RecurringFrequency,
        dayOfMonth: Int,
        nextDueMillis: Long,
        reminderDaysBefore: Int
    ) -> Unit
) {
    val copy = remember(language) { AppStrings.forLanguage(language) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember(type) {
        mutableStateOf(CategoryCatalog.categoriesFor(type).first().id)
    }
    var frequency by remember { mutableStateOf(RecurringFrequency.MONTHLY) }
    var dayOfMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }
    var reminderDaysBefore by remember { mutableIntStateOf(2) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val symbol = CurrencyOption.fromCode(currencyCode).symbol

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = copy.addSchedule,
                style = MaterialTheme.typography.headlineSmall
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TransactionType.entries.forEachIndexed { idx, t ->
                    SegmentedButton(
                        selected = type == t,
                        onClick = {
                            type = t
                            category = CategoryCatalog.categoriesFor(t).first().id
                        },
                        shape = SegmentedButtonDefaults.itemShape(idx, TransactionType.entries.size)
                    ) {
                        Text(if (t == TransactionType.EXPENSE) copy.expense else copy.income)
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 80) title = it },
                label = { Text(if (language == AppLanguage.BN) "বিল বা আয়ের নাম" else "Payment Name") },
                placeholder = { Text(if (language == AppLanguage.BN) "যেমন: বাসা ভাড়া, ওয়াইফাই" else "e.g., Rent, WiFi, Salary") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recurring_title_input"),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { raw ->
                    val clean = raw.filter { it.isDigit() || it == '.' }
                    if (clean.count { it == '.' } <= 1) amountText = clean
                },
                label = { Text("${copy.amountPlaceholder} ($symbol)") },
                placeholder = { Text("0") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recurring_amount_input"),
                shape = RoundedCornerShape(14.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(copy.categoryLabel, style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryCatalog.categoriesFor(type).forEach { cat ->
                        FilterChip(
                            selected = category == cat.id,
                            onClick = { category = cat.id },
                            label = { Text("${cat.emoji} ${cat.displayName(language)}") }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (language == AppLanguage.BN) "সময়কাল" else "Frequency",
                    style = MaterialTheme.typography.titleSmall
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    RecurringFrequency.entries.forEachIndexed { idx, freq ->
                        SegmentedButton(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            shape = SegmentedButtonDefaults.itemShape(idx, RecurringFrequency.entries.size)
                        ) {
                            Text(freq.displayLabel(language))
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (language == AppLanguage.BN) "কত দিন আগে অ্যালার্ট" else "Notify Before Due Date",
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 1, 2, 3, 7).forEach { days ->
                        FilterChip(
                            selected = reminderDaysBefore == days,
                            onClick = { reminderDaysBefore = days },
                            label = {
                                Text(
                                    if (language == AppLanguage.BN) {
                                        if (days == 0) "একই দিনে" else "${FinanceFormatters.formatNumber(days, language)} দিন আগে"
                                    } else {
                                        if (days == 0) "On due day" else "$days day${if (days > 1) "s" else ""} early"
                                    }
                                )
                            }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (language == AppLanguage.BN) "মাসের কত তারিখ" else "Due Day of Month",
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 5, 10, 15, 20, 25, 28).forEach { day ->
                        FilterChip(
                            selected = dayOfMonth == day,
                            onClick = { dayOfMonth = day },
                            label = {
                                Text(
                                    if (language == AppLanguage.BN) "${FinanceFormatters.formatNumber(day, language)} তারিখ"
                                    else "Day $day"
                                )
                            }
                        )
                    }
                }
            }

            if (errorMsg != null) {
                Text(
                    text = errorMsg.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(copy.cancel)
                }
                Button(
                    onClick = {
                        val parsed = amountText.toDoubleOrNull()
                        when {
                            title.trim().isEmpty() -> errorMsg = if (language == AppLanguage.BN) "নাম লিখুন।" else "Please enter a name."
                            parsed == null || parsed <= 0.0 -> errorMsg = copy.enterValidAmount
                            else -> {
                                val nextDueCal = Calendar.getInstance().apply {
                                    val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                                    set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceAtMost(maxDay))
                                    if (timeInMillis < System.currentTimeMillis() - 86_400_000L) {
                                        add(Calendar.MONTH, 1)
                                    }
                                }
                                onSave(
                                    title.trim(),
                                    parsed,
                                    type,
                                    category,
                                    frequency,
                                    dayOfMonth,
                                    nextDueCal.timeInMillis,
                                    reminderDaysBefore
                                )
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("save_recurring_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(copy.save)
                }
            }
        }
    }
}
