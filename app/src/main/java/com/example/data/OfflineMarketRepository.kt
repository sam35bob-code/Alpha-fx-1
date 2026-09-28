package com.example.data

import com.example.model.Candle
import com.example.model.TradeSetup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository providing high-level offline data persistence and synchronization
 * for historical price charts and trade signals using Room.
 */
class OfflineMarketRepository(
    private val historicalPriceDao: HistoricalPriceDao,
    private val tradeSignalDao: TradeSignalDao
) {
    /**
     * Persists a batch of historical price candles to the local database for offline usage.
     */
    suspend fun saveHistoricalPrices(pairSymbol: String, timeframe: String, candles: List<Candle>) {
        if (candles.isEmpty()) return
        val entities = candles.map { candle ->
            HistoricalPriceEntity.fromDomainCandle(candle, pairSymbol, timeframe)
        }
        historicalPriceDao.insertPrices(entities)
    }

    /**
     * Retrieves cached historical price candles sorted chronologically (oldest to newest).
     */
    suspend fun getHistoricalPrices(pairSymbol: String, timeframe: String): List<Candle> {
        return historicalPriceDao.getHistoricalPricesList(pairSymbol, timeframe)
            .map { it.toDomainCandle() }
    }

    /**
     * Observes cached historical price candles as a Flow.
     */
    fun observeHistoricalPrices(pairSymbol: String, timeframe: String): Flow<List<Candle>> {
        return historicalPriceDao.getHistoricalPrices(pairSymbol, timeframe)
            .map { list -> list.map { it.toDomainCandle() } }
    }

    /**
     * Checks if local cached candles exist for the given pair and timeframe.
     */
    suspend fun hasOfflinePrices(pairSymbol: String, timeframe: String): Boolean {
        return historicalPriceDao.getPriceCount(pairSymbol, timeframe) > 0
    }

    /**
     * Saves a trade setup / signal into the local Room database for offline access.
     */
    suspend fun saveTradeSignal(
        setup: TradeSetup,
        signalType: String = "AI_CONFLUENCE",
        confidenceScore: Double = 0.85
    ): Long {
        val entity = TradeSignalEntity.fromTradeSetup(
            setup = setup,
            signalType = signalType,
            confidenceScore = confidenceScore
        )
        return tradeSignalDao.insertSignal(entity)
    }

    /**
     * Observes all trade signals in descending chronological order.
     */
    fun observeAllSignals(): Flow<List<TradeSignalEntity>> {
        return tradeSignalDao.getAllSignals()
    }

    /**
     * Observes trade signals for a specific currency pair / index.
     */
    fun observeSignalsForPair(pairSymbol: String): Flow<List<TradeSignalEntity>> {
        return tradeSignalDao.getSignalsForPair(pairSymbol)
    }

    /**
     * Observes only active trade signals.
     */
    fun observeActiveSignals(): Flow<List<TradeSignalEntity>> {
        return tradeSignalDao.getActiveSignals()
    }

    /**
     * Updates the status of an existing trade signal (e.g. "ACTIVE" -> "TP1_HIT").
     */
    suspend fun updateSignalStatus(signalId: Long, newStatus: String) {
        tradeSignalDao.updateSignalStatus(signalId, newStatus)
    }

    /**
     * Purges expired trade signals.
     */
    suspend fun cleanupExpiredSignals(currentTime: Long = System.currentTimeMillis()) {
        tradeSignalDao.deleteExpiredSignals(currentTime)
    }

    /**
     * Clears all trade signals and historical price data.
     */
    suspend fun clearAllOfflineData() {
        tradeSignalDao.clearAllSignals()
        historicalPriceDao.clearAllHistoricalPrices()
    }
}
