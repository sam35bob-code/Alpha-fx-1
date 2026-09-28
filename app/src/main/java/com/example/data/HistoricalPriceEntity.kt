package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.Candle

/**
 * Room entity representing historical OHLCV price data stored locally
 * for offline chart rendering, indicator calculation, and technical analysis.
 */
@Entity(
    tableName = "historical_prices",
    indices = [
        Index(value = ["pairSymbol", "timeframe", "timestamp"], unique = true),
        Index(value = ["pairSymbol", "timeframe"]),
        Index(value = ["timestamp"])
    ]
)
data class HistoricalPriceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pairSymbol: String,
    val timeframe: String,
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long = 1000L
) {
    /**
     * Converts this database entity to the domain Candle model.
     */
    fun toDomainCandle(): Candle = Candle(
        timestamp = timestamp,
        open = open,
        high = high,
        low = low,
        close = close,
        volume = volume
    )

    companion object {
        /**
         * Creates a HistoricalPriceEntity from a domain Candle.
         */
        fun fromDomainCandle(
            candle: Candle,
            pairSymbol: String,
            timeframe: String
        ): HistoricalPriceEntity = HistoricalPriceEntity(
            pairSymbol = pairSymbol,
            timeframe = timeframe,
            timestamp = candle.timestamp,
            open = candle.open,
            high = candle.high,
            low = candle.low,
            close = candle.close,
            volume = candle.volume
        )
    }
}
