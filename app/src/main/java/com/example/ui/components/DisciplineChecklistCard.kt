package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TradeAction
import com.example.model.TradeSetup
import com.example.ui.theme.*

@Composable
fun DisciplineChecklistCard(
    setup: TradeSetup?,
    onRunAiAnalysis: () -> Unit,
    onCalculateRisk: () -> Unit,
    onSaveSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedDetails by remember { mutableStateOf(false) }

    if (setup == null) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                if (setup.isValid) BullishGreen.copy(alpha = 0.8f) else GoldAccent.copy(alpha = 0.5f),
                RoundedCornerShape(14.dp)
            )
            .testTag("discipline_checklist_card"),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (setup.isValid) BullishGreen.copy(alpha = 0.15f) else GoldAccent.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Discipline Gate",
                            tint = if (setup.isValid) BullishGreen else GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "DISCIPLINE RADAR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = if (setup.isValid) "All 4 Strict Rules Passed" else "Patience Filter Active",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (setup.isValid) BullishGreen else GoldAccent
                        )
                    }
                }

                // Action Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when (setup.action) {
                        TradeAction.BUY -> BullishGreen
                        TradeAction.SELL -> BearishRed
                        TradeAction.WAIT -> TerminalSurfaceVariant
                    },
                    modifier = Modifier.testTag("setup_action_badge")
                ) {
                    Text(
                        text = setup.action.label,
                        color = if (setup.action == TradeAction.WAIT) TextSecondary else Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4 Strict Checklist Items
            setup.checklist.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(TerminalBackground.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("checklist_item_${index + 1}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (item.isMet) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                        contentDescription = if (item.isMet) "Condition Met" else "Awaiting Condition",
                        tint = if (item.isMet) BullishGreen else GoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${index + 1}. ${item.title}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = item.valueLabel,
                            fontSize = 11.sp,
                            color = if (item.isMet) BullishGreen else TextSecondary
                        )
                    }
                }
            }

            // Summary Text
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = setup.statusSummary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = if (setup.isValid) TextPrimary else TextSecondary,
                modifier = Modifier
                    .background(TerminalSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(8.dp)
                    .fillMaxWidth()
            )

            // Collapsible Detailed Reasoning
            AnimatedVisibility(visible = expandedDetails) {
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(TerminalBackground, RoundedCornerShape(8.dp))
                        .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Technical Structure Breakdown:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = CyanAccent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = setup.detailedReasoning,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            // Toggle Expand Details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedDetails = !expandedDetails }
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expandedDetails) "Hide Technical Breakdown" else "View Technical Breakdown",
                    fontSize = 11.sp,
                    color = CyanAccent
                )
                Icon(
                    imageVector = if (expandedDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCalculateRisk,
                    modifier = Modifier.weight(1f).testTag("action_calc_risk_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("Risk Calc", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onRunAiAnalysis,
                    modifier = Modifier.weight(1.2f).testTag("action_run_ai_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("AI Audit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onSaveSetup,
                    modifier = Modifier.weight(1f).testTag("action_save_journal_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = TerminalSurfaceVariant,
                        contentColor = TextPrimary
                    ),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("Journal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
