package com.example

import com.example.analysis.AdrAnalysisEngine
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class AdrVolatilityMonitorTest {

    @Test
    fun `test ADR 14 calculation with synthetic daily ranges`() {
        val eurusdRanges = AdrAnalysisEngine.getSynthetic14DayDailyRanges("EUR/USD")
        assertEquals(14, eurusdRanges.size)

        val adr14 = AdrAnalysisEngine.calculateAdr14(eurusdRanges, "EUR/USD")
        assertTrue("ADR should be positive and close to ~78 pips", adr14 in 70.0..85.0)

        val us30Ranges = AdrAnalysisEngine.getSynthetic14DayDailyRanges("US30")
        val us30Adr = AdrAnalysisEngine.calculateAdr14(us30Ranges, "US30")
        assertTrue("US30 ADR should be around ~460 points", us30Adr in 400.0..500.0)
    }

    @Test
    fun `test normal ADR volatility calculation`() {
        val pair = ForexPair.ALL_PAIRS.first { it.symbol == "EUR/USD" }
        // Baseline ~ 78 pips. 55 pips range = ~70% ADR (Normal)
        val metric = AdrAnalysisEngine.computePairAdrMetric(
            pair = pair,
            currentPrice = 1.0845,
            dayHigh = 1.0875,
            dayLow = 1.0820
        )

        assertEquals("EUR/USD", metric.pair.symbol)
        assertEquals(55.0, metric.todayRangePips, 0.5)
        assertEquals(AdrVolatilityLevel.NORMAL, metric.volatilityLevel)
        assertFalse("Normal volatility should not be flagged abnormal", metric.isAbnormalDeviation)

        val alert = AdrAnalysisEngine.checkAbnormalDeviation(metric, thresholdPercentage = 125.0)
        assertNull("Normal volatility should not produce an alert", alert)
    }

    @Test
    fun `test abnormal volatility deviation detection and alert generation`() {
        val pair = ForexPair.ALL_PAIRS.first { it.symbol == "EUR/USD" }
        // Day range of 115 pips when 14d ADR is ~78 pips -> ~147% ADR -> Abnormal Spike
        val highRange = 0.0115 // 115 pips
        val metric = AdrAnalysisEngine.computePairAdrMetric(
            pair = pair,
            currentPrice = 1.0845,
            dayHigh = 1.0845 + (highRange / 2.0),
            dayLow = 1.0845 - (highRange / 2.0)
        )

        assertEquals(115.0, metric.todayRangePips, 0.5)
        assertTrue("ADR% should exceed 140%", metric.adrPercentage > 140.0)
        assertTrue("Deviation% should be positive and significant", metric.deviationPercentage > 40.0)
        assertTrue("Volatility level should be abnormal", metric.volatilityLevel.isAbnormal)
        assertTrue(metric.isAbnormalDeviation)

        val alert = AdrAnalysisEngine.checkAbnormalDeviation(metric, thresholdPercentage = 125.0)
        assertNotNull("Alert should be generated when exceeding 125%", alert)
        assertEquals("EUR/USD", alert!!.pairSymbol)
        assertTrue(alert.headline.contains("ABNORMAL ADR VOLATILITY"))
        assertTrue(alert.details.contains("115.0 pips"))
    }

    @Test
    fun `test extreme volatility deviation detection`() {
        val pair = ForexPair.ALL_PAIRS.first { it.symbol == "US30" }
        // US30 baseline ~ 460 points. Range of 780 points = ~169% ADR -> Extreme Deviation
        val metric = AdrAnalysisEngine.computePairAdrMetric(
            pair = pair,
            currentPrice = 43850.0,
            dayHigh = 44240.0,
            dayLow = 43460.0
        )

        assertEquals(780.0, metric.todayRangePips, 1.0)
        assertEquals(AdrVolatilityLevel.EXTREME_DEVIATION, metric.volatilityLevel)
        assertTrue(metric.isAbnormalDeviation)

        val alert = AdrAnalysisEngine.checkAbnormalDeviation(metric, thresholdPercentage = 120.0)
        assertNotNull(alert)
        assertEquals("US30", alert!!.pairSymbol)
        assertEquals(AdrVolatilityLevel.EXTREME_DEVIATION, alert.volatilityLevel)
    }

    @Test
    fun `test compression volatility detection`() {
        val pair = ForexPair.ALL_PAIRS.first { it.symbol == "GBP/USD" }
        // GBP/USD baseline ~ 105 pips. Range of 35 pips = ~33% ADR -> Compression
        val metric = AdrAnalysisEngine.computePairAdrMetric(
            pair = pair,
            currentPrice = 1.2980,
            dayHigh = 1.2995,
            dayLow = 1.2960
        )

        assertEquals(35.0, metric.todayRangePips, 0.5)
        assertEquals(AdrVolatilityLevel.COMPRESSION, metric.volatilityLevel)
        assertFalse(metric.isAbnormalDeviation)
    }
}
