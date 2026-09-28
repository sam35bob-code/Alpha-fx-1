package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.RiskManagementComponent
import com.example.ui.theme.*
import com.example.ui.viewmodel.ForexViewModel

@Composable
fun RiskCalculatorScreen(
    viewModel: ForexViewModel,
    modifier: Modifier = Modifier
) {
    val selectedPair by viewModel.selectedPair.collectAsState()
    val calcState by viewModel.calculatorState.collectAsState()
    val setup by viewModel.currentSetup.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSavedMessage by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = TerminalBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp)
                .testTag("risk_calculator_screen_column"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "STRICT RISK CALCULATOR",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "Automated Position Sizing & Capital Preservation Engine",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Dedicated Reusable Risk Management UI Component
            item {
                RiskManagementComponent(
                    selectedPair = selectedPair,
                    calcState = calcState,
                    setup = setup,
                    onUpdateCalculation = { balance, risk, sl ->
                        viewModel.updateCalculator(balance, risk, sl)
                    },
                    initialExpanded = true,
                    showCardHeader = true,
                    onSaveToJournal = {
                        viewModel.saveCurrentSetup(
                            "Position Sized: ${calcState.calculatedLots} Lots (${calcState.riskPercentage}% Risk = $${String.format(java.util.Locale.US, "%.2f", calcState.totalDollarRisk)})"
                        )
                        showSavedMessage = true
                    }
                )
            }

            // Institutional Position Sizing Formula Breakdown
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
                        .testTag("sizing_formula_breakdown_card"),
                    colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "MATHEMATICAL POSITION FORMULA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = CyanAccent
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TerminalBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Lots = (Account Balance × Risk %) / (Stop-Loss Pips × Pip Value)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Text(
                            text = "• Current Asset (${selectedPair.symbol}): 1 Standard Lot = $${String.format(java.util.Locale.US, "%.2f", selectedPair.pipValuePerStandardLot)} per ${if (selectedPair.symbol == "US30") "point" else "pip"}.\n" +
                                    "• Dollar at Risk: $${String.format(java.util.Locale.US, "%,.2f", calcState.accountBalance)} × ${calcState.riskPercentage}% = $${String.format(java.util.Locale.US, "%.2f", calcState.totalDollarRisk)}.\n" +
                                    "• Stop-Loss Distance: ${String.format(java.util.Locale.US, "%.1f", calcState.stopLossPipsOrPoints)} ${if (selectedPair.symbol == "US30") "Points" else "Pips"}.\n" +
                                    "• Result: Exact lot sizing of ${calcState.calculatedLots} Lots ensures your account never loses more than planned.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // Strict Discipline Manifesto
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TerminalBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                    modifier = Modifier.fillMaxWidth().testTag("risk_manifesto_surface")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "RULE 4: STRICT RISK MANAGEMENT CONVENTION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. Stop Loss must be determined by market structure invalidation (swing high/low), never arbitrary monetary limits.\n" +
                                    "2. Position size MUST automatically adapt to the stop-loss distance—never widen or tighten your stop to fit a lot size.\n" +
                                    "3. Only execute setups offering a minimum 1:2.0 Risk-to-Reward ratio.\n" +
                                    "4. Risking >2% per trade violates strict Forex analyst discipline.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }

        if (showSavedMessage) {
            AlertDialog(
                onDismissRequest = { showSavedMessage = false },
                title = { Text("Setup Logged to Journal", fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    Text(
                        "The position size of ${calcState.calculatedLots} Lots and risk parameters for ${selectedPair.symbol} have been saved to your local database journal.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showSavedMessage = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = androidx.compose.ui.graphics.Color.Black)
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = TerminalSurface
            )
        }
    }
}
