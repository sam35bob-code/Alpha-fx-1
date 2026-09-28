package com.example.model

enum class NewsImpact(val label: String) {
    HIGH("HIGH"),
    MEDIUM("MED"),
    LOW("LOW")
}

enum class FundamentalSentiment(val label: String) {
    BULLISH("BULLISH"),
    BEARISH("BEARISH"),
    NEUTRAL("NEUTRAL")
}

data class ForexNewsItem(
    val id: String,
    val headline: String,
    val pairSymbol: String, // "US30", "EUR/USD", "GBP/USD", "USD/JPY", "XAU/USD", "GLOBAL"
    val category: String, // "Central Bank / Fed", "Inflation / CPI", "Employment / NFP", "Geopolitical", "Yield Differential"
    val impact: NewsImpact,
    val sentiment: FundamentalSentiment,
    val summary: String,
    val fundamentalContext: String,
    val technicalConfluence: String,
    val source: String, // "Bloomberg Markets", "Federal Reserve Wire", "Reuters FX", "ECB Monitor", "BoJ Wire"
    val timeAgo: String,
    val timestamp: Long = System.currentTimeMillis(),
    val economicData: Map<String, String> = emptyMap(), // e.g. "Actual" to "3.0%", "Forecast" to "2.8%", "Previous" to "2.8%"
    val isBreaking: Boolean = false
)
