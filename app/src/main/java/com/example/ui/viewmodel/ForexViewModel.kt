package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.analysis.AdrAnalysisEngine
import com.example.analysis.ForexNewsRepository
import com.example.analysis.ForexNotificationManager
import com.example.analysis.MarketCorrelationRepository
import com.example.analysis.MarketDataRepository
import com.example.analysis.TechnicalAnalysisEngine
import com.example.data.AlphaFxDatabase
import com.example.data.HistoricalPriceEntity
import com.example.data.OfflineMarketRepository
import com.example.data.SavedSetupEntity
import com.example.data.TradeSignalEntity
import com.example.gemini.GeminiForexAnalyst
import com.example.model.*
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class CalculatorState(
    val accountBalance: Double = 10000.0,
    val riskPercentage: Double = 1.0, // 1%
    val stopLossPipsOrPoints: Double = 25.0,
    val calculatedLots: Double = 0.40,
    val totalDollarRisk: Double = 100.0,
    val potentialProfitTp1: Double = 200.0, // 1:2 R:R
    val potentialProfitTp2: Double = 320.0, // 1:3.2 R:R
    val riskWarning: String? = null
)

class ForexViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AlphaFxDatabase.getDatabase(application)
    private val tradeDao = database.tradeDao()
    private val historicalPriceDao = database.historicalPriceDao()
    private val tradeSignalDao = database.tradeSignalDao()
    val offlineRepository = OfflineMarketRepository(historicalPriceDao, tradeSignalDao)
    private val marketRepository = MarketDataRepository()

    private val _selectedPair = MutableStateFlow(ForexPair.defaultPair()) // US30
    val selectedPair: StateFlow<ForexPair> = _selectedPair.asStateFlow()

    private val _selectedTimeframe = MutableStateFlow(Timeframe.H1)
    val selectedTimeframe: StateFlow<Timeframe> = _selectedTimeframe.asStateFlow()

    val currentScenario = marketRepository.currentScenario

    private val _candles = MutableStateFlow<List<Candle>>(emptyList())
    val candles: StateFlow<List<Candle>> = _candles.asStateFlow()

    private val _indicators = MutableStateFlow<TechnicalIndicators?>(null)
    val indicators: StateFlow<TechnicalIndicators?> = _indicators.asStateFlow()

    private val _currentSetup = MutableStateFlow<TradeSetup?>(null)
    val currentSetup: StateFlow<TradeSetup?> = _currentSetup.asStateFlow()

    private val _aiAnalysisText = MutableStateFlow<String?>(null)
    val aiAnalysisText: StateFlow<String?> = _aiAnalysisText.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _calculatorState = MutableStateFlow(CalculatorState())
    val calculatorState: StateFlow<CalculatorState> = _calculatorState.asStateFlow()

    // MT5 Screenshot Scanner State
    private val _activeMt5Bitmap = MutableStateFlow<android.graphics.Bitmap?>(null)
    val activeMt5Bitmap: StateFlow<android.graphics.Bitmap?> = _activeMt5Bitmap.asStateFlow()

    private val _activeMt5ScanResult = MutableStateFlow<Mt5ScanResult?>(null)
    val activeMt5ScanResult: StateFlow<Mt5ScanResult?> = _activeMt5ScanResult.asStateFlow()

    private val _isScanningMt5 = MutableStateFlow(false)
    val isScanningMt5: StateFlow<Boolean> = _isScanningMt5.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    // 100% Risk-Free Auto-Trader Bot (Virtual Demo Account) State
    private val _autoTraderBotState = MutableStateFlow(AutoTraderBotState())
    val autoTraderBotState: StateFlow<AutoTraderBotState> = _autoTraderBotState.asStateFlow()

    val savedSetups: StateFlow<List<SavedSetupEntity>> = tradeDao.getAllSetups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offlineTradeSignals: StateFlow<List<TradeSignalEntity>> = tradeSignalDao.getAllSignals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTradeSignals: StateFlow<List<TradeSignalEntity>> = tradeSignalDao.getActiveSignals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePaperTrades: StateFlow<List<SavedSetupEntity>> = savedSetups.map { list ->
        list.filter { it.status == "ACTIVE" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accuracyDashboardData: StateFlow<AccuracyDashboardData> = savedSetups.map { setups ->
        buildAccuracyDashboardData(setups)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccuracyDashboardData())

    val weeklyHeatmapData: StateFlow<WeeklyHeatmapData> = savedSetups.map { setups ->
        com.example.analysis.WeeklyHeatmapEngine.computeHeatmap(setups)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeeklyHeatmapData())

    // Intelligent EA State
    private val _eaAutoPilotActive = MutableStateFlow(true)
    val eaAutoPilotActive: StateFlow<Boolean> = _eaAutoPilotActive.asStateFlow()

    private val _eaActivePositions = MutableStateFlow<List<EaActivePosition>>(emptyList())
    val eaActivePositions: StateFlow<List<EaActivePosition>> = _eaActivePositions.asStateFlow()

    // Live Broker Gateway & Multi-Broker Account Manager
    val brokerManager = com.example.analysis.LiveBrokerManager()
    val brokerCredentials: StateFlow<BrokerCredentials> = brokerManager.credentials
    val brokerAccountInfo: StateFlow<BrokerAccountInfo> = brokerManager.accountInfo
    val brokerOpenPositions: StateFlow<List<BrokerPosition>> = brokerManager.openPositions
    val brokerLastMessage: StateFlow<String?> = brokerManager.lastExecutionMessage

    // Real-Time Macro News Ticker State
    private val _allNewsHeadlines = MutableStateFlow<List<ForexNewsItem>>(ForexNewsRepository.getInitialHeadlines())
    private val _filterNewsByPair = MutableStateFlow(false)
    val filterNewsByPair: StateFlow<Boolean> = _filterNewsByPair.asStateFlow()

    val newsTickerItems: StateFlow<List<ForexNewsItem>> = combine(
        _allNewsHeadlines,
        _filterNewsByPair,
        _selectedPair
    ) { allNews, filterByPair, pair ->
        if (filterByPair) {
            val filtered = allNews.filter {
                it.pairSymbol.equals(pair.symbol, ignoreCase = true) ||
                it.pairSymbol.equals("GLOBAL", ignoreCase = true)
            }
            if (filtered.isNotEmpty()) filtered else allNews
        } else {
            allNews
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ForexNewsRepository.getInitialHeadlines())

    private val _currentNewsIndex = MutableStateFlow(0)
    val currentNewsIndex: StateFlow<Int> = _currentNewsIndex.asStateFlow()

    private val _isTickerPaused = MutableStateFlow(false)
    val isTickerPaused: StateFlow<Boolean> = _isTickerPaused.asStateFlow()

    private val _selectedNewsDetail = MutableStateFlow<ForexNewsItem?>(null)
    val selectedNewsDetail: StateFlow<ForexNewsItem?> = _selectedNewsDetail.asStateFlow()

    private val _isAiBriefingLoading = MutableStateFlow(false)
    val isAiBriefingLoading: StateFlow<Boolean> = _isAiBriefingLoading.asStateFlow()

    private val _aiBriefingResult = MutableStateFlow<String?>(null)
    val aiBriefingResult: StateFlow<String?> = _aiBriefingResult.asStateFlow()

    // AI Setup Push Alert System (50/200 EMA & RSI Reversal criteria)
    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _setupAlertHistory = MutableStateFlow<List<SetupAlertNotification>>(emptyList())
    val setupAlertHistory: StateFlow<List<SetupAlertNotification>> = _setupAlertHistory.asStateFlow()

    private val _headsUpAlert = MutableStateFlow<SetupAlertNotification?>(null)
    val headsUpAlert: StateFlow<SetupAlertNotification?> = _headsUpAlert.asStateFlow()

    // Automated ADR (Average Daily Range) Volatility Monitoring System
    private val _adrMonitoringActive = MutableStateFlow(true)
    val adrMonitoringActive: StateFlow<Boolean> = _adrMonitoringActive.asStateFlow()

    private val _adrThresholdPercentage = MutableStateFlow(125.0) // 125% ADR default abnormal threshold
    val adrThresholdPercentage: StateFlow<Double> = _adrThresholdPercentage.asStateFlow()

    private val _adrMetrics = MutableStateFlow<List<PairAdrMetric>>(initAllPairsAdrMetrics())
    val adrMetrics: StateFlow<List<PairAdrMetric>> = _adrMetrics.asStateFlow()

    val currentPairAdrMetric: StateFlow<PairAdrMetric?> = combine(_adrMetrics, _selectedPair) { metrics, pair ->
        metrics.find { it.pair.symbol == pair.symbol }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _adrAlertHistory = MutableStateFlow<List<AdrVolatilityAlert>>(emptyList())
    val adrAlertHistory: StateFlow<List<AdrVolatilityAlert>> = _adrAlertHistory.asStateFlow()

    private val _headsUpAdrAlert = MutableStateFlow<AdrVolatilityAlert?>(null)
    val headsUpAdrAlert: StateFlow<AdrVolatilityAlert?> = _headsUpAdrAlert.asStateFlow()

    // Market Correlation Engine (US30, EUR/USD, GBP/USD)
    private val _correlationTimeHorizon = MutableStateFlow("24H")
    val correlationTimeHorizon: StateFlow<String> = _correlationTimeHorizon.asStateFlow()

    private val _marketCorrelationData = MutableStateFlow(
        MarketCorrelationRepository.getCorrelationData("24H")
    )
    val marketCorrelationData: StateFlow<MarketCorrelationData> = _marketCorrelationData.asStateFlow()

    private var tickJob: Job? = null
    private var newsTickerJob: Job? = null

    init {
        com.example.analysis.ForexNotificationManager.createNotificationChannel(application)
        loadMarketData()
        startLiveTickSimulation()
        initHistoricalDataIfEmpty()
        startNewsTickerCycle()
    }

    private fun initHistoricalDataIfEmpty() {
        viewModelScope.launch {
            val list = tradeDao.getAllSetups().first()
            if (list.size < 5) {
                val samples = com.example.analysis.HistoricalAiDataSeeder.generateHistoricalRecommendations()
                tradeDao.insertAllSetups(samples)
            }
            if (tradeSignalDao.getSignalCount() == 0) {
                val samples = com.example.analysis.HistoricalAiDataSeeder.generateHistoricalRecommendations()
                val signals = samples.map { s ->
                    TradeSignalEntity(
                        signalId = java.util.UUID.randomUUID().toString(),
                        pairSymbol = s.pairSymbol,
                        timeframe = s.timeframe,
                        action = s.action,
                        isValid = true,
                        entryPrice = s.entryPrice,
                        stopLoss = s.stopLoss,
                        takeProfit1 = s.takeProfit1,
                        takeProfit2 = s.takeProfit2,
                        riskRewardRatio = s.riskRewardRatio,
                        confidenceScore = 0.88,
                        signalType = "AI_CONFLUENCE",
                        status = if (s.status == "ACTIVE" || s.status == "PENDING") "ACTIVE" else s.status,
                        statusSummary = "50/200 EMA & RSI Confluence signal",
                        detailedReasoning = s.notes,
                        ema50 = s.ema50,
                        ema200 = s.ema200,
                        rsi14 = s.rsi14,
                        keyZoneLabel = s.keyZoneLabel,
                        timestamp = s.timestamp,
                        expiryTimestamp = s.timestamp + (24 * 3600 * 1000L),
                        isOfflineAvailable = true
                    )
                }
                tradeSignalDao.insertSignals(signals)
            }
        }
    }

    fun seedHistoricalAiRecommendations(force: Boolean = false) {
        viewModelScope.launch {
            if (force) {
                tradeDao.deleteAllSetups()
            }
            val samples = com.example.analysis.HistoricalAiDataSeeder.generateHistoricalRecommendations()
            tradeDao.insertAllSetups(samples)
        }
    }

    fun clearAllRecommendations() {
        viewModelScope.launch {
            tradeDao.deleteAllSetups()
        }
    }

    fun simulateAiTradeOutcome(pairSymbol: String = "US30", action: String = "BUY", isWin: Boolean = true) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val pnl = if (isWin) 220.0 else -100.0
            val status = if (isWin) "WON_TP1" else "STOPPED_OUT"
            val setup = SavedSetupEntity(
                pairSymbol = pairSymbol,
                timeframe = "H1",
                action = action,
                entryPrice = if (pairSymbol == "US30") 41200.0 else 1.0880,
                stopLoss = if (pairSymbol == "US30") 41100.0 else 1.0855,
                takeProfit1 = if (pairSymbol == "US30") 41400.0 else 1.0930,
                takeProfit2 = if (pairSymbol == "US30") 41600.0 else 1.0965,
                riskRewardRatio = 2.0,
                ema50 = if (pairSymbol == "US30") 41150.0 else 1.0875,
                ema200 = if (pairSymbol == "US30") 40700.0 else 1.0830,
                rsi14 = if (isWin) 38.5 else 52.0,
                keyZoneLabel = if (isWin) "Key S/R Demand Zone Confluence" else "False Breakout Liquidity Grab",
                status = status,
                notes = if (isWin) "AI Live Recommendation: Hit 1:2 R:R Target. RSI-14 oversold bounce."
                        else "AI Live Recommendation: Stopped out at -1% max risk. Strict discipline preserved.",
                timestamp = now,
                isAutoExecuted = true,
                lots = 0.50,
                currentPnl = pnl,
                exitPrice = if (isWin) (if (pairSymbol == "US30") 41400.0 else 1.0930) else (if (pairSymbol == "US30") 41100.0 else 1.0855),
                closedTimestamp = now
            )
            tradeDao.insertSetup(setup)
        }
    }

    private fun buildAccuracyDashboardData(setups: List<SavedSetupEntity>): AccuracyDashboardData {
        val dateFormat = java.text.SimpleDateFormat("MMM dd", java.util.Locale.US)
        val sorted = setups.sortedBy { it.timestamp }
        val closedSetups = sorted.filter { 
            it.status == "WON_TP1" || it.status == "WON_TP2" || it.status == "STOPPED_OUT" || it.status == "CLOSED"
        }

        var runningWins = 0
        var runningPnl = 0.0
        val points = mutableListOf<TradeRecommendationPoint>()
        val recentOutcomes = java.util.ArrayDeque<Boolean>()

        closedSetups.forEachIndexed { index, s ->
            val isWin = s.status == "WON_TP1" || s.status == "WON_TP2" || s.currentPnl > 0
            if (isWin) runningWins++
            runningPnl += s.currentPnl

            recentOutcomes.addLast(isWin)
            if (recentOutcomes.size > 5) recentOutcomes.removeFirst()
            val rollingRate = if (recentOutcomes.isNotEmpty()) {
                (recentOutcomes.count { it }.toDouble() / recentOutcomes.size) * 100.0
            } else 0.0

            val cumulativeTotal = index + 1
            val cumulativeAcc = (runningWins.toDouble() / cumulativeTotal) * 100.0

            points.add(
                TradeRecommendationPoint(
                    id = s.id,
                    timestamp = s.timestamp,
                    dateLabel = dateFormat.format(java.util.Date(s.timestamp)),
                    pairSymbol = s.pairSymbol,
                    timeframe = s.timeframe,
                    action = s.action,
                    entryPrice = s.entryPrice,
                    exitPrice = s.exitPrice,
                    stopLoss = s.stopLoss,
                    takeProfit1 = s.takeProfit1,
                    status = s.status,
                    isWin = isWin,
                    pnl = s.currentPnl,
                    cumulativeWins = runningWins,
                    cumulativeTotal = cumulativeTotal,
                    cumulativeAccuracyPercent = Math.round(cumulativeAcc * 10.0) / 10.0,
                    rollingAccuracyPercent = Math.round(rollingRate * 10.0) / 10.0,
                    cumulativePnl = Math.round(runningPnl * 100.0) / 100.0,
                    notes = s.notes
                )
            )
        }

        val totalRecs = setups.size
        val closedCount = closedSetups.size
        val wonCount = closedSetups.count { it.status == "WON_TP1" || it.status == "WON_TP2" || it.currentPnl > 0 }
        val lostCount = closedSetups.count { it.status == "STOPPED_OUT" || (it.status == "CLOSED" && it.currentPnl < 0) }
        val activeCount = setups.count { it.status == "ACTIVE" }
        val overallAcc = if (closedCount > 0) (wonCount.toDouble() / closedCount) * 100.0 else 0.0

        val totalGains = closedSetups.filter { it.currentPnl > 0 }.sumOf { it.currentPnl }
        val totalLosses = Math.abs(closedSetups.filter { it.currentPnl < 0 }.sumOf { it.currentPnl })
        val profitFactor = if (totalLosses > 0) totalGains / totalLosses else if (totalGains > 0) 9.99 else 0.0

        val assetGroups = closedSetups.groupBy { it.pairSymbol }
        val assetBreakdown = assetGroups.map { (symbol, list) ->
            val w = list.count { it.status == "WON_TP1" || it.status == "WON_TP2" || it.currentPnl > 0 }
            val l = list.count { it.status == "STOPPED_OUT" || (it.status == "CLOSED" && it.currentPnl < 0) }
            val rate = if (list.isNotEmpty()) (w.toDouble() / list.size) * 100.0 else 0.0
            AssetAccuracyStat(
                symbol = symbol,
                totalTrades = list.size,
                wonTrades = w,
                lostTrades = l,
                winRatePercent = Math.round(rate * 10.0) / 10.0,
                netPnl = Math.round(list.sumOf { it.currentPnl } * 100.0) / 100.0
            )
        }.sortedByDescending { it.winRatePercent }

        val tfGroups = closedSetups.groupBy { it.timeframe }
        val tfBreakdown = tfGroups.map { (tf, list) ->
            val w = list.count { it.status == "WON_TP1" || it.status == "WON_TP2" || it.currentPnl > 0 }
            val rate = if (list.isNotEmpty()) (w.toDouble() / list.size) * 100.0 else 0.0
            TimeframeAccuracyStat(
                timeframe = tf,
                totalTrades = list.size,
                wonTrades = w,
                winRatePercent = Math.round(rate * 10.0) / 10.0
            )
        }

        val buys = closedSetups.filter { it.action == "BUY" }
        val sells = closedSetups.filter { it.action == "SELL" }
        val buyAcc = if (buys.isNotEmpty()) (buys.count { it.status == "WON_TP1" || it.status == "WON_TP2" || it.currentPnl > 0 }.toDouble() / buys.size) * 100.0 else 0.0
        val sellAcc = if (sells.isNotEmpty()) (sells.count { it.status == "WON_TP1" || it.status == "WON_TP2" || it.currentPnl > 0 }.toDouble() / sells.size) * 100.0 else 0.0

        return AccuracyDashboardData(
            totalRecommendations = totalRecs,
            closedRecommendations = closedCount,
            wonCount = wonCount,
            lostCount = lostCount,
            activeCount = activeCount,
            overallAccuracyPercent = Math.round(overallAcc * 10.0) / 10.0,
            benchmarkTargetPercent = 70.0,
            profitFactor = Math.round(profitFactor * 100.0) / 100.0,
            netPnl = Math.round(closedSetups.sumOf { it.currentPnl } * 100.0) / 100.0,
            buyAccuracyPercent = Math.round(buyAcc * 10.0) / 10.0,
            sellAccuracyPercent = Math.round(sellAcc * 10.0) / 10.0,
            accuracyOverTime = points,
            assetBreakdown = assetBreakdown,
            timeframeBreakdown = tfBreakdown
        )
    }

    fun selectPair(pair: ForexPair) {
        _selectedPair.value = pair
        loadMarketData()
        updateCalculatorFromCurrentSetup()
    }

    fun selectTimeframe(timeframe: Timeframe) {
        _selectedTimeframe.value = timeframe
        loadMarketData()
        updateCalculatorFromCurrentSetup()
    }

    fun selectScenario(scenario: MarketDataRepository.MarketScenario) {
        marketRepository.setScenario(scenario)
        // Automatically switch pair if scenario matches specific asset
        when (scenario) {
            MarketDataRepository.MarketScenario.PRIME_EURUSD_BUY -> {
                ForexPair.ALL_PAIRS.find { it.symbol == "EUR/USD" }?.let { _selectedPair.value = it }
            }
            MarketDataRepository.MarketScenario.PRIME_US30_SELL -> {
                ForexPair.ALL_PAIRS.find { it.symbol == "US30" }?.let { _selectedPair.value = it }
            }
            MarketDataRepository.MarketScenario.GBPUSD_NO_TRADE -> {
                ForexPair.ALL_PAIRS.find { it.symbol == "GBP/USD" }?.let { _selectedPair.value = it }
            }
            else -> {}
        }
        loadMarketData()
        updateCalculatorFromCurrentSetup()
    }

    fun refreshData() {
        loadMarketData()
        updateCalculatorFromCurrentSetup()
    }

    private fun loadMarketData() {
        val pair = _selectedPair.value
        val timeframe = _selectedTimeframe.value
        val scenario = currentScenario.value

        val generatedCandles = marketRepository.generateCandles(pair, timeframe, scenario)
        _candles.value = generatedCandles

        val (newIndicators, newSetup) = TechnicalAnalysisEngine.analyzeStructuralPriceAction(
            generatedCandles, pair, timeframe
        )
        _indicators.value = newIndicators
        _currentSetup.value = newSetup
        checkAndTriggerSetupNotification(newIndicators, newSetup, pair)

        // Cache historical prices and valid signals locally for offline access
        viewModelScope.launch {
            try {
                offlineRepository.saveHistoricalPrices(pair.symbol, timeframe.label, generatedCandles)
                if (newSetup.isValid && newSetup.action != TradeAction.WAIT) {
                    offlineRepository.saveTradeSignal(newSetup)
                }
            } catch (e: Exception) {
                // Background caching failure should not interrupt market feed
            }
        }
    }

    /**
     * Loads historical candles from local Room database when offline.
     */
    fun loadOfflineHistoricalPrices(pairSymbol: String = _selectedPair.value.symbol, timeframe: String = _selectedTimeframe.value.label) {
        viewModelScope.launch {
            val cached = offlineRepository.getHistoricalPrices(pairSymbol, timeframe)
            if (cached.isNotEmpty()) {
                _candles.value = cached
                val (newIndicators, newSetup) = TechnicalAnalysisEngine.analyzeStructuralPriceAction(
                    cached, _selectedPair.value, _selectedTimeframe.value
                )
                _indicators.value = newIndicators
                _currentSetup.value = newSetup
            }
        }
    }

    private fun startLiveTickSimulation() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                delay(2500)
                val currentList = _candles.value
                val pair = _selectedPair.value
                val timeframe = _selectedTimeframe.value
                if (currentList.isNotEmpty()) {
                    val last = currentList.last()
                    val updatedLast = marketRepository.tickCandle(last, pair)
                    val updatedList = currentList.toMutableList()
                    updatedList[updatedList.lastIndex] = updatedLast
                    _candles.value = updatedList

                    val (newIndicators, newSetup) = TechnicalAnalysisEngine.analyzeStructuralPriceAction(
                        updatedList, pair, timeframe
                    )
                    _indicators.value = newIndicators
                    _currentSetup.value = newSetup

                    processAutoTraderBot(newIndicators, newSetup, pair)
                    checkAndTriggerSetupNotification(newIndicators, newSetup, pair)
                    brokerManager.updateMarketPrices(pair.symbol, updatedLast.close, pair)
                    updateAdrMetricsForCurrentTick(pair, updatedLast)
                    evaluateAdrVolatilityMonitoring()
                    processIntelligentEaTicks(pair, updatedLast)
                }
            }
        }
    }

    // Broker Operations API
    fun updateBrokerCredentials(
        platform: BrokerPlatform,
        brokerName: String,
        serverName: String,
        accountNumber: String,
        passwordOrToken: String,
        bridgeUrl: String,
        isLiveReal: Boolean
    ) {
        brokerManager.updateBrokerCredentials(
            platform = platform,
            brokerName = brokerName,
            serverName = serverName,
            accountNumber = accountNumber,
            passwordOrToken = passwordOrToken,
            bridgeUrl = bridgeUrl,
            isLiveReal = isLiveReal
        )
    }

    fun setLiveRealTradingMode(enabled: Boolean) {
        brokerManager.setLiveRealTradingMode(enabled)
    }

    fun executeLiveBrokerOrder(
        symbol: String,
        action: String,
        lots: Double,
        entryPrice: Double,
        stopLoss: Double,
        takeProfit: Double
    ): BrokerExecutionResult {
        return brokerManager.executeLiveOrder(symbol, action, lots, entryPrice, stopLoss, takeProfit)
    }

    fun closeBrokerPosition(ticketId: Long): Boolean {
        return brokerManager.closePosition(ticketId)
    }

    fun emergencyLiquidateBrokerPositions(): Int {
        return brokerManager.emergencyLiquidateAllPositions()
    }

    fun requestAiAnalysis(userQuery: String? = null) {
        val pair = _selectedPair.value
        val timeframe = _selectedTimeframe.value
        val ind = _indicators.value ?: return
        val setup = _currentSetup.value ?: return

        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val report = GeminiForexAnalyst.analyzeSetup(pair, timeframe, ind, setup, userQuery)
                _aiAnalysisText.value = report
            } catch (e: Exception) {
                _aiAnalysisText.value = "Analysis request error: ${e.message}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun updateCalculator(balance: Double, riskPercent: Double, stopLossPips: Double) {
        val pair = _selectedPair.value
        val warning = if (riskPercent > 2.0) {
            "⚠️ STRICT RISK WARNING: Risking >2% breaches strict Forex discipline. Cap risk at 1-2%."
        } else null

        val dollarRisk = balance * (riskPercent / 100.0)
        // Lots = Dollar Risk / (StopLossPips * PipValuePerStandardLot)
        val pipValue = pair.pipValuePerStandardLot
        val lots = if (stopLossPips > 0 && pipValue > 0) {
            (dollarRisk / (stopLossPips * pipValue)).coerceIn(0.01, 100.0)
        } else 0.1

        val setup = _currentSetup.value
        val rr = setup?.riskRewardRatio ?: 2.0
        val profit1 = dollarRisk * 2.0
        val profit2 = dollarRisk * rr

        _calculatorState.value = CalculatorState(
            accountBalance = balance,
            riskPercentage = riskPercent,
            stopLossPipsOrPoints = stopLossPips,
            calculatedLots = Math.round(lots * 100.0) / 100.0,
            totalDollarRisk = dollarRisk,
            potentialProfitTp1 = profit1,
            potentialProfitTp2 = profit2,
            riskWarning = warning
        )
    }

    private fun updateCalculatorFromCurrentSetup() {
        val setup = _currentSetup.value ?: return
        val currentCalc = _calculatorState.value
        val pips = if (setup.riskPipsOrPoints > 0) setup.riskPipsOrPoints else 25.0
        updateCalculator(currentCalc.accountBalance, currentCalc.riskPercentage, pips)
    }

    fun saveCurrentSetup(notes: String = "") {
        val setup = _currentSetup.value ?: return
        viewModelScope.launch {
            val entity = SavedSetupEntity(
                pairSymbol = setup.pairSymbol,
                timeframe = setup.timeframe.label,
                action = setup.action.name,
                entryPrice = setup.entryPrice,
                stopLoss = setup.stopLoss,
                takeProfit1 = setup.takeProfit1,
                takeProfit2 = setup.takeProfit2,
                riskRewardRatio = setup.riskRewardRatio,
                ema50 = setup.ema50,
                ema200 = setup.ema200,
                rsi14 = setup.rsi14,
                keyZoneLabel = setup.keyZoneLabel,
                status = if (setup.isValid) "ACTIVE" else "PENDING",
                notes = notes
            )
            tradeDao.insertSetup(entity)
        }
    }

    fun updateSetupStatus(setup: SavedSetupEntity, newStatus: String) {
        viewModelScope.launch {
            tradeDao.updateSetup(setup.copy(status = newStatus))
        }
    }

    fun deleteSetup(setup: SavedSetupEntity) {
        viewModelScope.launch {
            tradeDao.deleteSetup(setup)
        }
    }

    // MT5 Screenshot Scanner Operations
    fun setMt5Screenshot(bitmap: android.graphics.Bitmap?) {
        _activeMt5Bitmap.value = bitmap
        _scanError.value = null
        if (bitmap != null) {
            scanMt5Chart()
        }
    }

    fun clearMt5Screenshot() {
        _activeMt5Bitmap.value = null
        _activeMt5ScanResult.value = null
        _scanError.value = null
    }

    fun scanMt5Chart(customNote: String? = null) {
        val bmp = _activeMt5Bitmap.value ?: return
        viewModelScope.launch {
            _isScanningMt5.value = true
            _scanError.value = null
            try {
                val result = GeminiForexAnalyst.scanMt5Screenshot(bmp, customNote)
                _activeMt5ScanResult.value = result

                // Auto-sync with matching pair if recognized
                ForexPair.ALL_PAIRS.find {
                    it.symbol.equals(result.detectedPair, ignoreCase = true) ||
                    result.detectedPair.contains(it.symbol, ignoreCase = true)
                }?.let { matchedPair ->
                    _selectedPair.value = matchedPair
                }
            } catch (e: Exception) {
                _scanError.value = "Failed to scan MT5 screenshot: ${e.message}"
            } finally {
                _isScanningMt5.value = false
            }
        }
    }

    fun applyMt5ScanToRiskCalculator() {
        val result = _activeMt5ScanResult.value ?: return
        val slPips = result.stopLossPips ?: 30.0
        val currentCalc = _calculatorState.value
        updateCalculator(currentCalc.accountBalance, currentCalc.riskPercentage, slPips)
    }

    fun saveMt5ScanToJournal(userNotes: String = "") {
        val result = _activeMt5ScanResult.value ?: return
        viewModelScope.launch {
            val entity = SavedSetupEntity(
                pairSymbol = result.detectedPair,
                timeframe = result.detectedTimeframe,
                action = result.tradeAction.name,
                entryPrice = result.entryPrice ?: 0.0,
                stopLoss = result.stopLossPrice ?: 0.0,
                takeProfit1 = result.takeProfit1Price ?: 0.0,
                takeProfit2 = result.takeProfit2Price ?: 0.0,
                riskRewardRatio = result.riskRewardRatio ?: 2.0,
                ema50 = 0.0,
                ema200 = 0.0,
                rsi14 = 0.0,
                keyZoneLabel = "MT5 Scan: ${result.srZoneStatus}",
                status = if (result.canTakeTrade) "ACTIVE" else "PENDING",
                notes = if (userNotes.isNotBlank()) "$userNotes | MT5: ${result.verdictSummary}" else "MT5 Scan: ${result.verdictSummary}"
            )
            tradeDao.insertSetup(entity)
        }
    }

    // ==========================================
    // 100% Risk-Free Auto-Trader Bot Engine
    // ==========================================

    fun toggleAutoTrader(enabled: Boolean) {
        _autoTraderBotState.update {
            it.copy(
                isEnabled = enabled,
                lastBotActionMessage = if (enabled)
                    "🛡️ Auto-Trader Bot ENABLED (Risk-Free Virtual Mode). Scanning charts for 4-rule confluence..."
                else
                    "⏸️ Auto-Trader Bot PAUSED by trader. No automated executions will occur.",
                lastActionTimestamp = System.currentTimeMillis()
            )
        }
    }

    fun resetVirtualAccount(initialBalance: Double = 10000.0) {
        _autoTraderBotState.value = AutoTraderBotState(
            demoBalance = initialBalance,
            lastBotActionMessage = "🔄 Virtual account reset to $${String.format("%,.0f", initialBalance)}. Ready for risk-free automated rule execution.",
            lastActionTimestamp = System.currentTimeMillis()
        )
    }

    fun setAutoTradeRiskPercent(percent: Double) {
        val safePercent = percent.coerceIn(0.25, 2.0)
        _autoTraderBotState.update {
            it.copy(riskPerTradePercent = safePercent)
        }
    }

    fun toggleTrailingStop(enabled: Boolean) {
        _autoTraderBotState.update {
            it.copy(trailingStopEnabled = enabled)
        }
    }

    fun manualTriggerAutoTrade(): Pair<Boolean, String> {
        val setup = _currentSetup.value ?: return Pair(false, "No active setup loaded")
        val pair = _selectedPair.value

        // Check the 4 strict discipline rules
        val failedRules = setup.checklist.filter { !it.isMet }
        if (failedRules.isNotEmpty() || setup.action == TradeAction.WAIT) {
            val ruleNames = failedRules.joinToString(", ") { it.title }
            val msg = "🚫 Auto-Trade BLOCKED: Setup failed strict discipline rules ($ruleNames). All 4 rules must be satisfied."
            _autoTraderBotState.update {
                it.copy(
                    lastBotActionMessage = msg,
                    lastActionTimestamp = System.currentTimeMillis()
                )
            }
            return Pair(false, msg)
        }

        // Execute trade
        executeAutoTradeInternal(setup, pair)
        val successMsg = "⚡ 100% Risk-Free Auto-Trade EXECUTED: ${setup.action} ${pair.symbol} following all 4 rules!"
        return Pair(true, successMsg)
    }

    private fun processAutoTraderBot(
        newIndicators: TechnicalIndicators,
        newSetup: TradeSetup,
        pair: ForexPair
    ) {
        val botState = _autoTraderBotState.value
        val allSetupsList = savedSetups.value
        val activeTrades = allSetupsList.filter { it.status == "ACTIVE" }
        val currentPrice = newIndicators.currentPrice

        // 1. Manage currently open active trades for this pair
        for (trade in activeTrades.filter { it.pairSymbol == pair.symbol }) {
            val isBuy = trade.action == "BUY"
            val pipDist = if (pair.pipSize > 0) pair.pipSize else 0.0001
            val pipsGained = if (isBuy) (currentPrice - trade.entryPrice) / pipDist else (trade.entryPrice - currentPrice) / pipDist
            val floatingPnl = pipsGained * trade.lots * pair.pipValuePerStandardLot

            // Check Take Profit 2 (Ultimate Target)
            val hitTp2 = if (isBuy) currentPrice >= trade.takeProfit2 else currentPrice <= trade.takeProfit2
            if (hitTp2 && trade.takeProfit2 > 0) {
                val realizedGain = Math.abs(trade.takeProfit2 - trade.entryPrice) / pipDist * trade.lots * pair.pipValuePerStandardLot
                viewModelScope.launch {
                    tradeDao.updateSetup(
                        trade.copy(
                            status = "WON_TP2",
                            currentPnl = realizedGain,
                            exitPrice = currentPrice,
                            closedTimestamp = System.currentTimeMillis(),
                            notes = trade.notes + " | Auto-exited at TP2 (+$${String.format("%.2f", realizedGain)})"
                        )
                    )
                }
                _autoTraderBotState.update {
                    it.copy(
                        realizedProfit = it.realizedProfit + realizedGain,
                        winningTradesCount = it.winningTradesCount + 1,
                        lastBotActionMessage = "🎯 WON TP2: Auto-exited ${trade.pairSymbol} ${trade.action} @ ${TechnicalAnalysisEngine.formatPrice(currentPrice, pair)} (+$$${String.format("%.2f", realizedGain)})!",
                        lastActionTimestamp = System.currentTimeMillis()
                    )
                }
                continue
            }

            // Check Take Profit 1 (1:2 R:R minimum guaranteed target)
            val hitTp1 = if (isBuy) currentPrice >= trade.takeProfit1 else currentPrice <= trade.takeProfit1
            if (hitTp1 && trade.status == "ACTIVE" && trade.takeProfit1 > 0) {
                val partialGain = Math.abs(trade.takeProfit1 - trade.entryPrice) / pipDist * trade.lots * pair.pipValuePerStandardLot
                // Move stop loss to entry price (Zero-Risk Break-Even)
                val newStopLoss = if (botState.trailingStopEnabled) trade.entryPrice else trade.stopLoss
                viewModelScope.launch {
                    tradeDao.updateSetup(
                        trade.copy(
                            status = "WON_TP1",
                            stopLoss = newStopLoss,
                            currentPnl = partialGain,
                            notes = trade.notes + " | Hit TP1. SL moved to Break-Even (Zero Risk)."
                        )
                    )
                }
                _autoTraderBotState.update {
                    it.copy(
                        realizedProfit = it.realizedProfit + partialGain,
                        winningTradesCount = it.winningTradesCount + 1,
                        lastBotActionMessage = "🎯 WON TP1: Hit 1:2 R:R on ${trade.pairSymbol} (+$$${String.format("%.2f", partialGain)}). Stop Loss moved to Break-Even (Zero Risk runner).",
                        lastActionTimestamp = System.currentTimeMillis()
                    )
                }
                continue
            }

            // Check Stop Loss
            val hitStopLoss = if (isBuy) currentPrice <= trade.stopLoss else currentPrice >= trade.stopLoss
            if (hitStopLoss && trade.stopLoss > 0) {
                val isBreakEven = Math.abs(trade.stopLoss - trade.entryPrice) < (pipDist * 2)
                val finalPnl = if (isBreakEven) 0.0 else -1.0 * Math.abs(trade.entryPrice - trade.stopLoss) / pipDist * trade.lots * pair.pipValuePerStandardLot
                val finalStatus = if (isBreakEven) "CLOSED" else "STOPPED_OUT"
                viewModelScope.launch {
                    tradeDao.updateSetup(
                        trade.copy(
                            status = finalStatus,
                            currentPnl = finalPnl,
                            exitPrice = currentPrice,
                            closedTimestamp = System.currentTimeMillis(),
                            notes = trade.notes + if (isBreakEven) " | Closed at Break-Even (0 Loss)" else " | Stopped Out (-$$${String.format("%.2f", Math.abs(finalPnl))})"
                        )
                    )
                }
                _autoTraderBotState.update {
                    it.copy(
                        realizedProfit = it.realizedProfit + finalPnl,
                        losingTradesCount = if (isBreakEven) it.losingTradesCount else it.losingTradesCount + 1,
                        lastBotActionMessage = if (isBreakEven)
                            "🛡️ Break-Even exit on ${trade.pairSymbol}. Zero capital lost (100% Capital preserved)."
                        else
                            "🛑 Stop Loss hit on ${trade.pairSymbol} @ ${TechnicalAnalysisEngine.formatPrice(currentPrice, pair)}. Strict 1% risk limit protected capital (-$$${String.format("%.2f", Math.abs(finalPnl))}).",
                        lastActionTimestamp = System.currentTimeMillis()
                    )
                }
                continue
            }
        }

        // 2. Calculate current total floating profit across active trades
        val updatedFloating = activeTrades.sumOf { tr ->
            val p = ForexPair.ALL_PAIRS.find { it.symbol == tr.pairSymbol } ?: pair
            val pDist = if (p.pipSize > 0) p.pipSize else 0.0001
            val pips = if (tr.action == "BUY") (currentPrice - tr.entryPrice) / pDist else (tr.entryPrice - currentPrice) / pDist
            pips * tr.lots * p.pipValuePerStandardLot
        }
        _autoTraderBotState.update { it.copy(floatingProfit = updatedFloating) }

        // 3. Automated Entry Gate: Scan for new trades if bot is active
        if (!botState.isEnabled) return
        if (activeTrades.size >= botState.maxConcurrentTrades) return

        // Prevent opening duplicate active trade on the same pair
        if (activeTrades.any { it.pairSymbol == pair.symbol }) return

        // Strict 4 Discipline Rules Verification
        val allRulesSatisfied = newSetup.isValid &&
                newSetup.action != TradeAction.WAIT &&
                newSetup.checklist.isNotEmpty() &&
                newSetup.checklist.all { it.isMet }

        if (allRulesSatisfied) {
            executeAutoTradeInternal(newSetup, pair)
        }
    }

    private fun executeAutoTradeInternal(setup: TradeSetup, pair: ForexPair) {
        val botState = _autoTraderBotState.value
        val equity = botState.totalEquity
        val dollarRisk = equity * (botState.riskPerTradePercent / 100.0)
        val stopPips = if (setup.riskPipsOrPoints > 0) setup.riskPipsOrPoints else 25.0
        val pipVal = if (pair.pipValuePerStandardLot > 0) pair.pipValuePerStandardLot else 10.0
        val calculatedLots = ((dollarRisk / (stopPips * pipVal)).coerceIn(0.01, 10.0) * 100.0).toInt() / 100.0

        val autoTradeEntity = SavedSetupEntity(
            pairSymbol = pair.symbol,
            timeframe = setup.timeframe.label,
            action = setup.action.name,
            entryPrice = setup.entryPrice,
            stopLoss = setup.stopLoss,
            takeProfit1 = setup.takeProfit1,
            takeProfit2 = setup.takeProfit2,
            riskRewardRatio = setup.riskRewardRatio,
            ema50 = setup.ema50,
            ema200 = setup.ema200,
            rsi14 = setup.rsi14,
            keyZoneLabel = setup.keyZoneLabel,
            status = "ACTIVE",
            notes = "⚡ AUTO-EXECUTED (100% Risk-Free Virtual Paper Trade). All 4 Strict Rules Met: EMA Trend + Zone Pullback + RSI-14 (${String.format("%.1f", setup.rsi14)}) + R:R 1:${String.format("%.1f", setup.riskRewardRatio)}",
            isAutoExecuted = true,
            lots = calculatedLots
        )

        viewModelScope.launch {
            tradeDao.insertSetup(autoTradeEntity)
        }

        _autoTraderBotState.update {
            it.copy(
                totalTradesExecuted = it.totalTradesExecuted + 1,
                lastBotActionMessage = "⚡ AUTO-TRADE OPENED: ${setup.action} ${pair.symbol} ($calculatedLots Lots) @ ${TechnicalAnalysisEngine.formatPrice(setup.entryPrice, pair)} | SL: ${TechnicalAnalysisEngine.formatPrice(setup.stopLoss, pair)} | TP1: ${TechnicalAnalysisEngine.formatPrice(setup.takeProfit1, pair)}. Max Risk strictly $$${String.format("%.2f", dollarRisk)} (1%).",
                lastActionTimestamp = System.currentTimeMillis()
            )
        }
    }

    private fun startNewsTickerCycle() {
        newsTickerJob?.cancel()
        newsTickerJob = viewModelScope.launch {
            while (isActive) {
                delay(6000)
                if (!_isTickerPaused.value) {
                    val count = newsTickerItems.value.size
                    if (count > 0) {
                        _currentNewsIndex.update { (it + 1) % count }
                    }
                }
            }
        }
    }

    fun nextNewsHeadline() {
        val count = newsTickerItems.value.size
        if (count > 0) {
            _currentNewsIndex.update { (it + 1) % count }
        }
    }

    fun prevNewsHeadline() {
        val count = newsTickerItems.value.size
        if (count > 0) {
            _currentNewsIndex.update { if (it - 1 < 0) count - 1 else it - 1 }
        }
    }

    fun toggleTickerPause() {
        _isTickerPaused.update { !it }
    }

    fun setFilterNewsByPair(enabled: Boolean) {
        _filterNewsByPair.value = enabled
        _currentNewsIndex.value = 0
    }

    fun openNewsDetail(item: ForexNewsItem) {
        _selectedNewsDetail.value = item
        _aiBriefingResult.value = null
    }

    fun closeNewsDetail() {
        _selectedNewsDetail.value = null
        _aiBriefingResult.value = null
    }

    fun refreshMarketNews() {
        viewModelScope.launch {
            val freshItem = ForexNewsRepository.generateFreshBreakingNews(_selectedPair.value.symbol)
            _allNewsHeadlines.update { current ->
                listOf(freshItem) + current
            }
            _currentNewsIndex.value = 0
        }
    }

    fun generateAiFundamentalBriefing(item: ForexNewsItem) {
        viewModelScope.launch {
            _isAiBriefingLoading.value = true
            try {
                val pair = ForexPair.ALL_PAIRS.firstOrNull { it.symbol.equals(item.pairSymbol, ignoreCase = true) } ?: _selectedPair.value
                val analysis = GeminiForexAnalyst.analyzeFundamentalNews(item, pair, _indicators.value)
                _aiBriefingResult.value = analysis
            } catch (e: Exception) {
                _aiBriefingResult.value = "⚠️ Error generating AI Fundamental Briefing: ${e.message}"
            } finally {
                _isAiBriefingLoading.value = false
            }
        }
    }

    private fun checkAndTriggerSetupNotification(
        indicators: TechnicalIndicators,
        setup: TradeSetup,
        pair: ForexPair
    ) {
        if (!_notificationsEnabled.value) return
        val alert = com.example.analysis.ForexNotificationManager.triggerSetupAlert(
            getApplication(), pair, indicators, setup
        )
        if (alert != null) {
            _setupAlertHistory.update { listOf(alert) + it.take(49) }
            _headsUpAlert.value = alert
        }
    }

    fun toggleNotificationsEnabled() {
        _notificationsEnabled.update { !it }
    }

    fun dismissHeadsUpAlert() {
        _headsUpAlert.value = null
    }

    fun testSetupNotificationAlert() {
        val pair = _selectedPair.value
        val indicators = _indicators.value ?: return
        val setup = _currentSetup.value ?: return

        val alert = com.example.analysis.ForexNotificationManager.triggerSetupAlert(
            getApplication(),
            pair,
            indicators.copy(
                rsiStatus = if (setup.action == TradeAction.BUY) RsiStatus.OVERSOLD else RsiStatus.OVERBOUGHT
            ),
            setup.copy(
                action = if (setup.action == TradeAction.WAIT) TradeAction.BUY else setup.action,
                riskRewardRatio = if (setup.riskRewardRatio < 2.0) 2.2 else setup.riskRewardRatio
            ),
            forceTest = true
        )
        if (alert != null) {
            _setupAlertHistory.update { listOf(alert) + it.take(49) }
            _headsUpAlert.value = alert
        }
    }

    fun selectCorrelationHorizon(horizon: String) {
        _correlationTimeHorizon.value = horizon
        _marketCorrelationData.value = MarketCorrelationRepository.getCorrelationData(horizon)
    }

    fun refreshCorrelationData() {
        _marketCorrelationData.value = MarketCorrelationRepository.getCorrelationData(_correlationTimeHorizon.value)
    }

    // --- Automated ADR (Average Daily Range) Volatility Alert System ---

    private fun initAllPairsAdrMetrics(): List<PairAdrMetric> {
        return ForexPair.ALL_PAIRS.map { pair ->
            val dailyRanges = AdrAnalysisEngine.getSynthetic14DayDailyRanges(pair.symbol)
            val adr14 = AdrAnalysisEngine.calculateAdr14(dailyRanges, pair.symbol)
            // Initial intraday range ~ 72% of ADR
            val initTodayRange = (adr14 * 0.72 * 10.0).roundToInt() / 10.0
            val pipsOffset = (initTodayRange * pair.pipSize) / 2.0
            val high = pair.basePrice + pipsOffset
            val low = pair.basePrice - pipsOffset
            AdrAnalysisEngine.computePairAdrMetric(
                pair = pair,
                currentPrice = pair.basePrice,
                dayHigh = high,
                dayLow = low,
                customDailyRanges = dailyRanges
            )
        }
    }

    private fun updateAdrMetricsForCurrentTick(pair: ForexPair, lastCandle: Candle) {
        _adrMetrics.update { list ->
            list.map { m ->
                if (m.pair.symbol == pair.symbol) {
                    val newHigh = maxOf(m.dayHigh, lastCandle.high)
                    val newLow = minOf(m.dayLow, lastCandle.low)
                    AdrAnalysisEngine.computePairAdrMetric(
                        pair = pair,
                        currentPrice = lastCandle.close,
                        dayHigh = newHigh,
                        dayLow = newLow,
                        customDailyRanges = m.dailyRangesHistory
                    )
                } else {
                    m
                }
            }
        }
    }

    private fun evaluateAdrVolatilityMonitoring() {
        if (!_adrMonitoringActive.value) return
        val threshold = _adrThresholdPercentage.value
        for (m in _adrMetrics.value) {
            val alert = AdrAnalysisEngine.checkAbnormalDeviation(m, threshold)
            if (alert != null) {
                val alreadyAlerted = _adrAlertHistory.value.any {
                    it.pairSymbol == m.pair.symbol && (System.currentTimeMillis() - it.timestamp < 120_000L)
                }
                if (!alreadyAlerted) {
                    _adrAlertHistory.update { listOf(alert) + it.take(49) }
                    _headsUpAdrAlert.value = alert
                    ForexNotificationManager.triggerAdrVolatilityNotification(
                        getApplication(), alert, m.pair
                    )
                }
            }
        }
    }

    fun toggleAdrMonitoring() {
        _adrMonitoringActive.update { !it }
    }

    fun setAdrThreshold(percentage: Double) {
        _adrThresholdPercentage.value = percentage
        evaluateAdrVolatilityMonitoring()
    }

    fun dismissHeadsUpAdrAlert() {
        _headsUpAdrAlert.value = null
    }

    fun testAdrVolatilityAlert(targetPairSymbol: String? = null) {
        val targetPair = if (targetPairSymbol != null) {
            ForexPair.ALL_PAIRS.find { it.symbol == targetPairSymbol } ?: _selectedPair.value
        } else {
            _selectedPair.value
        }
        val currentMetric = _adrMetrics.value.find { it.pair.symbol == targetPair.symbol }
            ?: return

        val spikeRangePips = (currentMetric.adr14DaysPips * 1.38 * 10.0).roundToInt() / 10.0
        val pipsOffset = (spikeRangePips * targetPair.pipSize) / 2.0
        val spikeMetric = AdrAnalysisEngine.computePairAdrMetric(
            pair = targetPair,
            currentPrice = targetPair.basePrice,
            dayHigh = targetPair.basePrice + pipsOffset,
            dayLow = targetPair.basePrice - pipsOffset
        )

        val alert = AdrAnalysisEngine.checkAbnormalDeviation(spikeMetric, thresholdPercentage = 100.0)
        if (alert != null) {
            val testAlert = alert.copy(isTest = true)
            _adrAlertHistory.update { listOf(testAlert) + it.take(49) }
            _headsUpAdrAlert.value = testAlert
            ForexNotificationManager.triggerAdrVolatilityNotification(
                getApplication(), testAlert, targetPair, forceTest = true
            )
        }
    }

    fun simulateVolatilitySpike(pairSymbol: String) {
        val targetPair = ForexPair.ALL_PAIRS.find { it.symbol == pairSymbol } ?: return
        val currentMetric = _adrMetrics.value.find { it.pair.symbol == targetPair.symbol } ?: return

        // Expand range to 142% of ADR (abnormal deviation)
        val expandedRangePips = (currentMetric.adr14DaysPips * 1.42 * 10.0).roundToInt() / 10.0
        val halfOffset = (expandedRangePips * targetPair.pipSize) / 2.0
        val spikeHigh = targetPair.basePrice + (halfOffset * 1.1)
        val spikeLow = targetPair.basePrice - (halfOffset * 0.9)

        val newMetric = AdrAnalysisEngine.computePairAdrMetric(
            pair = targetPair,
            currentPrice = targetPair.basePrice,
            dayHigh = spikeHigh,
            dayLow = spikeLow
        )

        _adrMetrics.update { list ->
            list.map { if (it.pair.symbol == pairSymbol) newMetric else it }
        }

        val alert = AdrAnalysisEngine.checkAbnormalDeviation(newMetric, _adrThresholdPercentage.value)
        if (alert != null) {
            _adrAlertHistory.update { listOf(alert) + it.take(49) }
            _headsUpAdrAlert.value = alert
            if (_adrMonitoringActive.value) {
                ForexNotificationManager.triggerAdrVolatilityNotification(
                    getApplication(), alert, targetPair, forceTest = true
                )
            }
        }
    }

    // --- Intelligent EA (Expert Advisor) Engine Operations ---

    private fun processIntelligentEaTicks(pair: ForexPair, lastCandle: Candle) {
        // 1. Update existing open EA positions
        _eaActivePositions.update { positions ->
            positions.map { pos ->
                if (pos.marketSymbol == pair.symbol && pos.status == "ACTIVE") {
                    com.example.analysis.IntelligentEaEngine.evaluateActivePosition(pos, lastCandle.close, pair)
                } else {
                    pos
                }
            }
        }

        // 2. If Auto-Pilot is enabled, evaluate if an auto-trade should be fired
        if (_eaAutoPilotActive.value) {
            val indicators = _indicators.value ?: return
            val currentPositionsForPair = _eaActivePositions.value.filter {
                it.marketSymbol == pair.symbol && it.status == "ACTIVE"
            }
            if (currentPositionsForPair.isEmpty()) {
                val (action, reason) = com.example.analysis.IntelligentEaEngine.evaluateStrategyTrigger(
                    _candles.value, pair, indicators, IndustryStrategy.QUAD_CONFLUENCE_ALL
                )
                if (action != TradeAction.WAIT) {
                    val (sl, tp1, tp2) = com.example.analysis.IntelligentEaEngine.computeSuggestedTpSl(
                        pair, lastCandle.close, action
                    )
                    executeEaAutoTrade(
                        pair = pair,
                        action = action,
                        strategy = IndustryStrategy.QUAD_CONFLUENCE_ALL,
                        slPrice = sl,
                        tpPrice = tp1,
                        lots = 0.10,
                        reason = reason
                    )
                }
            }
        }
    }

    fun toggleEaAutoPilot() {
        _eaAutoPilotActive.update { !it }
    }

    fun executeEaAutoTrade(
        pair: ForexPair,
        action: TradeAction,
        strategy: IndustryStrategy,
        slPrice: Double,
        tpPrice: Double,
        lots: Double,
        reason: String
    ) {
        val entryPrice = _candles.value.lastOrNull()?.close ?: pair.basePrice
        val autoEntity = com.example.analysis.IntelligentEaEngine.createAutoTradeEntity(
            pair = pair,
            action = action,
            strategy = strategy,
            entryPrice = entryPrice,
            stopLoss = slPrice,
            takeProfit1 = tpPrice,
            takeProfit2 = tpPrice,
            lots = lots,
            reasoning = reason
        )

        viewModelScope.launch {
            tradeDao.insertSetup(autoEntity)
        }

        val newPos = EaActivePosition(
            id = System.currentTimeMillis(),
            marketSymbol = pair.symbol,
            action = action,
            strategy = strategy,
            entryPrice = entryPrice,
            stopLoss = slPrice,
            takeProfit1 = tpPrice,
            takeProfit2 = tpPrice,
            lotSize = lots,
            currentPrice = entryPrice,
            currentPnl = 0.0,
            pnlPips = 0.0,
            status = "ACTIVE",
            executionLog = "⚡ EA Auto-Execution on ${pair.symbol} [${strategy.rankBadge}]: Entry @ ${TechnicalAnalysisEngine.formatPrice(entryPrice, pair)} | SL: ${TechnicalAnalysisEngine.formatPrice(slPrice, pair)} | TP: ${TechnicalAnalysisEngine.formatPrice(tpPrice, pair)}"
        )

        _eaActivePositions.update { listOf(newPos) + it }
    }

    fun closeEaPosition(positionId: Long) {
        _eaActivePositions.update { list ->
            list.map { if (it.id == positionId) it.copy(status = "CLOSED") else it }
        }
    }
}

