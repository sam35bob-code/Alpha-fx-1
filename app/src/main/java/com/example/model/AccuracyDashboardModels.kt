package com.example.model

data class TradeRecommendationPoint(
    val id: Long,
    val timestamp: Long,
    val dateLabel: String,
    val pairSymbol: String,
    val timeframe: String,
    val action: String, // BUY, SELL
    val entryPrice: Double,
    val exitPrice: Double,
    val stopLoss: Double,
    val takeProfit1: Double,
    val status: String, // WON_TP1, WON_TP2, STOPPED_OUT, ACTIVE
    val isWin: Boolean,
    val pnl: Double,
    val cumulativeWins: Int,
    val cumulativeTotal: Int,
    val cumulativeAccuracyPercent: Double,
    val rollingAccuracyPercent: Double,
    val cumulativePnl: Double,
    val notes: String = ""
)

data class AssetAccuracyStat(
    val symbol: String,
    val totalTrades: Int,
    val wonTrades: Int,
    val lostTrades: Int,
    val winRatePercent: Double,
    val netPnl: Double
)

data class TimeframeAccuracyStat(
    val timeframe: String,
    val totalTrades: Int,
    val wonTrades: Int,
    val winRatePercent: Double
)

data class AccuracyDashboardData(
    val totalRecommendations: Int = 0,
    val closedRecommendations: Int = 0,
    val wonCount: Int = 0,
    val lostCount: Int = 0,
    val activeCount: Int = 0,
    val overallAccuracyPercent: Double = 0.0,
    val benchmarkTargetPercent: Double = 70.0,
    val profitFactor: Double = 0.0,
    val netPnl: Double = 0.0,
    val buyAccuracyPercent: Double = 0.0,
    val sellAccuracyPercent: Double = 0.0,
    val accuracyOverTime: List<TradeRecommendationPoint> = emptyList(),
    val assetBreakdown: List<AssetAccuracyStat> = emptyList(),
    val timeframeBreakdown: List<TimeframeAccuracyStat> = emptyList()
)
