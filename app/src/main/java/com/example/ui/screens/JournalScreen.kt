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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WifiOff
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
import com.example.data.SavedSetupEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.ForexViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JournalScreen(
    viewModel: ForexViewModel,
    modifier: Modifier = Modifier,
    onNavigateToAccuracy: (() -> Unit)? = null
) {
    val setups by viewModel.savedSetups.collectAsState()
    val offlineSignals by viewModel.offlineTradeSignals.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredSetups = remember(setups, selectedFilter) {
        when (selectedFilter) {
            "ALL" -> setups
            "AUTO_BOT" -> setups.filter { it.isAutoExecuted }
            else -> setups.filter { it.status == selectedFilter }
        }
    }

    val wonCount = setups.count { it.status == "WON_TP1" || it.status == "WON_TP2" }
    val lossCount = setups.count { it.status == "STOPPED_OUT" }
    val totalClosed = wonCount + lossCount
    val winRate = if (totalClosed > 0) (wonCount.toDouble() / totalClosed.toDouble()) * 100.0 else 0.0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .padding(horizontal = 14.dp)
            .testTag("journal_screen_column"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Header Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "DISCIPLINE TRADE JOURNAL",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "Audit trail of verified structural executions",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Stats Ribbon
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
                    .testTag("journal_stats_ribbon"),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Total Logged", fontSize = 11.sp, color = TextSecondary)
                        Text(text = "${setups.size}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Win Rate", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = if (totalClosed > 0) "${String.format("%.0f", winRate)}%" else "--",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = if (winRate >= 50.0) BullishGreen else GoldAccent
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Avg Target R:R", fontSize = 11.sp, color = TextSecondary)
                        val avgRr = if (setups.isNotEmpty()) setups.map { it.riskRewardRatio }.average() else 2.2
                        Text(
                            text = "1:${String.format("%.1f", avgRr)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = CyanAccent
                        )
                    }
                }
            }
        }

        // Room DB Offline Storage Status Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp))
                    .testTag("journal_offline_storage_card"),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "ROOM OFFLINE DATABASE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = BullishGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = BullishGreen,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${offlineSignals.size} Signals & OHLCV Historical Prices cached locally",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = "Offline Ready",
                        tint = BullishGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Shortcut to Recharts Accuracy Analytics Dashboard
        if (onNavigateToAccuracy != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable { onNavigateToAccuracy() }
                        .testTag("journal_to_accuracy_card"),
                    colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "AI ACCURACY DASHBOARD (RECHARTS)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Interactive win-rate curves plotted from Room DB",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Filter Pills
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ALL", "ACTIVE", "AUTO_BOT", "WON_TP1", "STOPPED_OUT").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) CyanAccent else TerminalSurfaceVariant,
                        modifier = Modifier
                            .clickable { selectedFilter = filter }
                            .testTag("journal_filter_$filter")
                    ) {
                        Text(
                            text = if (filter == "AUTO_BOT") "⚡ AUTO-BOT" else filter.replace('_', ' '),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Setup Item Cards
        if (filteredSetups.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                        Text(text = "No saved setups in this category", fontSize = 13.sp, color = TextSecondary)
                        Text(text = "Execute or save a setup from the Terminal screen.", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        } else {
            items(filteredSetups, key = { it.id }) { setup ->
                JournalSetupCard(
                    setup = setup,
                    onStatusChange = { newStatus -> viewModel.updateSetupStatus(setup, newStatus) },
                    onDelete = { viewModel.deleteSetup(setup) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun JournalSetupCard(
    setup: SavedSetupEntity,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp))
            .testTag("journal_setup_card_${setup.id}"),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header Row: Pair, Action, Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${setup.pairSymbol} • ${setup.timeframe}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (setup.action == "BUY") BullishGreen.copy(alpha = 0.2f) else BearishRed.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = setup.action,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (setup.action == "BUY") BullishGreen else BearishRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (setup.isAutoExecuted) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CyanAccent.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "⚡ AUTO (${setup.lots}L)",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CyanAccent
                                )
                            }
                        }
                    }
                }

                // Status Badge (Clickable to change status)
                Box {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (setup.status) {
                            "WON_TP1", "WON_TP2" -> BullishGreen
                            "STOPPED_OUT" -> BearishRed
                            "ACTIVE" -> CyanAccent
                            else -> TerminalSurfaceVariant
                        },
                        modifier = Modifier.clickable { showStatusMenu = true }.testTag("status_badge_${setup.id}")
                    ) {
                        Text(
                            text = setup.status.replace('_', ' '),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (setup.status == "PENDING") TextSecondary else Color.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false },
                        modifier = Modifier.background(TerminalSurface)
                    ) {
                        listOf("ACTIVE", "WON_TP1", "WON_TP2", "STOPPED_OUT", "CLOSED").forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.replace('_', ' '), color = TextPrimary) },
                                onClick = {
                                    onStatusChange(status)
                                    showStatusMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Key Price Levels Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalBackground, RoundedCornerShape(6.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Entry", fontSize = 10.sp, color = TextSecondary)
                    Text(text = "${setup.entryPrice}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column {
                    Text(text = "Stop Loss", fontSize = 10.sp, color = TextSecondary)
                    Text(text = "${setup.stopLoss}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = BearishRed)
                }
                Column {
                    Text(text = "TP1 (1:2 R:R)", fontSize = 10.sp, color = TextSecondary)
                    Text(text = "${setup.takeProfit1}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = BullishGreen)
                }
                Column {
                    Text(text = "R:R", fontSize = 10.sp, color = TextSecondary)
                    Text(text = "1:${String.format("%.1f", setup.riskRewardRatio)}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = CyanAccent)
                }
            }

            // Technical Details & Zone
            Text(
                text = "Confluence: 50 EMA (${setup.ema50}) • 200 EMA (${setup.ema200}) • RSI-14 (${String.format("%.1f", setup.rsi14)}) • ${setup.keyZoneLabel}",
                fontSize = 10.sp,
                color = TextSecondary
            )

            if (setup.currentPnl != 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Automated Trade Result:", fontSize = 11.sp, color = TextSecondary)
                    val pnl = setup.currentPnl
                    val sign = if (pnl >= 0) "+" else ""
                    Text(
                        text = "$sign$${String.format("%.2f", pnl)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (pnl >= 0) BullishGreen else BearishRed
                    )
                }
            }

            if (setup.notes.isNotBlank()) {
                Text(
                    text = "Thesis: \"${setup.notes}\"",
                    fontSize = 11.sp,
                    color = TextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TerminalSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(6.dp)
                )
            }

            // Footer with Date and Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(setup.timestamp)),
                    fontSize = 10.sp,
                    color = TextSecondary
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp).testTag("delete_setup_button_${setup.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Setup",
                        tint = BearishRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
