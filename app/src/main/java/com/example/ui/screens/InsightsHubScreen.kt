package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AppPreferencesState
import com.example.data.model.RecurringPayment
import com.example.data.model.ThemeMode
import com.example.ui.components.AppTopBar
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.state.DashboardSummary
import com.example.ui.state.UiState
import com.example.ui.theme.LocalFinanceColors

@Composable
fun InsightsHubScreen(
    summaryState: UiState<DashboardSummary>,
    recurringState: UiState<List<RecurringPayment>>,
    selectedMonthKey: String,
    activeCurrencyCode: String,
    preferences: AppPreferencesState,
    userDisplayName: String,
    userEmail: String,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onShiftMonth: (Int) -> Unit,
    onExportPdfShare: () -> Unit,
    onExportExcelShare: () -> Unit,
    onSavePdfDevice: () -> Unit,
    onSaveExcelDevice: () -> Unit,
    onOpenBudgetEditor: (String, Double?) -> Unit,
    onDeleteBudget: (String) -> Unit,
    onOpenRecurringEditor: () -> Unit,
    onToggleRecurringActive: (String, Boolean) -> Unit,
    onLogRecurringNow: (RecurringPayment) -> Unit,
    onDeleteRecurring: (String) -> Unit,
    onTestRemindersNow: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onBalanceAlertLimitChange: (Double) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onCopiedShareLink: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val language = preferences.language
    val copy = remember(language) { AppStrings.forLanguage(language) }
    val isDark = LocalFinanceColors.current.isDark
    var selectedSection by rememberSaveable { mutableIntStateOf(0) }

    val sectionLabels = remember(copy) {
        listOf(copy.tabTrendsExport, copy.tabBudgetsBills, copy.tabSettings)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppTopBar(
                copy = copy,
                language = language,
                isDark = isDark,
                onToggleLanguage = onToggleLanguage,
                onToggleTheme = onToggleTheme,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp)
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                sectionLabels.forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = selectedSection == idx,
                        onClick = { selectedSection = idx },
                        shape = SegmentedButtonDefaults.itemShape(idx, sectionLabels.size),
                        modifier = Modifier.testTag("insights_section_$idx")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedSection) {
                    0 -> AnalyticsScreen(
                        summaryState = summaryState,
                        selectedMonthKey = selectedMonthKey,
                        currencyCode = activeCurrencyCode,
                        language = language,
                        onShiftMonth = onShiftMonth,
                        onExportPdfShare = onExportPdfShare,
                        onExportExcelShare = onExportExcelShare,
                        onSavePdfDevice = onSavePdfDevice,
                        onSaveExcelDevice = onSaveExcelDevice
                    )
                    1 -> PlanningScreen(
                        summaryState = summaryState,
                        recurringState = recurringState,
                        selectedMonthKey = selectedMonthKey,
                        currencyCode = activeCurrencyCode,
                        language = language,
                        notificationsEnabled = preferences.notificationsEnabled,
                        onOpenBudgetEditor = onOpenBudgetEditor,
                        onDeleteBudget = onDeleteBudget,
                        onOpenRecurringEditor = onOpenRecurringEditor,
                        onToggleRecurringActive = onToggleRecurringActive,
                        onLogRecurringNow = onLogRecurringNow,
                        onDeleteRecurring = onDeleteRecurring,
                        onTestRemindersNow = onTestRemindersNow
                    )
                    else -> SettingsScreen(
                        userDisplayName = userDisplayName,
                        userEmail = userEmail,
                        preferences = preferences,
                        activeCurrencyCode = activeCurrencyCode,
                        onLanguageChange = onLanguageChange,
                        onThemeModeChange = onThemeModeChange,
                        onToggleBiometric = onToggleBiometric,
                        onToggleNotifications = onToggleNotifications,
                        onBalanceAlertLimitChange = onBalanceAlertLimitChange,
                        onCurrencyChange = onCurrencyChange,
                        onExportPdf = onExportPdfShare,
                        onExportExcel = onExportExcelShare,
                        onCopiedShareLink = onCopiedShareLink,
                        onSignOut = onSignOut
                    )
                }
            }
        }
    }
}
