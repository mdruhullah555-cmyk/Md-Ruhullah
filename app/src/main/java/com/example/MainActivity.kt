package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.local.AppPreferencesState
import com.example.data.local.SettingsPreferences
import com.example.data.model.FinanceFormatters
import com.example.data.model.ThemeMode
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.data.remote.FinanceRepository
import com.example.export.ReportExporter
import com.example.notifications.NotificationHelper
import com.example.notifications.RecurringReminderScheduler
import com.example.security.BiometricAvailability
import com.example.security.BiometricHelper
import com.example.security.BiometricLockScreen
import com.example.ui.components.BudgetEditorSheet
import com.example.ui.components.RecurringEditorSheet
import com.example.ui.components.TransactionEditorSheet
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalizedCopy
import com.example.ui.screens.EntryTabScreen
import com.example.ui.screens.InsightsHubScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.LoansScreen
import com.example.ui.state.SpendWiseViewModel
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.SpendWiseTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private object SpendWiseRoutes {
    const val HOME = "home"
    const val EXPENSES = "expenses"
    const val LOANS = "loans"
    const val INCOME = "income"
    const val INSIGHTS = "insights"
}

private data class NavDestinationItem(
    val route: String,
    val labelProvider: (LocalizedCopy) -> String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val Destinations = listOf(
    NavDestinationItem(
        route = SpendWiseRoutes.HOME,
        labelProvider = { it.navHome },
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    NavDestinationItem(
        route = SpendWiseRoutes.EXPENSES,
        labelProvider = { it.navExpenses },
        selectedIcon = Icons.Rounded.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong
    ),
    NavDestinationItem(
        route = SpendWiseRoutes.LOANS,
        labelProvider = { it.navLoans },
        selectedIcon = Icons.Rounded.Handshake,
        unselectedIcon = Icons.Outlined.Handshake
    ),
    NavDestinationItem(
        route = SpendWiseRoutes.INCOME,
        labelProvider = { it.navIncome },
        selectedIcon = Icons.Rounded.Savings,
        unselectedIcon = Icons.Outlined.Savings
    ),
    NavDestinationItem(
        route = SpendWiseRoutes.INSIGHTS,
        labelProvider = { it.navInsights },
        selectedIcon = Icons.Rounded.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    )
)

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.ensureChannel(this)

        setContent {
            val context = LocalContext.current
            val prefsStore = remember(context) { SettingsPreferences(context.applicationContext) }
            val prefsState by prefsStore.preferencesFlow.collectAsStateWithLifecycle(
                initialValue = AppPreferencesState()
            )

            SpendWiseTheme(themeMode = prefsState.themeMode) {
                SpendWiseRoot(
                    preferencesState = prefsState,
                    settingsPreferences = prefsStore
                )
            }
        }
    }
}

@Composable
private fun SpendWiseRoot(
    preferencesState: AppPreferencesState,
    settingsPreferences: SettingsPreferences
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var biometricUnlocked by rememberSaveable(currentUser?.uid) { mutableStateOf(false) }
    val isDark = LocalFinanceColors.current.isDark

    val user = currentUser
    if (user == null) {
        AuthScreen(
            language = preferencesState.language,
            onToggleLanguage = {
                val next = if (preferencesState.language == AppLanguage.BN) AppLanguage.EN else AppLanguage.BN
                scope.launch { settingsPreferences.setLanguage(next) }
            },
            onToggleTheme = {
                val next = if (isDark) ThemeMode.LIGHT else ThemeMode.DARK
                scope.launch { settingsPreferences.setThemeMode(next) }
            },
            onAuthSuccess = { /* AuthStateListener updates currentUser reactively */ }
        )
    } else {
        androidx.compose.runtime.key(user.uid) {
            if (preferencesState.biometricEnabled && !biometricUnlocked) {
                BiometricLockScreen(
                    language = preferencesState.language,
                    onUnlocked = { biometricUnlocked = true },
                    onSignOutRequested = {
                        signOutUser(credentialManager, scope)
                    }
                )
            } else {
                val resolvedDisplayName = user.displayName?.takeIf { it.isNotBlank() }
                    ?: user.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
                    ?: "SpendWise User"
                val resolvedContact = user.email.orEmpty()

                val viewModel: SpendWiseViewModel = viewModel(
                    key = user.uid,
                    factory = viewModelFactory {
                        initializer {
                            val appContext = context.applicationContext
                            val databaseId = appContext.getString(R.string.firestore_database_id)
                            val db = FirebaseFirestore.getInstance(databaseId)
                            SpendWiseViewModel(
                                financeRepository = FinanceRepository(db, auth),
                                settingsPreferences = settingsPreferences,
                                currentUserId = user.uid,
                                userDisplayName = resolvedDisplayName,
                                userEmail = resolvedContact
                            )
                        }
                    }
                )
                SpendWiseMainScaffold(
                    viewModel = viewModel,
                    userDisplayName = resolvedDisplayName,
                    userEmail = resolvedContact,
                    onSignOut = {
                        biometricUnlocked = false
                        signOutUser(credentialManager, scope)
                    }
                )
            }
        }
    }
}

@Composable
private fun SpendWiseMainScaffold(
    viewModel: SpendWiseViewModel,
    userDisplayName: String,
    userEmail: String,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val summaryState by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val recurringState by viewModel.recurringState.collectAsStateWithLifecycle()
    val loansState by viewModel.loansState.collectAsStateWithLifecycle()
    val selectedMonthKey by viewModel.selectedMonthKey.collectAsStateWithLifecycle()
    val ledgerFilter by viewModel.ledgerFilter.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val prefs by viewModel.appPreferences.collectAsStateWithLifecycle()
    val activeCurrency by viewModel.activeCurrencyCode.collectAsStateWithLifecycle()
    val lowBalanceDialogBalance by viewModel.lowBalanceDialogBalance.collectAsStateWithLifecycle()

    val copy = remember(prefs.language) { AppStrings.forLanguage(prefs.language) }
    val isDark = LocalFinanceColors.current.isDark

    var showTransactionSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionItem?>(null) }
    var showBudgetSheet by remember { mutableStateOf(false) }
    var editingBudgetCategory by remember { mutableStateOf("OVERALL") }
    var editingBudgetLimit by remember { mutableStateOf<Double?>(null) }
    var showRecurringSheet by remember { mutableStateOf(false) }

    LaunchedEffect(prefs.notificationsEnabled) {
        if (prefs.notificationsEnabled) {
            RecurringReminderScheduler.scheduleDailyCheck(context)
        } else {
            RecurringReminderScheduler.cancelDailyCheck(context)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setNotificationsEnabled(true)
            RecurringReminderScheduler.scheduleDailyCheck(context)
            RecurringReminderScheduler.triggerImmediateCheck(context)
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (prefs.language == AppLanguage.BN) "নিয়মিত পেমেন্ট রিমাইন্ডার চালু হয়েছে"
                    else "Recurring payment reminders enabled"
                )
            }
        } else {
            viewModel.setNotificationsEnabled(false)
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (prefs.language == AppLanguage.BN) "নোটিফিকেশন অনুমতি দেওয়া হয়নি"
                    else "Notification permission denied"
                )
            }
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val payload = viewModel.buildExportPayload() ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            ReportExporter.writePdfToStream(payload, out)
                        }
                    }
                }.onSuccess {
                    snackbarHostState.showSnackbar(
                        if (prefs.language == AppLanguage.BN) "PDF রিপোর্ট ডিভাইসে সংরক্ষিত হয়েছে"
                        else "PDF report saved to device"
                    )
                }.onFailure {
                    snackbarHostState.showSnackbar(it.localizedMessage ?: "Failed to save PDF")
                }
            }
        }
    }

    val saveExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        val payload = viewModel.buildExportPayload() ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            ReportExporter.writeExcelXlsxToStream(payload, out)
                        }
                    }
                }.onSuccess {
                    snackbarHostState.showSnackbar(
                        if (prefs.language == AppLanguage.BN) "Excel (.xlsx) রিপোর্ট ডিভাইসে সংরক্ষিত হয়েছে"
                        else "Excel (.xlsx) report saved to device"
                    )
                }.onFailure {
                    snackbarHostState.showSnackbar(it.localizedMessage ?: "Failed to save Excel workbook")
                }
            }
        }
    }

    fun sharePdfReport() {
        val payload = viewModel.buildExportPayload() ?: run {
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (prefs.language == AppLanguage.BN) "তথ্য লোড হচ্ছে..." else "Report data still loading"
                )
            }
            return
        }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    ReportExporter.createShareablePdf(context, payload)
                }
            }.onSuccess { uri ->
                val intent = ReportExporter.buildShareIntent(
                    uri = uri,
                    mimeType = "application/pdf",
                    title = "${copy.appTitle} ${payload.monthKey} (PDF)"
                )
                context.startActivity(intent)
            }.onFailure {
                snackbarHostState.showSnackbar(it.localizedMessage ?: "PDF export failed")
            }
        }
    }

    fun shareExcelReport() {
        val payload = viewModel.buildExportPayload() ?: run {
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (prefs.language == AppLanguage.BN) "তথ্য লোড হচ্ছে..." else "Report data still loading"
                )
            }
            return
        }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    ReportExporter.createShareableExcel(context, payload)
                }
            }.onSuccess { uri ->
                val intent = ReportExporter.buildShareIntent(
                    uri = uri,
                    mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    title = "${copy.appTitle} ${payload.monthKey} (Excel)"
                )
                context.startActivity(intent)
            }.onFailure {
                snackbarHostState.showSnackbar(it.localizedMessage ?: "Excel export failed")
            }
        }
    }

    fun handleNotificationToggle(enable: Boolean) {
        if (!enable) {
            viewModel.setNotificationsEnabled(false)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationHelper.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setNotificationsEnabled(true)
            RecurringReminderScheduler.scheduleDailyCheck(context)
        }
    }

    fun handleBiometricToggle(enable: Boolean) {
        if (!enable) {
            viewModel.setBiometricEnabled(false)
            return
        }
        val availability = BiometricHelper.checkAvailability(context)
        if (availability != BiometricAvailability.AVAILABLE || activity == null) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (prefs.language == AppLanguage.BN) {
                        "প্রথমে ফোনের Settings থেকে ফিঙ্গারপ্রিন্ট, ফেস বা পিন সেট করুন।"
                    } else {
                        "Set up a fingerprint, face unlock, or PIN in Android Settings first."
                    }
                )
            }
            return
        }
        BiometricHelper.authenticate(
            activity = activity,
            title = copy.biometricLockTitle,
            subtitle = copy.lockScreenSubtitle,
            onSuccess = {
                viewModel.setBiometricEnabled(true)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (prefs.language == AppLanguage.BN) "বায়োমেট্রিক লক চালু হয়েছে"
                        else "Biometric lock enabled"
                    )
                }
            },
            onError = { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            }
        )
    }

    fun notifyShareCopied() {
        scope.launch {
            snackbarHostState.showSnackbar(
                if (prefs.language == AppLanguage.BN) "ক্লিপবোর্ডে কপি করা হয়েছে"
                else "Copied to clipboard"
            )
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: SpendWiseRoutes.HOME

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 840.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Destinations.forEach { dest ->
                        val selected = currentRoute == dest.route
                        val label = dest.labelProvider(copy)
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = label
                                )
                            },
                            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    if (currentRoute == SpendWiseRoutes.HOME) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                editingTransaction = null
                                showTransactionSheet = true
                            },
                            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                            text = { Text(copy.logDailyEntry) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("add_entry_fab")
                        )
                    }
                },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Destinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                val label = dest.labelProvider(copy)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(dest.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    alwaysShowLabel = true,
                                    modifier = Modifier.testTag("nav_${dest.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = SpendWiseRoutes.HOME,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(SpendWiseRoutes.HOME) {
                        LedgerScreen(
                            summaryState = summaryState,
                            selectedMonthKey = selectedMonthKey,
                            currencyCode = activeCurrency,
                            language = prefs.language,
                            balanceAlertLimit = prefs.balanceAlertLimit,
                            ledgerFilter = ledgerFilter,
                            selectedCategoryFilter = selectedCategoryFilter,
                            searchQuery = searchQuery,
                            onToggleLanguage = viewModel::toggleLanguage,
                            onToggleTheme = { viewModel.toggleQuickTheme(isDark) },
                            onShiftMonth = viewModel::shiftMonth,
                            onSelectFilter = viewModel::setLedgerFilter,
                            onToggleCategoryFilter = viewModel::setCategoryFilter,
                            onSearchQueryChange = viewModel::setSearchQuery,
                            onEditTransaction = { tx ->
                                editingTransaction = tx
                                showTransactionSheet = true
                            },
                            onDeleteTransaction = viewModel::deleteTransaction,
                            onAddTransactionClick = {
                                editingTransaction = null
                                showTransactionSheet = true
                            },
                            onExportPdfShare = ::sharePdfReport,
                            onExportExcelShare = ::shareExcelReport,
                            onSavePdfDevice = {
                                savePdfLauncher.launch("SpendWise_Report_$selectedMonthKey.pdf")
                            },
                            onSaveExcelDevice = {
                                saveExcelLauncher.launch("SpendWise_Report_$selectedMonthKey.xlsx")
                            },
                            onCopiedShareLink = ::notifyShareCopied,
                            onSignOut = onSignOut
                        )
                    }
                    composable(SpendWiseRoutes.EXPENSES) {
                        EntryTabScreen(
                            mode = TransactionType.EXPENSE,
                            summaryState = summaryState,
                            selectedMonthKey = selectedMonthKey,
                            currencyCode = activeCurrency,
                            language = prefs.language,
                            balanceAlertLimit = prefs.balanceAlertLimit,
                            onToggleLanguage = viewModel::toggleLanguage,
                            onToggleTheme = { viewModel.toggleQuickTheme(isDark) },
                            onShiftMonth = viewModel::shiftMonth,
                            onAddEntry = { title, amount, type, category, note, dateMillis ->
                                viewModel.saveTransaction(
                                    existingId = null,
                                    title = title,
                                    amount = amount,
                                    type = type,
                                    category = category,
                                    note = note,
                                    dateMillis = dateMillis
                                )
                            },
                            onEditTransaction = { tx ->
                                editingTransaction = tx
                                showTransactionSheet = true
                            },
                            onDeleteTransaction = viewModel::deleteTransaction
                        )
                    }
                    composable(SpendWiseRoutes.LOANS) {
                        LoansScreen(
                            loansState = loansState,
                            currencyCode = activeCurrency,
                            language = prefs.language,
                            onToggleLanguage = viewModel::toggleLanguage,
                            onToggleTheme = { viewModel.toggleQuickTheme(isDark) },
                            onAddLoan = viewModel::addLoan,
                            onRepayLoan = viewModel::repayLoan,
                            onReopenLoan = viewModel::reopenLoan,
                            onDeleteLoan = viewModel::deleteLoan
                        )
                    }
                    composable(SpendWiseRoutes.INCOME) {
                        EntryTabScreen(
                            mode = TransactionType.INCOME,
                            summaryState = summaryState,
                            selectedMonthKey = selectedMonthKey,
                            currencyCode = activeCurrency,
                            language = prefs.language,
                            balanceAlertLimit = prefs.balanceAlertLimit,
                            onToggleLanguage = viewModel::toggleLanguage,
                            onToggleTheme = { viewModel.toggleQuickTheme(isDark) },
                            onShiftMonth = viewModel::shiftMonth,
                            onAddEntry = { title, amount, type, category, note, dateMillis ->
                                viewModel.saveTransaction(
                                    existingId = null,
                                    title = title,
                                    amount = amount,
                                    type = type,
                                    category = category,
                                    note = note,
                                    dateMillis = dateMillis
                                )
                            },
                            onEditTransaction = { tx ->
                                editingTransaction = tx
                                showTransactionSheet = true
                            },
                            onDeleteTransaction = viewModel::deleteTransaction
                        )
                    }
                    composable(SpendWiseRoutes.INSIGHTS) {
                        InsightsHubScreen(
                            summaryState = summaryState,
                            recurringState = recurringState,
                            selectedMonthKey = selectedMonthKey,
                            activeCurrencyCode = activeCurrency,
                            preferences = prefs,
                            userDisplayName = userDisplayName,
                            userEmail = userEmail,
                            onToggleLanguage = viewModel::toggleLanguage,
                            onToggleTheme = { viewModel.toggleQuickTheme(isDark) },
                            onLanguageChange = viewModel::setLanguage,
                            onThemeModeChange = viewModel::setThemeMode,
                            onShiftMonth = viewModel::shiftMonth,
                            onExportPdfShare = ::sharePdfReport,
                            onExportExcelShare = ::shareExcelReport,
                            onSavePdfDevice = {
                                savePdfLauncher.launch("SpendWise_Report_$selectedMonthKey.pdf")
                            },
                            onSaveExcelDevice = {
                                saveExcelLauncher.launch("SpendWise_Report_$selectedMonthKey.xlsx")
                            },
                            onOpenBudgetEditor = { category, limit ->
                                editingBudgetCategory = category
                                editingBudgetLimit = limit
                                showBudgetSheet = true
                            },
                            onDeleteBudget = viewModel::deleteBudget,
                            onOpenRecurringEditor = { showRecurringSheet = true },
                            onToggleRecurringActive = viewModel::toggleRecurringActive,
                            onLogRecurringNow = viewModel::logRecurringNow,
                            onDeleteRecurring = viewModel::deleteRecurringPayment,
                            onTestRemindersNow = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !NotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    RecurringReminderScheduler.triggerImmediateCheck(context)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (prefs.language == AppLanguage.BN) "আসন্ন বিলগুলো পরীক্ষা করা হয়েছে"
                                            else "Checked upcoming recurring bills"
                                        )
                                    }
                                }
                            },
                            onToggleBiometric = ::handleBiometricToggle,
                            onToggleNotifications = ::handleNotificationToggle,
                            onBalanceAlertLimitChange = viewModel::setBalanceAlertLimit,
                            onCurrencyChange = viewModel::setCurrencyCode,
                            onCopiedShareLink = ::notifyShareCopied,
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }
    }

    if (showTransactionSheet) {
        TransactionEditorSheet(
            editingItem = editingTransaction,
            currencyCode = activeCurrency,
            language = prefs.language,
            onDismiss = { showTransactionSheet = false },
            onSave = viewModel::saveTransaction,
            onDelete = if (editingTransaction != null) {
                { id -> viewModel.deleteTransaction(id) }
            } else null
        )
    }

    if (showBudgetSheet) {
        BudgetEditorSheet(
            monthKey = selectedMonthKey,
            currencyCode = activeCurrency,
            language = prefs.language,
            initialCategory = editingBudgetCategory,
            initialLimit = editingBudgetLimit,
            onDismiss = { showBudgetSheet = false },
            onSave = viewModel::saveBudget
        )
    }

    if (showRecurringSheet) {
        RecurringEditorSheet(
            currencyCode = activeCurrency,
            language = prefs.language,
            onDismiss = { showRecurringSheet = false },
            onSave = viewModel::addRecurringPayment
        )
    }

    lowBalanceDialogBalance?.let { lowBal ->
        val balStr = FinanceFormatters.formatCurrency(lowBal, activeCurrency, lang = prefs.language)
        val limStr = FinanceFormatters.formatCurrency(prefs.balanceAlertLimit, activeCurrency, lang = prefs.language)
        AlertDialog(
            onDismissRequest = viewModel::dismissLowBalanceDialog,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(copy.lowBalancePopupTitle) },
            text = { Text(copy.lowBalancePopupBody(balStr, limStr)) },
            confirmButton = {
                TextButton(
                    onClick = viewModel::dismissLowBalanceDialog,
                    modifier = Modifier.testTag("low_balance_dialog_ok_button")
                ) {
                    Text(copy.ok)
                }
            }
        )
    }
}
