package com.example.model

data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long = 1000L
) {
    val isBullish: Boolean get() = close >= open
    val bodyTop: Double get() = maxOf(open, close)
    val bodyBottom: Double get() = minOf(open, close)
}

enum class Timeframe(val label: String, val minutes: Int) {
    M15("15M", 15),
    H1("1H", 60),
    H4("4H", 240),
    D1("Daily", 1440)
}
