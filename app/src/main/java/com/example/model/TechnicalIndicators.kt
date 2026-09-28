package com.example.model

enum class TrendDirection(val label: String) {
    STRONG_BULLISH("Strong Bullish (EMA 50 > EMA 200 & Price Above)"),
    BULLISH("Bullish (Price Above EMA 50)"),
    STRONG_BEARISH("Strong Bearish (EMA 50 < EMA 200 & Price Below)"),
    BEARISH("Bearish (Price Below EMA 50)"),
    RANGING("Consolidation / Sideways Compression")
}

enum class RsiStatus(val label: String, val isConfirmation: Boolean) {
    OVERSOLD("Oversold (< 30) - Prime Bullish Buy Reversal", true),
    BULLISH_REVERSAL("Bullish Reversal (RSI hook up from < 35)", true),
    OVERBOUGHT("Overbought (> 70) - Prime Bearish Sell Reversal", true),
    BEARISH_REVERSAL("Bearish Reversal (RSI hook down from > 65)", true),
    NEUTRAL("Neutral (35 - 65) - Waiting for Extreme Signal", false)
}

data class PriceZone(
    val type: ZoneType,
    val top: Double,
    val bottom: Double,
    val label: String,
    val touches: Int = 3
) {
    val midpoint: Double get() = (top + bottom) / 2.0
    fun contains(price: Double): Boolean = price in bottom..top
}

enum class ZoneType {
    SUPPORT, RESISTANCE
}

data class TechnicalIndicators(
    val currentPrice: Double,
    val ema50: Double,
    val ema200: Double,
    val rsi14: Double,
    val trendDirection: TrendDirection,
    val nearestSupport: PriceZone?,
    val nearestResistance: PriceZone?,
    val isTestingSupportZone: Boolean,
    val isTestingResistanceZone: Boolean,
    val rsiStatus: RsiStatus,
    val historicalEma50: List<Double>,
    val historicalEma200: List<Double>,
    val historicalRsi14: List<Double>
)
