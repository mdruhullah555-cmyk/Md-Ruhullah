package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryCatalog
import com.example.data.model.CurrencyOption
import com.example.data.model.FinanceFormatters
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditorSheet(
    editingItem: TransactionItem?,
    currencyCode: String,
    language: AppLanguage = AppLanguage.BN,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        note: String?,
        dateMillis: Long
    ) -> Unit,
    onDelete: ((String) -> Unit)?
) {
    val copy = remember(language) { AppStrings.forLanguage(language) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type by remember(editingItem) {
        mutableStateOf(editingItem?.parsedType ?: TransactionType.EXPENSE)
    }
    var title by remember(editingItem) { mutableStateOf(editingItem?.title ?: "") }
    var amountText by remember(editingItem) {
        mutableStateOf(editingItem?.amount?.let { if (it > 0) it.toString() else "" } ?: "")
    }
    var selectedCategory by remember(editingItem, type) {
        val initial = editingItem?.category?.let { CategoryCatalog.metaFor(it).id }
        val validForType = CategoryCatalog.categoriesFor(type).map { it.id }
        mutableStateOf(
            if (initial != null && validForType.contains(initial)) initial
            else validForType.first()
        )
    }
    var categoryDropdownExpanded by remember(editingItem, type) { mutableStateOf(false) }
    var note by remember(editingItem) { mutableStateOf(editingItem?.note ?: "") }
    var dateMillis by remember(editingItem) {
        mutableLongStateOf(editingItem?.dateMillis ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val categories = remember(type) { CategoryCatalog.categoriesFor(type) }
    val selectedCategoryMeta = remember(selectedCategory, categories) {
        categories.firstOrNull { it.id == selectedCategory } ?: categories.first()
    }
    val currencySymbol = CurrencyOption.fromCode(currencyCode).symbol

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingItem == null) copy.logDailyEntry else copy.editEntry,
                    style = MaterialTheme.typography.headlineSmall
                )
                if (editingItem != null && onDelete != null) {
                    IconButton(
                        onClick = {
                            onDelete(editingItem.id)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("delete_transaction_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = copy.delete,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TransactionType.entries.forEachIndexed { index, txType ->
                    SegmentedButton(
                        selected = type == txType,
                        onClick = {
                            type = txType
                            categoryDropdownExpanded = false
                            val validCats = CategoryCatalog.categoriesFor(txType)
                            if (validCats.none { it.id == selectedCategory }) {
                                selectedCategory = validCats.first().id
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TransactionType.entries.size
                        ),
                        modifier = Modifier.testTag("type_${txType.name.lowercase()}_button")
                    ) {
                        Text(if (txType == TransactionType.EXPENSE) copy.expense else copy.income)
                    }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { raw ->
                    val filtered = raw.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) {
                        amountText = filtered
                    }
                },
                label = { Text("${copy.amountPlaceholder} ($currencySymbol)") },
                placeholder = { Text("0") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_amount_input"),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 80) title = it },
                label = { Text(copy.titleOptionalPlaceholder) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_title_input"),
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
                        Text(if (type == TransactionType.EXPENSE) copy.categoryLabel else copy.sourceLabel)
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth()
                        .testTag("transaction_category_dropdown"),
                    shape = RoundedCornerShape(14.dp)
                )
                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    categories.forEach { cat ->
                        val selected = selectedCategory == cat.id
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${cat.emoji} ${cat.displayName(language)}",
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            },
                            trailingIcon = if (selected) {
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
                                .testTag("transaction_category_option_${cat.id}")
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(copy.dateLabel, style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = FinanceFormatters.formatDateFull(dateMillis, language),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    Text(
                        text = copy.changeDate,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 300) note = it },
                label = { Text(copy.noteOptional) },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_note_input"),
                shape = RoundedCornerShape(14.dp)
            )

            if (validationError != null) {
                Text(
                    text = validationError.orEmpty(),
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
                        val parsedAmount = amountText.toDoubleOrNull()
                        if (parsedAmount == null || parsedAmount <= 0.0) {
                            validationError = copy.enterValidAmount
                        } else {
                            val catMeta = CategoryCatalog.metaFor(selectedCategory)
                            val resolvedTitle = title.trim().ifBlank { catMeta.displayName(language) }
                            onSave(
                                editingItem?.id,
                                resolvedTitle,
                                parsedAmount,
                                type,
                                selectedCategory,
                                note.trim().ifBlank { null },
                                dateMillis
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("save_transaction_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (editingItem == null) copy.addButton else copy.updateEntry)
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
                ) {
                    Text(copy.ok)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(copy.cancel)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
