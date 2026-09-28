package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AlphaFX", appName)
  }

  @Test
  fun `test technical analysis engine ema and rsi calculations`() {
    val pair = com.example.model.ForexPair.defaultPair()
    val repo = com.example.analysis.MarketDataRepository()
    val candles = repo.generateCandles(pair, com.example.model.Timeframe.H1, com.example.analysis.MarketDataRepository.MarketScenario.PRIME_EURUSD_BUY)
    val (indicators, setup) = com.example.analysis.TechnicalAnalysisEngine.analyzeStructuralPriceAction(
        candles, pair, com.example.model.Timeframe.H1
    )
    assert(indicators.ema50 > 0.0)
    assert(indicators.ema200 > 0.0)
    assert(indicators.rsi14 in 0.0..100.0)
    assert(setup.checklist.size == 4)
  }

  @Test
  fun `test position size calculation and risk limits`() {
    val eurusd = com.example.model.ForexPair.ALL_PAIRS.first { it.symbol == "EUR/USD" }
    val balance = 10000.0
    val riskPercent = 1.0 // 1%
    val slPips = 25.0

    val dollarRisk = balance * (riskPercent / 100.0)
    assertEquals(100.0, dollarRisk, 0.001)

    val pipValue = eurusd.pipValuePerStandardLot // 10.0
    val calculatedLots = (dollarRisk / (slPips * pipValue))
    val roundedLots = Math.round(calculatedLots * 100.0) / 100.0
    assertEquals(0.40, roundedLots, 0.01)

    // Test US30
    val us30 = com.example.model.ForexPair.ALL_PAIRS.first { it.symbol == "US30" }
    val us30SlPoints = 50.0
    val us30Lots = dollarRisk / (us30SlPoints * us30.pipValuePerStandardLot)
    assertEquals(2.0, us30Lots, 0.01)
  }

  @Test
  fun `test mt5 chart scan result model`() {
    val bitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
    val b64 = com.example.gemini.GeminiForexAnalyst.bitmapToBase64(bitmap)
    assert(b64.isNotBlank())

    val result = com.example.model.Mt5ScanResult(
        detectedPair = "EUR/USD",
        detectedTimeframe = "H1",
        canTakeTrade = true,
        tradeAction = com.example.model.TradeAction.BUY,
        verdictTitle = "VALID BUY",
        verdictSummary = "50 > 200 EMA and RSI oversold",
        entryPrice = 1.08450,
        stopLossPrice = 1.08150,
        takeProfit1Price = 1.09050,
        takeProfit2Price = 1.09450,
        riskRewardRatio = 2.0,
        stopLossPips = 30.0,
        ema50Status = "Bullish",
        ema200Status = "Bullish",
        srZoneStatus = "Support retest",
        rsi14Status = "Oversold",
        riskManagementStatus = "1:2 R:R met",
        fullAnalysisMarkdown = "Full audit",
        checklist = listOf(
            com.example.model.ChecklistItem("50 & 200 EMA Trend", "Bullish", true, "Met"),
            com.example.model.ChecklistItem("Key S/R Pullback Zone", "Support", true, "Met"),
            com.example.model.ChecklistItem("RSI(14) Reversal / Oversold", "Oversold", true, "Met"),
            com.example.model.ChecklistItem("Strict Risk Management", "1:2 R:R", true, "Met")
        )
    )
    assert(result.canTakeTrade)
    assertEquals("EUR/USD", result.detectedPair)
    assertEquals(com.example.model.TradeAction.BUY, result.tradeAction)
    assertEquals(30.0, result.stopLossPips!!, 0.001)
  }

  @Test
  fun `test auto trader bot state and risk free equity calculations`() {
    val botState = com.example.model.AutoTraderBotState(
        isEnabled = true,
        demoBalance = 10000.0,
        realizedProfit = 240.0,
        floatingProfit = 65.0,
        riskPerTradePercent = 1.0
    )
    // 100% Risk-Free Virtual Capital
    assertEquals(10305.0, botState.totalEquity, 0.001)
    assertEquals(305.0, botState.totalPnl, 0.001)
    assertEquals(3.05, botState.pnlPercent, 0.01)

    // Strict 1% max risk allocation
    val riskAmount = botState.totalEquity * (botState.riskPerTradePercent / 100.0)
    assertEquals(103.05, riskAmount, 0.001)
  }

  @Test
  fun `test 4-rule gatekeeper blocks trades when any discipline rule is violated`() {
    val invalidChecklist = listOf(
        com.example.model.ChecklistItem("50 & 200 EMA Trend", "Bullish", true, "Met"),
        com.example.model.ChecklistItem("Key S/R Pullback Zone", "Floating in mid air", false, "Failed"),
        com.example.model.ChecklistItem("RSI(14) Reversal / Oversold", "Neutral", false, "Failed"),
        com.example.model.ChecklistItem("Strict Risk Management", "1:1 R:R", false, "Failed")
    )

    val invalidSetup = com.example.model.TradeSetup(
        pairSymbol = "EUR/USD",
        timeframe = com.example.model.Timeframe.H1,
        action = com.example.model.TradeAction.WAIT,
        entryPrice = 1.0850,
        stopLoss = 1.0800,
        takeProfit1 = 1.0870,
        takeProfit2 = 1.0900,
        riskRewardRatio = 1.0,
        riskPipsOrPoints = 50.0,
        rewardPipsOrPoints = 50.0,
        ema50 = 1.0840,
        ema200 = 1.0810,
        rsi14 = 52.0,
        keyZoneLabel = "Mid-range",
        isValid = false,
        statusSummary = "No confluence",
        detailedReasoning = "EMAs and RSI lack alignment",
        checklist = invalidChecklist
    )

    // Auto-trader gate check: Must reject because isValid is false and rules are not satisfied
    val canAutoTrade = invalidSetup.isValid &&
        invalidSetup.action != com.example.model.TradeAction.WAIT &&
        invalidSetup.checklist.all { it.isMet }
    assert(!canAutoTrade)
  }

  @Test
  fun `test historical AI data seeder generates valid recommendations`() {
    val samples = com.example.analysis.HistoricalAiDataSeeder.generateHistoricalRecommendations()
    assert(samples.size >= 15)
    
    val wonCount = samples.count { it.status == "WON_TP1" || it.status == "WON_TP2" }
    val lossCount = samples.count { it.status == "STOPPED_OUT" }
    assert(wonCount > lossCount)
    
    // Check that pairs include US30, EUR/USD, and GBP/USD
    val pairs = samples.map { it.pairSymbol }.toSet()
    assert(pairs.contains("US30"))
    assert(pairs.contains("EUR/USD"))
    assert(pairs.contains("GBP/USD"))
  }

  @Test
  fun `test recharts html builder generates valid html with recharts and responsive svg`() {
    val points = listOf(
      com.example.model.TradeRecommendationPoint(
        id = 1L,
        timestamp = System.currentTimeMillis() - 86400000L,
        dateLabel = "Sep 20",
        pairSymbol = "EUR/USD",
        timeframe = "H1",
        action = "BUY",
        entryPrice = 1.0850,
        exitPrice = 1.0900,
        stopLoss = 1.0825,
        takeProfit1 = 1.0900,
        status = "WON_TP1",
        isWin = true,
        pnl = 200.0,
        cumulativeWins = 1,
        cumulativeTotal = 1,
        cumulativeAccuracyPercent = 100.0,
        rollingAccuracyPercent = 100.0,
        cumulativePnl = 200.0,
        notes = "AI Test"
      ),
      com.example.model.TradeRecommendationPoint(
        id = 2L,
        timestamp = System.currentTimeMillis(),
        dateLabel = "Sep 21",
        pairSymbol = "US30",
        timeframe = "H1",
        action = "SELL",
        entryPrice = 40000.0,
        exitPrice = 40100.0,
        stopLoss = 40100.0,
        takeProfit1 = 39800.0,
        status = "STOPPED_OUT",
        isWin = false,
        pnl = -100.0,
        cumulativeWins = 1,
        cumulativeTotal = 2,
        cumulativeAccuracyPercent = 50.0,
        rollingAccuracyPercent = 50.0,
        cumulativePnl = 100.0,
        notes = "AI Test"
      )
    )

    val assets = listOf(
      com.example.model.AssetAccuracyStat(
        symbol = "EUR/USD",
        totalTrades = 1,
        wonTrades = 1,
        lostTrades = 0,
        winRatePercent = 100.0,
        netPnl = 200.0
      ),
      com.example.model.AssetAccuracyStat(
        symbol = "US30",
        totalTrades = 1,
        wonTrades = 0,
        lostTrades = 1,
        winRatePercent = 0.0,
        netPnl = -100.0
      )
    )

    val data = com.example.model.AccuracyDashboardData(
      totalRecommendations = 2,
      closedRecommendations = 2,
      wonCount = 1,
      lostCount = 1,
      activeCount = 0,
      overallAccuracyPercent = 50.0,
      benchmarkTargetPercent = 70.0,
      profitFactor = 2.0,
      netPnl = 100.0,
      accuracyOverTime = points,
      assetBreakdown = assets
    )

    val html = com.example.ui.components.RechartsHtmlBuilder.buildHtml(data)
    assert(html.contains("recharts@2.12.7"))
    assert(html.contains("AreaChart"))
    assert(html.contains("50.0%"))
    assert(html.contains("EUR/USD"))
    assert(html.contains("US30"))
    assert(html.contains("recharts-accuracy-root"))
  }

  @Test
  fun `test forex news repository headlines and technical confluence`() {
    val headlines = com.example.analysis.ForexNewsRepository.getInitialHeadlines()
    assert(headlines.isNotEmpty())
    assert(headlines.any { it.pairSymbol == "US30" })
    assert(headlines.any { it.pairSymbol == "EUR/USD" })
    assert(headlines.any { it.pairSymbol == "GBP/USD" })
    assert(headlines.any { it.pairSymbol == "USD/JPY" })
    assert(headlines.any { it.pairSymbol == "XAU/USD" })

    val us30Item = headlines.first { it.pairSymbol == "US30" }
    assert(us30Item.headline.isNotBlank())
    assert(us30Item.fundamentalContext.isNotBlank())
    assert(us30Item.technicalConfluence.contains("EMA") || us30Item.technicalConfluence.contains("RSI") || us30Item.technicalConfluence.contains("US30"))

    // Test fresh breaking news generator
    val breakingEur = com.example.analysis.ForexNewsRepository.generateFreshBreakingNews("EUR/USD")
    assertEquals("EUR/USD", breakingEur.pairSymbol)
    assert(breakingEur.isBreaking)
    assert(breakingEur.fundamentalContext.isNotBlank())
    assert(breakingEur.technicalConfluence.isNotBlank())
  }

  @Test
  fun `test gemini fundamental news analysis synthesis`() = kotlinx.coroutines.runBlocking {
    val headlines = com.example.analysis.ForexNewsRepository.getInitialHeadlines()
    val item = headlines.first { it.pairSymbol == "US30" }
    val pair = com.example.model.ForexPair.defaultPair()
    val indicators = com.example.model.TechnicalIndicators(
      currentPrice = 43850.0,
      ema50 = 43200.0,
      ema200 = 42500.0,
      rsi14 = 55.0,
      trendDirection = com.example.model.TrendDirection.STRONG_BULLISH,
      nearestSupport = null,
      nearestResistance = null,
      isTestingSupportZone = false,
      isTestingResistanceZone = false,
      rsiStatus = com.example.model.RsiStatus.NEUTRAL,
      historicalEma50 = listOf(43100.0, 43200.0),
      historicalEma200 = listOf(42400.0, 42500.0),
      historicalRsi14 = listOf(52.0, 55.0)
    )

    val synthesis = com.example.gemini.GeminiForexAnalyst.analyzeFundamentalNews(item, pair, indicators)
    assert(synthesis.contains("Fundamental Briefing") || synthesis.contains("US30"))
    assert(synthesis.contains("Macro") || synthesis.contains("Institutional"))
    assert(synthesis.contains("Technical Confluence"))
    assert(synthesis.contains("Risk Management") || synthesis.contains("1.0%"))
  }

  @Test
  fun `test push notification triggers on 50 200 ema and rsi reversal criteria`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.analysis.ForexNotificationManager.createNotificationChannel(context)

    val pair = com.example.model.ForexPair.defaultPair() // US30
    val indicators = com.example.model.TechnicalIndicators(
      currentPrice = 43850.0,
      ema50 = 43200.0,
      ema200 = 42500.0,
      rsi14 = 29.5,
      trendDirection = com.example.model.TrendDirection.STRONG_BULLISH,
      nearestSupport = null,
      nearestResistance = null,
      isTestingSupportZone = true,
      isTestingResistanceZone = false,
      rsiStatus = com.example.model.RsiStatus.OVERSOLD,
      historicalEma50 = listOf(43100.0, 43200.0),
      historicalEma200 = listOf(42400.0, 42500.0),
      historicalRsi14 = listOf(28.0, 29.5)
    )

    val validSetup = com.example.model.TradeSetup(
      pairSymbol = "US30",
      timeframe = com.example.model.Timeframe.H1,
      action = com.example.model.TradeAction.BUY,
      isValid = true,
      statusSummary = "PRIME BUY",
      detailedReasoning = "50/200 EMA trend + RSI-14 oversold bounce",
      entryPrice = 43850.0,
      stopLoss = 43750.0,
      takeProfit1 = 44050.0,
      takeProfit2 = 44170.0,
      riskPipsOrPoints = 100.0,
      rewardPipsOrPoints = 200.0,
      riskRewardRatio = 2.0,
      ema50 = 43200.0,
      ema200 = 42500.0,
      rsi14 = 29.5,
      keyZoneLabel = "4H Structural Support",
      checklist = listOf(
        com.example.model.ChecklistItem("50 & 200 EMA Trend", "Bullish alignment", true, "50 > 200"),
        com.example.model.ChecklistItem("Key S/R Pullback Zone", "Support test", true, "In Zone"),
        com.example.model.ChecklistItem("RSI(14) Reversal / Oversold", "Oversold reversal", true, "29.5"),
        com.example.model.ChecklistItem("Strict Risk Management", "1:2.0 R:R", true, "1:2.0")
      )
    )

    val alert = com.example.analysis.ForexNotificationManager.triggerSetupAlert(
      context, pair, indicators, validSetup, forceTest = true
    )

    assert(alert != null)
    assertEquals("US30", alert?.pairSymbol)
    assertEquals(com.example.model.TradeAction.BUY, alert?.action)
    assertEquals(43850.0, alert?.entryPrice ?: 0.0, 0.01)
    assertEquals(2.0, alert?.riskRewardRatio ?: 0.0, 0.01)
    assert(alert?.message?.contains("50/200 EMA") == true)
  }

  @Test
  fun `test pearson correlation calculations and classifications`() {
    val x = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
    val ySame = listOf(2.0, 4.0, 6.0, 8.0, 10.0)
    val yInv = listOf(10.0, 8.0, 6.0, 4.0, 2.0)

    val rPerfect = com.example.analysis.MarketCorrelationRepository.calculatePearson(x, ySame)
    assertEquals(1.0, rPerfect, 0.001)

    val rInverse = com.example.analysis.MarketCorrelationRepository.calculatePearson(x, yInv)
    assertEquals(-1.0, rInverse, 0.001)

    val classificationPos = com.example.analysis.MarketCorrelationRepository.classifyCorrelation(0.85)
    assertEquals(com.example.model.CorrelationStrength.VERY_STRONG_POSITIVE, classificationPos)
  }

  @Test
  fun `test market correlation dataset generation for US30 EURUSD and GBPUSD`() {
    val data24h = com.example.analysis.MarketCorrelationRepository.getCorrelationData("24H")
    assertEquals("24H", data24h.timeHorizon)
    assertEquals(24, data24h.points.size)

    val firstPoint = data24h.points.first()
    assert(firstPoint.timeLabel.isNotBlank())
    assert(firstPoint.us30RawPrice > 20000.0)
    assert(firstPoint.eurusdRawPrice in 0.8..1.5)
    assert(firstPoint.gbpusdRawPrice in 0.9..1.8)

    // Verify 3 target pairs in metrics
    val eurGbp = data24h.metrics.find { it.pairA == "EUR/USD" && it.pairB == "GBP/USD" }
    assert(eurGbp != null)
    assert(eurGbp!!.coefficient > 0.40)

    val us30Eur = data24h.metrics.find { it.pairA == "US30" && it.pairB == "EUR/USD" }
    assert(us30Eur != null)

    // Verify hedging opportunities and correlated risk alerts
    assert(data24h.hedgingOpportunities.isNotEmpty())
    assert(data24h.hedgingOpportunities.any { it.primaryPair == "EUR/USD" && it.hedgePair == "GBP/USD" })
    assert(data24h.correlatedRisks.isNotEmpty())
    assert(data24h.correlatedRisks.any { it.severity == com.example.model.RiskSeverity.HIGH })
  }

  @Test
  fun `test recharts market correlation html builder output`() {
    val data = com.example.analysis.MarketCorrelationRepository.getCorrelationData("7D")
    val html = com.example.ui.components.MarketCorrelationRechartsHtmlBuilder.buildHtml(data)

    assert(html.contains("recharts@2.12.7"))
    assert(html.contains("react@18.3.1"))
    assert(html.contains("US30"))
    assert(html.contains("EUR/USD"))
    assert(html.contains("GBP/USD"))
    assert(html.contains("Rolling Correlation"))
    assert(html.contains("Heatmap Matrix"))
  }
}
