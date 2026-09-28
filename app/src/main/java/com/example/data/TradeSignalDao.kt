package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for local persistence of trade signals.
 * Provides offline access to active, historical, and executed trading signals.
 */
@Dao
interface TradeSignalDao {

    @Query("SELECT * FROM trade_signals ORDER BY timestamp DESC")
    fun getAllSignals(): Flow<List<TradeSignalEntity>>

    @Query("SELECT * FROM trade_signals WHERE pairSymbol = :pairSymbol ORDER BY timestamp DESC")
    fun getSignalsForPair(pairSymbol: String): Flow<List<TradeSignalEntity>>

    @Query("SELECT * FROM trade_signals WHERE status = 'ACTIVE' ORDER BY timestamp DESC")
    fun getActiveSignals(): Flow<List<TradeSignalEntity>>

    @Query("SELECT * FROM trade_signals WHERE action = :action ORDER BY timestamp DESC")
    fun getSignalsByAction(action: String): Flow<List<TradeSignalEntity>>

    @Query("SELECT * FROM trade_signals WHERE status = :status ORDER BY timestamp DESC")
    fun getSignalsByStatus(status: String): Flow<List<TradeSignalEntity>>

    @Query("SELECT * FROM trade_signals WHERE id = :id LIMIT 1")
    suspend fun getSignalById(id: Long): TradeSignalEntity?

    @Query("SELECT * FROM trade_signals WHERE signalId = :signalId LIMIT 1")
    suspend fun getSignalBySignalId(signalId: String): TradeSignalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: TradeSignalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignals(signals: List<TradeSignalEntity>)

    @Update
    suspend fun updateSignal(signal: TradeSignalEntity)

    @Query("UPDATE trade_signals SET status = :newStatus WHERE id = :id")
    suspend fun updateSignalStatus(id: Long, newStatus: String)

    @Delete
    suspend fun deleteSignal(signal: TradeSignalEntity)

    @Query("DELETE FROM trade_signals WHERE id = :id")
    suspend fun deleteSignalById(id: Long)

    @Query("DELETE FROM trade_signals WHERE expiryTimestamp IS NOT NULL AND expiryTimestamp < :currentTime")
    suspend fun deleteExpiredSignals(currentTime: Long)

    @Query("DELETE FROM trade_signals")
    suspend fun clearAllSignals()

    @Query("SELECT COUNT(*) FROM trade_signals")
    suspend fun getSignalCount(): Int

    @Query("SELECT COUNT(*) FROM trade_signals WHERE status = 'ACTIVE'")
    suspend fun getActiveSignalCount(): Int
}
