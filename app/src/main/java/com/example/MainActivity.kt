package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.TransactionEntity
import com.example.ui.components.MaliBottomNavigationBar
import com.example.ui.components.MaliDesktopSidebar
import com.example.ui.components.NavRoutes
import com.example.ui.components.NotificationsSheet
import com.example.ui.components.PinLockScreen
import com.example.ui.components.TransactionEntrySheet
import com.example.ui.components.WalletFormDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MoreHubScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.screens.WalletDetailScreen
import com.example.ui.screens.WalletsScreen
import com.example.ui.theme.MaliAppTheme
import com.example.ui.viewmodel.MaliViewModel
import com.example.util.stringsFor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            val viewModel: MaliViewModel = viewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
            val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

            LaunchedEffect(state.prefs.darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (state.prefs.darkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (state.prefs.darkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                )
            }

            val layoutDir = if (state.prefs.languageCode == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
            val strings = remember(state.prefs.languageCode) { stringsFor(state.prefs.languageCode) }

            MaliAppTheme(darkTheme = state.prefs.darkTheme) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
                    if (state.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (state.prefs.pinLockEnabled && !isUnlocked) {
                        PinLockScreen(
                            biometricEnabled = state.prefs.biometricEnabled,
                            onVerifyPin = { viewModel.unlockWithPin(it) },
                            onBiometricSuccess = { viewModel.unlockBiometric() }
                        )
                    } else {
                        MaliRootShell(
                            viewModel = viewModel,
                            state = state,
                            strings = strings,
                            statusMessage = statusMessage
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MaliRootShell(
    viewModel: MaliViewModel,
    state: com.example.ui.viewmodel.MaliUiState,
    strings: com.example.util.AppStrings,
    statusMessage: String?
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: NavRoutes.HOME

    var showTxSheet by remember { mutableStateOf(false) }
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var showAddWalletDialog by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var moreInitialSection by remember { mutableStateOf("budgets") }

    fun navigateTop(route: String) {
        navController.navigate(route) {
            popUpTo(NavRoutes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isExpanded = maxWidth >= 840.dp

        val navHostContent: @Composable (Modifier) -> Unit = { mod ->
            NavHost(
                navController = navController,
                startDestination = NavRoutes.HOME,
                modifier = mod
            ) {
                composable(NavRoutes.HOME) {
                    HomeScreen(
                        state = state,
                        strings = strings,
                        computeChart = { viewModel.computeHomeChart(it) },
                        onToggleBalanceVisibility = { viewModel.toggleHideBalances() },
                        onOpenNotifications = { showNotificationsSheet = true },
                        onOpenSettings = {
                            moreInitialSection = "settings"
                            navigateTop(NavRoutes.MORE)
                        },
                        onClearDemoData = { viewModel.clearDemoData() },
                        onOpenAddWallet = { showAddWalletDialog = true },
                        onOpenWalletDetail = { id -> navController.navigate(NavRoutes.walletDetail(id)) },
                        onOpenAllTransactions = { navigateTop(NavRoutes.TRANSACTIONS) },
                        onOpenReports = { navigateTop(NavRoutes.REPORTS) },
                        onEditTransaction = { tx ->
                            editingTx = tx
                            showTxSheet = true
                        }
                    )
                }

                composable(NavRoutes.WALLETS) {
                    WalletsScreen(
                        state = state,
                        strings = strings,
                        onOpenWalletDetail = { id -> navController.navigate(NavRoutes.walletDetail(id)) },
                        onSaveWallet = { id, name, type, bal, cur, icon, color, desc ->
                            viewModel.saveWallet(id, name, type, bal, cur, icon, color, desc)
                        }
                    )
                }

                composable(
                    route = NavRoutes.WALLET_DETAIL,
                    arguments = listOf(navArgument("walletId") { type = NavType.StringType })
                ) { entry ->
                    val walletId = entry.arguments?.getString("walletId") ?: ""
                    WalletDetailScreen(
                        walletId = walletId,
                        state = state,
                        strings = strings,
                        onBack = { navController.popBackStack() },
                        onSaveWallet = { id, name, type, bal, cur, icon, color, desc ->
                            viewModel.saveWallet(id, name, type, bal, cur, icon, color, desc)
                        },
                        onToggleArchive = { viewModel.toggleArchiveWallet(it) },
                        onReconcile = { id, actual -> viewModel.reconcileWallet(id, actual) },
                        onEditTransaction = { tx ->
                            editingTx = tx
                            showTxSheet = true
                        }
                    )
                }

                composable(NavRoutes.TRANSACTIONS) {
                    TransactionsScreen(
                        state = state,
                        strings = strings,
                        convertToPrimary = { amt, cur, rate -> viewModel.convertToPrimary(amt, cur, rate) },
                        onAddTransaction = {
                            editingTx = null
                            showTxSheet = true
                        },
                        onEditTransaction = { tx ->
                            editingTx = tx
                            showTxSheet = true
                        },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) }
                    )
                }

                composable(NavRoutes.REPORTS) {
                    ReportsScreen(
                        state = state,
                        strings = strings,
                        convertToPrimary = { amt, cur, rate -> viewModel.convertToPrimary(amt, cur, rate) }
                    )
                }

                composable(NavRoutes.MORE) {
                    MoreHubScreen(
                        state = state,
                        strings = strings,
                        initialSection = moreInitialSection,
                        statusMessage = statusMessage,
                        convertToPrimary = { amt, cur, rate -> viewModel.convertToPrimary(amt, cur, rate) },
                        onOpenReports = { navigateTop(NavRoutes.REPORTS) },
                        onOpenWallets = { navigateTop(NavRoutes.WALLETS) },
                        onSaveBudget = { id, catId, limit -> viewModel.saveBudget(id, catId, limit) },
                        onDeleteBudget = { viewModel.deleteBudget(it) },
                        onSaveDebt = { dir, person, amt, due, wId, notes, cur ->
                            viewModel.saveDebt(null, dir, person, amt, due, wId, notes, cur)
                        },
                        onRecordDebtPayment = { id, amt, upd -> viewModel.recordDebtPayment(id, amt, upd) },
                        onDeleteDebt = { viewModel.deleteDebt(it) },
                        onSaveRecurring = { title, type, amt, wId, catId, freq, nextDate ->
                            viewModel.saveRecurring(null, title, type, amt, wId, catId, freq, nextDate)
                        },
                        onToggleRecurring = { viewModel.toggleRecurring(it) },
                        onExecuteRecurringNow = { viewModel.executeRecurringNow(it) },
                        onDeleteRecurring = { viewModel.deleteRecurring(it) },
                        onSaveCategory = { id, name, type, icon, color ->
                            viewModel.saveCategory(id, name, type, icon, color)
                        },
                        onDeleteCategory = { viewModel.deleteCategory(it) },
                        onSetLanguage = { viewModel.setLanguage(it) },
                        onSetPrimaryCurrency = { viewModel.setPrimaryCurrency(it) },
                        onSetCurrencyConversion = { viewModel.setCurrencyConversion(it) },
                        onUpdateCurrencyRate = { code, rate -> viewModel.updateCurrencyRate(code, rate) },
                        onSetDarkTheme = { viewModel.setDarkTheme(it) },
                        onSetNotifications = { viewModel.setNotificationsEnabled(it) },
                        onEnablePin = { viewModel.enablePin(it) },
                        onDisablePin = { viewModel.disablePin() },
                        onSetBiometric = { viewModel.setBiometric(it) },
                        onClearDemoData = { viewModel.clearDemoData() },
                        onRestoreDemoData = { viewModel.restoreDemoData() },
                        onExportJson = { viewModel.exportBackupJson(it) },
                        onExportCsv = { viewModel.exportTransactionsCsv(it) },
                        onImportJson = { json -> viewModel.importBackupJson(json) {} },
                        onClearStatusMessage = { viewModel.clearStatusMessage() }
                    )
                }
            }
        }

        if (isExpanded) {
            Row(modifier = Modifier.fillMaxSize()) {
                MaliDesktopSidebar(
                    currentRoute = currentRoute,
                    strings = strings,
                    onNavigate = { route ->
                        if (route == NavRoutes.MORE) moreInitialSection = "budgets"
                        navigateTop(route)
                    },
                    onAddTransaction = {
                        editingTx = null
                        showTxSheet = true
                    }
                )
                navHostContent(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .statusBarsPadding()
                )
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    MaliBottomNavigationBar(
                        currentRoute = currentRoute,
                        strings = strings,
                        onNavigate = { route ->
                            if (route == NavRoutes.MORE) moreInitialSection = "budgets"
                            navigateTop(route)
                        },
                        onAddTransaction = {
                            editingTx = null
                            showTxSheet = true
                        }
                    )
                }
            ) { innerPadding ->
                navHostContent(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }

    if (showTxSheet) {
        TransactionEntrySheet(
            existingTx = editingTx,
            initialType = editingTx?.type ?: "expense",
            wallets = state.wallets,
            categories = state.categories,
            strings = strings,
            langCode = state.prefs.languageCode,
            onDismiss = {
                showTxSheet = false
                editingTx = null
            },
            onSave = { existingId, type, amt, wId, toWId, catId, dateIso, note, attachUri ->
                viewModel.saveTransaction(
                    existingId, type, amt, wId, toWId, catId, dateIso, note, attachUri
                ) { ok ->
                    if (ok) {
                        showTxSheet = false
                        editingTx = null
                    }
                }
            }
        )
    }

    if (showAddWalletDialog) {
        WalletFormDialog(
            existingWallet = null,
            primaryCurrency = state.prefs.primaryCurrency,
            strings = strings,
            langCode = state.prefs.languageCode,
            onDismiss = { showAddWalletDialog = false },
            onSave = { id, name, type, bal, cur, icon, color, desc ->
                viewModel.saveWallet(id, name, type, bal, cur, icon, color, desc)
                showAddWalletDialog = false
            }
        )
    }

    if (showNotificationsSheet) {
        NotificationsSheet(
            alerts = state.alerts,
            onDismiss = { showNotificationsSheet = false }
        )
    }
}
