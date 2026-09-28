package com.example.model

enum class BrokerPlatform(val displayName: String, val defaultServer: String, val protocol: String) {
    MT5("MetaTrader 5 (MT5)", "MetaQuotes-Demo", "MT5 REST / WebAPI"),
    MT4("MetaTrader 4 (MT4)", "ICMarketsSC-Live03", "MT4 Manager API"),
    CTRADER("cTrader (Spotware)", "Spotware-OpenAPI-Live", "Spotware Open API v2"),
    OANDA("OANDA (v20 REST)", "api-fxtrade.oanda.com", "v20 REST API"),
    PROPFIRM_FTMO("FTMO / Prop Firm", "FTMO-Server2", "Prop Firm Bridge"),
    INTERACTIVE_BROKERS("Interactive Brokers", "gw.interactivebrokers.com", "Client Portal API")
}

data class BrokerCredentials(
    val platform: BrokerPlatform = BrokerPlatform.MT5,
    val brokerName: String = "IC Markets Global",
    val serverName: String = "ICMarketsSC-Live02",
    val accountNumber: String = "8540921",
    val passwordOrToken: String = "••••••••••••",
    val bridgeUrl: String = "https://bridge.alphafx.live/api/v1",
    val isLiveRealAccount: Boolean = false // false = Demo / Paper, true = Real Capital
)

data class BrokerAccountInfo(
    val brokerName: String = "IC Markets Global",
    val serverName: String = "ICMarketsSC-Live02",
    val accountNumber: String = "8540921",
    val currency: String = "USD",
    val leverage: Int = 100, // 1:100
    val balance: Double = 10000.00,
    val equity: Double = 10245.80,
    val margin: Double = 340.00,
    val freeMargin: Double = 9905.80,
    val marginLevelPercent: Double = 3013.47,
    val pingLatencyMs: Int = 14,
    val isConnected: Boolean = true,
    val isLiveRealAccount: Boolean = false,
    val tradeAllowed: Boolean = true,
    val spreadEurUsd: Double = 0.2, // pips
    val spreadUs30: Double = 1.6,   // points
    val spreadGbpUsd: Double = 0.4  // pips
)

data class BrokerPosition(
    val ticketId: Long,
    val symbol: String,
    val type: String, // BUY, SELL
    val lots: Double,
    val openPrice: Double,
    val currentPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val swap: Double = 0.0,
    val profit: Double,
    val openTime: Long = System.currentTimeMillis()
)

data class BrokerExecutionResult(
    val success: Boolean,
    val ticketId: Long = 0,
    val message: String,
    val executionPrice: Double = 0.0,
    val slippagePips: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
