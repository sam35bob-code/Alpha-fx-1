package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for local caching of historical OHLCV candle data.
 * Enables full offline charting and technical analysis calculations.
 */
@Dao
interface HistoricalPriceDao {

    @Query("SELECT * FROM historical_prices WHERE pairSymbol = :pairSymbol AND timeframe = :timeframe ORDER BY timestamp ASC")
    fun getHistoricalPrices(pairSymbol: String, timeframe: String): Flow<List<HistoricalPriceEntity>>

    @Query("SELECT * FROM historical_prices WHERE pairSymbol = :pairSymbol AND timeframe = :timeframe ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentPrices(pairSymbol: String, timeframe: String, limit: Int = 100): Flow<List<HistoricalPriceEntity>>

    @Query("SELECT * FROM historical_prices WHERE pairSymbol = :pairSymbol AND timeframe = :timeframe ORDER BY timestamp ASC")
    suspend fun getHistoricalPricesList(pairSymbol: String, timeframe: String): List<HistoricalPriceEntity>

    @Query("SELECT * FROM historical_prices WHERE pairSymbol = :pairSymbol AND timeframe = :timeframe ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentPricesList(pairSymbol: String, timeframe: String, limit: Int = 100): List<HistoricalPriceEntity>

    @Query("SELECT * FROM historical_prices WHERE pairSymbol = :pairSymbol AND timeframe = :timeframe ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestPrice(pairSymbol: String, timeframe: String): HistoricalPriceEntity?

    @Query("SELECT * FROM historical_prices WHERE pairSymbol = :pairSymbol AND timeframe = :timeframe AND timestamp BETWEEN :fromTime AND :toTime ORDER BY timestamp ASC")
    suspend fun getPricesBetween(
        pairSymbol: String,
        timeframe: String,
        fromTime: Long,
        toTime: Long
    ): List<HistoricalPriceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrice(price: HistoricalPriceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrices(prices: List<HistoricalPriceEntity>)

    @Query("SELECT COUNT(*) FROM historical_prices WHERE pairSymbol = :pairSymbol AND timeframe = :timeframe")
    suspend fun getPriceCount(pairSymbol: String, timeframe: String): Int

    @Query("DELETE FROM historical_prices WHERE pairSymbol = :pairSymbol AND timeframe = :timeframe")
    suspend fun deletePricesForPair(pairSymbol: String, timeframe: String)

    @Query("DELETE FROM historical_prices WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldPrices(beforeTimestamp: Long)

    @Query("DELETE FROM historical_prices")
    suspend fun clearAllHistoricalPrices()
}
