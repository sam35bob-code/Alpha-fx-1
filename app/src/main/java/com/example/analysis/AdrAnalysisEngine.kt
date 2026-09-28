package com.example.analysis

import com.example.model.*
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

object AdrAnalysisEngine {

    /**
     * Standard baseline 14-day Average Daily Range (in pips or points) for major pairs.
     */
    private val BASELINE_ADR_14_DAYS = mapOf(
        "EUR/USD" to 78.0,   // ~78 pips
        "GBP/USD" to 105.0,  // ~105 pips
        "USD/JPY" to 118.0,  // ~118 pips
        "US30" to 460.0,     // ~460 points
        "XAU/USD" to 420.0   // ~42.0 USD ($420 points / 0.10)
    )

    /**
     * Generates a 14-day history of daily ranges centered around the baseline ADR.
     */
    fun getSynthetic14DayDailyRanges(pairSymbol: String): List<Double> {
        val base = BASELINE_ADR_14_DAYS[pairSymbol] ?: 80.0
        val multipliers = listOf(
            0.88, 1.05, 0.94, 1.15, 0.78,
            1.22, 0.98, 1.02, 0.85, 1.10,
            0.92, 1.06, 0.95, 1.08
        )
        return multipliers.map { (base * it * 10.0).roundToInt() / 10.0 }
    }

    /**
     * Computes the 14-day Average Daily Range from daily candle ranges or historical list.
     */
    fun calculateAdr14(dailyRanges: List<Double>, pairSymbol: String): Double {
        if (dailyRanges.isEmpty()) {
            return BASELINE_ADR_14_DAYS[pairSymbol] ?: 80.0
        }
        val sum = dailyRanges.takeLast(14).sum()
        val count = dailyRanges.takeLast(14).size
        val avg = sum / count
        return (avg * 10.0).roundToInt() / 10.0
    }

    /**
     * Evaluates current ADR utilization and volatility metrics for a given pair.
     */
    fun computePairAdrMetric(
        pair: ForexPair,
        currentPrice: Double,
        dayHigh: Double,
        dayLow: Double,
        customDailyRanges: List<Double>? = null
    ): PairAdrMetric {
        val historicalRanges = customDailyRanges ?: getSynthetic14DayDailyRanges(pair.symbol)
        val adr14 = calculateAdr14(historicalRanges, pair.symbol)

        val rawRange = abs(dayHigh - dayLow)
        val todayRangePips = (rawRange / pair.pipSize * 10.0).roundToInt() / 10.0

        val adrPercentage = if (adr14 > 0) {
            ((todayRangePips / adr14) * 1000.0).roundToInt() / 10.0
        } else 100.0

        val deviationPips = ((todayRangePips - adr14) * 10.0).roundToInt() / 10.0
        val deviationPercentage = if (adr14 > 0) {
            (((todayRangePips - adr14) / adr14) * 1000.0).roundToInt() / 10.0
        } else 0.0

        val volatilityLevel = when {
            adrPercentage >= 150.0 -> AdrVolatilityLevel.EXTREME_DEVIATION
            adrPercentage >= 125.0 -> AdrVolatilityLevel.ABNORMAL_SPIKE
            adrPercentage >= 110.0 -> AdrVolatilityLevel.ELEVATED
            adrPercentage >= 90.0 -> AdrVolatilityLevel.EXPANDING
            adrPercentage >= 60.0 -> AdrVolatilityLevel.NORMAL
            else -> AdrVolatilityLevel.COMPRESSION
        }

        val isAbnormal = volatilityLevel.isAbnormal

        val statusDescription = when (volatilityLevel) {
            AdrVolatilityLevel.EXTREME_DEVIATION ->
                "⚠️ EXTREME VOLATILITY: Today's range ($todayRangePips pips) has exceeded 14-day ADR ($adr14 pips) by +$deviationPercentage%."
            AdrVolatilityLevel.ABNORMAL_SPIKE ->
                "⚡ ABNORMAL SPIKE: High intraday expansion at $adrPercentage% of typical 14-day ADR."
            AdrVolatilityLevel.ELEVATED ->
                "📈 ELEVATED: Market approaching maximum average daily expectation ($adrPercentage% ADR)."
            AdrVolatilityLevel.EXPANDING ->
                "⚖️ EXPANDING: Healthy session momentum within typical range parameters."
            AdrVolatilityLevel.NORMAL ->
                "🔹 NORMAL: Typical trading volume and range distribution."
            AdrVolatilityLevel.COMPRESSION ->
                "💤 COMPRESSION: Tight range preceding potential volatility expansion."
        }

        // Intraday velocity estimate
        val speedPipsPerHour = ((todayRangePips / 8.5) * 10.0).roundToInt() / 10.0

        return PairAdrMetric(
            pair = pair,
            adr14DaysPips = adr14,
            todayRangePips = todayRangePips,
            dayHigh = dayHigh,
            dayLow = dayLow,
            currentPrice = currentPrice,
            adrPercentage = adrPercentage,
            deviationPips = deviationPips,
            deviationPercentage = deviationPercentage,
            volatilityLevel = volatilityLevel,
            isAbnormalDeviation = isAbnormal,
            speedPipsPerHour = speedPipsPerHour,
            statusDescription = statusDescription,
            dailyRangesHistory = historicalRanges
        )
    }

    /**
     * Checks if the metric exceeds the abnormal threshold and builds an alert model if triggered.
     */
    fun checkAbnormalDeviation(
        metric: PairAdrMetric,
        thresholdPercentage: Double = 125.0
    ): AdrVolatilityAlert? {
        if (metric.adrPercentage < thresholdPercentage) {
            return null
        }

        val headline = "⚠️ ABNORMAL ADR VOLATILITY: ${metric.pair.symbol} at ${String.format("%.1f", metric.adrPercentage)}%"
        val details = buildString {
            append("${metric.pair.symbol} daily range (${metric.todayRangePips} pips) has deviated +${String.format("%.1f", metric.deviationPercentage)}% ")
            append("beyond the 14-day ADR baseline (${metric.adr14DaysPips} pips). ")
            append("High volatility expansion detected between Day Low (${TechnicalAnalysisEngine.formatPrice(metric.dayLow, metric.pair)}) ")
            append("and Day High (${TechnicalAnalysisEngine.formatPrice(metric.dayHigh, metric.pair)}).")
        }

        return AdrVolatilityAlert(
            id = UUID.randomUUID().toString(),
            pairSymbol = metric.pair.symbol,
            timestamp = System.currentTimeMillis(),
            adr14Pips = metric.adr14DaysPips,
            todayRangePips = metric.todayRangePips,
            adrPercentage = metric.adrPercentage,
            deviationPercentage = metric.deviationPercentage,
            volatilityLevel = metric.volatilityLevel,
            headline = headline,
            details = details,
            dayHigh = metric.dayHigh,
            dayLow = metric.dayLow,
            currentPrice = metric.currentPrice,
            isTest = false
        )
    }
}
