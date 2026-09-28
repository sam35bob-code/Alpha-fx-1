package com.example.model

/**
 * Top 4 Trading Industry Strategies supported by the Intelligent EA.
 */
enum class IndustryStrategy(
    val id: String,
    val title: String,
    val rankBadge: String,
    val indicatorsUsed: String,
    val description: String,
    val executionCriteria: String
) {
    EMA_GOLDEN_CROSS(
        id = "EMA_GOLDEN_CROSS",
        title = "50/200 EMA Trend Alignment & Golden Cross",
        rankBadge = "#1 TREND FOLLOWING",
        indicatorsUsed = "50 EMA, 200 EMA, Dynamic Trend Slope",
        description = "Global institutional standard for trend validation. Buys when 50 EMA is above 200 EMA with price pulling back into 50 EMA; Sells when 50 EMA is below 200 EMA with price retesting resistance.",
        executionCriteria = "50 EMA > 200 EMA (Uptrend) or 50 EMA < 200 EMA (Downtrend) + Slope Angle Confirmation"
    ),
    RSI_DIVERGENCE_REVERSAL(
        id = "RSI_DIVERGENCE",
        title = "RSI-14 Multi-Zone Reversal & Divergence",
        rankBadge = "#2 MOMENTUM OSCILLATOR",
        indicatorsUsed = "14-Period RSI, Overbought (70), Oversold (30), 50 Midline",
        description = "World's most utilized oscillator. Detects exhaustion when price enters extreme zones (<30 or >70) and hooks back toward the 50 median, catching institutional turning points.",
        executionCriteria = "RSI < 35 (Oversold Buy) or RSI > 65 (Overbought Sell) + Momentum Reversal Hook"
    ),
    SMC_ORDER_BLOCKS(
        id = "SMC_ORDER_BLOCKS",
        title = "SMC Supply / Demand Order Blocks & BOS",
        rankBadge = "#3 INSTITUTIONAL SMC",
        indicatorsUsed = "Order Blocks, Liquidity Sweeps, Break of Structure (BOS)",
        description = "Smart Money Concepts tracking bank footprints. Identifies unmitigated demand and supply blocks where institutional limit orders trigger explosive directional expansion.",
        executionCriteria = "Price tapping institutional Demand/Supply zone + Clean Break of Structure (BOS)"
    ),
    MACD_MOMENTUM_CROSS(
        id = "MACD_MOMENTUM",
        title = "MACD Zero-Line Crossover & Expanding Volume",
        rankBadge = "#4 HYBRID MOMENTUM",
        indicatorsUsed = "MACD (12, 26, 9), Zero-Line Cross, Volume Histogram",
        description = "Classic trend-momentum hybrid. Executes when MACD fast line crosses above the signal line and traverses the zero threshold with expanding histogram volume.",
        executionCriteria = "MACD Fast/Slow Line Crossover + Positive/Negative Histogram Expansion"
    ),
    QUAD_CONFLUENCE_ALL(
        id = "QUAD_CONFLUENCE",
        title = "Quad-Confluence Elite (All 4 Combined)",
        rankBadge = "★ 88%+ AI CONFLUENCE",
        indicatorsUsed = "50/200 EMA + RSI-14 + SMC Order Blocks + MACD",
        description = "Ultra high-probability filter requiring alignment of all top 4 industry strategies simultaneously before firing an automated execution.",
        executionCriteria = "Full 4-Rule Synchronization across Trend, Momentum, Zones, and MACD"
    )
}

/**
 * Intelligent EA Bot Configuration.
 */
data class EaBotConfig(
    val selectedMarket: ForexPair,
    val selectedStrategy: IndustryStrategy = IndustryStrategy.QUAD_CONFLUENCE_ALL,
    val actionDirection: TradeAction = TradeAction.BUY,
    val stopLossPrice: Double,
    val takeProfitPrice: Double,
    val takeProfit2Price: Double = 0.0,
    val stopLossPips: Double = 25.0,
    val takeProfitPips: Double = 50.0,
    val lotSize: Double = 0.10,
    val isAutoPilotActive: Boolean = true,
    val trailingStopEnabled: Boolean = true,
    val riskPercentage: Double = 1.0,
    val autoCalculateTpSlFromMarket: Boolean = false
)

/**
 * Active EA Position executed automatically.
 */
data class EaActivePosition(
    val id: Long = System.currentTimeMillis(),
    val marketSymbol: String,
    val action: TradeAction,
    val strategy: IndustryStrategy,
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit1: Double,
    val takeProfit2: Double,
    val lotSize: Double,
    val currentPrice: Double,
    val currentPnl: Double,
    val pnlPips: Double,
    val status: String = "ACTIVE", // "ACTIVE", "TP1_HIT", "STOPPED_OUT", "CLOSED"
    val timestamp: Long = System.currentTimeMillis(),
    val isTrailingActive: Boolean = false,
    val executionLog: String = ""
)
