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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyOption
import com.example.data.model.FinanceFormatters
import com.example.data.model.LoanItem
import com.example.data.model.LoanKind
import com.example.ui.components.AppTopBar
import com.example.ui.components.LiveClockCard
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalizedCopy
import com.example.ui.state.LoansSummary
import com.example.ui.state.UiState
import com.example.ui.theme.HeroGradientEnd
import com.example.ui.theme.HeroGradientStart
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.MonoMediumStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    loansState: UiState<LoansSummary>,
    currencyCode: String,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    onAddLoan: (kind: LoanKind, personName: String, amount: Double, dateMillis: Long, note: String?) -> Unit,
    onRepayLoan: (loan: LoanItem, amount: Double, dateMillis: Long) -> Unit,
    onReopenLoan: (String) -> Unit,
    onDeleteLoan: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = remember(language) { AppStrings.forLanguage(language) }
    val financeColors = LocalFinanceColors.current
    val symbol = CurrencyOption.fromCode(currencyCode).symbol

    var kind by remember { mutableStateOf(LoanKind.BORROW) }
    var personName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var noteText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    var repayTarget by remember { mutableStateOf<LoanItem?>(null) }

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
                    LiveClockCard(language = language)
                }
            }

            // Summary Row: আমি দেব (I owe) | আমি পাব (I'll receive)
            item {
                val summary = (loansState as? UiState.Success)?.data
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = copy.iOwe,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FinanceFormatters.formatCurrency(
                                    summary?.totalIOwe ?: 0.0,
                                    currencyCode,
                                    lang = language
                                ),
                                style = MonoMediumStyle.copy(fontSize = 22.sp),
                                fontWeight = FontWeight.Bold,
                                color = financeColors.expense
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = copy.iWillReceive,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FinanceFormatters.formatCurrency(
                                    summary?.totalIWillReceive ?: 0.0,
                                    currencyCode,
                                    lang = language
                                ),
                                style = MonoMediumStyle.copy(fontSize = 22.sp),
                                fontWeight = FontWeight.Bold,
                                color = financeColors.income
                            )
                        }
                    }
                }
            }

            // Inline Add Loan Card matching HTML
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
                            text = copy.addNewLoan,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            LoanKind.entries.forEachIndexed { idx, itemKind ->
                                SegmentedButton(
                                    selected = kind == itemKind,
                                    onClick = { kind = itemKind },
                                    shape = SegmentedButtonDefaults.itemShape(idx, LoanKind.entries.size),
                                    modifier = Modifier.testTag("loan_kind_${itemKind.name.lowercase()}")
                                ) {
                                    Text(if (itemKind == LoanKind.BORROW) copy.borrowedLabel else copy.lentLabel)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = personName,
                            onValueChange = { if (it.length <= 80) personName = it },
                            label = { Text(copy.personNamePlaceholder) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("loan_person_input"),
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
                                .testTag("loan_amount_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

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
                                .testTag("loan_note_input"),
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
                                    val cleanName = personName.trim()
                                    val parsed = amountText.toDoubleOrNull()
                                    if (cleanName.isEmpty() || parsed == null || parsed <= 0.0) {
                                        formError = copy.enterPersonAndAmount
                                    } else {
                                        formError = null
                                        onAddLoan(
                                            kind,
                                            cleanName,
                                            parsed,
                                            dateMillis,
                                            noteText.trim().ifBlank { null }
                                        )
                                        personName = ""
                                        amountText = ""
                                        noteText = ""
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("add_loan_button"),
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

            // Pending & Settled Lists
            when (loansState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Text(loansState.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is UiState.Success -> {
                    val data = loansState.data
                    item {
                        LoanListSectionCard(
                            title = copy.pendingList,
                            loans = data.pendingLoans,
                            emptyText = copy.noLoans,
                            currencyCode = currencyCode,
                            language = language,
                            copy = copy,
                            onAction = { loan -> repayTarget = loan },
                            onDelete = { loan -> onDeleteLoan(loan.id) }
                        )
                    }

                    if (data.settledLoans.isNotEmpty()) {
                        item {
                            LoanListSectionCard(
                                title = copy.settledList,
                                loans = data.settledLoans,
                                emptyText = copy.noLoans,
                                currencyCode = currencyCode,
                                language = language,
                                copy = copy,
                                onAction = { loan -> onReopenLoan(loan.id) },
                                onDelete = { loan -> onDeleteLoan(loan.id) }
                            )
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

    repayTarget?.let { loan ->
        RepayLoanDialog(
            loan = loan,
            currencyCode = currencyCode,
            language = language,
            copy = copy,
            onDismiss = { repayTarget = null },
            onConfirm = { amount, payDate ->
                onRepayLoan(loan, amount, payDate)
                repayTarget = null
            }
        )
    }
}

@Composable
private fun LoanListSectionCard(
    title: String,
    loans: List<LoanItem>,
    emptyText: String,
    currencyCode: String,
    language: AppLanguage,
    copy: LocalizedCopy,
    onAction: (LoanItem) -> Unit,
    onDelete: (LoanItem) -> Unit
) {
    val financeColors = LocalFinanceColors.current
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (loans.isEmpty()) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp)
                )
            } else {
                loans.forEachIndexed { idx, loan ->
                    val isBorrow = loan.parsedKind == LoanKind.BORROW
                    val subtitle = buildString {
                        append(
                            if (isBorrow) copy.borrowedLabel.replace("🤝 ", "")
                            else copy.lentLabel.replace("💸 ", "")
                        )
                        append(" · ")
                        append(FinanceFormatters.formatDateShort(loan.dateMillis, language))
                        if (loan.paidAmount > 0 && !loan.settled) {
                            append(" · ")
                            append(
                                copy.paidProgress(
                                    FinanceFormatters.formatCurrency(loan.paidAmount, currencyCode, lang = language),
                                    FinanceFormatters.formatCurrency(loan.amount, currencyCode, lang = language)
                                )
                            )
                        }
                        if (!loan.note.isNullOrBlank()) {
                            append(" · ")
                            append(loan.note)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (loan.settled) 0.58f else 1f)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = if (isBorrow) "🤝" else "💸", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = loan.personName,
                                style = MaterialTheme.typography.titleMedium,
                                textDecoration = if (loan.settled) TextDecoration.LineThrough else TextDecoration.None
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            loan.parsedRepayments().forEach { (ts, amt) ->
                                Text(
                                    text = "↳ ${FinanceFormatters.formatDateFull(ts, language)} · ${
                                        if (isBorrow) copy.repaidLabel else copy.receivedLabel
                                    } ${FinanceFormatters.formatCurrency(amt, currencyCode, lang = language)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = financeColors.income,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = FinanceFormatters.formatCurrency(
                                    if (loan.settled) loan.amount else loan.remainingAmount,
                                    currencyCode,
                                    lang = language
                                ),
                                style = MonoMediumStyle,
                                fontWeight = FontWeight.Bold,
                                color = if (isBorrow) financeColors.expense else financeColors.income
                            )
                            Text(
                                text = if (loan.settled) copy.reopenAction else copy.repayAction,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onAction(loan) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = { onDelete(loan) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = copy.delete,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (idx < loans.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepayLoanDialog(
    loan: LoanItem,
    currencyCode: String,
    language: AppLanguage,
    copy: LocalizedCopy,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, dateMillis: Long) -> Unit
) {
    val rem = loan.remainingAmount
    var amountText by remember(loan) {
        mutableStateOf(if (rem % 1.0 == 0.0) rem.toLong().toString() else rem.toString())
    }
    var payDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "💵 ${loan.personName}",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${copy.remainingLabel}: ${FinanceFormatters.formatCurrency(rem, currencyCode, lang = language)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { raw ->
                        val clean = raw.filter { it.isDigit() || it == '.' }
                        if (clean.count { it == '.' } <= 1) amountText = clean
                    },
                    label = { Text(copy.amountPlaceholder) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedButton(
                    onClick = { showPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(FinanceFormatters.formatDateFull(payDateMillis, language))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(copy.cancel) }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = amountText.toDoubleOrNull()
                    if (v != null && v > 0.0) {
                        onConfirm(v, payDateMillis)
                    }
                }
            ) {
                Text(copy.confirm)
            }
        }
    )

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = payDateMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { payDateMillis = it }
                        showPicker = false
                    }
                ) { Text(copy.ok) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(copy.cancel) }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
