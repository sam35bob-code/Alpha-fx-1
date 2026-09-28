package com.example.model

/**
 * Volatility status levels based on Average Daily Range (ADR) utilization.
 */
enum class AdrVolatilityLevel(val label: String, val isAbnormal: Boolean) {
    COMPRESSION("Compression", false),     // 0% - 60% ADR
    NORMAL("Normal Activity", false),       // 60% - 90% ADR
    EXPANDING("Expanding", false),          // 90% - 110% ADR
    ELEVATED("Elevated Volatility", false), // 110% - 125% ADR
    ABNORMAL_SPIKE("Abnormal Spike", true), // 125% - 150% ADR
    EXTREME_DEVIATION("Extreme Surge", true) // > 150% ADR
}

/**
 * Real-time ADR metrics for a specific currency pair or index.
 */
data class PairAdrMetric(
    val pair: ForexPair,
    val adr14DaysPips: Double,
    val todayRangePips: Double,
    val dayHigh: Double,
    val dayLow: Double,
    val currentPrice: Double,
    val adrPercentage: Double, // (todayRange / adr14) * 100
    val deviationPips: Double,  // todayRange - adr14
    val deviationPercentage: Double, // ((todayRange - adr14) / adr14) * 100
    val volatilityLevel: AdrVolatilityLevel,
    val isAbnormalDeviation: Boolean,
    val speedPipsPerHour: Double,
    val lastUpdated: Long = System.currentTimeMillis(),
    val statusDescription: String,
    val dailyRangesHistory: List<Double> = emptyList() // Last 14 days daily ranges
)

/**
 * Alert event generated when a currency pair displays abnormal ADR volatility deviation.
 */
data class AdrVolatilityAlert(
    val id: String,
    val pairSymbol: String,
    val timestamp: Long,
    val adr14Pips: Double,
    val todayRangePips: Double,
    val adrPercentage: Double,
    val deviationPercentage: Double,
    val volatilityLevel: AdrVolatilityLevel,
    val headline: String,
    val details: String,
    val dayHigh: Double,
    val dayLow: Double,
    val currentPrice: Double,
    val isTest: Boolean = false
)
