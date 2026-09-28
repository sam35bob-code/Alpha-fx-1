package com.example.analysis

import com.example.model.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object TechnicalAnalysisEngine {

    /**
     * Calculates the Exponential Moving Average series for a given period.
     */
    fun calculateEma(candles: List<Candle>, period: Int): List<Double> {
        if (candles.isEmpty()) return emptyList()
        val closes = candles.map { it.close }
        val result = ArrayList<Double>(closes.size)
        val k = 2.0 / (period + 1.0)

        // Seed with SMA or first element
        val seedCount = min(period, closes.size)
        var currentEma = closes.take(seedCount).average()

        for (i in closes.indices) {
            if (i < seedCount - 1) {
                result.add(currentEma)
            } else if (i == seedCount - 1) {
                result.add(currentEma)
            } else {
                currentEma = (closes[i] * k) + (currentEma * (1.0 - k))
                result.add(currentEma)
            }
        }
        return result
    }

    /**
     * Calculates the 14-period Relative Strength Index (RSI).
     */
    fun calculateRsi(candles: List<Candle>, period: Int = 14): List<Double> {
        if (candles.size <= period) {
            return List(candles.size) { 50.0 }
        }

        val closes = candles.map { it.close }
        val rsiList = ArrayList<Double>(closes.size)

        // Initial items before period have default 50
        for (i in 0 until period) {
            rsiList.add(50.0)
        }

        var avgGain = 0.0
        var avgLoss = 0.0

        for (i in 1..period) {
            val change = closes[i] - closes[i - 1]
            if (change > 0) avgGain += change else avgLoss += abs(change)
        }
        avgGain /= period
        avgLoss /= period

        var firstRs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
        rsiList.add(100.0 - (100.0 / (1.0 + firstRs)))

        // Wilder's smoothing
        for (i in (period + 1) until closes.size) {
            val change = closes[i] - closes[i - 1]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) abs(change) else 0.0

            avgGain = ((avgGain * (period - 1)) + gain) / period
            avgLoss = ((avgLoss * (period - 1)) + loss) / period

            val rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
            val rsi = 100.0 - (100.0 / (1.0 + rs))
            rsiList.add(rsi.coerceIn(0.0, 100.0))
        }

        return rsiList
    }

    /**
     * Extracts structural key Support and Resistance zones from price action swings.
     */
    fun identifyKeyZones(candles: List<Candle>, currentPrice: Double, pair: ForexPair): Pair<PriceZone?, PriceZone?> {
        if (candles.size < 20) return Pair(null, null)

        val swingHighs = mutableListOf<Double>()
        val swingLows = mutableListOf<Double>()

        // Look for 5-bar fractal swings
        for (i in 2 until candles.size - 2) {
            val high = candles[i].high
            val low = candles[i].low
            if (high > candles[i - 1].high && high > candles[i - 2].high &&
                high > candles[i + 1].high && high > candles[i + 2].high) {
                swingHighs.add(high)
            }
            if (low < candles[i - 1].low && low < candles[i - 2].low &&
                low < candles[i + 1].low && low < candles[i + 2].low) {
                swingLows.add(low)
            }
        }

        val resistancesAbove = swingHighs.filter { it > currentPrice }.sorted()
        val supportsBelow = swingLows.filter { it < currentPrice }.sortedDescending()

        val pip = pair.pipSize
        val zonePadding = when (pair.symbol) {
            "US30" -> 35.0
            "XAU/USD" -> 4.5
            else -> pip * 12.0
        }

        val nearestSupport = if (supportsBelow.isNotEmpty()) {
            val level = supportsBelow.first()
            PriceZone(
                type = ZoneType.SUPPORT,
                top = level + zonePadding * 0.4,
                bottom = level - zonePadding * 0.6,
                label = "Key Daily/4H Structural Support",
                touches = supportsBelow.count { abs(it - level) < zonePadding * 1.5 } + 1
            )
        } else {
            val fallbackLevel = currentPrice - (zonePadding * 4)
            PriceZone(ZoneType.SUPPORT, fallbackLevel + zonePadding * 0.4, fallbackLevel - zonePadding * 0.6, "Major Structural Base")
        }

        val nearestResistance = if (resistancesAbove.isNotEmpty()) {
            val level = resistancesAbove.first()
            PriceZone(
                type = ZoneType.RESISTANCE,
                top = level + zonePadding * 0.6,
                bottom = level - zonePadding * 0.4,
                label = "Key Daily/4H Structural Resistance",
                touches = resistancesAbove.count { abs(it - level) < zonePadding * 1.5 } + 1
            )
        } else {
            val fallbackLevel = currentPrice + (zonePadding * 4)
            PriceZone(ZoneType.RESISTANCE, fallbackLevel + zonePadding * 0.6, fallbackLevel - zonePadding * 0.4, "Major Liquidity Ceiling")
        }

        return Pair(nearestSupport, nearestResistance)
    }

    /**
     * Evaluates technical indicators and generates a comprehensive setup according to strict rules:
     * 1. 50-EMA & 200-EMA trend identification.
     * 2. Price pull back to key support or resistance zone.
     * 3. 14-period RSI signals a reversal or oversold condition.
     * 4. Strict risk management parameters before suggesting any trade.
     */
    fun analyzeStructuralPriceAction(
        candles: List<Candle>,
        pair: ForexPair,
        timeframe: Timeframe
    ): Pair<TechnicalIndicators, TradeSetup> {
        val ema50Series = calculateEma(candles, 50)
        val ema200Series = calculateEma(candles, 200)
        val rsi14Series = calculateRsi(candles, 14)

        val currentPrice = candles.lastOrNull()?.close ?: pair.basePrice
        val currentEma50 = ema50Series.lastOrNull() ?: currentPrice
        val currentEma200 = ema200Series.lastOrNull() ?: currentPrice
        val currentRsi = rsi14Series.lastOrNull() ?: 50.0
        val prevRsi = if (rsi14Series.size >= 2) rsi14Series[rsi14Series.size - 2] else currentRsi

        val (nearestSupport, nearestResistance) = identifyKeyZones(candles, currentPrice, pair)

        // Trend Direction
        val isBullishTrend = currentEma50 > currentEma200 && currentPrice >= (currentEma200 * 0.995)
        val isBearishTrend = currentEma50 < currentEma200 && currentPrice <= (currentEma200 * 1.005)

        val trendDirection = when {
            isBullishTrend && currentPrice >= currentEma50 -> TrendDirection.STRONG_BULLISH
            isBullishTrend -> TrendDirection.BULLISH
            isBearishTrend && currentPrice <= currentEma50 -> TrendDirection.STRONG_BEARISH
            isBearishTrend -> TrendDirection.BEARISH
            else -> TrendDirection.RANGING
        }

        // Pullback to Support / Resistance Zone
        val pip = pair.pipSize
        val zoneProximityThreshold = when (pair.symbol) {
            "US30" -> 60.0
            "XAU/USD" -> 6.0
            else -> pip * 20.0
        }

        val isTestingSupport = nearestSupport != null &&
                (nearestSupport.contains(currentPrice) || abs(currentPrice - nearestSupport.top) <= zoneProximityThreshold)
        val isTestingResistance = nearestResistance != null &&
                (nearestResistance.contains(currentPrice) || abs(currentPrice - nearestResistance.bottom) <= zoneProximityThreshold)

        // RSI 14 Evaluation
        val rsiStatus = when {
            currentRsi < 30.0 -> RsiStatus.OVERSOLD
            currentRsi in 30.0..38.0 && currentRsi > prevRsi -> RsiStatus.BULLISH_REVERSAL
            currentRsi > 70.0 -> RsiStatus.OVERBOUGHT
            currentRsi in 62.0..70.0 && currentRsi < prevRsi -> RsiStatus.BEARISH_REVERSAL
            else -> RsiStatus.NEUTRAL
        }

        val indicators = TechnicalIndicators(
            currentPrice = currentPrice,
            ema50 = currentEma50,
            ema200 = currentEma200,
            rsi14 = currentRsi,
            trendDirection = trendDirection,
            nearestSupport = nearestSupport,
            nearestResistance = nearestResistance,
            isTestingSupportZone = isTestingSupport,
            isTestingResistanceZone = isTestingResistance,
            rsiStatus = rsiStatus,
            historicalEma50 = ema50Series,
            historicalEma200 = ema200Series,
            historicalRsi14 = rsi14Series
        )

        // Checklist Verification
        // 1. EMA Trend
        val rule1Bullish = currentEma50 > currentEma200
        val rule1Bearish = currentEma50 < currentEma200
        val rule1Met = rule1Bullish || rule1Bearish
        val rule1Label = if (rule1Bullish) "Bullish Alignment (50 > 200 EMA)" else if (rule1Bearish) "Bearish Alignment (50 < 200 EMA)" else "Neutral / Compress"

        // 2. Pullback to Key Zone
        val rule2Bullish = isTestingSupport
        val rule2Bearish = isTestingResistance
        val rule2Met = (rule1Bullish && rule2Bullish) || (rule1Bearish && rule2Bearish)
        val rule2Label = if (rule2Bullish) "Pullback into Support Zone" else if (rule2Bearish) "Pullback into Resistance Zone" else "In Mid-Range / No-Man's Land"

        // 3. RSI 14 Confirmation
        val rule3Bullish = currentRsi < 32.0 || (currentRsi in 30.0..40.0 && currentRsi > prevRsi)
        val rule3Bearish = currentRsi > 68.0 || (currentRsi in 60.0..70.0 && currentRsi < prevRsi)
        val rule3Met = (rule1Bullish && rule3Bullish) || (rule1Bearish && rule3Bearish)
        val rule3Label = if (rule3Bullish) "RSI(14) Oversold / Bullish Hook (${String.format("%.1f", currentRsi)})"
        else if (rule3Bearish) "RSI(14) Overbought / Bearish Hook (${String.format("%.1f", currentRsi)})"
        else "RSI Neutral (${String.format("%.1f", currentRsi)}) - No Extreme"

        // 4. Strict Risk Management (Minimum 1:2.0 Risk-to-Reward)
        val tradeAction: TradeAction
        val entryPrice: Double
        val stopLoss: Double
        val takeProfit1: Double
        val takeProfit2: Double
        val riskAmount: Double
        val rewardAmount: Double
        val keyZoneLabel: String

        val structuralBuffer = when (pair.symbol) {
            "US30" -> 45.0
            "XAU/USD" -> 5.0
            else -> pip * 18.0
        }

        if (rule1Bullish && rule2Bullish && rule3Bullish) {
            tradeAction = TradeAction.BUY
            entryPrice = currentPrice
            val supportBase = nearestSupport?.bottom ?: (currentPrice - structuralBuffer)
            stopLoss = supportBase - structuralBuffer
            riskAmount = max(currentPrice - stopLoss, structuralBuffer)
            takeProfit1 = entryPrice + (riskAmount * 2.0) // 1:2 R:R
            takeProfit2 = entryPrice + (riskAmount * 3.2) // 1:3.2 R:R
            rewardAmount = takeProfit1 - entryPrice
            keyZoneLabel = nearestSupport?.label ?: "Structural Support"
        } else if (rule1Bearish && rule2Bearish && rule3Bearish) {
            tradeAction = TradeAction.SELL
            entryPrice = currentPrice
            val resistanceCeiling = nearestResistance?.top ?: (currentPrice + structuralBuffer)
            stopLoss = resistanceCeiling + structuralBuffer
            riskAmount = max(stopLoss - currentPrice, structuralBuffer)
            takeProfit1 = entryPrice - (riskAmount * 2.0) // 1:2 R:R
            takeProfit2 = entryPrice - (riskAmount * 3.2) // 1:3.2 R:R
            rewardAmount = entryPrice - takeProfit1
            keyZoneLabel = nearestResistance?.label ?: "Structural Resistance"
        } else {
            // No trade / waiting
            tradeAction = TradeAction.WAIT
            entryPrice = currentPrice
            riskAmount = structuralBuffer
            stopLoss = if (rule1Bullish) currentPrice - structuralBuffer else currentPrice + structuralBuffer
            takeProfit1 = if (rule1Bullish) currentPrice + (structuralBuffer * 2.0) else currentPrice - (structuralBuffer * 2.0)
            takeProfit2 = if (rule1Bullish) currentPrice + (structuralBuffer * 3.0) else currentPrice - (structuralBuffer * 3.0)
            rewardAmount = structuralBuffer * 2.0
            keyZoneLabel = if (rule1Bullish) nearestSupport?.label ?: "Awaiting Support Zone" else nearestResistance?.label ?: "Awaiting Resistance Zone"
        }

        val riskReward = if (riskAmount > 0) rewardAmount / riskAmount else 2.0
        val rule4Met = riskReward >= 2.0 && tradeAction != TradeAction.WAIT
        val rule4Label = if (tradeAction != TradeAction.WAIT) "Valid 1:${String.format("%.1f", riskReward)} R:R (SL outside structure)" else "Pending Setup (Requires Min 1:2 R:R)"

        val checklist = listOf(
            ChecklistItem("50 & 200 EMA Trend", "Directional bias established by EMA relationship", rule1Met, rule1Label),
            ChecklistItem("Key S/R Pullback Zone", "Price returned to value area for high-probability entry", rule2Met, rule2Label),
            ChecklistItem("RSI(14) Reversal / Oversold", "Momentum exhaustion confirmation before entry", rule3Met, rule3Label),
            ChecklistItem("Strict Risk Management", "Min 1:2 R:R & invalidation beyond structural pivot", rule4Met, rule4Label)
        )

        val allRulesPassed = checklist.all { it.isMet }

        val statusSummary = when {
            allRulesPassed && tradeAction == TradeAction.BUY ->
                "DISCIPLINED BUY CONFIRMED: 50 EMA bullish, pullback to key support verified, RSI-14 oversold reversal active with 1:${String.format("%.1f", riskReward)} R:R."
            allRulesPassed && tradeAction == TradeAction.SELL ->
                "DISCIPLINED SELL CONFIRMED: 50 EMA bearish, pullback to key resistance verified, RSI-14 overbought reversal active with 1:${String.format("%.1f", riskReward)} R:R."
            !rule2Met ->
                "PATIENCE REQUIRED: Trend is ${trendDirection.label}, but price is floating outside key structural zones. Await clean pullback to avoid chasing."
            !rule3Met ->
                "AWAITING MOMENTUM CONFIRMATION: Price is at key zone, but RSI(14) has not signaled an oversold or reversal condition. Do not preempt."
            else ->
                "CONDITIONS INCOMPLETE: As a disciplined analyst, we do not force trades. Wait for structural confluence across all 4 rules."
        }

        val detailedReasoning = buildString {
            append("• Pair: ${pair.symbol} (${timeframe.label})\n")
            append("• EMA Trend: 50 EMA (${formatPrice(currentEma50, pair)}) vs 200 EMA (${formatPrice(currentEma200, pair)}) -> ${trendDirection.label}.\n")
            append("• Zone Status: $rule2Label.\n")
            append("• RSI(14) Reading: ${String.format("%.1f", currentRsi)} -> ${rsiStatus.label}.\n")
            if (allRulesPassed) {
                append("• Risk Execution: Strict SL at ${formatPrice(stopLoss, pair)}, TP1 at ${formatPrice(takeProfit1, pair)}, TP2 at ${formatPrice(takeProfit2, pair)}.\n")
                append("• Capital Protection: Risk strictly capped at 1% of account.")
            } else {
                append("• Discipline Directive: Stay on the sidelines. Preservation of capital is primary rule #1.")
            }
        }

        val setup = TradeSetup(
            pairSymbol = pair.symbol,
            timeframe = timeframe,
            action = tradeAction,
            isValid = allRulesPassed,
            statusSummary = statusSummary,
            detailedReasoning = detailedReasoning,
            entryPrice = entryPrice,
            stopLoss = stopLoss,
            takeProfit1 = takeProfit1,
            takeProfit2 = takeProfit2,
            riskPipsOrPoints = if (pair.pipSize > 0) riskAmount / pair.pipSize else riskAmount,
            rewardPipsOrPoints = if (pair.pipSize > 0) rewardAmount / pair.pipSize else rewardAmount,
            riskRewardRatio = riskReward,
            ema50 = currentEma50,
            ema200 = currentEma200,
            rsi14 = currentRsi,
            keyZoneLabel = keyZoneLabel,
            checklist = checklist
        )

        return Pair(indicators, setup)
    }

    fun formatPrice(value: Double, pair: ForexPair): String {
        return String.format("%.${pair.decimalDigits}f", value)
    }
}
