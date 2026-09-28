package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {
    @Query("SELECT * FROM saved_setups ORDER BY timestamp DESC")
    fun getAllSetups(): Flow<List<SavedSetupEntity>>

    @Query("SELECT * FROM saved_setups WHERE pairSymbol = :symbol ORDER BY timestamp DESC")
    fun getSetupsForPair(symbol: String): Flow<List<SavedSetupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetup(setup: SavedSetupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSetups(setups: List<SavedSetupEntity>)

    @Update
    suspend fun updateSetup(setup: SavedSetupEntity)

    @Delete
    suspend fun deleteSetup(setup: SavedSetupEntity)

    @Query("DELETE FROM saved_setups WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM saved_setups")
    suspend fun deleteAllSetups()
}
