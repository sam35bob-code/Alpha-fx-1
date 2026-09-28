package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.analysis.MarketDataRepository
import com.example.analysis.TechnicalAnalysisEngine
import com.example.model.*
import com.example.ui.components.AdrMonitorDialog
import com.example.ui.components.AdrPairSummaryCard
import com.example.ui.components.AutoTraderBotComponent
import com.example.ui.components.DisciplineChecklistCard
import com.example.ui.components.ForexCandlestickChart
import com.example.ui.components.FundamentalIntelligenceDialog
import com.example.ui.components.RealtimeNewsTicker
import com.example.ui.components.RiskManagementComponent
import com.example.ui.theme.*
import com.example.ui.viewmodel.ForexViewModel

@Composable
fun TerminalScreen(
    viewModel: ForexViewModel,
    onNavigateToAi: () -> Unit,
    onNavigateToRiskCalc: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToCorrelation: () -> Unit = {},
    onNavigateToEa: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedPair by viewModel.selectedPair.collectAsState()
    val candles by viewModel.candles.collectAsState()
    val indicators by viewModel.indicators.collectAsState()
    val setup by viewModel.currentSetup.collectAsState()
    val currentScenario by viewModel.currentScenario.collectAsState()
    val calcState by viewModel.calculatorState.collectAsState()
    val botState by viewModel.autoTraderBotState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // News Ticker & Fundamental Context State
    val newsItems by viewModel.newsTickerItems.collectAsState()
    val currentNewsIndex by viewModel.currentNewsIndex.collectAsState()
    val isTickerPaused by viewModel.isTickerPaused.collectAsState()
    val isFilteredByPair by viewModel.filterNewsByPair.collectAsState()
    val selectedNewsDetail by viewModel.selectedNewsDetail.collectAsState()
    val aiBriefing by viewModel.aiBriefingResult.collectAsState()
    val isAiBriefingLoading by viewModel.isAiBriefingLoading.collectAsState()

    // ADR Volatility Monitor States
    val currentAdrMetric by viewModel.currentPairAdrMetric.collectAsState()
    val adrMetrics by viewModel.adrMetrics.collectAsState()
    val adrAlertHistory by viewModel.adrAlertHistory.collectAsState()
    val adrMonitoringActive by viewModel.adrMonitoringActive.collectAsState()
    val adrThresholdPercentage by viewModel.adrThresholdPercentage.collectAsState()
    var showAdrDialog by remember { mutableStateOf(false) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var notesInput by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    var showScenarioHelp by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = TerminalBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp)
                .testTag("terminal_screen_column"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Real-Time Macro News Ticker (Top of Dashboard)
            item {
                RealtimeNewsTicker(
                    newsItems = newsItems,
                    currentIndex = currentNewsIndex,
                    isPaused = isTickerPaused,
                    isFilteredByPair = isFilteredByPair,
                    selectedPair = selectedPair,
                    onNext = { viewModel.nextNewsHeadline() },
                    onPrev = { viewModel.prevNewsHeadline() },
                    onTogglePause = { viewModel.toggleTickerPause() },
                    onToggleFilter = { viewModel.setFilterNewsByPair(!isFilteredByPair) },
                    onRefreshNews = { viewModel.refreshMarketNews() },
                    onSelectNews = { viewModel.openNewsDetail(it) }
                )
            }

            // Pair Stats Ribbon
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TerminalSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                    modifier = Modifier.fillMaxWidth().testTag("pair_stats_ribbon")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedPair.name,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            indicators?.let { ind ->
                                Text(
                                    text = TechnicalAnalysisEngine.formatPrice(ind.currentPrice, selectedPair),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (candles.lastOrNull()?.isBullish == true) BullishGreen else BearishRed
                                )
                            }
                        }

                        // EMA Trend Status Tag
                        indicators?.let { ind ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (ind.ema50 > ind.ema200) BullishGreen.copy(alpha = 0.15f) else BearishRed.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (ind.ema50 > ind.ema200) BullishGreen else BearishRed
                                )
                            ) {
                                Text(
                                    text = if (ind.ema50 > ind.ema200) "EMA: BULLISH (50 > 200)" else "EMA: BEARISH (50 < 200)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ind.ema50 > ind.ema200) BullishGreen else BearishRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Candlestick & Indicator Chart
            item {
                ForexCandlestickChart(
                    candles = candles,
                    pair = selectedPair,
                    indicators = indicators,
                    setup = setup
                )
            }

            // Real-Time ADR Volatility Deviation Card
            currentAdrMetric?.let { adr ->
                item {
                    AdrPairSummaryCard(
                        metric = adr,
                        onOpenAdrDashboard = { showAdrDialog = true }
                    )
                }
            }

            // MT5 AI Chart Scanner Launch Banner
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TerminalSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToScanner() }
                        .testTag("launch_mt5_scanner_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(CyanAccent.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "SCAN MT5 CHART SCREENSHOT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    color = CyanAccent
                                )
                                Text(
                                    text = "Upload or paste your MT5 screenshot for instant trade audit",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open MT5 Scanner",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Market Correlation Recharts Visualizer Banner
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TerminalSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToCorrelation() }
                        .testTag("launch_correlation_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(GoldAccent.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                    contentDescription = "Market Correlation",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Market Correlation Visualizer",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = GoldAccent.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "Recharts",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldAccent,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "US30 • EUR/USD • GBP/USD co-movement & hedging",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Correlation Visualizer",
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Disciplined Checklist Card (The 4 Strict Criteria)
            item {
                DisciplineChecklistCard(
                    setup = setup,
                    onRunAiAnalysis = {
                        viewModel.requestAiAnalysis()
                        onNavigateToAi()
                    },
                    onCalculateRisk = onNavigateToRiskCalc,
                    onSaveSetup = { showSaveDialog = true }
                )
            }

            // 100% Risk-Free Auto-Trader Bot Component (Strict 4-Rule Virtual Automation)
            item {
                AutoTraderBotComponent(
                    botState = botState,
                    setup = setup,
                    pair = selectedPair,
                    onToggleBot = { viewModel.toggleAutoTrader(it) },
                    onTriggerManualExecute = {
                        val (success, message) = viewModel.manualTriggerAutoTrade()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = message,
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    onResetAccount = { viewModel.resetVirtualAccount() },
                    onSetRiskPercent = { viewModel.setAutoTradeRiskPercent(it) },
                    onToggleTrailingStop = { viewModel.toggleTrailingStop(it) }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = onNavigateToEa,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent.copy(alpha = 0.15f),
                        contentColor = CyanAccent
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("terminal_open_full_ea_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "OPEN INTELLIGENT EA (ALL MARKETS & TOP 4 STRATEGIES)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Interactive Risk Management & Automatic Position Sizing Component
            item {
                RiskManagementComponent(
                    selectedPair = selectedPair,
                    calcState = calcState,
                    setup = setup,
                    onUpdateCalculation = { balance, risk, sl ->
                        viewModel.updateCalculator(balance, risk, sl)
                    },
                    initialExpanded = false, // starts nicely summarized, can be expanded with 1 tap or by tapping 'Risk Calc'
                    showCardHeader = true,
                    onSaveToJournal = { showSaveDialog = true }
                )
            }

            // Scenario Tester Carousel
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DISCIPLINE SCENARIO TESTER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = TextSecondary
                        )
                        IconButton(
                            onClick = { showScenarioHelp = !showScenarioHelp },
                            modifier = Modifier.size(24.dp).testTag("scenario_help_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Scenario Info",
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (showScenarioHelp) {
                        Text(
                            text = "Test how strict rules respond under prime setups vs sideways markets where discipline forbids trading.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp).testTag("scenario_tester_row")
                    ) {
                        items(MarketDataRepository.MarketScenario.values()) { scenario ->
                            val isSelected = scenario == currentScenario
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CyanAccent.copy(alpha = 0.2f) else TerminalSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) CyanAccent else TerminalBorder
                                ),
                                modifier = Modifier
                                    .clickable { viewModel.selectScenario(scenario) }
                                    .testTag("scenario_chip_${scenario.name}")
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp).widthIn(max = 220.dp)) {
                                    Text(
                                        text = scenario.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) CyanAccent else TextPrimary
                                    )
                                    Text(
                                        text = scenario.description,
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        maxLines = 2,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }

        // Save Setup Dialog
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = {
                    Text(
                        text = "Journal Setup: ${selectedPair.symbol}",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Record this setup to local storage to track disciplined execution and adherence to the 4 strict rules.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text("Trade Notes / Thesis") },
                            placeholder = { Text("e.g., Pullback to 4H support after London open...") },
                            modifier = Modifier.fillMaxWidth().testTag("journal_notes_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveCurrentSetup(notesInput)
                            showSaveDialog = false
                            notesInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                        modifier = Modifier.testTag("confirm_save_setup_button")
                    ) {
                        Text("Save to Journal", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = TerminalSurface
            )
        }

        // Fundamental Intelligence & Technical Confluence Modal
        selectedNewsDetail?.let { detailItem ->
            FundamentalIntelligenceDialog(
                newsItem = detailItem,
                currentIndicators = indicators,
                currentPair = selectedPair,
                aiBriefing = aiBriefing,
                isAiLoading = isAiBriefingLoading,
                onDismiss = { viewModel.closeNewsDetail() },
                onSelectPair = { newPair ->
                    viewModel.selectPair(newPair)
                },
                onRequestAiBriefing = { item ->
                    viewModel.generateAiFundamentalBriefing(item)
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
                    }
                },
                onDismiss = { showAdrDialog = false }
            )
        }
    }
}
