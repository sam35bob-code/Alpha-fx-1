package com.example.model

enum class CorrelationStrength(val label: String) {
    VERY_STRONG_POSITIVE("Very Strong Positive (+0.80 to +1.00)"),
    MODERATE_POSITIVE("Moderate Positive (+0.40 to +0.79)"),
    WEAK_OR_NEUTRAL("Decoupled / Neutral (-0.39 to +0.39)"),
    MODERATE_NEGATIVE("Moderate Inverse (-0.40 to -0.79)"),
    VERY_STRONG_NEGATIVE("Very Strong Inverse (-0.80 to -1.00)")
}

data class CorrelationPoint(
    val id: Int,
    val timeLabel: String,
    val timestamp: Long,
    val us30Normalized: Double,   // % change from baseline
    val eurusdNormalized: Double, // % change from baseline
    val gbpusdNormalized: Double, // % change from baseline
    val us30RawPrice: Double,
    val eurusdRawPrice: Double,
    val gbpusdRawPrice: Double,
    val rollingCorrEurGbp: Double, // Pearson r (-1.0 to 1.0)
    val rollingCorrUs30Eur: Double,
    val rollingCorrUs30Gbp: Double
)

data class PairCorrelationMetric(
    val pairA: String,
    val pairB: String,
    val coefficient: Double, // Pearson r: -1.0 to +1.0
    val strength: CorrelationStrength,
    val beta: Double, // volatility sensitivity
    val description: String
)

enum class HedgeStrategyType(val label: String) {
    PAIR_HEDGE("Cross-Currency Pair Hedge"),
    BETA_NEUTRAL("Macro Equity / FX Beta Hedge"),
    DIVERGENCE_REVERSION("Statistical Arbitrage / Spread Reversion")
}

data class HedgingOpportunity(
    val id: String,
    val title: String,
    val strategyType: HedgeStrategyType,
    val primaryPair: String,
    val hedgePair: String,
    val correlation: Double,
    val primaryDirection: String,
    val hedgeDirection: String,
    val recommendedRatio: String, // e.g., "1.0 Lot EUR/USD vs 0.85 Lot GBP/USD"
    val rationalization: String,
    val riskReductionPercent: Int
)

enum class RiskSeverity(val label: String) {
    HIGH("HIGH RISK"),
    MEDIUM("MODERATE RISK"),
    LOW("ADVISORY")
}

data class CorrelatedRiskWarning(
    val id: String,
    val severity: RiskSeverity,
    val title: String,
    val pairA: String,
    val pairB: String,
    val correlation: Double,
    val riskMultiplier: Double, // e.g. 1.85x
    val description: String,
    val mitigationAction: String
)

data class MarketCorrelationData(
    val timeHorizon: String, // "24H", "7D", "30D"
    val points: List<CorrelationPoint>,
    val metrics: List<PairCorrelationMetric>,
    val hedgingOpportunities: List<HedgingOpportunity>,
    val correlatedRisks: List<CorrelatedRiskWarning>,
    val lastUpdated: Long = System.currentTimeMillis()
)
