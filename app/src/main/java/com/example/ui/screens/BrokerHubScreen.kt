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
import com.example.model.BrokerPlatform
import com.example.model.BrokerPosition
import com.example.ui.theme.*
import com.example.ui.viewmodel.ForexViewModel

@Composable
fun BrokerHubScreen(
    viewModel: ForexViewModel,
    modifier: Modifier = Modifier
) {
    val credentials by viewModel.brokerCredentials.collectAsState()
    val accountInfo by viewModel.brokerAccountInfo.collectAsState()
    val openPositions by viewModel.brokerOpenPositions.collectAsState()
    val lastMessage by viewModel.brokerLastMessage.collectAsState()
    val selectedPair by viewModel.selectedPair.collectAsState()
    val currentSetup by viewModel.currentSetup.collectAsState()

    var showConnectDialog by remember { mutableStateOf(false) }
    var showLiveRiskWarningDialog by remember { mutableStateOf(false) }
    var tradeLotsInput by remember { mutableStateOf("0.50") }

    val totalFloatingPnl = openPositions.sumOf { it.profit }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .padding(horizontal = 14.dp)
            .testTag("broker_hub_column"),
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
                        imageVector = Icons.Default.Hub,
                        contentDescription = "Broker Hub",
                        tint = CyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "LIVE BROKER GATEWAY",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "${credentials.platform.displayName} • ${accountInfo.serverName}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Ping Latency Chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (accountInfo.isConnected) BullishGreen.copy(alpha = 0.15f) else BearishRed.copy(alpha = 0.15f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (accountInfo.isConnected) BullishGreen else BearishRed
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (accountInfo.isConnected) BullishGreen else BearishRed, CircleShape)
                        )
                        Text(
                            text = if (accountInfo.isConnected) "${accountInfo.pingLatencyMs}ms" else "OFFLINE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (accountInfo.isConnected) BullishGreen else BearishRed
                        )
                    }
                }
            }
        }

        // Live Real Trading Safety Switcher Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (accountInfo.isLiveRealAccount) BearishRed.copy(alpha = 0.8f) else CyanAccent.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    )
                    .testTag("live_trading_mode_card"),
                colors = CardDefaults.cardColors(
                    containerColor = if (accountInfo.isLiveRealAccount) BearishRed.copy(alpha = 0.12f) else TerminalSurface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (accountInfo.isLiveRealAccount) "⚠️ LIVE REAL ACCOUNT ACTIVE" else "🛡️ DEMO / VIRTUAL MODE",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (accountInfo.isLiveRealAccount) BearishRed else CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = if (accountInfo.isLiveRealAccount)
                                "Real capital is deployed on broker market orders with 1-2% risk enforcement."
                            else
                                "Orders routed in safe virtual paper mode. No real capital at risk.",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            lineHeight = 14.sp
                        )
                    }

                    Switch(
                        checked = accountInfo.isLiveRealAccount,
                        onCheckedChange = { enableLive ->
                            if (enableLive) {
                                showLiveRiskWarningDialog = true
                            } else {
                                viewModel.setLiveRealTradingMode(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BearishRed,
                            checkedTrackColor = BearishRed.copy(alpha = 0.35f),
                            uncheckedThumbColor = CyanAccent,
                            uncheckedTrackColor = TerminalSurfaceVariant
                        ),
                        modifier = Modifier.testTag("switch_live_real_trading")
                    )
                }
            }
        }

        // Live Execution Notification Banner
        if (lastMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TerminalSurfaceVariant,
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Text(text = lastMessage ?: "", fontSize = 10.sp, color = TextPrimary, lineHeight = 14.sp)
                    }
                }
            }
        }

        // Broker Account Telemetry Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
                    .testTag("broker_telemetry_card"),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = accountInfo.brokerName.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Acct #${accountInfo.accountNumber} • 1:${accountInfo.leverage} Leverage",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }

                        Button(
                            onClick = { showConnectDialog = true },
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("btn_configure_broker"),
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalSurfaceVariant),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyanAccent)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Switch Broker", fontSize = 10.sp, color = TextPrimary)
                        }
                    }

                    HorizontalDivider(color = TerminalBorder, thickness = 1.dp)

                    // Big Balance & Equity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Account Balance", fontSize = 9.sp, color = TextSecondary)
                            Text(
                                "$${String.format("%,.2f", accountInfo.balance)} ${accountInfo.currency}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Live Equity", fontSize = 9.sp, color = TextSecondary)
                            Text(
                                "$${String.format("%,.2f", accountInfo.equity)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (totalFloatingPnl >= 0) BullishGreen else BearishRed,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Margin & Risk Telemetry Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Margin Used", fontSize = 9.sp, color = TextSecondary)
                            Text(
                                "$${String.format("%,.2f", accountInfo.margin)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column {
                            Text("Free Margin", fontSize = 9.sp, color = TextSecondary)
                            Text(
                                "$${String.format("%,.2f", accountInfo.freeMargin)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Margin Level", fontSize = 9.sp, color = TextSecondary)
                            Text(
                                "${String.format("%,.1f", accountInfo.marginLevelPercent)}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (accountInfo.marginLevelPercent >= 500) BullishGreen else GoldAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Spreads Banner
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TerminalBackground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Live Spreads:", fontSize = 10.sp, color = TextSecondary)
                            Text("EUR/USD: ${accountInfo.spreadEurUsd} pips", fontSize = 10.sp, color = CyanAccent, fontFamily = FontFamily.Monospace)
                            Text("US30: ${accountInfo.spreadUs30} pts", fontSize = 10.sp, color = CyanAccent, fontFamily = FontFamily.Monospace)
                            Text("GBP/USD: ${accountInfo.spreadGbpUsd} pips", fontSize = 10.sp, color = CyanAccent, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Live Order Execution Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyanAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .testTag("broker_order_execution_card"),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE MARKET ORDER EXECUTION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${selectedPair.symbol} Market",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    val setup = currentSetup
                    val entryPrice = setup?.entryPrice ?: 1.0850
                    val sl = setup?.stopLoss ?: (entryPrice - 0.0030)
                    val tp = setup?.takeProfit1 ?: (entryPrice + 0.0060)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tradeLotsInput,
                            onValueChange = { tradeLotsInput = it },
                            label = { Text("Lots", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("input_broker_lots"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = TerminalBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Button(
                            onClick = {
                                val lots = tradeLotsInput.toDoubleOrNull() ?: 0.50
                                viewModel.executeLiveBrokerOrder(
                                    symbol = selectedPair.symbol,
                                    action = "BUY",
                                    lots = lots,
                                    entryPrice = entryPrice,
                                    stopLoss = sl,
                                    takeProfit = tp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("btn_broker_buy"),
                            colors = ButtonDefaults.buttonColors(containerColor = BullishGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("BUY ${selectedPair.symbol}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        Button(
                            onClick = {
                                val lots = tradeLotsInput.toDoubleOrNull() ?: 0.50
                                viewModel.executeLiveBrokerOrder(
                                    symbol = selectedPair.symbol,
                                    action = "SELL",
                                    lots = lots,
                                    entryPrice = entryPrice,
                                    stopLoss = sl,
                                    takeProfit = tp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("btn_broker_sell"),
                            colors = ButtonDefaults.buttonColors(containerColor = BearishRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("SELL ${selectedPair.symbol}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Open Broker Positions Header & Panic Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OPEN BROKER POSITIONS (${openPositions.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )

                if (openPositions.isNotEmpty()) {
                    Button(
                        onClick = { viewModel.emergencyLiquidateBrokerPositions() },
                        colors = ButtonDefaults.buttonColors(containerColor = BearishRed),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("btn_emergency_close_all"),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CLOSE ALL POSITIONS", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Open Positions List
        if (openPositions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No open positions on broker account. Account flat.", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        } else {
            items(openPositions) { pos ->
                BrokerPositionCard(
                    position = pos,
                    onClose = { viewModel.closeBrokerPosition(pos.ticketId) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    // Modal: Connect to Any Broker Account
    if (showConnectDialog) {
        ConnectBrokerDialog(
            currentCredentials = credentials,
            onDismiss = { showConnectDialog = false },
            onConnect = { platform, broker, server, acct, pwd, url, isLive ->
                viewModel.updateBrokerCredentials(platform, broker, server, acct, pwd, url, isLive)
                showConnectDialog = false
            }
        )
    }

    // Modal: Live Capital Risk Warning Confirmation
    if (showLiveRiskWarningDialog) {
        AlertDialog(
            onDismissRequest = { showLiveRiskWarningDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = BearishRed)
                    Text("LIVE CAPITAL RISK WARNING", fontSize = 14.sp, fontWeight = FontWeight.Black, color = BearishRed)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "You are about to activate LIVE REAL TRADING on your connected broker account (${accountInfo.brokerName}).",
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    Text(
                        "• All orders will execute with real funds in the broker's live market.\n" +
                        "• Strict 1% risk management and SL targets are enforced.\n" +
                        "• The Emergency Close All button remains active at all times.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setLiveRealTradingMode(true)
                        showLiveRiskWarningDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BearishRed),
                    modifier = Modifier.testTag("btn_confirm_live_trading")
                ) {
                    Text("I Acknowledge & Enable Live Trading", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLiveRiskWarningDialog = false }) {
                    Text("Keep in Demo Mode", fontSize = 11.sp)
                }
            },
            containerColor = TerminalSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun BrokerPositionCard(
    position: BrokerPosition,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
            .testTag("broker_ticket_${position.ticketId}"),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${position.symbol} ${position.type}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (position.type == "BUY") BullishGreen else BearishRed,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• ${position.lots} Lots",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "#${position.ticketId}",
                        fontSize = 9.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "Open: ${position.openPrice} ➔ Current: ${position.currentPrice}",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "SL: ${position.stopLoss} | TP: ${position.takeProfit}",
                    fontSize = 10.sp,
                    color = TextTertiaryHelper,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${if (position.profit >= 0) "+" else ""}$${String.format("%.2f", position.profit)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = if (position.profit >= 0) BullishGreen else BearishRed,
                    fontFamily = FontFamily.Monospace
                )

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalSurfaceVariant),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(26.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Close", fontSize = 9.sp, color = TextPrimary)
                }
            }
        }
    }
}

val TextTertiaryHelper = Color(0xFF8A99AD)

@Composable
fun ConnectBrokerDialog(
    currentCredentials: com.example.model.BrokerCredentials,
    onDismiss: () -> Unit,
    onConnect: (BrokerPlatform, String, String, String, String, String, Boolean) -> Unit
) {
    var selectedPlatform by remember { mutableStateOf(currentCredentials.platform) }
    var brokerNameInput by remember { mutableStateOf(currentCredentials.brokerName) }
    var serverNameInput by remember { mutableStateOf(currentCredentials.serverName) }
    var accountNumberInput by remember { mutableStateOf(currentCredentials.accountNumber) }
    var passwordInput by remember { mutableStateOf("") }
    var bridgeUrlInput by remember { mutableStateOf(currentCredentials.bridgeUrl) }
    var isLiveReal by remember { mutableStateOf(currentCredentials.isLiveRealAccount) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("CONNECT ANY BROKER ACCOUNT", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextPrimary)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Platform Selector
                Text("Select Trading Protocol / Broker Platform:", fontSize = 10.sp, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BrokerPlatform.values().take(3).forEach { p ->
                        FilterChip(
                            selected = selectedPlatform == p,
                            onClick = {
                                selectedPlatform = p
                                serverNameInput = p.defaultServer
                            },
                            label = { Text(p.name, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent.copy(alpha = 0.2f),
                                selectedLabelColor = CyanAccent
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = brokerNameInput,
                    onValueChange = { brokerNameInput = it },
                    label = { Text("Broker Name (e.g. IC Markets, FTMO, Exness)", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = serverNameInput,
                    onValueChange = { serverNameInput = it },
                    label = { Text("Server (e.g. ICMarketsSC-Live02, FTMO-Server)", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = accountNumberInput,
                    onValueChange = { accountNumberInput = it },
                    label = { Text("Account Login / Number", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Master Password / API Token", fontSize = 10.sp) },
                    singleLine = true,
                    placeholder = { Text("Optional for demo, required for live") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bridgeUrlInput,
                    onValueChange = { bridgeUrlInput = it },
                    label = { Text("Broker Gateway / Webhook Bridge Endpoint", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConnect(
                        selectedPlatform,
                        brokerNameInput,
                        serverNameInput,
                        accountNumberInput,
                        passwordInput.ifEmpty { "••••••••" },
                        bridgeUrlInput,
                        isLiveReal
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                modifier = Modifier.testTag("btn_save_broker_connection")
            ) {
                Text("Connect & Sync Broker", fontSize = 11.sp, color = TerminalBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", fontSize = 11.sp)
            }
        },
        containerColor = TerminalSurface,
        shape = RoundedCornerShape(12.dp)
    )
}
