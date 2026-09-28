package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ForexPair
import com.example.ui.components.AdrMonitorDialog
import com.example.ui.components.ForexTopBar
import com.example.ui.components.HeadsUpAdrAlertBanner
import com.example.ui.components.HeadsUpSetupAlertBanner
import com.example.ui.components.SetupNotificationDialog
import com.example.ui.screens.AccuracyDashboardScreen
import com.example.ui.screens.AiAnalystScreen
import com.example.ui.screens.BrokerHubScreen
import com.example.ui.screens.IntelligentEaScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.MarketCorrelationScreen
import com.example.ui.screens.Mt5ScannerScreen
import com.example.ui.screens.RiskCalculatorScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.viewmodel.ForexViewModel

enum class ScreenTab(val title: String) {
    TERMINAL("Terminal"),
    INTELLIGENT_EA("EA Bot"),
    MT5_SCANNER("MT5"),
    AI_ANALYST("AI Analyst"),
    CORRELATION("Correlation"),
    ACCURACY("Accuracy"),
    BROKER("Broker"),
    RISK_CALC("Risk Calc"),
    JOURNAL("Journal")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: ForexViewModel = viewModel()
                val selectedPair by viewModel.selectedPair.collectAsState()
                val selectedTimeframe by viewModel.selectedTimeframe.collectAsState()
                val brokerAccountInfo by viewModel.brokerAccountInfo.collectAsState()
                val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
                val alertHistory by viewModel.setupAlertHistory.collectAsState()
                val headsUpAlert by viewModel.headsUpAlert.collectAsState()

                // ADR Volatility Monitoring States
                val currentAdrMetric by viewModel.currentPairAdrMetric.collectAsState()
                val adrMetrics by viewModel.adrMetrics.collectAsState()
                val adrAlertHistory by viewModel.adrAlertHistory.collectAsState()
                val headsUpAdrAlert by viewModel.headsUpAdrAlert.collectAsState()
                val adrMonitoringActive by viewModel.adrMonitoringActive.collectAsState()
                val adrThresholdPercentage by viewModel.adrThresholdPercentage.collectAsState()

                var currentTab by remember { mutableStateOf(ScreenTab.TERMINAL) }
                var showNotificationDialog by remember { mutableStateOf(false) }
                var showAdrDialog by remember { mutableStateOf(false) }

                // BackHandler returns to Terminal screen when on secondary tabs
                BackHandler(enabled = currentTab != ScreenTab.TERMINAL) {
                    currentTab = ScreenTab.TERMINAL
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        ForexTopBar(
                            selectedPair = selectedPair,
                            selectedTimeframe = selectedTimeframe,
                            onSelectPair = { viewModel.selectPair(it) },
                            onSelectTimeframe = { viewModel.selectTimeframe(it) },
                            brokerAccountInfo = brokerAccountInfo,
                            onOpenBrokerHub = { currentTab = ScreenTab.BROKER },
                            notificationsEnabled = notificationsEnabled,
                            alertCount = alertHistory.size,
                            onOpenNotifications = { showNotificationDialog = true },
                            currentAdrMetric = currentAdrMetric,
                            onOpenAdrDashboard = { showAdrDialog = true },
                            onOpenEa = { currentTab = ScreenTab.INTELLIGENT_EA }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = TerminalSurface,
                            tonalElevation = 0.dp,
                            modifier = Modifier.testTag("main_bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentTab == ScreenTab.TERMINAL,
                                onClick = { currentTab = ScreenTab.TERMINAL },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.CandlestickChart,
                                        contentDescription = "Terminal"
                                    )
                                },
                                label = { Text(ScreenTab.TERMINAL.title, fontSize = 9.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanAccent,
                                    selectedTextColor = CyanAccent,
                                    indicatorColor = CyanAccent.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_tab_terminal")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.INTELLIGENT_EA,
                                onClick = { currentTab = ScreenTab.INTELLIGENT_EA },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = "Intelligent EA"
                                    )
                                },
                                label = { Text(ScreenTab.INTELLIGENT_EA.title, fontSize = 9.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanAccent,
                                    selectedTextColor = CyanAccent,
                                    indicatorColor = CyanAccent.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_tab_ea_bot")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.MT5_SCANNER,
                                onClick = { currentTab = ScreenTab.MT5_SCANNER },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.DocumentScanner,
                                        contentDescription = "MT5 Scanner"
                                    )
                                },
                                label = { Text(ScreenTab.MT5_SCANNER.title, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanAccent,
                                    selectedTextColor = CyanAccent,
                                    indicatorColor = CyanAccent.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_tab_mt5_scanner")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.AI_ANALYST,
                                onClick = { currentTab = ScreenTab.AI_ANALYST },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Analyst"
                                    )
                                },
                                label = { Text(ScreenTab.AI_ANALYST.title, fontSize = 9.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanAccent,
                                    selectedTextColor = CyanAccent,
                                    indicatorColor = CyanAccent.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_tab_ai_analyst")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.CORRELATION,
                                onClick = { currentTab = ScreenTab.CORRELATION },
                                icon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                        contentDescription = "Correlation"
                                    )
                                },
                                label = { Text(ScreenTab.CORRELATION.title, fontSize = 9.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanAccent,
                                    selectedTextColor = CyanAccent,
                                    indicatorColor = CyanAccent.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_tab_correlation")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.ACCURACY,
                                onClick = { currentTab = ScreenTab.ACCURACY },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Analytics,
                                        contentDescription = "Accuracy"
                                    )
                                },
                                label = { Text(ScreenTab.ACCURACY.title, fontSize = 9.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanAccent,
                                    selectedTextColor = CyanAccent,
                                    indicatorColor = CyanAccent.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_tab_accuracy")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.BROKER,
                                onClick = { currentTab = ScreenTab.BROKER },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Hub,
                                        contentDescription = "Broker"
                                    )
                                },
                                label = { Text(ScreenTab.BROKER.title, fontSize = 9.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanAccent,
                                    selectedTextColor = CyanAccent,
                                    indicatorColor = CyanAccent.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_tab_broker")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.JOURNAL,
                                onClick = { currentTab = ScreenTab.JOURNAL },
                                icon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = "Journal"
                                    )
                                },
                                label = { Text(ScreenTab.JOURNAL.title, fontSize = 9.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanAccent,
                                    selectedTextColor = CyanAccent,
                                    indicatorColor = CyanAccent.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_tab_journal")
                            )
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // In-App Heads-Up Alert Banner
                        HeadsUpSetupAlertBanner(
                            alert = headsUpAlert,
                            onDismiss = { viewModel.dismissHeadsUpAlert() },
                            onNavigateToPair = { pairSymbol ->
                                ForexPair.ALL_PAIRS.find { it.symbol == pairSymbol }?.let {
                                    viewModel.selectPair(it)
                                    currentTab = ScreenTab.TERMINAL
                                }
                            }
                        )

                        // In-App Heads-Up ADR Volatility Alert Banner
                        HeadsUpAdrAlertBanner(
                            alert = headsUpAdrAlert,
                            onDismiss = { viewModel.dismissHeadsUpAdrAlert() },
                            onNavigateToPair = { pairSymbol ->
                                ForexPair.ALL_PAIRS.find { it.symbol == pairSymbol }?.let {
                                    viewModel.selectPair(it)
                                    currentTab = ScreenTab.TERMINAL
                                }
                            }
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            when (currentTab) {
                                ScreenTab.TERMINAL -> {
                                    TerminalScreen(
                                        viewModel = viewModel,
                                        onNavigateToAi = { currentTab = ScreenTab.AI_ANALYST },
                                        onNavigateToRiskCalc = { currentTab = ScreenTab.RISK_CALC },
                                        onNavigateToJournal = { currentTab = ScreenTab.JOURNAL },
                                        onNavigateToScanner = { currentTab = ScreenTab.MT5_SCANNER },
                                        onNavigateToCorrelation = { currentTab = ScreenTab.CORRELATION },
                                        onNavigateToEa = { currentTab = ScreenTab.INTELLIGENT_EA }
                                    )
                                }
                                ScreenTab.INTELLIGENT_EA -> {
                                    IntelligentEaScreen(
                                        viewModel = viewModel
                                    )
                                }
                                ScreenTab.MT5_SCANNER -> {
                                    Mt5ScannerScreen(
                                        viewModel = viewModel,
                                        onNavigateToRiskCalc = { currentTab = ScreenTab.RISK_CALC },
                                        onNavigateToJournal = { currentTab = ScreenTab.JOURNAL }
                                    )
                                }
                                ScreenTab.AI_ANALYST -> {
                                    AiAnalystScreen(
                                        viewModel = viewModel,
                                        onNavigateToAccuracy = { currentTab = ScreenTab.ACCURACY }
                                    )
                                }
                                ScreenTab.CORRELATION -> {
                                    MarketCorrelationScreen(
                                        viewModel = viewModel,
                                        onNavigateToPair = { pairSymbol ->
                                            ForexPair.ALL_PAIRS.find { it.symbol == pairSymbol }?.let {
                                                viewModel.selectPair(it)
                                                currentTab = ScreenTab.TERMINAL
                                            }
                                        }
                                    )
                                }
                                ScreenTab.ACCURACY -> {
                                    AccuracyDashboardScreen(
                                        viewModel = viewModel
                                    )
                                }
                                ScreenTab.BROKER -> {
                                    BrokerHubScreen(
                                        viewModel = viewModel
                                    )
                                }
                                ScreenTab.RISK_CALC -> {
                                    RiskCalculatorScreen(
                                        viewModel = viewModel
                                    )
                                }
                                ScreenTab.JOURNAL -> {
                                    JournalScreen(
                                        viewModel = viewModel,
                                        onNavigateToAccuracy = { currentTab = ScreenTab.ACCURACY }
                                    )
                                }
                            }
                        }
                    }
                }

                // AI Setup Notifications Dialog
                if (showNotificationDialog) {
                    SetupNotificationDialog(
                        notificationsEnabled = notificationsEnabled,
                        alertHistory = alertHistory,
                        onToggleNotifications = { viewModel.toggleNotificationsEnabled() },
                        onSendTestAlert = { viewModel.testSetupNotificationAlert() },
                        onDismiss = { showNotificationDialog = false },
                        onSelectAlertPair = { pairSymbol ->
                            ForexPair.ALL_PAIRS.find { it.symbol == pairSymbol }?.let {
                                viewModel.selectPair(it)
                                currentTab = ScreenTab.TERMINAL
                            }
                        }
                    )
                }

                // Automated ADR Volatility Monitoring Dialog
                if (showAdrDialog) {
                    AdrMonitorDialog(
                        metrics = adrMetrics,
                        alertHistory = adrAlertHistory,
                        monitoringActive = adrMonitoringActive,
                        thresholdPercentage = adrThresholdPercentage,
                        onToggleMonitoring = { viewModel.toggleAdrMonitoring() },
                        onSetThreshold = { viewModel.setAdrThreshold(it) },
                        onSendTestAlert = { viewModel.testAdrVolatilityAlert(it) },
                        onSimulateSpike = { viewModel.simulateVolatilitySpike(it) },
                        onSelectPair = { pairSymbol ->
                            ForexPair.ALL_PAIRS.find { it.symbol == pairSymbol }?.let {
                                viewModel.selectPair(it)
                                currentTab = ScreenTab.TERMINAL
                            }
                        },
                        onDismiss = { showAdrDialog = false }
                    )
                }
            }
        }
    }
}
