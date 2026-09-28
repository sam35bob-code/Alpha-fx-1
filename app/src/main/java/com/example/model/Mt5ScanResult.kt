package com.example.model

data class Mt5ScanResult(
    val detectedPair: String,
    val detectedTimeframe: String,
    val canTakeTrade: Boolean,
    val tradeAction: TradeAction, // BUY, SELL, WAIT
    val verdictTitle: String,
    val verdictSummary: String,
    val entryPrice: Double?,
    val stopLossPrice: Double?,
    val takeProfit1Price: Double?,
    val takeProfit2Price: Double?,
    val riskRewardRatio: Double?,
    val stopLossPips: Double?,
    val ema50Status: String,
    val ema200Status: String,
    val srZoneStatus: String,
    val rsi14Status: String,
    val riskManagementStatus: String,
    val fullAnalysisMarkdown: String,
    val checklist: List<ChecklistItem>,
    val timestamp: Long = System.currentTimeMillis()
)
