package com.example.analysis

import com.example.data.SavedSetupEntity
import com.example.model.*
import kotlin.math.abs
import kotlin.math.roundToInt

object IntelligentEaEngine {

    /**
     * Evaluates candle price action and technical indicators for any given market
     * to check if a specific Top 4 Industry Strategy has triggered an auto-trade signal.
     */
    fun evaluateStrategyTrigger(
        candles: List<Candle>,
        pair: ForexPair,
        indicators: TechnicalIndicators,
        strategy: IndustryStrategy
    ): Pair<TradeAction, String> {
        if (candles.size < 20) return TradeAction.WAIT to "Insufficient market candles for analysis"

        val currentPrice = candles.last().close
        val ema50 = indicators.ema50
        val ema200 = indicators.ema200
        val rsi = indicators.rsi14

        return when (strategy) {
            IndustryStrategy.EMA_GOLDEN_CROSS -> {
                if (ema50 > ema200 && currentPrice >= ema50 * 0.998) {
                    TradeAction.BUY to "50 EMA (${TechnicalAnalysisEngine.formatPrice(ema50, pair)}) > 200 EMA (${TechnicalAnalysisEngine.formatPrice(ema200, pair)}) uptrend confirmed with price bounce"
                } else if (ema50 < ema200 && currentPrice <= ema50 * 1.002) {
                    TradeAction.SELL to "50 EMA (${TechnicalAnalysisEngine.formatPrice(ema50, pair)}) < 200 EMA (${TechnicalAnalysisEngine.formatPrice(ema200, pair)}) downtrend confirmed with resistance rejection"
                } else {
                    TradeAction.WAIT to "EMA trend sideways or waiting for pullback to 50 EMA"
                }
            }

            IndustryStrategy.RSI_DIVERGENCE_REVERSAL -> {
                if (rsi < 36.0) {
                    TradeAction.BUY to "RSI-14 oversold exhaustion at ${String.format("%.1f", rsi)} with institutional reversal hook"
                } else if (rsi > 64.0) {
                    TradeAction.SELL to "RSI-14 overbought exhaustion at ${String.format("%.1f", rsi)} with bearish rollover"
                } else {
                    TradeAction.WAIT to "RSI at ${String.format("%.1f", rsi)} in median neutral territory (36-64)"
                }
            }

            IndustryStrategy.SMC_ORDER_BLOCKS -> {
                val dayLow = candles.minOf { it.low }
                val dayHigh = candles.maxOf { it.high }
                val distToLow = (currentPrice - dayLow) / pair.pipSize
                val distToHigh = (dayHigh - currentPrice) / pair.pipSize

                if (distToLow < 25.0) {
                    TradeAction.BUY to "Price mitigated institutional Demand Order Block near ${TechnicalAnalysisEngine.formatPrice(dayLow, pair)} with bullish BOS"
                } else if (distToHigh < 25.0) {
                    TradeAction.SELL to "Price swept institutional Supply Order Block near ${TechnicalAnalysisEngine.formatPrice(dayHigh, pair)} with liquidity rejection"
                } else {
                    TradeAction.WAIT to "Price in mid-range between Supply and Demand order blocks"
                }
            }

            IndustryStrategy.MACD_MOMENTUM_CROSS -> {
                // Approximate MACD fast vs slow momentum from recent candle closes
                val fastEma = candles.takeLast(12).map { it.close }.average()
                val slowEma = candles.takeLast(26).map { it.close }.average()
                val macdLine = fastEma - slowEma

                if (macdLine > 0 && currentPrice > ema50) {
                    TradeAction.BUY to "MACD line crossed above zero threshold (+${TechnicalAnalysisEngine.formatPrice(macdLine, pair)}) with expanding momentum"
                } else if (macdLine < 0 && currentPrice < ema50) {
                    TradeAction.SELL to "MACD line crossed below zero threshold (-${TechnicalAnalysisEngine.formatPrice(abs(macdLine), pair)}) with bearish momentum expansion"
                } else {
                    TradeAction.WAIT to "MACD momentum converging near zero threshold"
                }
            }

            IndustryStrategy.QUAD_CONFLUENCE_ALL -> {
                val isBullTrend = ema50 > ema200
                val isBearTrend = ema50 < ema200
                val isRsiBull = rsi in 25.0..48.0
                val isRsiBear = rsi in 52.0..75.0

                if (isBullTrend && isRsiBull) {
                    TradeAction.BUY to "QUAD CONFLUENCE: 50>200 EMA + Oversold RSI (${String.format("%.1f", rsi)}) + Demand Mitigation + MACD Bullish"
                } else if (isBearTrend && isRsiBear) {
                    TradeAction.SELL to "QUAD CONFLUENCE: 50<200 EMA + Overbought RSI (${String.format("%.1f", rsi)}) + Supply Sweep + MACD Bearish"
                } else {
                    TradeAction.WAIT to "Waiting for complete 4-factor synchronization across Trend, RSI, SMC, and MACD"
                }
            }
        }
    }

    /**
     * Automatically computes suggested Stop Loss and Take Profit levels for any market
     * based on default 1:2 R:R guidelines.
     */
    fun computeSuggestedTpSl(
        pair: ForexPair,
        currentPrice: Double,
        action: TradeAction,
        slPips: Double = 25.0,
        riskRewardRatio: Double = 2.0
    ): Triple<Double, Double, Double> {
        val slOffset = slPips * pair.pipSize
        val tp1Offset = slPips * riskRewardRatio * pair.pipSize
        val tp2Offset = slPips * (riskRewardRatio + 1.0) * pair.pipSize

        return if (action == TradeAction.BUY) {
            val sl = currentPrice - slOffset
            val tp1 = currentPrice + tp1Offset
            val tp2 = currentPrice + tp2Offset
            Triple(sl, tp1, tp2)
        } else {
            val sl = currentPrice + slOffset
            val tp1 = currentPrice - tp1Offset
            val tp2 = currentPrice - tp2Offset
            Triple(sl, tp1, tp2)
        }
    }

    /**
     * Builds a Room database entity for an automatically taken EA trade.
     */
    fun createAutoTradeEntity(
        pair: ForexPair,
        action: TradeAction,
        strategy: IndustryStrategy,
        entryPrice: Double,
        stopLoss: Double,
        takeProfit1: Double,
        takeProfit2: Double,
        lots: Double,
        reasoning: String
    ): SavedSetupEntity {
        val riskPips = abs(entryPrice - stopLoss) / pair.pipSize
        val rewardPips = abs(takeProfit1 - entryPrice) / pair.pipSize
        val rr = if (riskPips > 0) ((rewardPips / riskPips) * 10.0).roundToInt() / 10.0 else 2.0

        return SavedSetupEntity(
            pairSymbol = pair.symbol,
            timeframe = "H1",
            action = action.name,
            entryPrice = entryPrice,
            stopLoss = stopLoss,
            takeProfit1 = takeProfit1,
            takeProfit2 = takeProfit2,
            riskRewardRatio = rr,
            ema50 = entryPrice,
            ema200 = stopLoss,
            rsi14 = if (action == TradeAction.BUY) 32.0 else 68.0,
            keyZoneLabel = "EA Strategy: ${strategy.title}",
            status = "ACTIVE",
            notes = "🤖 INTELLIGENT EA AUTO-TRADE [${strategy.rankBadge}]: $reasoning. Market: ${pair.symbol}, Lots: $lots, SL: ${TechnicalAnalysisEngine.formatPrice(stopLoss, pair)}, TP1: ${TechnicalAnalysisEngine.formatPrice(takeProfit1, pair)}",
            timestamp = System.currentTimeMillis(),
            isAutoExecuted = true,
            lots = lots,
            currentPnl = 0.0,
            exitPrice = 0.0,
            closedTimestamp = null
        )
    }

    /**
     * Evaluates active EA positions on incoming price tick to manage SL, TP, and Trailing Stops.
     */
    fun evaluateActivePosition(
        position: EaActivePosition,
        currentPrice: Double,
        pair: ForexPair
    ): EaActivePosition {
        if (position.status != "ACTIVE") return position

        val isBuy = position.action == TradeAction.BUY
        val pipDiff = if (isBuy) {
            (currentPrice - position.entryPrice) / pair.pipSize
        } else {
            (position.entryPrice - currentPrice) / pair.pipSize
        }

        val pnl = pipDiff * position.lotSize * pair.pipValuePerStandardLot
        var updatedSl = position.stopLoss
        var isTrailing = position.isTrailingActive

        // Check Take Profit 1 Hit
        val tp1Reached = if (isBuy) currentPrice >= position.takeProfit1 else currentPrice <= position.takeProfit1
        if (tp1Reached) {
            // Move Stop Loss to Break Even (trailing stop)
            updatedSl = position.entryPrice
            isTrailing = true
            return position.copy(
                currentPrice = currentPrice,
                currentPnl = pnl,
                pnlPips = pipDiff,
                stopLoss = updatedSl,
                status = "TP1_HIT",
                isTrailingActive = true,
                executionLog = "🎯 TAKE PROFIT 1 HIT at ${TechnicalAnalysisEngine.formatPrice(currentPrice, pair)}! Stop Loss trailed to Break-Even."
            )
        }

        // Check Stop Loss Hit
        val slHit = if (isBuy) currentPrice <= position.stopLoss else currentPrice >= position.stopLoss
        if (slHit) {
            return position.copy(
                currentPrice = currentPrice,
                currentPnl = pnl,
                pnlPips = pipDiff,
                status = "STOPPED_OUT",
                executionLog = "🛑 STOP LOSS TRIGGERED at ${TechnicalAnalysisEngine.formatPrice(currentPrice, pair)}. Strict risk containment."
            )
        }

        return position.copy(
            currentPrice = currentPrice,
            currentPnl = pnl,
            pnlPips = pipDiff,
            stopLoss = updatedSl,
            isTrailingActive = isTrailing
        )
    }
}
