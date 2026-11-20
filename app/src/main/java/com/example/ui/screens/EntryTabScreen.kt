package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryCatalog
import com.example.data.model.CurrencyOption
import com.example.data.model.FinanceFormatters
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.components.AppTopBar
import com.example.ui.components.LiveClockCard
import com.example.ui.components.LowBalanceAlertBanner
import com.example.ui.components.MonthSwitcherBar
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.state.DashboardSummary
import com.example.ui.state.UiState
import com.example.ui.theme.HeroGradientEnd
import com.example.ui.theme.HeroGradientStart
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.MonoMediumStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryTabScreen(
    mode: TransactionType,
    summaryState: UiState<DashboardSummary>,
    selectedMonthKey: String,
    currencyCode: String,
    language: AppLanguage,
    balanceAlertLimit: Double,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    onShiftMonth: (Int) -> Unit,
    onAddEntry: (
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        note: String?,
        dateMillis: Long
    ) -> Unit,
    onEditTransaction: (TransactionItem) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = remember(language) { AppStrings.forLanguage(language) }
    val financeColors = LocalFinanceColors.current
    val categories = remember(mode) { CategoryCatalog.categoriesFor(mode) }
    val symbol = CurrencyOption.fromCode(currencyCode).symbol

    var amountText by remember(mode) { mutableStateOf("") }
    var selectedCategory by remember(mode) { mutableStateOf(categories.first().id) }
    var categoryDropdownExpanded by remember(mode) { mutableStateOf(false) }
    var dateMillis by remember(mode) { mutableLongStateOf(System.currentTimeMillis()) }
    var noteText by remember(mode) { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var formError by remember(mode) { mutableStateOf<String?>(null) }
    val selectedCategoryMeta = remember(selectedCategory, categories) {
        categories.firstOrNull { it.id == selectedCategory } ?: categories.first()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppTopBar(
                        copy = copy,
                        language = language,
                        isDark = financeColors.isDark,
                        onToggleLanguage = onToggleLanguage,
                        onToggleTheme = onToggleTheme
                    )
                    if (summaryState is UiState.Success &&
                        summaryState.data.monthTransactions.isNotEmpty() &&
                        summaryState.data.netBalance <= balanceAlertLimit
                    ) {
                        LowBalanceAlertBanner(
                            balance = summaryState.data.netBalance,
                            limit = balanceAlertLimit,
                            currencyCode = currencyCode,
                            language = language,
                            copy = copy
                        )
                    }
                    LiveClockCard(language = language)
                    MonthSwitcherBar(
                        monthKey = selectedMonthKey,
                        language = language,
                        onShiftMonth = onShiftMonth
                    )
                }
            }

            // Inline Add Card matching HTML exp/inc form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (mode == TransactionType.EXPENSE) copy.addNewExpense else copy.addNewIncome,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
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
                                .testTag("inline_entry_amount_${mode.name.lowercase()}"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        ExposedDropdownMenuBox(
                            expanded = categoryDropdownExpanded,
                            onExpandedChange = { categoryDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = "${selectedCategoryMeta.emoji} ${selectedCategoryMeta.displayName(language)}",
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                label = {
                                    Text(if (mode == TransactionType.EXPENSE) copy.categoryLabel else copy.sourceLabel)
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded)
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .fillMaxWidth()
                                    .testTag("inline_entry_category_dropdown_${mode.name.lowercase()}"),
                                shape = RoundedCornerShape(14.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false }
                            ) {
                                categories.forEach { cat ->
                                    val isSelected = selectedCategory == cat.id
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${cat.emoji} ${cat.displayName(language)}",
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        },
                                        trailingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                        onClick = {
                                            selectedCategory = cat.id
                                            categoryDropdownExpanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("inline_entry_category_option_${cat.id}")
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = FinanceFormatters.formatDateFull(dateMillis, language),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Text(
                                    text = copy.changeDate,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { if (it.length <= 300) noteText = it },
                            label = { Text(copy.noteOptional) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("inline_entry_note_${mode.name.lowercase()}"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        if (formError != null) {
                            Text(
                                text = formError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(HeroGradientStart, HeroGradientEnd))
                                )
                        ) {
                            Button(
                                onClick = {
                                    val parsed = amountText.toDoubleOrNull()
                                    if (parsed == null || parsed <= 0.0) {
                                        formError = copy.enterValidAmount
                                    } else {
                                        formError = null
                                        val meta = CategoryCatalog.metaFor(selectedCategory)
                                        onAddEntry(
                                            meta.displayName(language),
                                            parsed,
                                            mode,
                                            selectedCategory,
                                            noteText.trim().ifBlank { null },
                                            dateMillis
                                        )
                                        amountText = ""
                                        noteText = ""
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("inline_entry_add_${mode.name.lowercase()}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = copy.addButton,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // This month's list matching HTML
            item {
                when (summaryState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Error -> {
                        Text(summaryState.message, color = MaterialTheme.colorScheme.error)
                    }
                    is UiState.Success -> {
                        val summary = summaryState.data
                        val list = summary.monthTransactions
                            .filter { it.parsedType == mode }
                            .sortedByDescending { it.dateMillis }
                        val totalAmt = if (mode == TransactionType.EXPENSE) summary.totalExpense else summary.totalIncome

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = copy.thisMonthList,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = FinanceFormatters.formatCurrency(totalAmt, currencyCode, lang = language),
                                        style = MonoMediumStyle,
                                        fontWeight = FontWeight.Bold,
                                        color = if (mode == TransactionType.INCOME) financeColors.income else financeColors.expense
                                    )
                                }

                                if (list.isEmpty()) {
                                    Text(
                                        text = copy.nothingYet,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 18.dp)
                                    )
                                } else {
                                    Column {
                                        list.forEachIndexed { idx, tx ->
                                            TransactionListRow(
                                                item = tx,
                                                currencyCode = currencyCode,
                                                language = language,
                                                onEdit = { onEditTransaction(tx) },
                                                onDelete = { onDeleteTransaction(tx.id) }
                                            )
                                            if (idx < list.lastIndex) {
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis = it }
                        showDatePicker = false
                    }
                ) { Text(copy.ok) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(copy.cancel) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
