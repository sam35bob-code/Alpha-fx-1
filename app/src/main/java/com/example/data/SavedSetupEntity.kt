package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_setups")
data class SavedSetupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pairSymbol: String,
    val timeframe: String,
    val action: String, // BUY, SELL, WAIT
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit1: Double,
    val takeProfit2: Double,
    val riskRewardRatio: Double,
    val ema50: Double,
    val ema200: Double,
    val rsi14: Double,
    val keyZoneLabel: String,
    val status: String, // PENDING, ACTIVE, WON_TP1, WON_TP2, STOPPED_OUT, CLOSED
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isAutoExecuted: Boolean = false,
    val lots: Double = 0.1,
    val currentPnl: Double = 0.0,
    val exitPrice: Double = 0.0,
    val closedTimestamp: Long? = null
)
