package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WeeklyHeatmapData
import com.example.ui.theme.*

@Composable
fun WeeklyHeatmapComposeChart(
    data: WeeklyHeatmapData,
    modifier: Modifier = Modifier
) {
    var selectedCellInfo by remember { mutableStateOf<String?>(null) }
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
    val hourLabels = (0..23 step 2).map { String.format("%02dh", it) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .background(TerminalSurface)
            .padding(14.dp)
            .testTag("weekly_heatmap_compose_chart"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "WEEKLY PERFORMANCE HEATMAP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                    color = TextPrimary
                )
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = CyanAccent.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${data.totalTradesAnalyzed} ROOM DB TRADES",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Golden Trading Window Banner
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = TerminalBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(CyanAccent.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "GOLDEN TRADING WINDOW: ${data.bestDay.uppercase()} @ ${data.bestHour}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = CyanAccent
                    )
                    Text(
                        text = "Institutional flow peaks at ${data.bestDayWinRate}% win rate during ${data.bestSession}.",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Quick Stats Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatPill("Best Day", "${data.bestDay} (${data.bestDayWinRate}%)", BullishGreen, Modifier.weight(1f))
            StatPill("Best Hour", "${data.bestHour} (${data.bestHourWinRate}%)", CyanAccent, Modifier.weight(1f))
            StatPill("Avoid Times", "${data.worstDay} (${data.worstDayWinRate}%)", BearishRed, Modifier.weight(1f))
        }

        // Heatmap Matrix Grid (Horizontally scrollable for all hours)
        Text(
            text = "Trading Hours Matrix (UTC):",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .background(TerminalBackground, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            // Hours Header
            Row {
                Text(
                    text = "Day",
                    fontSize = 9.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp)
                )
                (0..23 step 2).forEach { hour ->
                    Text(
                        text = String.format("%02dh", hour),
                        fontSize = 9.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Day Rows
            days.forEach { dayName ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dayName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.width(36.dp)
                    )

                    (0..23 step 2).forEach { hour ->
                        val matched = data.cells.filter { it.dayName == dayName && (it.hour == hour || it.hour == hour + 1) }
                        val total = matched.sumOf { it.totalTrades }
                        val wins = matched.sumOf { it.wonTrades }
                        val rate = if (total > 0) Math.round((wins.toDouble() / total) * 1000) / 10.0 else 0.0

                        val cellBg = when {
                            total == 0 -> TerminalSurfaceVariant
                            rate >= 80.0 -> CyanAccent
                            rate >= 70.0 -> BullishGreen
                            rate >= 60.0 -> Color(0xFF26A69A)
                            rate >= 50.0 -> Color(0xFFFFB703)
                            else -> BearishRed
                        }

                        val textColor = if (rate >= 70.0) Color.Black else Color.White

                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(22.dp)
                                .padding(1.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(cellBg)
                                .clickable {
                                    selectedCellInfo = "$dayName ${String.format("%02d:00", hour)}: $rate% Win ($wins W / ${total - wins} L across $total trades)"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (total > 0) "${rate.toInt()}%" else "·",
                                fontSize = 8.sp,
                                fontWeight = if (total > 0) FontWeight.Black else FontWeight.Normal,
                                color = textColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                }
            }
        }

        // Selected Cell Info or Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedCellInfo ?: "Tap any block for details",
                fontSize = 10.sp,
                color = if (selectedCellInfo != null) CyanAccent else TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (selectedCellInfo != null) FontWeight.Bold else FontWeight.Normal
            )

            // Mini Legend
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(CyanAccent, RoundedCornerShape(2.dp)))
                Text("≥80%", fontSize = 8.sp, color = TextSecondary)
                Box(modifier = Modifier.size(8.dp).background(BullishGreen, RoundedCornerShape(2.dp)))
                Text("70%", fontSize = 8.sp, color = TextSecondary)
                Box(modifier = Modifier.size(8.dp).background(BearishRed, RoundedCornerShape(2.dp)))
                Text("<50%", fontSize = 8.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = TerminalBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 8.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            Text(
                text = value,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
