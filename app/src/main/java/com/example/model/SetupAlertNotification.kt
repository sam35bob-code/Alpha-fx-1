package com.example.model

data class SetupAlertNotification(
    val id: String,
    val pairSymbol: String,
    val action: TradeAction,
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit1: Double,
    val riskRewardRatio: Double,
    val ema50: Double,
    val ema200: Double,
    val rsi14: Double,
    val rsiStatus: String,
    val keyZoneLabel: String,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String
)
