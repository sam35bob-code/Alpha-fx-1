package com.example.model

enum class TradeAction(val label: String) {
    BUY("LONG / BUY"),
    SELL("SHORT / SELL"),
    WAIT("NO TRADE - PATIENCE")
}

data class ChecklistItem(
    val title: String,
    val description: String,
    val isMet: Boolean,
    val valueLabel: String
)

data class TradeSetup(
    val pairSymbol: String,
    val timeframe: Timeframe,
    val action: TradeAction,
    val isValid: Boolean, // True ONLY when all 4 strict criteria pass!
    val statusSummary: String,
    val detailedReasoning: String,
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit1: Double, // 1:2 R:R
    val takeProfit2: Double, // 1:3+ R:R
    val riskPipsOrPoints: Double,
    val rewardPipsOrPoints: Double,
    val riskRewardRatio: Double,
    val ema50: Double,
    val ema200: Double,
    val rsi14: Double,
    val keyZoneLabel: String,
    val checklist: List<ChecklistItem>,
    val timestamp: Long = System.currentTimeMillis()
)
