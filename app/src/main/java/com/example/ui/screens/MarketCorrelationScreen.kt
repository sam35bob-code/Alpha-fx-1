package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
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
import com.example.model.*
import com.example.ui.components.MarketCorrelationComposeChart
import com.example.ui.components.MarketCorrelationRechartsWebView
import com.example.ui.theme.*
import com.example.ui.viewmodel.ForexViewModel
import java.util.Locale

@Composable
fun MarketCorrelationScreen(
    viewModel: ForexViewModel,
    onNavigateToPair: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val correlationData by viewModel.marketCorrelationData.collectAsState()
    val selectedHorizon by viewModel.correlationTimeHorizon.collectAsState()

    var chartEngineMode by remember { mutableStateOf(ChartEngineMode.RECHARTS_WEB) }
    var showUs30 by remember { mutableStateOf(true) }
    var showEurUsd by remember { mutableStateOf(true) }
    var showGbpUsd by remember { mutableStateOf(true) }
    var selectedMetricInfo by remember { mutableStateOf<PairCorrelationMetric?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .padding(horizontal = 14.dp)
            .testTag("market_correlation_screen_column"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Top Header with Engine Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                            contentDescription = "Market Correlation",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "MARKET CORRELATION",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                        Text(
                            text = "US30 • EUR/USD • GBP/USD Visualizer",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Recharts Engine Selector
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TerminalSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                    modifier = Modifier.clickable {
                        chartEngineMode = if (chartEngineMode == ChartEngineMode.RECHARTS_WEB) {
                            ChartEngineMode.RECHARTS_COMPOSE
                        } else {
                            ChartEngineMode.RECHARTS_WEB
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (chartEngineMode == ChartEngineMode.RECHARTS_WEB) CyanAccent else BullishGreen)
                        )
                        Text(
                            text = if (chartEngineMode == ChartEngineMode.RECHARTS_WEB) "Recharts Web" else "Native Compose",
                            fontSize = 10.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Time Horizon & Asset Filter Ribbon
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Horizon Chips (24H, 7D, 30D)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("24H", "7D", "30D").forEach { horizon ->
                        val isSelected = selectedHorizon == horizon
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) CyanAccent.copy(alpha = 0.2f) else TerminalSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CyanAccent else TerminalBorder
                            ),
                            modifier = Modifier
                                .clickable { viewModel.selectCorrelationHorizon(horizon) }
                                .testTag("horizon_tab_$horizon")
                        ) {
                            Text(
                                text = horizon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) CyanAccent else TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // If in Native Compose Mode, show toggles for pairs
                if (chartEngineMode == ChartEngineMode.RECHARTS_COMPOSE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = showUs30,
                            onClick = { showUs30 = !showUs30 },
                            label = { Text("US30", fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent.copy(alpha = 0.2f),
                                selectedLabelColor = GoldAccent
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                        FilterChip(
                            selected = showEurUsd,
                            onClick = { showEurUsd = !showEurUsd },
                            label = { Text("EUR", fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent.copy(alpha = 0.2f),
                                selectedLabelColor = CyanAccent
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                        FilterChip(
                            selected = showGbpUsd,
                            onClick = { showGbpUsd = !showGbpUsd },
                            label = { Text("GBP", fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BullishGreen.copy(alpha = 0.2f),
                                selectedLabelColor = BullishGreen
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }
        }

        // Primary Recharts Visualizer Component
        item {
            if (chartEngineMode == ChartEngineMode.RECHARTS_WEB) {
                MarketCorrelationRechartsWebView(correlationData = correlationData)
            } else {
                MarketCorrelationComposeChart(
                    points = correlationData.points,
                    showUs30 = showUs30,
                    showEurUsd = showEurUsd,
                    showGbpUsd = showGbpUsd
                )
            }
        }

        // 3x3 Correlation Matrix Table
        item {
            val eurGbp = correlationData.metrics.find { it.pairA == "EUR/USD" && it.pairB == "GBP/USD" }?.coefficient ?: 0.86
            val us30Eur = correlationData.metrics.find { it.pairA == "US30" && it.pairB == "EUR/USD" }?.coefficient ?: 0.54
            val us30Gbp = correlationData.metrics.find { it.pairA == "US30" && it.pairB == "GBP/USD" }?.coefficient ?: 0.62

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("correlation_matrix_card"),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PEARSON CORRELATION MATRIX ($selectedHorizon)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                        Text(
                            text = "-1.00 (Inverse) to +1.00 (Positive)",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4-column grid (Label, US30, EUR/USD, GBP/USD)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Header Row
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("PAIR", modifier = Modifier.weight(1f), fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text("US30", modifier = Modifier.weight(1f), fontSize = 10.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                            Text("EUR/USD", modifier = Modifier.weight(1f), fontSize = 10.sp, color = CyanAccent, fontWeight = FontWeight.Bold)
                            Text("GBP/USD", modifier = Modifier.weight(1f), fontSize = 10.sp, color = BullishGreen, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(color = TerminalBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // US30 Row
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("US30", modifier = Modifier.weight(1f), fontSize = 11.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                            CorrelationCell(1.00, modifier = Modifier.weight(1f))
                            CorrelationCell(us30Eur, modifier = Modifier.weight(1f)) {
                                selectedMetricInfo = correlationData.metrics.find { it.pairA == "US30" && it.pairB == "EUR/USD" }
                            }
                            CorrelationCell(us30Gbp, modifier = Modifier.weight(1f)) {
                                selectedMetricInfo = correlationData.metrics.find { it.pairA == "US30" && it.pairB == "GBP/USD" }
                            }
                        }

                        // EUR/USD Row
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("EUR/USD", modifier = Modifier.weight(1f), fontSize = 11.sp, color = CyanAccent, fontWeight = FontWeight.Bold)
                            CorrelationCell(us30Eur, modifier = Modifier.weight(1f)) {
                                selectedMetricInfo = correlationData.metrics.find { it.pairA == "US30" && it.pairB == "EUR/USD" }
                            }
                            CorrelationCell(1.00, modifier = Modifier.weight(1f))
                            CorrelationCell(eurGbp, modifier = Modifier.weight(1f)) {
                                selectedMetricInfo = correlationData.metrics.find { it.pairA == "EUR/USD" && it.pairB == "GBP/USD" }
                            }
                        }

                        // GBP/USD Row
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("GBP/USD", modifier = Modifier.weight(1f), fontSize = 11.sp, color = BullishGreen, fontWeight = FontWeight.Bold)
                            CorrelationCell(us30Gbp, modifier = Modifier.weight(1f)) {
                                selectedMetricInfo = correlationData.metrics.find { it.pairA == "US30" && it.pairB == "GBP/USD" }
                            }
                            CorrelationCell(eurGbp, modifier = Modifier.weight(1f)) {
                                selectedMetricInfo = correlationData.metrics.find { it.pairA == "EUR/USD" && it.pairB == "GBP/USD" }
                            }
                            CorrelationCell(1.00, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Section: Hedging Opportunities
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "IDENTIFIED HEDGING OPPORTUNITIES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
            }
        }

        items(correlationData.hedgingOpportunities) { hedge ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hedge_card_${hedge.id}"),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CyanAccent.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = hedge.strategyType.label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BullishGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BullishGreen.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "-${hedge.riskReductionPercent}% RISK",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BullishGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = hedge.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Pair action breakdown
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TerminalBackground, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("PRIMARY LEG", fontSize = 8.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text("${hedge.primaryPair}: ${hedge.primaryDirection}", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }

                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text("HEDGE LEG", fontSize = 8.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text("${hedge.hedgePair}: ${hedge.hedgeDirection}", fontSize = 11.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "RATIO: ${hedge.recommendedRatio}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = hedge.rationalization,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { onNavigateToPair(hedge.primaryPair) },
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Inspect ${hedge.primaryPair} in Terminal →", fontSize = 10.sp, color = CyanAccent)
                        }
                    }
                }
            }
        }

        // Section: Correlated Risks
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = BearishRed,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "CORRELATED RISKS & EXPOSURE MULTIPLIERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
            }
        }

        items(correlationData.correlatedRisks) { risk ->
            val isHigh = risk.severity == RiskSeverity.HIGH
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("risk_card_${risk.id}"),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isHigh) BearishRed.copy(alpha = 0.5f) else GoldAccent.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isHigh) BearishRed.copy(alpha = 0.15f) else GoldAccent.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = risk.severity.label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isHigh) BearishRed else GoldAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "${risk.riskMultiplier}x DRAWDOWN MULTIPLIER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isHigh) BearishRed else GoldAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = risk.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = risk.description,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isHigh) BearishRed.copy(alpha = 0.08f) else GoldAccent.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isHigh) BearishRed else GoldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "ACTION: ${risk.mitigationAction}",
                            fontSize = 9.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(18.dp)) }
    }

    // Modal Sheet or Dialog when clicking a Correlation Metric Cell
    selectedMetricInfo?.let { metric ->
        AlertDialog(
            onDismissRequest = { selectedMetricInfo = null },
            title = {
                Text(
                    text = "${metric.pairA} ↔ ${metric.pairB}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Pearson Coefficient: ${if (metric.coefficient >= 0) "+" else ""}${String.format(Locale.US, "%.2f", metric.coefficient)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Classification: ${metric.strength.label}",
                        fontSize = 11.sp,
                        color = BullishGreen
                    )
                    Text(
                        text = "Beta Volatility: ${metric.beta}x",
                        fontSize = 11.sp,
                        color = GoldAccent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = metric.description,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMetricInfo = null }) {
                    Text("Close", color = CyanAccent)
                }
            },
            containerColor = TerminalSurface
        )
    }
}

@Composable
private fun CorrelationCell(
    value: Double,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isSelf = value == 1.00
    val formatted = if (isSelf) "1.00" else (if (value >= 0) "+" else "") + String.format(Locale.US, "%.2f", value)

    val bgColor = when {
        isSelf -> TerminalBorder.copy(alpha = 0.4f)
        value >= 0.80 -> BullishGreen.copy(alpha = 0.20f)
        value >= 0.40 -> CyanAccent.copy(alpha = 0.16f)
        value <= -0.40 -> BearishRed.copy(alpha = 0.20f)
        else -> TerminalBorder.copy(alpha = 0.3f)
    }

    val textColor = when {
        isSelf -> TextSecondary
        value >= 0.80 -> BullishGreen
        value >= 0.40 -> CyanAccent
        value <= -0.40 -> BearishRed
        else -> TextPrimary
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor,
        modifier = modifier
            .padding(2.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = textColor
            )
        }
    }
}
