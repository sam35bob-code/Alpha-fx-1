package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.analysis.TechnicalAnalysisEngine
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun ForexCandlestickChart(
    candles: List<Candle>,
    pair: ForexPair,
    indicators: TechnicalIndicators?,
    setup: TradeSetup?,
    modifier: Modifier = Modifier
) {
    var showEma50 by remember { mutableStateOf(true) }
    var showEma200 by remember { mutableStateOf(true) }
    var showZones by remember { mutableStateOf(true) }
    var showRsi by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalSurface, RoundedCornerShape(12.dp))
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("forex_candlestick_chart_container")
    ) {
        // Indicator Toggles & Legend Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // EMA 50 Legend Tag
                FilterChip(
                    selected = showEma50,
                    onClick = { showEma50 = !showEma50 },
                    label = {
                        Text(
                            text = "50 EMA: ${indicators?.let { TechnicalAnalysisEngine.formatPrice(it.ema50, pair) } ?: "--"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showEma50) Ema50Color else TextSecondary
                        )
                    },
                    modifier = Modifier.height(28.dp).testTag("toggle_ema50_chip"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Ema50Color.copy(alpha = 0.15f),
                        selectedLabelColor = Ema50Color
                    )
                )

                // EMA 200 Legend Tag
                FilterChip(
                    selected = showEma200,
                    onClick = { showEma200 = !showEma200 },
                    label = {
                        Text(
                            text = "200 EMA: ${indicators?.let { TechnicalAnalysisEngine.formatPrice(it.ema200, pair) } ?: "--"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showEma200) Ema200Color else TextSecondary
                        )
                    },
                    modifier = Modifier.height(28.dp).testTag("toggle_ema200_chip"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Ema200Color.copy(alpha = 0.15f),
                        selectedLabelColor = Ema200Color
                    )
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // S/R Zones toggle
                FilterChip(
                    selected = showZones,
                    onClick = { showZones = !showZones },
                    label = { Text("S/R Zones", fontSize = 10.sp) },
                    modifier = Modifier.height(26.dp).testTag("toggle_zones_chip")
                )
                // RSI toggle
                FilterChip(
                    selected = showRsi,
                    onClick = { showRsi = !showRsi },
                    label = { Text("RSI(14)", fontSize = 10.sp) },
                    modifier = Modifier.height(26.dp).testTag("toggle_rsi_chip")
                )
            }
        }

        // Main Price Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(TerminalBackground, RoundedCornerShape(8.dp))
                .border(1.dp, TerminalBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp, horizontal = 4.dp)
                    .testTag("candlestick_canvas")
            ) {
                if (candles.isEmpty()) return@Canvas

                // Compute high and low bounds with padding
                val visibleCandles = candles.takeLast(40)
                var minPrice = visibleCandles.minOf { it.low }
                var maxPrice = visibleCandles.maxOf { it.high }

                if (showEma50 && indicators != null && indicators.historicalEma50.isNotEmpty()) {
                    val emaSub = indicators.historicalEma50.takeLast(visibleCandles.size)
                    minPrice = min(minPrice, emaSub.minOrNull() ?: minPrice)
                    maxPrice = max(maxPrice, emaSub.maxOrNull() ?: maxPrice)
                }
                if (showEma200 && indicators != null && indicators.historicalEma200.isNotEmpty()) {
                    val emaSub = indicators.historicalEma200.takeLast(visibleCandles.size)
                    minPrice = min(minPrice, emaSub.minOrNull() ?: minPrice)
                    maxPrice = max(maxPrice, emaSub.maxOrNull() ?: maxPrice)
                }

                val pricePadding = (maxPrice - minPrice) * 0.12
                val lowBound = minPrice - pricePadding
                val highBound = maxPrice + pricePadding
                val priceRange = if (highBound - lowBound == 0.0) 1.0 else highBound - lowBound

                val w = size.width
                val h = size.height
                val candleCount = visibleCandles.size
                val candleSlot = w / candleCount
                val candleWidth = candleSlot * 0.65f

                fun priceToY(price: Double): Float {
                    val normalized = (price - lowBound) / priceRange
                    return (h - (normalized * h)).toFloat()
                }

                // Draw Background Grid Lines
                val gridLines = 4
                for (g in 0..gridLines) {
                    val y = (h / gridLines) * g
                    drawLine(
                        color = TerminalBorder.copy(alpha = 0.35f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                }

                // Draw Support & Resistance Zones
                if (showZones && indicators != null) {
                    indicators.nearestSupport?.let { support ->
                        val topY = priceToY(support.top)
                        val bottomY = priceToY(support.bottom)
                        val zoneHeight = max(abs(bottomY - topY), 4f)
                        drawRect(
                            color = BullishGreenAlpha,
                            topLeft = Offset(0f, min(topY, bottomY)),
                            size = Size(w, zoneHeight)
                        )
                        drawLine(
                            color = BullishGreen.copy(alpha = 0.5f),
                            start = Offset(0f, min(topY, bottomY)),
                            end = Offset(w, min(topY, bottomY)),
                            strokeWidth = 1.5f
                        )
                    }

                    indicators.nearestResistance?.let { resistance ->
                        val topY = priceToY(resistance.top)
                        val bottomY = priceToY(resistance.bottom)
                        val zoneHeight = max(abs(bottomY - topY), 4f)
                        drawRect(
                            color = BearishRedAlpha,
                            topLeft = Offset(0f, min(topY, bottomY)),
                            size = Size(w, zoneHeight)
                        )
                        drawLine(
                            color = BearishRed.copy(alpha = 0.5f),
                            start = Offset(0f, max(topY, bottomY)),
                            end = Offset(w, max(topY, bottomY)),
                            strokeWidth = 1.5f
                        )
                    }
                }

                // Draw Candlesticks
                for (i in visibleCandles.indices) {
                    val c = visibleCandles[i]
                    val centerX = (i * candleSlot) + (candleSlot / 2f)
                    val highY = priceToY(c.high)
                    val lowY = priceToY(c.low)
                    val openY = priceToY(c.open)
                    val closeY = priceToY(c.close)
                    val isBull = c.isBullish
                    val color = if (isBull) BullishGreen else BearishRed

                    // Wick
                    drawLine(
                        color = color,
                        start = Offset(centerX, highY),
                        end = Offset(centerX, lowY),
                        strokeWidth = 1.5f
                    )

                    // Body
                    val bodyTop = min(openY, closeY)
                    val bodyHeight = max(abs(openY - closeY), 2.5f)
                    val bodyLeft = centerX - (candleWidth / 2f)

                    drawRect(
                        color = color,
                        topLeft = Offset(bodyLeft, bodyTop),
                        size = Size(candleWidth, bodyHeight)
                    )
                }

                // Draw EMA 50 Path
                if (showEma50 && indicators != null && indicators.historicalEma50.isNotEmpty()) {
                    val emaSub = indicators.historicalEma50.takeLast(visibleCandles.size)
                    val emaPath = Path()
                    var started = false
                    for (i in emaSub.indices) {
                        val centerX = (i * candleSlot) + (candleSlot / 2f)
                        val y = priceToY(emaSub[i])
                        if (!started) {
                            emaPath.moveTo(centerX, y)
                            started = true
                        } else {
                            emaPath.lineTo(centerX, y)
                        }
                    }
                    drawPath(
                        path = emaPath,
                        color = Ema50Color,
                        style = Stroke(width = 2.5f)
                    )
                }

                // Draw EMA 200 Path
                if (showEma200 && indicators != null && indicators.historicalEma200.isNotEmpty()) {
                    val emaSub = indicators.historicalEma200.takeLast(visibleCandles.size)
                    val emaPath = Path()
                    var started = false
                    for (i in emaSub.indices) {
                        val centerX = (i * candleSlot) + (candleSlot / 2f)
                        val y = priceToY(emaSub[i])
                        if (!started) {
                            emaPath.moveTo(centerX, y)
                            started = true
                        } else {
                            emaPath.lineTo(centerX, y)
                        }
                    }
                    drawPath(
                        path = emaPath,
                        color = Ema200Color,
                        style = Stroke(width = 2.5f)
                    )
                }

                // Draw Entry / Stop Loss / TP lines if trade is active
                if (setup != null && setup.isValid) {
                    val slY = priceToY(setup.stopLoss)
                    val tp1Y = priceToY(setup.takeProfit1)
                    val entryY = priceToY(setup.entryPrice)

                    // Stop Loss line
                    drawLine(
                        color = BearishRed,
                        start = Offset(0f, slY),
                        end = Offset(w, slY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )

                    // TP1 line
                    drawLine(
                        color = BullishGreen,
                        start = Offset(0f, tp1Y),
                        end = Offset(w, tp1Y),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )

                    // Entry Line
                    drawLine(
                        color = CyanAccent,
                        start = Offset(0f, entryY),
                        end = Offset(w, entryY),
                        strokeWidth = 1.5f
                    )
                }
            }

            // Current Price Tag on Top-Right
            indicators?.let { ind ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(TerminalSurfaceVariant.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                        .border(1.dp, TerminalBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LIVE: ${TechnicalAnalysisEngine.formatPrice(ind.currentPrice, pair)}",
                        color = if (candles.lastOrNull()?.isBullish == true) BullishGreen else BearishRed,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Sub-Chart: RSI 14-Period Oscillator
        if (showRsi && indicators != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalBackground, RoundedCornerShape(8.dp))
                    .border(1.dp, TerminalBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RSI (14-Period)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "${String.format("%.1f", indicators.rsi14)} • ${indicators.rsiStatus.label}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (indicators.rsiStatus) {
                            RsiStatus.OVERSOLD, RsiStatus.BULLISH_REVERSAL -> BullishGreen
                            RsiStatus.OVERBOUGHT, RsiStatus.BEARISH_REVERSAL -> BearishRed
                            else -> TextSecondary
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp)
                        .padding(top = 4.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().testTag("rsi_canvas")) {
                        val w = size.width
                        val h = size.height

                        fun rsiToY(v: Double): Float {
                            val clamped = v.coerceIn(0.0, 100.0)
                            return (h - ((clamped / 100.0) * h)).toFloat()
                        }

                        val y70 = rsiToY(70.0)
                        val y50 = rsiToY(50.0)
                        val y30 = rsiToY(30.0)

                        // Overbought zone background (>70)
                        drawRect(
                            color = BearishRedAlpha,
                            topLeft = Offset(0f, 0f),
                            size = Size(w, y70)
                        )
                        // Oversold zone background (<30)
                        drawRect(
                            color = BullishGreenAlpha,
                            topLeft = Offset(0f, y30),
                            size = Size(w, h - y30)
                        )

                        // 70 line
                        drawLine(
                            color = BearishRed.copy(alpha = 0.6f),
                            start = Offset(0f, y70),
                            end = Offset(w, y70),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                        )

                        // 50 line
                        drawLine(
                            color = TextSecondary.copy(alpha = 0.3f),
                            start = Offset(0f, y50),
                            end = Offset(w, y50),
                            strokeWidth = 1f
                        )

                        // 30 line
                        drawLine(
                            color = BullishGreen.copy(alpha = 0.6f),
                            start = Offset(0f, y30),
                            end = Offset(w, y30),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                        )

                        // RSI 14 Line
                        val visibleCandleCount = candles.takeLast(40).size
                        val rsiSub = indicators.historicalRsi14.takeLast(visibleCandleCount)
                        if (rsiSub.isNotEmpty()) {
                            val slot = w / rsiSub.size
                            val rsiPath = Path()
                            var started = false
                            for (i in rsiSub.indices) {
                                val x = (i * slot) + (slot / 2f)
                                val y = rsiToY(rsiSub[i])
                                if (!started) {
                                    rsiPath.moveTo(x, y)
                                    started = true
                                } else {
                                    rsiPath.lineTo(x, y)
                                }
                            }
                            drawPath(
                                path = rsiPath,
                                color = RsiLineColor,
                                style = Stroke(width = 2f)
                            )
                        }
                    }
                }
            }
        }
    }
}
