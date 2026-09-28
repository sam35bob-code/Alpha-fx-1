package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SavedSetupEntity::class,
        HistoricalPriceEntity::class,
        TradeSignalEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AlphaFxDatabase : RoomDatabase() {
    abstract fun tradeDao(): TradeDao
    abstract fun historicalPriceDao(): HistoricalPriceDao
    abstract fun tradeSignalDao(): TradeSignalDao

    companion object {
        @Volatile
        private var INSTANCE: AlphaFxDatabase? = null

        fun getDatabase(context: Context): AlphaFxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlphaFxDatabase::class.java,
                    "alphafx_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
