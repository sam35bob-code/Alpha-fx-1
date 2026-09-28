package com.example.model

data class AutoTraderBotState(
    val isEnabled: Boolean = true,
    val demoBalance: Double = 10000.0,
    val realizedProfit: Double = 0.0,
    val floatingProfit: Double = 0.0,
    val maxConcurrentTrades: Int = 2,
    val riskPerTradePercent: Double = 1.0,
    val trailingStopEnabled: Boolean = true,
    val totalTradesExecuted: Int = 0,
    val winningTradesCount: Int = 0,
    val losingTradesCount: Int = 0,
    val lastBotActionMessage: String = "Bot active in 100% Risk-Free Virtual Mode. Scanning charts for 4-rule confluence...",
    val lastActionTimestamp: Long = System.currentTimeMillis()
) {
    val totalEquity: Double get() = demoBalance + realizedProfit + floatingProfit
    val totalPnl: Double get() = realizedProfit + floatingProfit
    val pnlPercent: Double get() = if (demoBalance > 0) (totalPnl / demoBalance) * 100.0 else 0.0
}
