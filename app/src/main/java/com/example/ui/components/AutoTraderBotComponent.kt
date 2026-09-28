package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun AutoTraderBotComponent(
    botState: AutoTraderBotState,
    setup: TradeSetup?,
    pair: ForexPair,
    onToggleBot: (Boolean) -> Unit,
    onTriggerManualExecute: () -> Unit,
    onResetAccount: () -> Unit,
    onSetRiskPercent: (Double) -> Unit,
    onToggleTrailingStop: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var showResetDialog by remember { mutableStateOf(false) }

    val allRulesMet = setup != null &&
            setup.isValid &&
            setup.action != TradeAction.WAIT &&
            setup.checklist.isNotEmpty() &&
            setup.checklist.all { it.isMet }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (botState.isEnabled) CyanAccent.copy(alpha = 0.6f) else TerminalBorder,
                RoundedCornerShape(12.dp)
            )
            .animateContentSize()
            .testTag("auto_trader_bot_card"),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Bot Status and Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { isExpanded = !isExpanded }
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (botState.isEnabled) CyanAccent.copy(alpha = 0.15f) else TerminalSurfaceVariant,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Auto-Trader Bot",
                            tint = if (botState.isEnabled) CyanAccent else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "AUTO-TRADER ENGINE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = TextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = BullishGreen.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BullishGreen.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "0 RISK VIRTUAL",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BullishGreen,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (botState.isEnabled) "Automated 4-rule discipline execution ON" else "Execution paused",
                            fontSize = 10.sp,
                            color = if (botState.isEnabled) CyanAccent else TextSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Switch(
                        checked = botState.isEnabled,
                        onCheckedChange = onToggleBot,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanAccent,
                            checkedTrackColor = CyanAccent.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = TerminalSurfaceVariant
                        ),
                        modifier = Modifier.testTag("auto_trader_switch")
                    )

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(24.dp).testTag("auto_trader_expand_button")
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Details",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Always Visible: Capital & Performance Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalBackground, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Virtual Equity", fontSize = 10.sp, color = TextSecondary)
                    Text(
                        text = "$${String.format("%,.2f", botState.totalEquity)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Total Return", fontSize = 10.sp, color = TextSecondary)
                    val pnl = botState.totalPnl
                    val sign = if (pnl >= 0) "+" else ""
                    Text(
                        text = "$sign$${String.format("%.2f", pnl)} (${String.format("%.1f", botState.pnlPercent)}%)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (pnl >= 0) BullishGreen else BearishRed
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Trades", fontSize = 10.sp, color = TextSecondary)
                    Text(
                        text = "${botState.totalTradesExecuted} (${botState.winningTradesCount}W / ${botState.losingTradesCount}L)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Safety Guarantee Callout
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BullishGreen.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BullishGreen.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = BullishGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Zero Financial Risk: Executes safely on a $10,000 paper balance. Real money is never accessed or exposed.",
                                fontSize = 10.sp,
                                color = TextPrimary
                            )
                        }
                    }

                    // 4-Rule Confluence Gatekeeper Status
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TerminalBackground, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "4-RULE AUTOMATION GATEKEEPER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = if (allRulesMet) "GATE OPEN: ALL 4 PASS" else "GATE LOCKED: RULES INCOMPLETE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (allRulesMet) BullishGreen else GoldAccent
                            )
                        }

                        // 4 mini rule checks
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val rules = setup?.checklist ?: emptyList()
                            for (i in 1..4) {
                                val item = rules.getOrNull(i - 1)
                                val isPassed = item?.isMet == true
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isPassed) BullishGreen.copy(alpha = 0.15f) else TerminalSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isPassed) BullishGreen else TerminalBorder
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Rule $i",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPassed) BullishGreen else TextSecondary
                                        )
                                        Text(
                                            text = when (i) {
                                                1 -> "Trend"
                                                2 -> "Pullback"
                                                3 -> "RSI"
                                                else -> "1:2 R:R"
                                            },
                                            fontSize = 8.sp,
                                            color = if (isPassed) TextPrimary else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Risk Management Rules Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Max Risk Per Trade: ${botState.riskPerTradePercent}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(0.5, 1.0, 1.5, 2.0).forEach { pct ->
                                val selected = botState.riskPerTradePercent == pct
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (selected) CyanAccent else TerminalSurfaceVariant,
                                    modifier = Modifier
                                        .clickable { onSetRiskPercent(pct) }
                                        .testTag("bot_risk_pct_$pct")
                                ) {
                                    Text(
                                        text = "$pct%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.Black else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Trailing Stop to Break-Even Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Auto Trail Stop to Break-Even at TP1",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Locks in zero risk once 1:2 R:R is achieved",
                                fontSize = 9.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = botState.trailingStopEnabled,
                            onCheckedChange = onToggleTrailingStop,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyanAccent,
                                checkedTrackColor = CyanAccent.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = TerminalSurfaceVariant
                            ),
                            modifier = Modifier.testTag("bot_trailing_stop_switch")
                        )
                    }

                    // Interactive Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onTriggerManualExecute,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (allRulesMet) CyanAccent else TerminalSurfaceVariant,
                                contentColor = if (allRulesMet) Color.Black else TextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bot_trigger_trade_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (allRulesMet) "Execute Paper Trade" else "Check 4 Rules & Trade",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        OutlinedButton(
                            onClick = { showResetDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("bot_reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset Demo",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Live Bot Action Log Ticker
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TerminalBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = botState.lastBotActionMessage,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary,
                                maxLines = 2,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Risk-Free Demo Account?", color = TextPrimary) },
            text = {
                Text(
                    "This will reset your virtual paper trading equity back to $10,000.00 and clear performance history. Real funds are never affected.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAccount()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                ) {
                    Text("Reset to $10,000", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = TerminalSurface
        )
    }
}
