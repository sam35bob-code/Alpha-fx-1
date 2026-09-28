package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.example.model.TradeRecommendationPoint
import com.example.ui.theme.*

@Composable
fun RechartsComposeChart(
    points: List<TradeRecommendationPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(TerminalSurface, RoundedCornerShape(10.dp))
                .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No recommendation data points available in Room database.",
                color = TextSecondary,
                fontSize = 12.sp
            )
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
            .testTag("recharts_compose_chart_container")
    ) {
        // Chart Header with active indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AI ACCURACY OVER TIME",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Cumulative win rate plotted per recommendation",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }

            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(CyanAccent, RoundedCornerShape(2.dp)))
                    Text("Acc %", fontSize = 10.sp, color = TextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(GoldAccent, RoundedCornerShape(2.dp)))
                    Text("70% Target", fontSize = 10.sp, color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected Point Tooltip Bar
        activePoint?.let { pt ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalSurfaceVariant, RoundedCornerShape(6.dp))
                    .border(1.dp, CyanAccent.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${pt.dateLabel} • ${pt.pairSymbol} ${pt.action}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Outcome: ${pt.status} (${if (pt.isWin) "WIN" else "STOPPED"})",
                        color = if (pt.isWin) BullishGreen else BearishRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Accuracy", fontSize = 9.sp, color = TextSecondary)
                        Text(
                            text = "${String.format("%.1f", pt.cumulativeAccuracyPercent)}%",
                            color = CyanAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("P&L", fontSize = 9.sp, color = TextSecondary)
                        Text(
                            text = "${if (pt.pnl >= 0) "+" else ""}$${String.format("%.2f", pt.pnl)}",
                            color = if (pt.pnl >= 0) BullishGreen else BearishRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Canvas Recharts Area Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            val padL = 35.dp.toPx()
                            val padR = 12.dp.toPx()
                            val plotW = size.width - padL - padR
                            if (offset.x >= padL && offset.x <= size.width - padR && points.size > 1) {
                                val ratio = ((offset.x - padL) / plotW).coerceIn(0f, 1f)
                                val idx = (ratio * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
                                selectedIndex = idx
                            }
                        }
                    }
                    .pointerInput(points) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val padL = 35.dp.toPx()
                            val padR = 12.dp.toPx()
                            val plotW = size.width - padL - padR
                            if (points.size > 1) {
                                val ratio = ((change.position.x - padL) / plotW).coerceIn(0f, 1f)
                                val idx = (ratio * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
                                selectedIndex = idx
                            }
                        }
                    }
            ) {
                val padL = 35.dp.toPx()
                val padR = 12.dp.toPx()
                val padT = 12.dp.toPx()
                val padB = 24.dp.toPx()
                val plotW = size.width - padL - padR
                val plotH = size.height - padT - padB

                if (plotW <= 0 || plotH <= 0 || points.isEmpty()) return@Canvas

                val getY = { acc: Double ->
                    padT + (1f - (acc.toFloat() / 100f).coerceIn(0f, 1f)) * plotH
                }
                val getX = { idx: Int ->
                    if (points.size == 1) padL + plotW / 2f
                    else padL + (idx.toFloat() / (points.size - 1).toFloat()) * plotW
                }

                // Grid lines at 25%, 50%, 70% (target), 100%
                listOf(0.0, 25.0, 50.0, 70.0, 100.0).forEach { level ->
                    val y = getY(level)
                    val isTarget = level == 70.0
                    val strokeColor = if (isTarget) GoldAccent.copy(alpha = 0.85f) else Color(0xFF1E293B)
                    val pathEffect = if (isTarget) PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f) else PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    drawLine(
                        color = strokeColor,
                        start = Offset(padL, y),
                        end = Offset(size.width - padR, y),
                        strokeWidth = if (isTarget) 1.5.dp.toPx() else 1.dp.toPx(),
                        pathEffect = pathEffect
                    )
                }

                // Smooth Path for Area and Line
                val linePath = Path()
                val areaPath = Path()

                points.forEachIndexed { i, pt ->
                    val x = getX(i)
                    val y = getY(pt.cumulativeAccuracyPercent)
                    if (i == 0) {
                        linePath.moveTo(x, y)
                        areaPath.moveTo(x, getY(0.0))
                        areaPath.lineTo(x, y)
                    } else {
                        val prevX = getX(i - 1)
                        val prevY = getY(points[i - 1].cumulativeAccuracyPercent)
                        val cx1 = prevX + (x - prevX) / 2f
                        val cx2 = prevX + (x - prevX) / 2f
                        linePath.cubicTo(cx1, prevY, cx2, y, x, y)
                        areaPath.cubicTo(cx1, prevY, cx2, y, x, y)
                    }
                }

                areaPath.lineTo(getX(points.lastIndex), getY(0.0))
                areaPath.close()

                // Draw glowing gradient area
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CyanAccent.copy(alpha = 0.35f),
                            CyanAccent.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        startY = padT,
                        endY = padT + plotH
                    )
                )

                // Draw main curve
                drawPath(
                    path = linePath,
                    color = CyanAccent,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Draw data points
                points.forEachIndexed { i, pt ->
                    val x = getX(i)
                    val y = getY(pt.cumulativeAccuracyPercent)
                    val dotColor = if (pt.isWin) BullishGreen else BearishRed
                    drawCircle(
                        color = Color(0xFF0B0F19),
                        radius = 4.5.dp.toPx(),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = dotColor,
                        radius = 3.dp.toPx(),
                        center = Offset(x, y)
                    )
                }

                // If scrubbing/selected, draw cursor line
                selectedIndex?.let { selIdx ->
                    if (selIdx in points.indices) {
                        val selX = getX(selIdx)
                        val selY = getY(points[selIdx].cumulativeAccuracyPercent)
                        drawLine(
                            color = CyanAccent.copy(alpha = 0.7f),
                            start = Offset(selX, padT),
                            end = Offset(selX, padT + plotH),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 5.dp.toPx(),
                            center = Offset(selX, selY)
                        )
                        drawCircle(
                            color = CyanAccent,
                            radius = 3.dp.toPx(),
                            center = Offset(selX, selY)
                        )
                    }
                }
            }
        }

        // X-Axis Date Footnotes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 35.dp, end = 12.dp, top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val firstDate = points.firstOrNull()?.dateLabel ?: ""
            val midDate = points.getOrNull(points.size / 2)?.dateLabel ?: ""
            val lastDate = points.lastOrNull()?.dateLabel ?: ""
            Text(firstDate, fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
            if (points.size > 2) {
                Text(midDate, fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
            }
            Text(lastDate, fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
        }
    }
}
