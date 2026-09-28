package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ForexPair
import com.example.model.Timeframe
import com.example.ui.theme.*

@Composable
fun ForexTopBar(
    selectedPair: ForexPair,
    selectedTimeframe: Timeframe,
    onSelectPair: (ForexPair) -> Unit,
    onSelectTimeframe: (Timeframe) -> Unit,
    brokerAccountInfo: com.example.model.BrokerAccountInfo? = null,
    onOpenBrokerHub: (() -> Unit)? = null,
    notificationsEnabled: Boolean = true,
    alertCount: Int = 0,
    onOpenNotifications: (() -> Unit)? = null,
    currentAdrMetric: com.example.model.PairAdrMetric? = null,
    onOpenAdrDashboard: (() -> Unit)? = null,
    onOpenEa: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var pairMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("forex_top_bar")
    ) {
        // App Title & Pair Dropdown Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(CyanAccent.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CandlestickChart,
                        contentDescription = "AlphaFX Logo",
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "AlphaFX",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "DISCIPLINED STRUCTURAL ANALYST",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = CyanAccent
                    )
                }
            }

            // Right Actions: Notification Bell + Broker Badge + Pair Dropdown
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // AI Setup Notification Bell
                if (onOpenNotifications != null) {
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.size(32.dp).testTag("topbar_notification_bell_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                if (alertCount > 0) {
                                    Badge(
                                        containerColor = BearishRed,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = if (alertCount > 9) "9+" else alertCount.toString(),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else if (notificationsEnabled) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(BullishGreen)
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (notificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = "Setup Alerts",
                                tint = if (notificationsEnabled) CyanAccent else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Intelligent EA Auto-Trader Quick Button
                if (onOpenEa != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CyanAccent.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clickable { onOpenEa() }
                            .testTag("topbar_ea_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "Intelligence EA",
                                tint = CyanAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "EA BOT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = CyanAccent
                            )
                        }
                    }
                }

                // Broker Status Badge
                if (brokerAccountInfo != null && onOpenBrokerHub != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (brokerAccountInfo.isLiveRealAccount) BearishRed.copy(alpha = 0.18f) else CyanAccent.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (brokerAccountInfo.isLiveRealAccount) BearishRed.copy(alpha = 0.6f) else CyanAccent.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .clickable { onOpenBrokerHub() }
                        .testTag("topbar_broker_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (brokerAccountInfo.isLiveRealAccount) BearishRed else BullishGreen, CircleShape)
                        )
                        Text(
                            text = if (brokerAccountInfo.isLiveRealAccount) "LIVE REAL" else "DEMO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (brokerAccountInfo.isLiveRealAccount) BearishRed else CyanAccent
                        )
                        Text(
                            text = "$${String.format("%.0f", brokerAccountInfo.equity)}",
                            fontSize = 9.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Pair Selector Dropdown
            Box {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TerminalSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                    modifier = Modifier
                        .clickable { pairMenuExpanded = true }
                        .testTag("pair_selector_dropdown")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(BullishGreen, CircleShape)
                        )
                        Text(
                            text = selectedPair.symbol,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Pair",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = pairMenuExpanded,
                    onDismissRequest = { pairMenuExpanded = false },
                    modifier = Modifier.background(TerminalSurface)
                ) {
                    ForexPair.ALL_PAIRS.forEach { pair ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = pair.symbol,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pair == selectedPair) CyanAccent else TextPrimary
                                    )
                                    Text(
                                        text = pair.name,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            },
                            onClick = {
                                onSelectPair(pair)
                                pairMenuExpanded = false
                            },
                            modifier = Modifier.testTag("pair_option_${pair.symbol.replace('/', '_')}")
                        )
                    }
                }
            }
        }
    }

        Spacer(modifier = Modifier.height(10.dp))

        // Timeframe Selector & Session Badge Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timeframe Selector Pills
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Timeframe.values().forEach { tf ->
                    val isSelected = tf == selectedTimeframe
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) CyanAccent else TerminalBackground,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) CyanAccent else TerminalBorder
                        ),
                        modifier = Modifier
                            .clickable { onSelectTimeframe(tf) }
                            .testTag("timeframe_pill_${tf.label}")
                    ) {
                        Text(
                            text = tf.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // ADR Volatility Badge
                if (currentAdrMetric != null && onOpenAdrDashboard != null) {
                    val isAbnormal = currentAdrMetric.isAbnormalDeviation
                    val adrColor = if (isAbnormal) BearishRed else CyanAccent
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAbnormal) BearishRed.copy(alpha = 0.2f) else TerminalSurfaceVariant.copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            if (isAbnormal) BearishRed else TerminalBorder
                        ),
                        modifier = Modifier
                            .clickable { onOpenAdrDashboard() }
                            .testTag("topbar_adr_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isAbnormal) BearishRed else BullishGreen, CircleShape)
                            )
                            Text(
                                text = "ADR ${String.format("%.0f", currentAdrMetric.adrPercentage)}%${if (isAbnormal) " ⚠️" else ""}",
                                fontSize = 10.sp,
                                color = if (isAbnormal) BearishRed else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }

                // Market Session Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TerminalSurfaceVariant.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, TerminalBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(BullishGreen, CircleShape))
                        Text(
                            text = "NY / London",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
