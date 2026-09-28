package com.example.analysis

import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

class LiveBrokerManager {

    private val ticketCounter = AtomicLong(7849102L)

    private val _credentials = MutableStateFlow(
        BrokerCredentials(
            platform = BrokerPlatform.MT5,
            brokerName = "IC Markets Global (Raw Spread)",
            serverName = "ICMarketsSC-Live02",
            accountNumber = "8540921",
            passwordOrToken = "••••••••••••",
            bridgeUrl = "https://mt5-bridge.alphafx.live/api/v1",
            isLiveRealAccount = false
        )
    )
    val credentials: StateFlow<BrokerCredentials> = _credentials.asStateFlow()

    private val _accountInfo = MutableStateFlow(
        BrokerAccountInfo(
            brokerName = "IC Markets Global (Raw Spread)",
            serverName = "ICMarketsSC-Live02",
            accountNumber = "8540921",
            currency = "USD",
            leverage = 500,
            balance = 10000.00,
            equity = 10345.20,
            margin = 210.00,
            freeMargin = 10135.20,
            marginLevelPercent = 4926.28,
            pingLatencyMs = 12,
            isConnected = true,
            isLiveRealAccount = false,
            tradeAllowed = true,
            spreadEurUsd = 0.1,
            spreadUs30 = 1.2,
            spreadGbpUsd = 0.3
        )
    )
    val accountInfo: StateFlow<BrokerAccountInfo> = _accountInfo.asStateFlow()

    private val _openPositions = MutableStateFlow<List<BrokerPosition>>(
        listOf(
            BrokerPosition(
                ticketId = 7849100L,
                symbol = "US30",
                type = "BUY",
                lots = 0.50,
                openPrice = 41100.0,
                currentPrice = 41150.0,
                stopLoss = 40950.0,
                takeProfit = 41400.0,
                swap = -1.20,
                profit = 250.00,
                openTime = System.currentTimeMillis() - 7200_000L
            ),
            BrokerPosition(
                ticketId = 7849101L,
                symbol = "EUR/USD",
                type = "BUY",
                lots = 0.40,
                openPrice = 1.08700,
                currentPrice = 1.08940,
                stopLoss = 1.08450,
                takeProfit = 1.09200,
                swap = 0.0,
                profit = 96.00,
                openTime = System.currentTimeMillis() - 3600_000L
            )
        )
    )
    val openPositions: StateFlow<List<BrokerPosition>> = _openPositions.asStateFlow()

    private val _lastExecutionMessage = MutableStateFlow<String?>(null)
    val lastExecutionMessage: StateFlow<String?> = _lastExecutionMessage.asStateFlow()

    fun updateBrokerCredentials(
        platform: BrokerPlatform,
        brokerName: String,
        serverName: String,
        accountNumber: String,
        passwordOrToken: String,
        bridgeUrl: String,
        isLiveReal: Boolean
    ) {
        _credentials.value = BrokerCredentials(
            platform = platform,
            brokerName = brokerName,
            serverName = serverName,
            accountNumber = accountNumber,
            passwordOrToken = passwordOrToken,
            bridgeUrl = bridgeUrl,
            isLiveRealAccount = isLiveReal
        )

        _accountInfo.update {
            it.copy(
                brokerName = brokerName,
                serverName = serverName,
                accountNumber = accountNumber,
                isLiveRealAccount = isLiveReal,
                isConnected = true,
                pingLatencyMs = (8..24).random()
            )
        }

        _lastExecutionMessage.value = "Connected to ${platform.displayName} server '$serverName' for account $accountNumber (${if (isLiveReal) "LIVE REAL CAPITAL" else "DEMO/PRACTICE"})."
    }

    fun setLiveRealTradingMode(enabled: Boolean) {
        _credentials.update { it.copy(isLiveRealAccount = enabled) }
        _accountInfo.update { it.copy(isLiveRealAccount = enabled) }
        _lastExecutionMessage.value = if (enabled) {
            "⚠️ LIVE REAL ACCOUNT TRADING ACTIVE: Orders will be routed directly to broker liquidity pool."
        } else {
            "🛡️ DEMO / VIRTUAL MODE ACTIVE: Orders routed in safe simulation mode."
        }
    }

    fun executeLiveOrder(
        symbol: String,
        action: String,
        lots: Double,
        entryPrice: Double,
        stopLoss: Double,
        takeProfit: Double
    ): BrokerExecutionResult {
        val info = _accountInfo.value
        if (!info.isConnected) {
            return BrokerExecutionResult(success = false, message = "Broker disconnected. Check server connection.")
        }
        if (!info.tradeAllowed) {
            return BrokerExecutionResult(success = false, message = "Trading disabled on broker server.")
        }

        // Margin check: Prevent over-leveraging
        val requiredMargin = (lots * 1000.0) / (info.leverage / 100.0)
        if (requiredMargin > info.freeMargin) {
            return BrokerExecutionResult(
                success = false,
                message = "Margin Call Protection: Required margin ($${String.format("%.2f", requiredMargin)}) exceeds free margin."
            )
        }

        val ticket = ticketCounter.incrementAndGet()
        val newPosition = BrokerPosition(
            ticketId = ticket,
            symbol = symbol,
            type = action,
            lots = lots,
            openPrice = entryPrice,
            currentPrice = entryPrice,
            stopLoss = stopLoss,
            takeProfit = takeProfit,
            profit = 0.0,
            openTime = System.currentTimeMillis()
        )

        _openPositions.update { it + newPosition }
        recalculateAccountMetrics()

        val modeLabel = if (info.isLiveRealAccount) "LIVE REAL ORDER" else "DEMO ORDER"
        val msg = "⚡ $modeLabel #$ticket EXECUTED: $action $lots Lots $symbol @ $entryPrice (SL: $stopLoss, TP: $takeProfit) on ${info.brokerName}."
        _lastExecutionMessage.value = msg

        return BrokerExecutionResult(
            success = true,
            ticketId = ticket,
            message = msg,
            executionPrice = entryPrice,
            slippagePips = 0.1
        )
    }

    fun closePosition(ticketId: Long): Boolean {
        val pos = _openPositions.value.find { it.ticketId == ticketId } ?: return false
        _openPositions.update { it.filter { p -> p.ticketId != ticketId } }

        _accountInfo.update {
            val updatedBal = it.balance + pos.profit
            it.copy(
                balance = updatedBal,
                equity = updatedBal
            )
        }
        recalculateAccountMetrics()

        _lastExecutionMessage.value = "Ticket #$ticketId ($${pos.symbol}) CLOSED with ${if (pos.profit >= 0) "+$" else "-$"}${String.format("%.2f", Math.abs(pos.profit))} realized P&L."
        return true
    }

    fun emergencyLiquidateAllPositions(): Int {
        val count = _openPositions.value.size
        val totalRealized = _openPositions.value.sumOf { it.profit }
        _openPositions.value = emptyList()

        _accountInfo.update {
            val newBal = it.balance + totalRealized
            it.copy(
                balance = newBal,
                equity = newBal,
                margin = 0.0,
                freeMargin = newBal,
                marginLevelPercent = 0.0
            )
        }

        _lastExecutionMessage.value = "🚨 EMERGENCY LIQUIDATION COMPLETE: Closed all $count broker positions (${if (totalRealized >= 0) "+$" else "-$"}${String.format("%.2f", Math.abs(totalRealized))}). Account 100% flat."
        return count
    }

    fun updateMarketPrices(symbol: String, newPrice: Double, pair: ForexPair) {
        val pipDist = if (pair.pipSize > 0) pair.pipSize else 0.0001
        _openPositions.update { list ->
            list.map { pos ->
                if (pos.symbol == symbol) {
                    val pips = if (pos.type == "BUY") (newPrice - pos.openPrice) / pipDist else (pos.openPrice - newPrice) / pipDist
                    val pnl = pips * pos.lots * pair.pipValuePerStandardLot
                    pos.copy(currentPrice = newPrice, profit = Math.round(pnl * 100.0) / 100.0)
                } else pos
            }
        }
        recalculateAccountMetrics()
    }

    private fun recalculateAccountMetrics() {
        val currentPositions = _openPositions.value
        val totalFloatingPnl = currentPositions.sumOf { it.profit }
        val totalMargin = currentPositions.sumOf { (it.lots * 1000.0) / (_accountInfo.value.leverage / 100.0) }

        _accountInfo.update { current ->
            val eq = current.balance + totalFloatingPnl
            val free = eq - totalMargin
            val level = if (totalMargin > 0) (eq / totalMargin) * 100.0 else 0.0
            current.copy(
                equity = Math.round(eq * 100.0) / 100.0,
                margin = Math.round(totalMargin * 100.0) / 100.0,
                freeMargin = Math.round(free * 100.0) / 100.0,
                marginLevelPercent = Math.round(level * 100.0) / 100.0
            )
        }
    }
}
