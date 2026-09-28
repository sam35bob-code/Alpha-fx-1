package com.example.analysis

import com.example.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

object MarketCorrelationRepository {

    /**
     * Calculates mathematical Pearson correlation coefficient between two lists of doubles.
     * r = Σ((x - x̄)(y - ȳ)) / sqrt(Σ(x - x̄)² * Σ(y - ȳ)²)
     */
    fun calculatePearson(x: List<Double>, y: List<Double>): Double {
        if (x.size != y.size || x.size < 2) return 0.0
        val n = x.size
        val meanX = x.average()
        val meanY = y.average()

        var numerator = 0.0
        var sumSqX = 0.0
        var sumSqY = 0.0

        for (i in 0 until n) {
            val diffX = x[i] - meanX
            val diffY = y[i] - meanY
            numerator += diffX * diffY
            sumSqX += diffX * diffX
            sumSqY += diffY * diffY
        }

        val denominator = sqrt(sumSqX * sumSqY)
        if (denominator == 0.0) return 0.0
        val r = numerator / denominator
        return Math.max(-1.0, Math.min(1.0, r))
    }

    /**
     * Categorizes Pearson correlation into strength levels
     */
    fun classifyCorrelation(r: Double): CorrelationStrength {
        return when {
            r >= 0.80 -> CorrelationStrength.VERY_STRONG_POSITIVE
            r >= 0.40 -> CorrelationStrength.MODERATE_POSITIVE
            r > -0.40 -> CorrelationStrength.WEAK_OR_NEUTRAL
            r > -0.80 -> CorrelationStrength.MODERATE_NEGATIVE
            else -> CorrelationStrength.VERY_STRONG_NEGATIVE
        }
    }

    /**
     * Generates comprehensive correlation dataset for the 3 target assets: US30, EUR/USD, and GBP/USD.
     */
    fun getCorrelationData(horizon: String = "24H"): MarketCorrelationData {
        val count = when (horizon) {
            "24H" -> 24  // Hourly points
            "7D" -> 28   // 6-hour interval points
            "30D" -> 30  // Daily interval points
            else -> 24
        }

        val now = System.currentTimeMillis()
        val stepMs = when (horizon) {
            "24H" -> 3600_000L
            "7D" -> 6 * 3600_000L
            "30D" -> 24 * 3600_000L
            else -> 3600_000L
        }

        val timeFormat = when (horizon) {
            "24H" -> SimpleDateFormat("HH:mm", Locale.US)
            "7D" -> SimpleDateFormat("EEE HH'h'", Locale.US)
            "30D" -> SimpleDateFormat("MMM dd", Locale.US)
            else -> SimpleDateFormat("HH:mm", Locale.US)
        }

        // Realistic seed base prices
        val baseUs30 = 43850.0
        val baseEurUsd = 1.08450
        val baseGbpUsd = 1.29800

        val us30Returns = mutableListOf<Double>()
        val eurReturns = mutableListOf<Double>()
        val gbpReturns = mutableListOf<Double>()

        val rawUs30 = mutableListOf<Double>()
        val rawEur = mutableListOf<Double>()
        val rawGbp = mutableListOf<Double>()

        // Generate correlated economic cycles
        // Macro shock factor drives both US30 and currencies (Risk-On / Risk-Off)
        // EUR/USD and GBP/USD share heavy common European-USD factor (~0.85 correlation)
        // US30 has moderate positive correlation with currencies during risk-on (~0.55 correlation)
        val rand = Random((42L + horizon.hashCode().toLong()))

        var us30Price = baseUs30 * (1.0 - (count * 0.0004))
        var eurPrice = baseEurUsd * (1.0 - (count * 0.0003))
        var gbpPrice = baseGbpUsd * (1.0 - (count * 0.00035))

        for (i in 0 until count) {
            val macroGlobalRiskFactor = (rand.nextGaussian() * 0.0025)
            val usdIndexShock = (rand.nextGaussian() * 0.0018)

            // Asset specific idiosyncratic noise
            val us30EquityDrift = (rand.nextGaussian() * 0.0020) + (macroGlobalRiskFactor * 1.4)
            val eurFxDrift = (rand.nextGaussian() * 0.0010) + (macroGlobalRiskFactor * 0.7) - (usdIndexShock * 0.9)
            // GBP/USD has higher beta and 85% covariance with EUR/USD
            val gbpFxDrift = (eurFxDrift * 0.88) + (rand.nextGaussian() * 0.0011) - (usdIndexShock * 0.95)

            us30Price *= (1.0 + us30EquityDrift)
            eurPrice *= (1.0 + eurFxDrift)
            gbpPrice *= (1.0 + gbpFxDrift)

            rawUs30.add(us30Price)
            rawEur.add(eurPrice)
            rawGbp.add(gbpPrice)
        }

        val initialUs30 = rawUs30.first()
        val initialEur = rawEur.first()
        val initialGbp = rawGbp.first()

        // Calculate normalized % returns from t0
        for (i in 0 until count) {
            us30Returns.add(((rawUs30[i] - initialUs30) / initialUs30) * 100.0)
            eurReturns.add(((rawEur[i] - initialEur) / initialEur) * 100.0)
            gbpReturns.add(((rawGbp[i] - initialGbp) / initialGbp) * 100.0)
        }

        // Build rolling correlation points
        val points = mutableListOf<CorrelationPoint>()
        val window = 6 // rolling window

        for (i in 0 until count) {
            val ts = now - ((count - 1 - i) * stepMs)
            val dateLabel = timeFormat.format(Date(ts))

            val rollingStart = Math.max(0, i - window + 1)
            val subUs30 = us30Returns.subList(rollingStart, i + 1)
            val subEur = eurReturns.subList(rollingStart, i + 1)
            val subGbp = gbpReturns.subList(rollingStart, i + 1)

            val corrEurGbp = if (subEur.size >= 3) calculatePearson(subEur, subGbp) else 0.86
            val corrUs30Eur = if (subEur.size >= 3) calculatePearson(subUs30, subEur) else 0.54
            val corrUs30Gbp = if (subGbp.size >= 3) calculatePearson(subUs30, subGbp) else 0.62

            points.add(
                CorrelationPoint(
                    id = i + 1,
                    timeLabel = dateLabel,
                    timestamp = ts,
                    us30Normalized = Math.round(us30Returns[i] * 100.0) / 100.0,
                    eurusdNormalized = Math.round(eurReturns[i] * 100.0) / 100.0,
                    gbpusdNormalized = Math.round(gbpReturns[i] * 100.0) / 100.0,
                    us30RawPrice = Math.round(rawUs30[i] * 10.0) / 10.0,
                    eurusdRawPrice = Math.round(rawEur[i] * 100000.0) / 100000.0,
                    gbpusdRawPrice = Math.round(rawGbp[i] * 100000.0) / 100000.0,
                    rollingCorrEurGbp = Math.round(corrEurGbp * 100.0) / 100.0,
                    rollingCorrUs30Eur = Math.round(corrUs30Eur * 100.0) / 100.0,
                    rollingCorrUs30Gbp = Math.round(corrUs30Gbp * 100.0) / 100.0
                )
            )
        }

        // Compute overall period correlations
        val overallEurGbp = Math.round(calculatePearson(eurReturns, gbpReturns) * 100.0) / 100.0
        val overallUs30Eur = Math.round(calculatePearson(us30Returns, eurReturns) * 100.0) / 100.0
        val overallUs30Gbp = Math.round(calculatePearson(us30Returns, gbpReturns) * 100.0) / 100.0

        val metrics = listOf(
            PairCorrelationMetric(
                pairA = "EUR/USD",
                pairB = "GBP/USD",
                coefficient = overallEurGbp,
                strength = classifyCorrelation(overallEurGbp),
                beta = 1.18,
                description = "High positive co-movement driven by USD denominator and shared transatlantic flows. Redundant risk occurs when both are traded in the same direction."
            ),
            PairCorrelationMetric(
                pairA = "US30",
                pairB = "EUR/USD",
                coefficient = overallUs30Eur,
                strength = classifyCorrelation(overallUs30Eur),
                beta = 0.68,
                description = "Moderate risk-on co-movement. When Wall Street rallies under soft-landing easing, the USD often weakens, driving EUR/USD upward."
            ),
            PairCorrelationMetric(
                pairA = "US30",
                pairB = "GBP/USD",
                coefficient = overallUs30Gbp,
                strength = classifyCorrelation(overallUs30Gbp),
                beta = 0.74,
                description = "Moderate-to-strong risk appetite proxy. Sterling demonstrates elevated beta volatility to Wall Street momentum swings."
            )
        )

        // Hedging Opportunities based on market conditions & divergences
        val hedgingOpportunities = listOf(
            HedgingOpportunity(
                id = "HEDGE-01",
                title = "EUR/USD vs GBP/USD Relative Strength Pair Hedge",
                strategyType = HedgeStrategyType.PAIR_HEDGE,
                primaryPair = "EUR/USD",
                hedgePair = "GBP/USD",
                correlation = overallEurGbp,
                primaryDirection = "BUY",
                hedgeDirection = "SELL",
                recommendedRatio = "1.00 Lot EUR/USD vs 0.85 Lot GBP/USD",
                rationalization = "With EUR/USD and GBP/USD correlation at ${overallEurGbp}, trading long EUR/USD while shorting GBP/USD neutralizes systemic USD risk and isolates European relative economic outperformance.",
                riskReductionPercent = 42
            ),
            HedgingOpportunity(
                id = "HEDGE-02",
                title = "US30 Equity Long with EUR/USD Downside Tail Hedge",
                strategyType = HedgeStrategyType.BETA_NEUTRAL,
                primaryPair = "US30",
                hedgePair = "EUR/USD",
                correlation = overallUs30Eur,
                primaryDirection = "BUY",
                hedgeDirection = "SELL (Inverse Hedge)",
                recommendedRatio = "0.50 Lot US30 vs 0.30 Lot EUR/USD",
                rationalization = "In sudden risk-off market shocks, rapid flight-to-safety into USD spikes the dollar and causes US equities to correct. A partial short EUR/USD absorbs liquidity drawdown.",
                riskReductionPercent = 35
            ),
            HedgingOpportunity(
                id = "HEDGE-03",
                title = "Cable Divergence Arbitrage (Spread Reversion)",
                strategyType = HedgeStrategyType.DIVERGENCE_REVERSION,
                primaryPair = "GBP/USD",
                hedgePair = "EUR/USD",
                correlation = overallEurGbp,
                primaryDirection = "BUY Over-sold",
                hedgeDirection = "SELL Over-bought",
                recommendedRatio = "Equal Cash Volatility Sizing (0.80 Lot each)",
                rationalization = "Rolling correlation temporarily dipped below historical mean (+0.70). Statistical spread reversion generates non-directional alpha as the pairs reconnect.",
                riskReductionPercent = 50
            )
        )

        // Correlated Risk Warnings
        val correlatedRisks = listOf(
            CorrelatedRiskWarning(
                id = "RISK-01",
                severity = RiskSeverity.HIGH,
                title = "Overlapping Long USD Exposure (EUR/USD + GBP/USD)",
                pairA = "EUR/USD",
                pairB = "GBP/USD",
                correlation = overallEurGbp,
                riskMultiplier = 1.88,
                description = "Opening concurrent BUY or concurrent SELL positions on EUR/USD and GBP/USD compounds your portfolio risk by nearly 2x due to high co-dependence.",
                mitigationAction = "Halve lot sizes (0.5x each) or select only the higher-conviction setup to avoid doubling drawdown."
            ),
            CorrelatedRiskWarning(
                id = "RISK-02",
                severity = RiskSeverity.MEDIUM,
                title = "Risk-On Equity & Currency Double Exposure (US30 + GBP/USD)",
                pairA = "US30",
                pairB = "GBP/USD",
                correlation = overallUs30Gbp,
                riskMultiplier = 1.45,
                description = "Long positions on both US30 and GBP/USD create an unhedged Risk-On bias. An unexpected Fed hawkish pivot will trigger synchronized stop-outs on both trades.",
                mitigationAction = "Stagger entries across different trading sessions or utilize trailing stops on the secondary asset."
            ),
            CorrelatedRiskWarning(
                id = "RISK-03",
                severity = RiskSeverity.LOW,
                title = "False Diversification Alert",
                pairA = "US30",
                pairB = "EUR/USD",
                correlation = overallUs30Eur,
                riskMultiplier = 1.25,
                description = "Traders often assume trading US30 and EUR/USD provides portfolio diversification. During macro news (CPI, FOMC), their correlation spikes above +0.75.",
                mitigationAction = "Check the Economic Calendar before holding simultaneous open positions during Tier 1 data releases."
            )
        )

        return MarketCorrelationData(
            timeHorizon = horizon,
            points = points,
            metrics = metrics,
            hedgingOpportunities = hedgingOpportunities,
            correlatedRisks = correlatedRisks
        )
    }
}
