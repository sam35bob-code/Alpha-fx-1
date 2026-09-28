package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.analysis.TechnicalAnalysisEngine
import com.example.model.*
import com.example.ui.theme.*

/**
 * Animated In-App Heads-Up Alert Banner displayed when an abnormal ADR volatility deviation is detected.
 */
@Composable
fun HeadsUpAdrAlertBanner(
    alert: AdrVolatilityAlert?,
    onDismiss: () -> Unit,
    onNavigateToPair: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = alert != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("heads_up_adr_alert_banner")
    ) {
        if (alert != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardBackground,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, BearishRed),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BearishRed.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "ADR Volatility Spike",
                                tint = BearishRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "ABNORMAL ADR VOLATILITY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BearishRed,
                                    letterSpacing = 0.6.sp
                                )
                                Text(
                                    text = alert.pairSymbol,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "Today's Range (${alert.todayRangePips} pips) at ${String.format("%.1f", alert.adrPercentage)}% of 14d ADR (+${String.format("%.1f", alert.deviationPercentage)}%)",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = {
                                onNavigateToPair(alert.pairSymbol)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanAccent,
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp).testTag("adr_alert_view_btn")
                        ) {
                            Text("Inspect", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp).testTag("adr_alert_dismiss_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ADR Card for current pair display on the Terminal screen.
 */
@Composable
fun AdrPairSummaryCard(
    metric: PairAdrMetric,
    onOpenAdrDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barColor = when {
        metric.adrPercentage >= 125.0 -> BearishRed
        metric.adrPercentage >= 110.0 -> Color(0xFFFF9800)
        metric.adrPercentage >= 90.0 -> Color(0xFFFFD54F)
        metric.adrPercentage >= 60.0 -> BullishGreen
        else -> CyanAccent
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenAdrDashboard() }
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .testTag("terminal_adr_summary_card"),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                        imageVector = Icons.Default.Speed,
                        contentDescription = "ADR Monitor",
                        tint = if (metric.isAbnormalDeviation) BearishRed else CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ADR (14-DAY) VOLATILITY MONITOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = barColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, barColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = metric.volatilityLevel.label.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = barColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            val progressFraction = (metric.adrPercentage / 150.0).coerceIn(0.0, 1.0).toFloat()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(TerminalBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progressFraction)
                        .background(barColor)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "14d ADR: ${metric.adr14DaysPips} pips",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Day Range: ${metric.todayRangePips} pips (${String.format("%.1f", metric.adrPercentage)}%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Deviation: ${if (metric.deviationPercentage >= 0) "+" else ""}${String.format("%.1f", metric.deviationPercentage)}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (metric.isAbnormalDeviation) BearishRed else TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Tap to open ADR Hub ➔",
                        fontSize = 9.sp,
                        color = CyanAccent
                    )
                }
            }
        }
    }
}

/**
 * Full ADR Volatility Monitoring Dialog with real-time controls, alerts, and major pairs overview.
 */
@Composable
fun AdrMonitorDialog(
    metrics: List<PairAdrMetric>,
    alertHistory: List<AdrVolatilityAlert>,
    monitoringActive: Boolean,
    thresholdPercentage: Double,
    onToggleMonitoring: () -> Unit,
    onSetThreshold: (Double) -> Unit,
    onSendTestAlert: (String?) -> Unit,
    onSimulateSpike: (String) -> Unit,
    onSelectPair: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = TerminalSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .testTag("adr_monitor_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
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
                                .background(CyanAccent.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "ADR Monitor",
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "AUTOMATED ADR MONITOR",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = "Major Pairs Volatility Deviation Engine",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp).testTag("close_adr_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Monitoring Toggle & Threshold Controls Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = TerminalBackground),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Auto-Push Notifications",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (monitoringActive) "Active • Monitoring major pairs" else "Paused",
                                    fontSize = 10.sp,
                                    color = if (monitoringActive) BullishGreen else TextSecondary
                                )
                            }
                            Switch(
                                checked = monitoringActive,
                                onCheckedChange = { onToggleMonitoring() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = CyanAccent
                                ),
                                modifier = Modifier.testTag("adr_monitoring_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Threshold Selector
                        Text(
                            text = "Alert Threshold (% of 14d ADR):",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(110.0, 120.0, 125.0, 140.0, 150.0).forEach { th ->
                                val isSelected = th == thresholdPercentage
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) CyanAccent else TerminalSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) CyanAccent else TerminalBorder
                                    ),
                                    modifier = Modifier
                                        .clickable { onSetThreshold(th) }
                                        .testTag("adr_threshold_pill_${th.toInt()}")
                                ) {
                                    Text(
                                        text = "${th.toInt()}%",
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.Black else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { onSendTestAlert(null) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanAccent.copy(alpha = 0.15f),
                                contentColor = CyanAccent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .testTag("send_test_adr_alert_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dispatch Test ADR Volatility Notification", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs: Major Pairs Live ADR / Alert History
                var selectedTab by remember { mutableStateOf(0) }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Major Pairs (${metrics.size})",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) CyanAccent else TextSecondary
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Alert History (${alertHistory.size})",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) CyanAccent else TextSecondary
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedTab == 0) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(metrics) { m ->
                            val itemColor = when {
                                m.adrPercentage >= 125.0 -> BearishRed
                                m.adrPercentage >= 110.0 -> Color(0xFFFF9800)
                                m.adrPercentage >= 90.0 -> Color(0xFFFFD54F)
                                m.adrPercentage >= 60.0 -> BullishGreen
                                else -> CyanAccent
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = TerminalBackground),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (m.isAbnormalDeviation) BearishRed.copy(alpha = 0.7f) else TerminalBorder
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("adr_pair_item_${m.pair.symbol.replace('/', '_')}")
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = m.pair.symbol,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = TechnicalAnalysisEngine.formatPrice(m.currentPrice, m.pair),
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = CyanAccent
                                                )
                                            }
                                            Text(
                                                text = "ADR: ${m.adr14DaysPips} p | Today: ${m.todayRangePips} p",
                                                fontSize = 10.sp,
                                                color = TextSecondary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${String.format("%.1f", m.adrPercentage)}% ADR",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                color = itemColor,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "${if (m.deviationPercentage >= 0) "+" else ""}${String.format("%.1f", m.deviationPercentage)}% dev",
                                                fontSize = 10.sp,
                                                color = if (m.isAbnormalDeviation) BearishRed else TextSecondary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Bar
                                    val frac = (m.adrPercentage / 150.0).coerceIn(0.0, 1.0).toFloat()
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(TerminalSurfaceVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fraction = frac)
                                                .background(itemColor)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = m.volatilityLevel.label,
                                            fontSize = 9.sp,
                                            color = itemColor,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            // Simulate Spike button for quick testing
                                            OutlinedButton(
                                                onClick = { onSimulateSpike(m.pair.symbol) },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp).testTag("simulate_spike_btn_${m.pair.symbol.replace('/', '_')}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Bolt,
                                                    contentDescription = null,
                                                    tint = BearishRed,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("Spike", fontSize = 9.sp, color = BearishRed)
                                            }

                                            Button(
                                                onClick = {
                                                    onSelectPair(m.pair.symbol)
                                                    onDismiss()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = CyanAccent,
                                                    contentColor = Color.Black
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text("Chart", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Alert History List
                    if (alertHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = BullishGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("No Abnormal ADR Deviations Logged", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Automated monitor will trigger when a pair crosses ${thresholdPercentage.toInt()}% ADR.", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(alertHistory) { item ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = TerminalBackground),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BearishRed.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = BearishRed,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = item.pairSymbol,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                if (item.isTest) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = CyanAccent.copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "TEST",
                                                            fontSize = 8.sp,
                                                            color = CyanAccent,
                                                            fontWeight = FontWeight.Black,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "${String.format("%.1f", item.adrPercentage)}% ADR (+${String.format("%.1f", item.deviationPercentage)}%)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BearishRed,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.details,
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
