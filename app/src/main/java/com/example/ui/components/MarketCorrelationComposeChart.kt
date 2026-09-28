package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CorrelationPoint
import com.example.ui.theme.*

@Composable
fun MarketCorrelationComposeChart(
    points: List<CorrelationPoint>,
    showUs30: Boolean = true,
    showEurUsd: Boolean = true,
    showGbpUsd: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(TerminalSurface, RoundedCornerShape(10.dp))
                .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("No correlation time-series data available.", color = TextSecondary, fontSize = 12.sp)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val activePoint = selectedIndex?.let { points.getOrNull(it) } ?: points.lastOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalSurface, RoundedCornerShape(10.dp))
            .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
            .testTag("market_correlation_compose_chart_container")
    ) {
        // Chart Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RELATIVE PERFORMANCE (%)",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Normalized return from interval start",
                    color = TextSecondary,
                    fontSize = 9.sp
                )
            }

            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showUs30) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(7.dp).background(GoldAccent, CircleShape))
                        Text("US30", fontSize = 9.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                    }
                }
                if (showEurUsd) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(7.dp).background(CyanAccent, CircleShape))
                        Text("EUR/USD", fontSize = 9.sp, color = CyanAccent, fontWeight = FontWeight.Bold)
                    }
                }
                if (showGbpUsd) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(7.dp).background(BullishGreen, CircleShape))
                        Text("GBP/USD", fontSize = 9.sp, color = BullishGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tooltip inspection ribbon
        activePoint?.let { pt ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalBackground, RoundedCornerShape(6.dp))
                    .border(1.dp, TerminalBorder.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TIME: ${pt.timeLabel}",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (showUs30) {
                        Text(
                            text = "US30: ${if (pt.us30Normalized >= 0) "+" else ""}${String.format(java.util.Locale.US, "%.2f", pt.us30Normalized)}%",
                            color = GoldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (showEurUsd) {
                        Text(
                            text = "EUR: ${if (pt.eurusdNormalized >= 0) "+" else ""}${String.format(java.util.Locale.US, "%.2f", pt.eurusdNormalized)}%",
                            color = CyanAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (showGbpUsd) {
                        Text(
                            text = "GBP: ${if (pt.gbpusdNormalized >= 0) "+" else ""}${String.format(java.util.Locale.US, "%.2f", pt.gbpusdNormalized)}%",
                            color = BullishGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Canvas Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val itemWidth = size.width / points.size.coerceAtLeast(1)
                        val idx = (offset.x / itemWidth).toInt().coerceIn(0, points.size - 1)
                        selectedIndex = idx
                    }
                }
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val itemWidth = size.width / points.size.coerceAtLeast(1)
                            val idx = (offset.x / itemWidth).toInt().coerceIn(0, points.size - 1)
                            selectedIndex = idx
                        },
                        onDrag = { change, _ ->
                            val itemWidth = size.width / points.size.coerceAtLeast(1)
                            val idx = (change.position.x / itemWidth).toInt().coerceIn(0, points.size - 1)
                            selectedIndex = idx
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val padY = 16f

                // Find min & max across selected series
                val allValues = mutableListOf<Double>()
                points.forEach { pt ->
                    if (showUs30) allValues.add(pt.us30Normalized)
                    if (showEurUsd) allValues.add(pt.eurusdNormalized)
                    if (showGbpUsd) allValues.add(pt.gbpusdNormalized)
                }

                val minVal = (allValues.minOrNull() ?: -1.0).coerceAtMost(-0.5)
                val maxVal = (allValues.maxOrNull() ?: 1.0).coerceAtLeast(0.5)
                val valRange = (maxVal - minVal).coerceAtLeast(0.1)

                fun getY(v: Double): Float {
                    val normalized = (v - minVal) / valRange
                    return (h - padY - (normalized * (h - 2 * padY))).toFloat()
                }

                // Zero line
                val zeroY = getY(0.0)
                drawLine(
                    color = Color(0xFF334155),
                    start = Offset(0f, zeroY),
                    end = Offset(w, zeroY),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                // Grid lines at 25%, 50%, 75%
                listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
                    val y = padY + (fraction * (h - 2 * padY))
                    drawLine(
                        color = Color(0xFF1E293B).copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                val stepX = w / (points.size - 1).coerceAtLeast(1)

                fun drawSeriesPath(values: List<Double>, color: Color) {
                    val path = Path()
                    values.forEachIndexed { i, v ->
                        val x = i * stepX
                        val y = getY(v)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 2.5f)
                    )
                }

                if (showUs30) {
                    drawSeriesPath(points.map { it.us30Normalized }, GoldAccent)
                }
                if (showEurUsd) {
                    drawSeriesPath(points.map { it.eurusdNormalized }, CyanAccent)
                }
                if (showGbpUsd) {
                    drawSeriesPath(points.map { it.gbpusdNormalized }, BullishGreen)
                }

                // Scrubber line if index selected
                selectedIndex?.let { idx ->
                    val scrubX = idx * stepX
                    drawLine(
                        color = Color.White.copy(alpha = 0.7f),
                        start = Offset(scrubX, 0f),
                        end = Offset(scrubX, h),
                        strokeWidth = 1.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    )
                }
            }
        }
    }
}
