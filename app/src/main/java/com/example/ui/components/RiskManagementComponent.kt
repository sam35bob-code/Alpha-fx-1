package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ForexPair
import com.example.model.TradeSetup
import com.example.ui.theme.*
import com.example.ui.viewmodel.CalculatorState
import kotlin.math.roundToInt

@Composable
fun RiskManagementComponent(
    selectedPair: ForexPair,
    calcState: CalculatorState,
    setup: TradeSetup?,
    onUpdateCalculation: (balance: Double, riskPercent: Double, stopLossPips: Double) -> Unit,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = true,
    showCardHeader: Boolean = true,
    onSaveToJournal: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var balanceInput by remember(calcState.accountBalance) {
        mutableStateOf(calcState.accountBalance.toInt().toString())
    }
    var riskPercentInput by remember(calcState.riskPercentage) {
        mutableStateOf(String.format(java.util.Locale.US, "%.1f", calcState.riskPercentage))
    }
    var slPipsInput by remember(calcState.stopLossPipsOrPoints) {
        mutableStateOf(String.format(java.util.Locale.US, "%.1f", calcState.stopLossPipsOrPoints))
    }
    var isExpanded by remember { mutableStateOf(initialExpanded) }

    val currentRiskPercent = riskPercentInput.toDoubleOrNull() ?: calcState.riskPercentage
    val currentBalance = balanceInput.toDoubleOrNull() ?: calcState.accountBalance
    val currentSlPips = slPipsInput.toDoubleOrNull() ?: calcState.stopLossPipsOrPoints

    // Discipline Color Logic:
    // <= 1.0% = BullishGreen (Strict Discipline Compliant)
    // 1.01% - 2.0% = GoldAccent (Moderate Risk Warning)
    // > 2.0% = BearishRed (Rule Breach)
    val riskColor by animateColorAsState(
        targetValue = when {
            currentRiskPercent <= 1.0 -> BullishGreen
            currentRiskPercent <= 2.0 -> GoldAccent
            else -> BearishRed
        },
        label = "riskColorAnim"
    )

    fun notifyCalculation(b: Double = currentBalance, r: Double = currentRiskPercent, sl: Double = currentSlPips) {
        onUpdateCalculation(b.coerceAtLeast(10.0), r.coerceIn(0.1, 10.0), sl.coerceAtLeast(1.0))
    }

    val unitName = if (selectedPair.symbol == "US30") "Points" else "Pips"

    // Lot variations
    val standardLots = calcState.calculatedLots
    val miniLots = (standardLots * 10.0 * 100.0).roundToInt() / 100.0
    val microLots = (standardLots * 100.0 * 10.0).roundToInt() / 10.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (currentRiskPercent > 2.0) BearishRed.copy(alpha = 0.8f) else TerminalBorder,
                RoundedCornerShape(14.dp)
            )
            .testTag("risk_management_component"),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Optional Card Header with Expand/Collapse
            if (showCardHeader) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded },
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
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "Risk Calculator Icon",
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "AUTOMATIC POSITION SIZER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = CyanAccent
                            )
                            Text(
                                text = "Risk Management Engine (${selectedPair.symbol})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    // Quick Summary Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TerminalSurfaceVariant,
                            border = BorderStroke(1.dp, TerminalBorder)
                        ) {
                            Text(
                                text = "${calcState.calculatedLots} Lots",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = CyanAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.size(28.dp).testTag("toggle_expand_sizer_btn")
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse Sizer" else "Expand Sizer",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sync with active chart setup button (if SL distance detected)
                    if (setup != null && setup.riskPipsOrPoints > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TerminalBackground,
                            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().testTag("sync_setup_sl_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Chart Structure SL: ${String.format(java.util.Locale.US, "%.1f", setup.riskPipsOrPoints)} $unitName",
                                        fontSize = 11.sp,
                                        color = TextPrimary
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        slPipsInput = String.format(java.util.Locale.US, "%.1f", setup.riskPipsOrPoints)
                                        notifyCalculation(sl = setup.riskPipsOrPoints)
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("apply_chart_sl_button")
                                ) {
                                    Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                                }
                            }
                        }
                    }

                    // INPUT 1: Account Balance
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Account Balance",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Text(
                                text = "Equity: $${String.format(java.util.Locale.US, "%,.2f", currentBalance)}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                        }

                        OutlinedTextField(
                            value = balanceInput,
                            onValueChange = { newVal ->
                                balanceInput = newVal.filter { it.isDigit() || it == '.' }
                                val parsed = balanceInput.toDoubleOrNull()
                                if (parsed != null && parsed > 0) {
                                    notifyCalculation(b = parsed)
                                }
                            },
                            prefix = { Text("$", color = CyanAccent, fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = TerminalBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_account_balance")
                        )

                        // Preset Balance Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp).testTag("balance_presets_row")
                        ) {
                            items(listOf(1000, 5000, 10000, 25000, 50000, 100000)) { preset ->
                                val isSelected = currentBalance.toInt() == preset
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) CyanAccent.copy(alpha = 0.2f) else TerminalSurfaceVariant,
                                    border = BorderStroke(1.dp, if (isSelected) CyanAccent else TerminalBorder),
                                    modifier = Modifier
                                        .clickable {
                                            balanceInput = preset.toString()
                                            notifyCalculation(b = preset.toDouble())
                                        }
                                        .testTag("preset_balance_$preset")
                                ) {
                                    Text(
                                        text = "$${preset / 1000}k",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) CyanAccent else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // INPUT 2: Stop-Loss Pips / Points
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Stop-Loss Distance ($unitName)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Text(
                                text = "Invalidation Buffer",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = slPipsInput,
                                onValueChange = { newVal ->
                                    slPipsInput = newVal.filter { it.isDigit() || it == '.' }
                                    val parsed = slPipsInput.toDoubleOrNull()
                                    if (parsed != null && parsed > 0) {
                                        notifyCalculation(sl = parsed)
                                    }
                                },
                                suffix = { Text(unitName, color = TextSecondary, fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = TerminalBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_stop_loss_pips")
                            )

                            // Step Adjust Buttons (-5, +5, -1, +1)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilledTonalIconButton(
                                    onClick = {
                                        val newVal = (currentSlPips - 5.0).coerceAtLeast(1.0)
                                        slPipsInput = String.format(java.util.Locale.US, "%.1f", newVal)
                                        notifyCalculation(sl = newVal)
                                    },
                                    modifier = Modifier.size(44.dp).testTag("sl_minus_5_btn"),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = TerminalSurfaceVariant
                                    )
                                ) {
                                    Text("-5", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }

                                FilledTonalIconButton(
                                    onClick = {
                                        val newVal = currentSlPips + 5.0
                                        slPipsInput = String.format(java.util.Locale.US, "%.1f", newVal)
                                        notifyCalculation(sl = newVal)
                                    },
                                    modifier = Modifier.size(44.dp).testTag("sl_plus_5_btn"),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = TerminalSurfaceVariant
                                    )
                                ) {
                                    Text("+5", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                        }
                    }

                    // INPUT 3: Risk Percentage Slider & Discipline Gate
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Risk Percentage per Trade",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(riskColor, CircleShape)
                                )
                            }

                            Text(
                                text = "${String.format(java.util.Locale.US, "%.1f", currentRiskPercent)}% ($${String.format(java.util.Locale.US, "%.2f", calcState.totalDollarRisk)})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = riskColor
                            )
                        }

                        // Risk Slider (0.25% to 5.0%)
                        Slider(
                            value = currentRiskPercent.toFloat().coerceIn(0.25f, 5.0f),
                            onValueChange = { newVal ->
                                val rounded = (newVal * 10f).roundToInt() / 10.0
                                riskPercentInput = String.format(java.util.Locale.US, "%.1f", rounded)
                                notifyCalculation(r = rounded)
                            },
                            valueRange = 0.25f..5.0f,
                            steps = 18,
                            colors = SliderDefaults.colors(
                                thumbColor = riskColor,
                                activeTrackColor = riskColor,
                                inactiveTrackColor = TerminalBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("risk_percentage_slider")
                        )

                        // Strict Discipline Quick Selectors
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(0.5, 1.0, 1.5, 2.0).forEach { r ->
                                    val isSelected = (currentRiskPercent - r).let { kotlin.math.abs(it) < 0.05 }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) riskColor.copy(alpha = 0.2f) else TerminalSurfaceVariant,
                                        border = BorderStroke(1.dp, if (isSelected) riskColor else TerminalBorder),
                                        modifier = Modifier
                                            .clickable {
                                                riskPercentInput = String.format(java.util.Locale.US, "%.1f", r)
                                                notifyCalculation(r = r)
                                            }
                                            .testTag("preset_risk_${r.toString().replace('.', '_')}")
                                    ) {
                                        Text(
                                            text = "$r%",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) riskColor else TextSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = if (currentRiskPercent <= 1.0) "Strict 1% Cap" else if (currentRiskPercent <= 2.0) "Max Cap (2%)" else "High Risk!",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = riskColor
                            )
                        }

                        // Discipline Warning Notice if > 2.0%
                        if (currentRiskPercent > 2.0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BearishRed.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, BearishRed.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .testTag("strict_risk_warning_box")
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Risk Warning",
                                        tint = BearishRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Risk exceeds 2.0%! Disciplined trading requires capping single-trade risk at 1-2% max to prevent account drawdown.",
                                        fontSize = 10.sp,
                                        color = BearishRed,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = TerminalBorder.copy(alpha = 0.6f))

                    // AUTOMATIC SIZING OUTPUT CARD
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = TerminalBackground,
                        border = BorderStroke(1.5.dp, CyanAccent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().testTag("position_size_output_surface")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RECOMMENDED POSITION SIZE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    color = CyanAccent
                                )
                                Text(
                                    text = "${selectedPair.symbol} ($${String.format(java.util.Locale.US, "%.1f", selectedPair.pipValuePerStandardLot)}/pip)",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }

                            // Main Lot Display
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "$standardLots",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyanAccent,
                                        modifier = Modifier.testTag("calculated_position_size_lots")
                                    )
                                    Text(
                                        text = "Standard Lots",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$miniLots Mini Lots",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "$microLots Micro Lots",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary
                                    )
                                }
                            }

                            HorizontalDivider(color = TerminalBorder.copy(alpha = 0.4f))

                            // P&L Targets Matrix
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Max Loss
                                Column {
                                    Text("Max Loss (SL)", fontSize = 10.sp, color = TextSecondary)
                                    Text(
                                        "-$${String.format(java.util.Locale.US, "%.2f", calcState.totalDollarRisk)}",
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = BearishRed,
                                        modifier = Modifier.testTag("max_dollar_loss_text")
                                    )
                                }

                                // TP1 (1:2 R:R)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("TP1 (1:2 R:R)", fontSize = 10.sp, color = TextSecondary)
                                    Text(
                                        "+$${String.format(java.util.Locale.US, "%.2f", calcState.potentialProfitTp1)}",
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = BullishGreen,
                                        modifier = Modifier.testTag("profit_tp1_text")
                                    )
                                }

                                // TP2 (1:3+ R:R)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("TP2 (1:3.2 R:R)", fontSize = 10.sp, color = TextSecondary)
                                    Text(
                                        "+$${String.format(java.util.Locale.US, "%.2f", calcState.potentialProfitTp2)}",
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = BullishGreen,
                                        modifier = Modifier.testTag("profit_tp2_text")
                                    )
                                }
                            }
                        }
                    }

                    // Action Buttons (Copy Sizing & Optional Journal Button)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val summary = buildString {
                                    append("AlphaFX Position Size for ${selectedPair.symbol}:\n")
                                    append("• Standard Lots: $standardLots\n")
                                    append("• Account Balance: $${String.format(java.util.Locale.US, "%.2f", currentBalance)}\n")
                                    append("• Risk: ${String.format(java.util.Locale.US, "%.1f", currentRiskPercent)}% ($${String.format(java.util.Locale.US, "%.2f", calcState.totalDollarRisk)})\n")
                                    append("• Stop Loss: ${String.format(java.util.Locale.US, "%.1f", currentSlPips)} $unitName\n")
                                    append("• TP1 (1:2 R:R): +$${String.format(java.util.Locale.US, "%.2f", calcState.potentialProfitTp1)}\n")
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("AlphaFX Risk Sizing", summary)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Position size copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("copy_position_size_btn"),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, TerminalBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Sizing", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (onSaveToJournal != null) {
                            Button(
                                onClick = onSaveToJournal,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("save_sized_trade_btn"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanAccent,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log to Journal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
