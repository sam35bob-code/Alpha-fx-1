package com.example.model

/**
 * Data model representing an individual hour-by-day cell in the Weekly Performance Heatmap.
 */
data class WeeklyHeatmapCell(
    val dayOfWeek: Int, // Calendar.MONDAY = 2, Calendar.TUESDAY = 3, etc. (1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri, 7=Sat)
    val dayName: String, // "Mon", "Tue", "Wed", "Thu", "Fri"
    val hour: Int, // 0..23
    val hourLabel: String, // "00:00", "08:00", "14:00"
    val session: String, // "Asian (Tokyo)", "London Open", "NY Overlap", "NY Afternoon", "Sydney/Close"
    val totalTrades: Int,
    val wonTrades: Int,
    val lostTrades: Int,
    val winRatePercent: Double,
    val netPnl: Double
) {
    val isGoldenZone: Boolean get() = totalTrades >= 2 && winRatePercent >= 75.0
}

data class DayPerformanceSummary(
    val dayOfWeek: Int,
    val dayName: String,
    val totalTrades: Int,
    val wonTrades: Int,
    val lostTrades: Int,
    val winRatePercent: Double,
    val netPnl: Double
)

data class HourPerformanceSummary(
    val hour: Int,
    val hourLabel: String,
    val session: String,
    val totalTrades: Int,
    val wonTrades: Int,
    val lostTrades: Int,
    val winRatePercent: Double,
    val netPnl: Double
)

data class WeeklyHeatmapData(
    val cells: List<WeeklyHeatmapCell> = emptyList(),
    val daySummaries: List<DayPerformanceSummary> = emptyList(),
    val hourSummaries: List<HourPerformanceSummary> = emptyList(),
    val bestDay: String = "Tuesday",
    val bestDayWinRate: Double = 78.5,
    val bestHour: String = "14:00 UTC",
    val bestHourWinRate: Double = 84.2,
    val bestSession: String = "NY / London Overlap",
    val bestSessionWinRate: Double = 81.0,
    val worstDay: String = "Friday",
    val worstDayWinRate: Double = 52.0,
    val totalTradesAnalyzed: Int = 0,
    val overallWinRate: Double = 0.0
)
