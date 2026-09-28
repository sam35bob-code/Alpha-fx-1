package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.analysis.IntelligentEaEngine
import com.example.analysis.TechnicalAnalysisEngine
import com.example.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ForexViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntelligentEaScreen(
    viewModel: ForexViewModel,
    modifier: Modifier = Modifier
) {
    val selectedPair by viewModel.selectedPair.collectAsState()
    val candles by viewModel.candles.collectAsState()
    val indicators by viewModel.indicators.collectAsState()
    val currentSetup by viewModel.currentSetup.collectAsState()
    val activeEaPositions by viewModel.eaActivePositions.collectAsState()
    val isAutoPilotActive by viewModel.eaAutoPilotActive.collectAsState()

    var selectedStrategy by remember { mutableStateOf(IndustryStrategy.QUAD_CONFLUENCE_ALL) }
    var selectedAction by remember { mutableStateOf(TradeAction.BUY) }
    var lotSizeInput by remember { mutableStateOf("0.10") }
    var slPipsInput by remember { mutableStateOf("25") }
    var tpPipsInput by remember { mutableStateOf("50") }

    val currentPrice = candles.lastOrNull()?.close ?: selectedPair.basePrice

    // Computed SL and TP based on pips input
    val slPips = slPipsInput.toDoubleOrNull() ?: 25.0
    val tpPips = tpPipsInput.toDoubleOrNull() ?: 50.0

    val suggestedLevels = remember(selectedPair, currentPrice, selectedAction, slPips, tpPips) {
        val slOffset = slPips * selectedPair.pipSize
        val tpOffset = tpPips * selectedPair.pipSize
        if (selectedAction == TradeAction.BUY) {
            Pair(currentPrice - slOffset, currentPrice + tpOffset)
        } else {
            Pair(currentPrice + slOffset, currentPrice - tpOffset)
        }
    }

    var customSlPrice by remember(suggestedLevels) {
        mutableStateOf(TechnicalAnalysisEngine.formatPrice(suggestedLevels.first, selectedPair))
    }
    var customTpPrice by remember(suggestedLevels) {
        mutableStateOf(TechnicalAnalysisEngine.formatPrice(suggestedLevels.second, selectedPair))
    }

    var trailingStopEnabled by remember { mutableStateOf(true) }
    var marketDropdownExpanded by remember { mutableStateOf(false) }
    var strategyDropdownExpanded by remember { mutableStateOf(false) }

    // Evaluate live strategy condition
    val (strategyTriggerAction, strategyReason) = remember(candles, selectedPair, indicators, selectedStrategy) {
        if (indicators != null) {
            IntelligentEaEngine.evaluateStrategyTrigger(candles, selectedPair, indicators!!, selectedStrategy)
        } else {
            TradeAction.WAIT to "Loading market technical indicators..."
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .padding(14.dp)
            .testTag("intelligent_ea_screen_column"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(CyanAccent.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Intelligent EA",
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "INTELLIGENT EXPERT ADVISOR",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Automated Order Execution • Top 4 Strategies",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Auto-Pilot Toggle
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isAutoPilotActive) BullishGreen.copy(alpha = 0.15f) else TerminalSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isAutoPilotActive) BullishGreen else TerminalBorder
                    ),
                    modifier = Modifier.clickable { viewModel.toggleEaAutoPilot() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isAutoPilotActive) BullishGreen else TextSecondary, CircleShape)
                        )
                        Text(
                            text = if (isAutoPilotActive) "AUTO-PILOT ON" else "PAUSED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isAutoPilotActive) BullishGreen else TextSecondary
                        )
                    }
                }
            }
        }

        // 1. Market Selection Strip (ALL FOREX MARKETS)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth().testTag("ea_market_selector_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "1. SELECT MARKET (ALL FOREX, INDICES & METALS)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = CyanAccent,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box {
                        OutlinedButton(
                            onClick = { marketDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("ea_market_dropdown_btn"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = TerminalBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = selectedPair.symbol,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = selectedPair.name,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    text = TechnicalAnalysisEngine.formatPrice(currentPrice, selectedPair),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = marketDropdownExpanded,
                            onDismissRequest = { marketDropdownExpanded = false },
                            modifier = Modifier.background(TerminalSurface).heightIn(max = 380.dp)
                        ) {
                            ForexPair.ALL_PAIRS.forEach { pair ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(pair.symbol, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                Text(pair.name, fontSize = 10.sp, color = TextSecondary)
                                            }
                                            Text(
                                                pair.category.name.replace('_', ' '),
                                                fontSize = 9.sp,
                                                color = CyanAccent,
                                                modifier = Modifier.padding(start = 12.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectPair(pair)
                                        marketDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Strategy Selector (TOP 4 INDUSTRY STRATEGIES)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth().testTag("ea_strategy_selector_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "2. TOP NUMBER 4 TRADING INDUSTRY STRATEGIES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = CyanAccent,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    IndustryStrategy.values().forEach { strat ->
                        val isSelected = strat == selectedStrategy
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) CyanAccent.copy(alpha = 0.12f) else TerminalBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CyanAccent else TerminalBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { selectedStrategy = strat }
                                .testTag("ea_strat_option_${strat.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isSelected) CyanAccent else TerminalSurfaceVariant
                                        ) {
                                            Text(
                                                text = strat.rankBadge,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isSelected) Color.Black else TextSecondary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = strat.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) TextPrimary else TextSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = strat.description,
                                        fontSize = 9.sp,
                                        color = TextSecondary,
                                        lineHeight = 12.sp
                                    )
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedStrategy = strat },
                                    colors = RadioButtonDefaults.colors(selectedColor = CyanAccent)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Stop Loss & Take Profit Target Parameters
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth().testTag("ea_tp_sl_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "3. DEFINE STOP LOSS & TAKE PROFIT PARAMETERS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = CyanAccent,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Direction Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { selectedAction = TradeAction.BUY },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAction == TradeAction.BUY) BullishGreen else TerminalBackground
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp).testTag("ea_select_buy_btn")
                        ) {
                            Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("BUY / LONG", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { selectedAction = TradeAction.SELL },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAction == TradeAction.SELL) BearishRed else TerminalBackground
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp).testTag("ea_select_sell_btn")
                        ) {
                            Icon(imageVector = Icons.Default.TrendingDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SELL / SHORT", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Stop Loss and Take Profit Inputs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = slPipsInput,
                            onValueChange = { slPipsInput = it },
                            label = { Text("SL (Pips)", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BearishRed,
                                unfocusedBorderColor = TerminalBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f).testTag("ea_sl_pips_input")
                        )

                        OutlinedTextField(
                            value = tpPipsInput,
                            onValueChange = { tpPipsInput = it },
                            label = { Text("TP (Pips)", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BullishGreen,
                                unfocusedBorderColor = TerminalBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f).testTag("ea_tp_pips_input")
                        )

                        OutlinedTextField(
                            value = lotSizeInput,
                            onValueChange = { lotSizeInput = it },
                            label = { Text("Lots", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = TerminalBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f).testTag("ea_lots_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Price Level Display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TerminalBackground, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Execution Stop Loss:", fontSize = 9.sp, color = TextSecondary)
                            Text(
                                text = customSlPrice,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BearishRed,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Current Entry:", fontSize = 9.sp, color = TextSecondary)
                            Text(
                                text = TechnicalAnalysisEngine.formatPrice(currentPrice, selectedPair),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Execution Take Profit:", fontSize = 9.sp, color = TextSecondary)
                            Text(
                                text = customTpPrice,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BullishGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Trailing Stop to Break-Even Checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Trailing Stop to Break-Even", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Automatically locks entry price when 50% TP is reached", fontSize = 9.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = trailingStopEnabled,
                            onCheckedChange = { trailingStopEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = CyanAccent.copy(alpha = 0.3f)),
                            modifier = Modifier.testTag("ea_trailing_switch")
                        )
                    }
                }
            }
        }

        // 4. Live Strategy Confluence Check & Automated Trade Action
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (strategyTriggerAction != TradeAction.WAIT) BullishGreen else TerminalBorder
                ),
                modifier = Modifier.fillMaxWidth().testTag("ea_confluence_action_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = if (strategyTriggerAction != TradeAction.WAIT) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (strategyTriggerAction != TradeAction.WAIT) BullishGreen else GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "STRATEGY CONFLUENCE MONITOR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (strategyTriggerAction != TradeAction.WAIT) BullishGreen.copy(alpha = 0.2f) else TerminalSurfaceVariant
                        ) {
                            Text(
                                text = if (strategyTriggerAction != TradeAction.WAIT) "READY: $strategyTriggerAction" else "SEARCHING",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (strategyTriggerAction != TradeAction.WAIT) BullishGreen else GoldAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strategyReason,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 13.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // ONE-CLICK AUTOMATED TRADE BUTTON
                    Button(
                        onClick = {
                            val lots = lotSizeInput.toDoubleOrNull() ?: 0.10
                            viewModel.executeEaAutoTrade(
                                pair = selectedPair,
                                action = selectedAction,
                                strategy = selectedStrategy,
                                slPrice = suggestedLevels.first,
                                tpPrice = suggestedLevels.second,
                                lots = lots,
                                reason = strategyReason
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("ea_take_trade_automatically_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EXECUTE AUTOMATIC TRADE NOW",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // 5. Active EA Orders Managed in Real-Time
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE EA POSITIONS (${activeEaPositions.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "Managed via Room DB",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }

        if (activeEaPositions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TerminalSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("No Active EA Trades", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Select a market and click 'Execute Automatic Trade Now' or let Auto-Pilot take trades on confluence.", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        } else {
            items(activeEaPositions) { pos ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                    modifier = Modifier.fillMaxWidth().testTag("ea_active_trade_${pos.id}")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (pos.action == TradeAction.BUY) BullishGreen.copy(alpha = 0.2f) else BearishRed.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${pos.action} ${pos.lotSize}L",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (pos.action == TradeAction.BUY) BullishGreen else BearishRed,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = pos.marketSymbol,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            val pnl = pos.currentPnl
                            Text(
                                text = "${if (pnl >= 0) "+" else ""}$${String.format("%.2f", pnl)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (pnl >= 0) BullishGreen else BearishRed,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Entry: ${pos.entryPrice} | SL: ${pos.stopLoss} | TP1: ${pos.takeProfit1}",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                            if (pos.isTrailingActive) {
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = CyanAccent.copy(alpha = 0.2f)
                                ) {
                                    Text("TRAILING BE", fontSize = 8.sp, color = CyanAccent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                }
                            }
                        }

                        if (pos.executionLog.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = pos.executionLog,
                                fontSize = 9.sp,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
