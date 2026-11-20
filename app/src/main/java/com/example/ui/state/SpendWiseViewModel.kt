package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppPreferencesState
import com.example.data.local.SettingsPreferences
import com.example.data.model.CategoryBudget
import com.example.data.model.CategoryCatalog
import com.example.data.model.CurrencyOption
import com.example.data.model.FinanceFormatters
import com.example.data.model.LoanItem
import com.example.data.model.LoanKind
import com.example.data.model.RecurringFrequency
import com.example.data.model.RecurringPayment
import com.example.data.model.ThemeMode
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.data.remote.FinanceRepository
import com.example.export.ExportReportPayload
import com.example.ui.i18n.AppLanguage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

private const val TAG = "SpendWiseVM"
private const val STOP_TIMEOUT_MILLIS = 5000L

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class LedgerFilter { ALL, INCOME, EXPENSE }

data class CategoryBreakdownItem(
    val category: String,
    val total: Double,
    val percentage: Float
)

data class MonthTrendPoint(
    val monthKey: String,
    val label: String,
    val income: Double,
    val expense: Double,
    val net: Double
)

data class DailySpendPoint(
    val dayOfMonth: Int,
    val expense: Double,
    val income: Double
)

data class BudgetProgressItem(
    val budget: CategoryBudget,
    val spent: Double,
    val ratio: Float
)

data class DashboardSummary(
    val monthKey: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double,
    val savingsRatePercent: Int,
    val activeExpenseDaysCount: Int,
    val dailyAverageExpense: Double,
    val monthTransactions: List<TransactionItem>,
    val filteredTransactions: List<TransactionItem>,
    val categoryBreakdowns: List<CategoryBreakdownItem>,
    val sixMonthTrends: List<MonthTrendPoint>,
    val dailyPoints: List<DailySpendPoint>,
    val overallBudgetProgress: BudgetProgressItem?,
    val categoryBudgetProgress: List<BudgetProgressItem>
)

data class LoansSummary(
    val totalIOwe: Double,
    val totalIWillReceive: Double,
    val pendingLoans: List<LoanItem>,
    val settledLoans: List<LoanItem>
)

class SpendWiseViewModel(
    private val financeRepository: FinanceRepository,
    private val settingsPreferences: SettingsPreferences,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String
) : ViewModel() {

    private val _selectedMonthKey = MutableStateFlow(
        FinanceFormatters.monthKeyFromMillis(System.currentTimeMillis())
    )
    val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

    private val _ledgerFilter = MutableStateFlow(LedgerFilter.ALL)
    val ledgerFilter: StateFlow<LedgerFilter> = _ledgerFilter.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private val _lowBalanceDialogBalance = MutableStateFlow<Double?>(null)
    val lowBalanceDialogBalance: StateFlow<Double?> = _lowBalanceDialogBalance.asStateFlow()

    val appPreferences: StateFlow<AppPreferencesState> = settingsPreferences.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AppPreferencesState()
        )

    val userProfileState: StateFlow<UiState<UserProfile?>> = financeRepository
        .observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load profile")))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val transactionsState: StateFlow<UiState<List<TransactionItem>>> = financeRepository
        .observeTransactions(currentUserId)
        .map<List<TransactionItem>, UiState<List<TransactionItem>>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to sync transactions")))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val budgetsState: StateFlow<UiState<List<CategoryBudget>>> = financeRepository
        .observeBudgets(currentUserId)
        .map<List<CategoryBudget>, UiState<List<CategoryBudget>>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to sync budgets")))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val recurringState: StateFlow<UiState<List<RecurringPayment>>> = financeRepository
        .observeRecurringPayments(currentUserId)
        .map<List<RecurringPayment>, UiState<List<RecurringPayment>>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to sync recurring payments")))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val loansState: StateFlow<UiState<LoansSummary>> = financeRepository
        .observeLoans(currentUserId)
        .map<List<LoanItem>, UiState<LoansSummary>> { loans ->
            val pending = loans.filter { !it.settled }.sortedByDescending { it.dateMillis }
            val settled = loans.filter { it.settled }.sortedByDescending { it.dateMillis }
            val iOwe = pending.filter { it.parsedKind == LoanKind.BORROW }.sumOf { it.remainingAmount }
            val iReceive = pending.filter { it.parsedKind == LoanKind.LEND }.sumOf { it.remainingAmount }
            UiState.Success(
                LoansSummary(
                    totalIOwe = iOwe,
                    totalIWillReceive = iReceive,
                    pendingLoans = pending,
                    settledLoans = settled
                )
            )
        }
        .catch { error ->
            emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to sync loans")))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val dashboardSummary: StateFlow<UiState<DashboardSummary>> = combine(
        transactionsState,
        budgetsState,
        _selectedMonthKey,
        _ledgerFilter,
        _selectedCategoryFilter,
        _searchQuery,
        appPreferences
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val txState = values[0] as UiState<List<TransactionItem>>
        @Suppress("UNCHECKED_CAST")
        val budgetState = values[1] as UiState<List<CategoryBudget>>
        val monthKey = values[2] as String
        val filter = values[3] as LedgerFilter
        val catFilter = values[4] as String?
        val query = values[5] as String
        val prefs = values[6] as AppPreferencesState

        when {
            txState is UiState.Error -> UiState.Error(txState.message)
            budgetState is UiState.Error -> UiState.Error(budgetState.message)
            txState is UiState.Loading || budgetState is UiState.Loading -> UiState.Loading
            txState is UiState.Success && budgetState is UiState.Success -> {
                UiState.Success(
                    computeDashboardSummary(
                        allTransactions = txState.data,
                        allBudgets = budgetState.data,
                        monthKey = monthKey,
                        filter = filter,
                        categoryFilter = catFilter,
                        searchQuery = query,
                        lang = prefs.language
                    )
                )
            }
            else -> UiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    val activeCurrencyCode: StateFlow<String> = combine(
        userProfileState,
        appPreferences
    ) { profileState, prefs ->
        val remoteCurrency = (profileState as? UiState.Success)?.data?.currencyCode
        remoteCurrency ?: prefs.currencyCode
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CurrencyOption.BDT.code
    )

    init {
        viewModelScope.launch {
            financeRepository.ensureUserProfile(
                userId = currentUserId,
                displayName = userDisplayName,
                email = userEmail,
                defaultCurrency = appPreferences.value.currencyCode
            )
        }
    }

    fun shiftMonth(delta: Int) {
        _selectedMonthKey.value = FinanceFormatters.shiftMonthKey(_selectedMonthKey.value, delta)
    }

    fun selectMonthForDate(dateMillis: Long) {
        _selectedMonthKey.value = FinanceFormatters.monthKeyFromMillis(dateMillis)
    }

    fun setLedgerFilter(filter: LedgerFilter) {
        _ledgerFilter.value = filter
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == category) null else category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun dismissLowBalanceDialog() {
        _lowBalanceDialogBalance.value = null
    }

    fun saveTransaction(
        existingId: String?,
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        note: String?,
        dateMillis: Long
    ) {
        val currentLang = appPreferences.value.language
        val currentSummary = (dashboardSummary.value as? UiState.Success)?.data
        val alertLimit = appPreferences.value.balanceAlertLimit
        val targetMonth = FinanceFormatters.monthKeyFromMillis(dateMillis)

        viewModelScope.launch {
            val result = if (existingId.isNullOrBlank()) {
                financeRepository.addTransaction(
                    userId = currentUserId,
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    note = note,
                    dateMillis = dateMillis
                )
            } else {
                financeRepository.updateTransaction(
                    userId = currentUserId,
                    txId = existingId,
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    note = note,
                    dateMillis = dateMillis
                )
            }
            result.onSuccess {
                _selectedMonthKey.value = targetMonth
                val savedMsg = if (currentLang == AppLanguage.BN) {
                    if (existingId == null) "হিসাব সংরক্ষিত হয়েছে" else "হিসাব হালনাগাদ হয়েছে"
                } else {
                    if (existingId == null) "Entry saved to cloud" else "Entry updated"
                }
                _snackbarEvents.tryEmit(savedMsg)

                if (existingId == null && type == TransactionType.EXPENSE && currentSummary != null && currentSummary.monthKey == targetMonth) {
                    val projected = currentSummary.netBalance - amount
                    if (projected <= alertLimit) {
                        _lowBalanceDialogBalance.value = projected
                    }
                }
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to save entry")
            }
        }
    }

    fun deleteTransaction(txId: String) {
        val lang = appPreferences.value.language
        viewModelScope.launch {
            financeRepository.deleteTransaction(currentUserId, txId)
                .onSuccess {
                    _snackbarEvents.tryEmit(if (lang == AppLanguage.BN) "মুছে ফেলা হয়েছে" else "Entry deleted")
                }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to delete entry") }
        }
    }

    fun addLoan(
        kind: LoanKind,
        personName: String,
        amount: Double,
        dateMillis: Long,
        note: String?
    ) {
        val lang = appPreferences.value.language
        viewModelScope.launch {
            financeRepository.addLoan(
                userId = currentUserId,
                kind = kind,
                personName = personName,
                amount = amount,
                dateMillis = dateMillis,
                note = note
            ).onSuccess {
                _snackbarEvents.tryEmit(if (lang == AppLanguage.BN) "ধারের হিসাব যোগ হয়েছে" else "Loan entry saved")
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to save loan")
            }
        }
    }

    fun repayLoan(loan: LoanItem, paymentAmount: Double, paymentDateMillis: Long) {
        val lang = appPreferences.value.language
        viewModelScope.launch {
            financeRepository.repayLoan(currentUserId, loan, paymentAmount, paymentDateMillis)
                .onSuccess {
                    _snackbarEvents.tryEmit(if (lang == AppLanguage.BN) "পরিশোধ সংরক্ষিত হয়েছে" else "Repayment recorded")
                }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to record repayment") }
        }
    }

    fun reopenLoan(loanId: String) {
        val lang = appPreferences.value.language
        viewModelScope.launch {
            financeRepository.reopenLoan(currentUserId, loanId)
                .onSuccess {
                    _snackbarEvents.tryEmit(if (lang == AppLanguage.BN) "ধার আবার খোলা হয়েছে" else "Loan reopened")
                }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to reopen loan") }
        }
    }

    fun deleteLoan(loanId: String) {
        val lang = appPreferences.value.language
        viewModelScope.launch {
            financeRepository.deleteLoan(currentUserId, loanId)
                .onSuccess {
                    _snackbarEvents.tryEmit(if (lang == AppLanguage.BN) "ধার মুছে ফেলা হয়েছে" else "Loan deleted")
                }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to delete loan") }
        }
    }

    fun saveBudget(category: String, limitAmount: Double) {
        val monthKey = _selectedMonthKey.value
        val lang = appPreferences.value.language
        viewModelScope.launch {
            financeRepository.upsertBudget(
                userId = currentUserId,
                category = category,
                limitAmount = limitAmount,
                monthKey = monthKey
            ).onSuccess {
                val label = if (category == "OVERALL") {
                    if (lang == AppLanguage.BN) "মাসিক মোট" else "Overall"
                } else {
                    CategoryCatalog.metaFor(category).displayName(lang)
                }
                _snackbarEvents.tryEmit(
                    if (lang == AppLanguage.BN) "$label বাজেট সংরক্ষিত হয়েছে" else "Budget saved for $label"
                )
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to save budget")
            }
        }
    }

    fun deleteBudget(budgetId: String) {
        val lang = appPreferences.value.language
        viewModelScope.launch {
            financeRepository.deleteBudget(currentUserId, budgetId)
                .onSuccess {
                    _snackbarEvents.tryEmit(if (lang == AppLanguage.BN) "বাজেট মুছে ফেলা হয়েছে" else "Budget removed")
                }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to remove budget") }
        }
    }

    fun addRecurringPayment(
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        frequency: RecurringFrequency,
        dayOfMonth: Int,
        nextDueMillis: Long,
        reminderDaysBefore: Int
    ) {
        val lang = appPreferences.value.language
        viewModelScope.launch {
            financeRepository.addRecurringPayment(
                userId = currentUserId,
                title = title,
                amount = amount,
                type = type,
                category = category,
                frequency = frequency,
                dayOfMonth = dayOfMonth,
                nextDueMillis = nextDueMillis,
                reminderDaysBefore = reminderDaysBefore
            ).onSuccess {
                _snackbarEvents.tryEmit(
                    if (lang == AppLanguage.BN) "নিয়মিত পেমেন্ট শিডিউল হয়েছে" else "Recurring payment scheduled"
                )
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to schedule recurring payment")
            }
        }
    }

    fun toggleRecurringActive(recurringId: String, active: Boolean) {
        viewModelScope.launch {
            financeRepository.toggleRecurringActive(currentUserId, recurringId, active)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update status") }
        }
    }

    fun logRecurringNow(payment: RecurringPayment) {
        val lang = appPreferences.value.language
        viewModelScope.launch {
            financeRepository.logRecurringPaymentNow(currentUserId, payment)
                .onSuccess {
                    _snackbarEvents.tryEmit(
                        if (lang == AppLanguage.BN) "'${payment.title}' লেনদেন হিসেবে যোগ হয়েছে"
                        else "Logged '${payment.title}' & updated next due date"
                    )
                }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to log payment") }
        }
    }

    fun deleteRecurringPayment(recurringId: String) {
        val lang = appPreferences.value.language
        viewModelScope.launch {
            financeRepository.deleteRecurringPayment(currentUserId, recurringId)
                .onSuccess {
                    _snackbarEvents.tryEmit(if (lang == AppLanguage.BN) "শিডিউল মুছে ফেলা হয়েছে" else "Recurring schedule deleted")
                }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to delete schedule") }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsPreferences.setThemeMode(mode) }
    }

    fun toggleQuickTheme(currentlyDark: Boolean) {
        setThemeMode(if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK)
    }

    fun toggleLanguage() {
        val next = if (appPreferences.value.language == AppLanguage.BN) AppLanguage.EN else AppLanguage.BN
        viewModelScope.launch { settingsPreferences.setLanguage(next) }
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch { settingsPreferences.setLanguage(lang) }
    }

    fun setBalanceAlertLimit(limit: Double) {
        val lang = appPreferences.value.language
        viewModelScope.launch {
            settingsPreferences.setBalanceAlertLimit(limit)
            _snackbarEvents.tryEmit(
                if (lang == AppLanguage.BN) "সতর্কতা সীমা সংরক্ষিত হয়েছে" else "Balance alert limit saved"
            )
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setBiometricEnabled(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setNotificationsEnabled(enabled) }
    }

    fun setCurrencyCode(code: String) {
        viewModelScope.launch {
            settingsPreferences.setCurrencyCode(code)
            financeRepository.updateUserCurrency(currentUserId, code)
                .onSuccess { _snackbarEvents.tryEmit("Currency updated to $code") }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to sync currency") }
        }
    }

    fun buildExportPayload(): ExportReportPayload? {
        val summary = (dashboardSummary.value as? UiState.Success)?.data ?: return null
        val budgets = (budgetsState.value as? UiState.Success)?.data
            ?.filter { it.monthKey == summary.monthKey }
            ?: emptyList()
        val recurring = (recurringState.value as? UiState.Success)?.data ?: emptyList()

        return ExportReportPayload(
            monthKey = summary.monthKey,
            currencyCode = activeCurrencyCode.value,
            userName = userDisplayName.ifBlank { userEmail },
            language = appPreferences.value.language,
            totalIncome = summary.totalIncome,
            totalExpense = summary.totalExpense,
            netBalance = summary.netBalance,
            categoryTotals = summary.categoryBreakdowns.map { it.category to it.total },
            transactions = summary.monthTransactions,
            budgets = budgets,
            recurringPayments = recurring
        )
    }

    private fun computeDashboardSummary(
        allTransactions: List<TransactionItem>,
        allBudgets: List<CategoryBudget>,
        monthKey: String,
        filter: LedgerFilter,
        categoryFilter: String?,
        searchQuery: String,
        lang: AppLanguage
    ): DashboardSummary {
        val monthTxs = allTransactions.filter { it.monthKey == monthKey }
            .sortedByDescending { it.dateMillis }

        val income = monthTxs.filter { it.parsedType == TransactionType.INCOME }.sumOf { it.amount }
        val expense = monthTxs.filter { it.parsedType == TransactionType.EXPENSE }.sumOf { it.amount }
        val net = income - expense
        val savingsRate = if (income > 0.0) (((income - expense) / income) * 100.0).toInt().coerceIn(-99, 100) else 0

        val expenseDayKeys = monthTxs
            .filter { it.parsedType == TransactionType.EXPENSE }
            .map {
                val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
                "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
            }
            .toSet()
        val activeExpenseDays = expenseDayKeys.size
        val dailyAvg = if (activeExpenseDays > 0) expense / activeExpenseDays else 0.0

        val q = searchQuery.trim().lowercase()
        val filtered = monthTxs.filter { tx ->
            val typeOk = when (filter) {
                LedgerFilter.ALL -> true
                LedgerFilter.INCOME -> tx.parsedType == TransactionType.INCOME
                LedgerFilter.EXPENSE -> tx.parsedType == TransactionType.EXPENSE
            }
            val catOk = categoryFilter == null || tx.category == categoryFilter
            val meta = CategoryCatalog.metaFor(tx.category)
            val searchOk = q.isEmpty() ||
                tx.title.lowercase().contains(q) ||
                tx.category.lowercase().contains(q) ||
                meta.labelEn.lowercase().contains(q) ||
                meta.labelBn.contains(q) ||
                (tx.note?.lowercase()?.contains(q) == true)
            typeOk && catOk && searchOk
        }

        val expenseByCategory = monthTxs
            .filter { it.parsedType == TransactionType.EXPENSE }
            .groupBy { CategoryCatalog.metaFor(it.category).id }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }

        val breakdowns = expenseByCategory.map { (cat, total) ->
            CategoryBreakdownItem(
                category = cat,
                total = total,
                percentage = if (expense > 0.0) (total / expense).toFloat() else 0f
            )
        }

        val recentMonths = FinanceFormatters.recentMonthKeys(monthKey, 6)
        val trends = recentMonths.map { key ->
            val txs = allTransactions.filter { it.monthKey == key }
            val mIn = txs.filter { it.parsedType == TransactionType.INCOME }.sumOf { it.amount }
            val mEx = txs.filter { it.parsedType == TransactionType.EXPENSE }.sumOf { it.amount }
            MonthTrendPoint(
                monthKey = key,
                label = FinanceFormatters.shortMonthLabel(key, lang),
                income = mIn,
                expense = mEx,
                net = mIn - mEx
            )
        }

        val cal = Calendar.getInstance()
        val dailyMap = mutableMapOf<Int, Pair<Double, Double>>()
        monthTxs.forEach { tx ->
            cal.timeInMillis = tx.dateMillis
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val prev = dailyMap[day] ?: (0.0 to 0.0)
            dailyMap[day] = if (tx.parsedType == TransactionType.EXPENSE) {
                (prev.first + tx.amount) to prev.second
            } else {
                prev.first to (prev.second + tx.amount)
            }
        }
        val dailyPoints = dailyMap.entries
            .sortedBy { it.key }
            .map { (day, pair) -> DailySpendPoint(dayOfMonth = day, expense = pair.first, income = pair.second) }

        val monthBudgets = allBudgets.filter { it.monthKey == monthKey }
        val overallBudget = monthBudgets.firstOrNull { it.category == "OVERALL" }
        val overallProgress = overallBudget?.let { b ->
            BudgetProgressItem(
                budget = b,
                spent = expense,
                ratio = if (b.limitAmount > 0.0) (expense / b.limitAmount).toFloat() else 0f
            )
        }
        val categoryProgress = monthBudgets
            .filter { it.category != "OVERALL" }
            .map { b ->
                val targetId = CategoryCatalog.metaFor(b.category).id
                val spent = expenseByCategory.firstOrNull { it.key == targetId }?.value ?: 0.0
                BudgetProgressItem(
                    budget = b,
                    spent = spent,
                    ratio = if (b.limitAmount > 0.0) (spent / b.limitAmount).toFloat() else 0f
                )
            }
            .sortedByDescending { it.ratio }

        return DashboardSummary(
            monthKey = monthKey,
            totalIncome = income,
            totalExpense = expense,
            netBalance = net,
            savingsRatePercent = savingsRate,
            activeExpenseDaysCount = activeExpenseDays,
            dailyAverageExpense = dailyAvg,
            monthTransactions = monthTxs,
            filteredTransactions = filtered,
            categoryBreakdowns = breakdowns,
            sixMonthTrends = trends,
            dailyPoints = dailyPoints,
            overallBudgetProgress = overallProgress,
            categoryBudgetProgress = categoryProgress
        )
    }

    private fun unwrapFirestoreMessage(error: Throwable, fallback: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, fallback, root)
        return root.message ?: fallback
    }
}
