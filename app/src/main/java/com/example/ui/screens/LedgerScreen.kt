package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CategoryCatalog
import com.example.data.model.FinanceFormatters
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.components.AppTopBar
import com.example.ui.components.LiveClockCard
import com.example.ui.components.LowBalanceAlertBanner
import com.example.ui.components.MonthSwitcherBar
import com.example.ui.components.PrivacyPolicyDialog
import com.example.ui.components.ShareAppCard
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalizedCopy
import com.example.ui.state.DashboardSummary
import com.example.ui.state.LedgerFilter
import com.example.ui.state.UiState
import com.example.ui.theme.HeroGradientEnd
import com.example.ui.theme.HeroGradientMid
import com.example.ui.theme.HeroGradientStart
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.MonoLargeStyle
import com.example.ui.theme.MonoMediumStyle
import com.example.ui.theme.MonoSmallStyle

@Composable
fun LedgerScreen(
    summaryState: UiState<DashboardSummary>,
    selectedMonthKey: String,
    currencyCode: String,
    language: AppLanguage,
    balanceAlertLimit: Double,
    ledgerFilter: LedgerFilter,
    selectedCategoryFilter: String?,
    searchQuery: String,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    onShiftMonth: (Int) -> Unit,
    onSelectFilter: (LedgerFilter) -> Unit,
    onToggleCategoryFilter: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onEditTransaction: (TransactionItem) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onAddTransactionClick: () -> Unit,
    onExportPdfShare: () -> Unit,
    onExportExcelShare: () -> Unit,
    onSavePdfDevice: () -> Unit,
    onSaveExcelDevice: () -> Unit,
    onCopiedShareLink: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = remember(language) { AppStrings.forLanguage(language) }
    val isDark = LocalFinanceColors.current.isDark
    var showPrivacyDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (summaryState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = summaryState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                val summary = summaryState.data
                val hasActiveSearchOrFilter = searchQuery.isNotBlank() ||
                    ledgerFilter != LedgerFilter.ALL ||
                    selectedCategoryFilter != null
                val recentList = if (hasActiveSearchOrFilter) {
                    summary.filteredTransactions
                } else {
                    summary.monthTransactions.take(5)
                }

                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top bar (Brand + Language + Theme) matching HTML
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AppTopBar(
                                copy = copy,
                                language = language,
                                isDark = isDark,
                                onToggleLanguage = onToggleLanguage,
                                onToggleTheme = onToggleTheme
                            )

                            // Low Balance Alert Banner matching HTML (.al)
                            if (summary.monthTransactions.isNotEmpty() && summary.netBalance <= balanceAlertLimit) {
                                LowBalanceAlertBanner(
                                    balance = summary.netBalance,
                                    limit = balanceAlertLimit,
                                    currencyCode = currencyCode,
                                    language = language,
                                    copy = copy
                                )
                            }

                            // Live Digital Clock + Date Card matching HTML (.clk)
                            LiveClockCard(language = language)

                            // Month Switcher matching HTML (.mon)
                            MonthSwitcherBar(
                                monthKey = selectedMonthKey,
                                language = language,
                                onShiftMonth = onShiftMonth
                            )
                        }
                    }

                    // Hero Balance Card matching HTML (.hero)
                    item {
                        HeroBalanceCard(
                            summary = summary,
                            currencyCode = currencyCode,
                            language = language,
                            copy = copy
                        )
                    }

                    // Expenses by Category Card matching HTML
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
                                    text = copy.expensesByCategory,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (summary.categoryBreakdowns.isEmpty()) {
                                    Text(
                                        text = copy.noExpensesThisMonth,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    summary.categoryBreakdowns.forEach { cat ->
                                        val meta = CategoryCatalog.metaFor(cat.category)
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${meta.emoji} ${meta.displayName(language)}",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = FinanceFormatters.formatCurrency(
                                                        cat.total,
                                                        currencyCode,
                                                        lang = language
                                                    ),
                                                    style = MonoSmallStyle,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.background)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(cat.percentage.coerceIn(0.02f, 1f))
                                                        .height(8.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            Brush.horizontalGradient(
                                                                listOf(HeroGradientStart, HeroGradientEnd)
                                                            )
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Daily Average Spending Card matching HTML
                    if (summary.activeExpenseDaysCount > 0) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = copy.dailyAvgSpending,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = FinanceFormatters.formatCurrency(
                                            summary.dailyAverageExpense,
                                            currencyCode,
                                            lang = language
                                        ),
                                        style = MonoMediumStyle.copy(fontSize = 22.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Recent Transactions + Search/Filter Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = copy.recentTransactions,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    placeholder = { Text(copy.searchPlaceholder) },
                                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { onSearchQueryChange("") }) {
                                                Icon(Icons.Rounded.Close, contentDescription = copy.cancel)
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ledger_search_input"),
                                    shape = RoundedCornerShape(14.dp)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LedgerFilter.entries.forEach { filter ->
                                        FilterChip(
                                            selected = ledgerFilter == filter,
                                            onClick = { onSelectFilter(filter) },
                                            label = {
                                                Text(
                                                    when (filter) {
                                                        LedgerFilter.ALL -> copy.filterAll(summary.monthTransactions.size)
                                                        LedgerFilter.INCOME -> copy.income
                                                        LedgerFilter.EXPENSE -> copy.expenses
                                                    }
                                                )
                                            }
                                        )
                                    }
                                    summary.categoryBreakdowns.take(5).forEach { cat ->
                                        val meta = CategoryCatalog.metaFor(cat.category)
                                        FilterChip(
                                            selected = selectedCategoryFilter == cat.category,
                                            onClick = { onToggleCategoryFilter(cat.category) },
                                            label = { Text("${meta.emoji} ${meta.displayName(language)}") }
                                        )
                                    }
                                }

                                if (recentList.isEmpty()) {
                                    EmptyLedgerInline(
                                        hasAnyInMonth = summary.monthTransactions.isNotEmpty(),
                                        copy = copy,
                                        onAddClick = onAddTransactionClick
                                    )
                                } else {
                                    Column {
                                        recentList.forEachIndexed { idx, tx ->
                                            TransactionListRow(
                                                item = tx,
                                                currencyCode = currencyCode,
                                                language = language,
                                                onEdit = { onEditTransaction(tx) },
                                                onDelete = { onDeleteTransaction(tx.id) }
                                            )
                                            if (idx < recentList.lastIndex) {
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Monthly Report Card (PDF & Excel) matching HTML
                    item {
                        val monthName = FinanceFormatters.formatMonthDisplay(selectedMonthKey, language)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = copy.monthlyReportTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = copy.monthlyReportDesc(monthName),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = onExportPdfShare,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("home_export_pdf_button"),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(copy.downloadSharePdf)
                                    }
                                    Button(
                                        onClick = onExportExcelShare,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("home_export_excel_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary
                                        )
                                    ) {
                                        Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(copy.downloadShareExcel)
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onSavePdfDevice,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(copy.savePdfToDevice)
                                    }
                                    OutlinedButton(
                                        onClick = onSaveExcelDevice,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(copy.saveExcelToDevice)
                                    }
                                }
                            }
                        }
                    }

                    // Share With Friends Card matching HTML
                    item {
                        ShareAppCard(
                            copy = copy,
                            onCopied = onCopiedShareLink
                        )
                    }

                    // Account & Privacy Card matching HTML bottom card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = copy.accountSectionTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedButton(
                                    onClick = { showPrivacyDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("home_privacy_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(copy.privacyPolicyLabel.replace("🔒 ", ""))
                                }
                                Button(
                                    onClick = onSignOut,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("home_sign_out_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(copy.signOut)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(
            copy = copy,
            onDismiss = { showPrivacyDialog = false }
        )
    }
}

@Composable
private fun HeroBalanceCard(
    summary: DashboardSummary,
    currencyCode: String,
    language: AppLanguage,
    copy: LocalizedCopy
) {
    val financeColors = LocalFinanceColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(HeroGradientStart, HeroGradientMid, HeroGradientEnd)
                )
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = copy.remainingBalance,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FinanceFormatters.formatCurrency(summary.netBalance, currencyCode, lang = language),
                        style = MonoLargeStyle.copy(fontSize = 32.sp),
                        color = Color.White
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = copy.savedPercent(summary.savingsRatePercent),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (summary.savingsRatePercent >= 0) Color(0xFFA7F3D0) else Color(0xFFFFB3C0),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(
                            text = copy.income,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = FinanceFormatters.formatCurrency(summary.totalIncome, currencyCode, lang = language),
                            style = MonoMediumStyle,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(
                            text = copy.expense,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = FinanceFormatters.formatCurrency(summary.totalExpense, currencyCode, lang = language),
                            style = MonoMediumStyle,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            summary.overallBudgetProgress?.let { overall ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = copy.monthlyLimitLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "${FinanceFormatters.formatNumber((overall.ratio * 100).toInt(), language)}% / ${
                                FinanceFormatters.formatCurrency(overall.budget.limitAmount, currencyCode, lang = language)
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (overall.ratio >= 1f) Color(0xFFFFB3C0) else Color(0xFFA7F3D0)
                        )
                    }
                    LinearProgressIndicator(
                        progress = { overall.ratio.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50)),
                        color = when {
                            overall.ratio >= 1f -> financeColors.expense
                            overall.ratio >= 0.75f -> financeColors.warning
                            else -> Color(0xFF7CFFCB)
                        },
                        trackColor = Color.White.copy(alpha = 0.20f)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionListRow(
    item: TransactionItem,
    currencyCode: String,
    language: AppLanguage,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val financeColors = LocalFinanceColors.current
    val meta = CategoryCatalog.metaFor(item.category)
    val isIncome = item.parsedType == TransactionType.INCOME
    val catDisplay = meta.displayName(language)
    val primaryLabel = if (item.title.isNotBlank() && !item.title.equals(catDisplay, ignoreCase = true)) {
        "$catDisplay · ${item.title}"
    } else {
        catDisplay
    }
    val dateSub = buildString {
        append(FinanceFormatters.formatDateShort(item.dateMillis, language))
        if (!item.note.isNullOrBlank()) {
            append(" · ")
            append(item.note)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(vertical = 10.dp)
            .testTag("transaction_card_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(text = meta.emoji, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dateSub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = FinanceFormatters.formatCurrency(
                amount = item.amount,
                currencyCode = currencyCode,
                showSign = true,
                isPositive = isIncome,
                lang = language
            ),
            style = MonoMediumStyle,
            fontWeight = FontWeight.Bold,
            color = if (isIncome) financeColors.income else financeColors.expense
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun EmptyLedgerInline(
    hasAnyInMonth: Boolean,
    copy: LocalizedCopy,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onAddClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_ledger),
            contentDescription = copy.nothingYet,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop
        )
        Text(
            text = if (hasAnyInMonth) copy.viewAllOrFilter else copy.nothingYet,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
