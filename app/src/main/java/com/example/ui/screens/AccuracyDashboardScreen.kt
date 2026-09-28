package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AccuracyDashboardData
import com.example.model.TradeRecommendationPoint
import com.example.ui.components.RechartsComposeChart
import com.example.ui.components.RechartsWebView
import com.example.ui.components.WeeklyHeatmapComposeChart
import com.example.ui.components.WeeklyHeatmapRechartsWebView
import com.example.ui.theme.*
import com.example.ui.viewmodel.ForexViewModel

enum class ChartEngineMode(val label: String) {
    RECHARTS_WEB("Recharts (Web Engine)"),
    RECHARTS_COMPOSE("Recharts (Native Compose)")
}

enum class AccuracySubTab(val label: String) {
    WEEKLY_HEATMAP("Weekly Heatmap (Best Days & Hours)"),
    ACCURACY_CURVE("Accuracy Curve (Recharts)")
}

@Composable
fun AccuracyDashboardScreen(
    viewModel: ForexViewModel,
    modifier: Modifier = Modifier
) {
    val rawDashboardData by viewModel.accuracyDashboardData.collectAsState()
    val weeklyHeatmapData by viewModel.weeklyHeatmapData.collectAsState()
    var selectedSubTab by remember { mutableStateOf(AccuracySubTab.WEEKLY_HEATMAP) }
    var selectedTimeHorizon by remember { mutableStateOf("ALL") }
    var selectedAssetFilter by remember { mutableStateOf("ALL") }
    var chartEngineMode by remember { mutableStateOf(ChartEngineMode.RECHARTS_WEB) }
    var showSeedDialog by remember { mutableStateOf(false) }

    // Filter points based on selected horizon and asset
    val filteredDashboardData = remember(rawDashboardData, selectedTimeHorizon, selectedAssetFilter) {
        val now = System.currentTimeMillis()
        val horizonMs = when (selectedTimeHorizon) {
            "7D" -> 7 * 86_400_000L
            "14D" -> 14 * 86_400_000L
            "30D" -> 30 * 86_400_000L
            else -> Long.MAX_VALUE
        }

        val filteredPoints = rawDashboardData.accuracyOverTime.filter { pt ->
            (now - pt.timestamp) <= horizonMs &&
            (selectedAssetFilter == "ALL" || pt.pairSymbol.equals(selectedAssetFilter, ignoreCase = true))
        }

        if (selectedTimeHorizon == "ALL" && selectedAssetFilter == "ALL") {
            rawDashboardData
        } else {
            // Recompute filtered stats
            val won = filteredPoints.count { it.isWin }
            val total = filteredPoints.size
            val acc = if (total > 0) (won.toDouble() / total) * 100.0 else 0.0
            val pnl = filteredPoints.sumOf { it.pnl }
            rawDashboardData.copy(
                closedRecommendations = total,
                wonCount = won,
                lostCount = total - won,
                overallAccuracyPercent = Math.round(acc * 10.0) / 10.0,
                netPnl = Math.round(pnl * 100.0) / 100.0,
                accuracyOverTime = filteredPoints
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .padding(horizontal = 14.dp)
            .testTag("accuracy_dashboard_column"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Top Header
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
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Analytics",
                        tint = CyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "AI ACCURACY DASHBOARD",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "Visualized with Recharts • Local Room DB",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Live Sync Chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CyanAccent.copy(alpha = 0.12f),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyanAccent.copy(alpha = 0.4f)))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(BullishGreen, CircleShape)
                        )
                        Text(
                            text = "ROOM DB SYNC",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                    }
                }
            }
        }

        // Dashboard Navigation Tabs: Weekly Heatmap vs Accuracy Curve
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AccuracySubTab.values().forEach { subTab ->
                        val isSelected = selectedSubTab == subTab
                        Button(
                            onClick = { selectedSubTab = subTab },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("subtab_${subTab.name}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) CyanAccent else Color.Transparent,
                                contentColor = if (isSelected) TerminalBackground else TextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (subTab == AccuracySubTab.WEEKLY_HEATMAP) Icons.Default.GridOn else Icons.Default.ShowChart,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = subTab.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Engine Selector Tabs (Recharts Web vs Recharts Native Compose)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ChartEngineMode.values().forEach { mode ->
                        val isSelected = chartEngineMode == mode
                        Button(
                            onClick = { chartEngineMode = mode },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("engine_mode_${mode.name}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) CyanAccent.copy(alpha = 0.2f) else Color.Transparent,
                                contentColor = if (isSelected) CyanAccent else TextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyanAccent) else null,
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Text(
                                text = mode.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Horizon & Asset Filter Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Time Horizon Filter
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val horizons = listOf("ALL", "30D", "14D", "7D")
                    items(horizons) { horizon ->
                        val isSel = selectedTimeHorizon == horizon
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedTimeHorizon = horizon },
                            label = { Text(horizon, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent.copy(alpha = 0.2f),
                                selectedLabelColor = CyanAccent
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSel,
                                borderColor = if (isSel) CyanAccent else TerminalBorder
                            ),
                            modifier = Modifier.testTag("filter_horizon_$horizon")
                        )
                    }
                }

                // Asset Filter
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val assets = listOf("ALL", "US30", "EUR/USD", "GBP/USD", "NAS100", "USD/JPY")
                    items(assets) { asset ->
                        val isSel = selectedAssetFilter == asset
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedAssetFilter = asset },
                            label = { Text(asset, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent.copy(alpha = 0.2f),
                                selectedLabelColor = GoldAccent
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSel,
                                borderColor = if (isSel) GoldAccent else TerminalBorder
                            ),
                            modifier = Modifier.testTag("filter_asset_$asset")
                        )
                    }
                }
            }
        }

        // Executive KPI Stat Cards
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
                    .testTag("accuracy_kpi_card"),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Big Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "OVERALL AI ACCURACY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${filteredDashboardData.overallAccuracyPercent}%",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (filteredDashboardData.overallAccuracyPercent >= 70.0) BullishGreen else CyanAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Target: 70%",
                                    fontSize = 11.sp,
                                    color = GoldAccent,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }

                        // Status Badge
                        val delta = filteredDashboardData.overallAccuracyPercent - 70.0
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (delta >= 0) BullishGreen.copy(alpha = 0.15f) else BearishRed.copy(alpha = 0.15f),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (delta >= 0) BullishGreen else BearishRed
                                )
                            )
                        ) {
                            Text(
                                text = if (delta >= 0) "+${String.format("%.1f", delta)}% EDGE" else "${String.format("%.1f", delta)}% UNDER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (delta >= 0) BullishGreen else BearishRed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = TerminalBorder, thickness = 1.dp)

                    // Secondary Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Win / Loss Record", fontSize = 9.sp, color = TextSecondary)
                            Text(
                                "${filteredDashboardData.wonCount}W - ${filteredDashboardData.lostCount}L",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column {
                            Text("Profit Factor", fontSize = 9.sp, color = TextSecondary)
                            Text(
                                "${filteredDashboardData.profitFactor}x",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column {
                            Text("Net AI P&L", fontSize = 9.sp, color = TextSecondary)
                            Text(
                                "${if (filteredDashboardData.netPnl >= 0) "+" else ""}$${String.format("%,.2f", filteredDashboardData.netPnl)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (filteredDashboardData.netPnl >= 0) BullishGreen else BearishRed,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column {
                            Text("Active Trades", fontSize = 9.sp, color = TextSecondary)
                            Text(
                                "${filteredDashboardData.activeCount} Running",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Chart View (Weekly Heatmap or Accuracy Curve via Recharts Web or Recharts Native Compose)
        item {
            if (selectedSubTab == AccuracySubTab.WEEKLY_HEATMAP) {
                if (chartEngineMode == ChartEngineMode.RECHARTS_WEB) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RECHARTS WEEKLY PERFORMANCE HEATMAP",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${weeklyHeatmapData.totalTradesAnalyzed} Room DB Trades",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                            WeeklyHeatmapRechartsWebView(
                                data = weeklyHeatmapData,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    WeeklyHeatmapComposeChart(
                        data = weeklyHeatmapData,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                if (chartEngineMode == ChartEngineMode.RECHARTS_WEB) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RECHARTS ENGINE (RESPONSIVE SVG & D3)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${filteredDashboardData.accuracyOverTime.size} Room DB Points",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                            RechartsWebView(
                                dashboardData = filteredDashboardData,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    RechartsComposeChart(
                        points = filteredDashboardData.accuracyOverTime,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Asset Win-Rate Breakdown
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
                    .testTag("asset_accuracy_card"),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "AI WIN RATE BY ASSET / CURRENCY PAIR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )

                    if (filteredDashboardData.assetBreakdown.isEmpty()) {
                        Text("No asset data recorded yet.", fontSize = 11.sp, color = TextSecondary)
                    } else {
                        filteredDashboardData.assetBreakdown.forEach { asset ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = asset.symbol,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "${asset.wonTrades}W / ${asset.lostTrades}L",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "${asset.winRatePercent}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (asset.winRatePercent >= 70) BullishGreen else CyanAccent,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "${if (asset.netPnl >= 0) "+" else ""}$${String.format("%.0f", asset.netPnl)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (asset.netPnl >= 0) BullishGreen else BearishRed,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                // Progress bar
                                LinearProgressIndicator(
                                    progress = { (asset.winRatePercent / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = if (asset.winRatePercent >= 70) BullishGreen else CyanAccent,
                                    trackColor = TerminalBorder,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Simulation & Database Actions Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyanAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .testTag("simulation_controls_card"),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE ROOM DATABASE TEST CONTROLS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Auto-Updates Charts",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.simulateAiTradeOutcome(
                                    pairSymbol = if (selectedAssetFilter == "ALL") "US30" else selectedAssetFilter,
                                    action = "BUY",
                                    isWin = true
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_simulate_win"),
                            colors = ButtonDefaults.buttonColors(containerColor = BullishGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("⚡ Add Won Trade", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        Button(
                            onClick = {
                                viewModel.simulateAiTradeOutcome(
                                    pairSymbol = if (selectedAssetFilter == "ALL") "EUR/USD" else selectedAssetFilter,
                                    action = "SELL",
                                    isWin = false
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_simulate_loss"),
                            colors = ButtonDefaults.buttonColors(containerColor = BearishRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🛑 Add Lost Trade", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.seedHistoricalAiRecommendations(force = true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("btn_reset_seed_data"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset & Seed 30-Day Historical AI Recommendations", fontSize = 10.sp)
                    }
                }
            }
        }

        // Room DB Audit Trail List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTORICAL AUDIT LOG (${filteredDashboardData.accuracyOverTime.size} TRADES)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Stored in Room DB",
                    fontSize = 10.sp,
                    color = CyanAccent
                )
            }
        }

        // List of recent recommendation items
        items(filteredDashboardData.accuracyOverTime.reversed().take(15)) { pt ->
            TradeAuditCard(point = pt)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun TradeAuditCard(point: TradeRecommendationPoint) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
            .testTag("audit_card_${point.id}"),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${point.pairSymbol} ${point.action}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (point.action == "BUY") BullishGreen else BearishRed,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• ${point.timeframe}",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "• ${point.dateLabel}",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }

                Text(
                    text = "Entry: ${point.entryPrice} | SL: ${point.stopLoss} | TP: ${point.takeProfit1}",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )

                if (point.notes.isNotBlank()) {
                    Text(
                        text = point.notes.take(55) + if (point.notes.length > 55) "..." else "",
                        fontSize = 9.sp,
                        color = NeutralGray
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (point.isWin) BullishGreen.copy(alpha = 0.15f) else BearishRed.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = point.status,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (point.isWin) BullishGreen else BearishRed,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "${if (point.pnl >= 0) "+" else ""}$${String.format("%.2f", point.pnl)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (point.pnl >= 0) BullishGreen else BearishRed,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "Acc: ${point.cumulativeAccuracyPercent}%",
                    fontSize = 9.sp,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
