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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
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
import com.example.gemini.GeminiForexAnalyst
import com.example.ui.theme.*
import com.example.ui.viewmodel.ForexViewModel

@Composable
fun AiAnalystScreen(
    viewModel: ForexViewModel,
    modifier: Modifier = Modifier,
    onNavigateToAccuracy: (() -> Unit)? = null
) {
    val selectedPair by viewModel.selectedPair.collectAsState()
    val selectedTimeframe by viewModel.selectedTimeframe.collectAsState()
    val aiAnalysisText by viewModel.aiAnalysisText.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    var userQueryInput by remember { mutableStateOf("") }
    var showInstructionDetail by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPair, selectedTimeframe) {
        if (aiAnalysisText == null) {
            viewModel.requestAiAnalysis()
        }
    }

    val quickQuestions = listOf(
        "Deep Structural Audit",
        "Explain Invalidation & SL",
        "Verify 14-period RSI status",
        "Risk:Reward Confluence Check",
        "Why wait for key S/R zone?"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .padding(horizontal = 14.dp)
            .testTag("ai_analyst_screen_column"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Recharts Accuracy Dashboard Banner
        if (onNavigateToAccuracy != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable { onNavigateToAccuracy() }
                        .testTag("banner_accuracy_analytics"),
                    colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "AI ACCURACY ANALYTICS (RECHARTS)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "View live trendlines & win rates stored in Room DB",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open",
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Mandatory System Directive Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable { showInstructionDetail = !showInstructionDetail }
                    .testTag("system_directive_banner"),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "MANDATORY ANALYST DIRECTIVE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = CyanAccent
                            )
                        }
                        Text(
                            text = if (showInstructionDetail) "Collapse" else "View Rule",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"Act as a disciplined Forex analyst. Focus on analyzing structural price action across major pairs, including US30, EUR/USD, and GBP/USD. Use 50 & 200 EMA to identify trend, wait for pullback to key S/R, confirm entry with RSI-14 reversal, and ensure strict risk management.\"",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp,
                        color = TextPrimary
                    )

                    if (showInstructionDetail) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Full System Instruction:\n${GeminiForexAnalyst.SYSTEM_INSTRUCTION}",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // Active Pair & Action Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${selectedPair.symbol} (${selectedTimeframe.label}) Audit",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Real-time structural confluence analysis",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = { viewModel.requestAiAnalysis() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(TerminalSurfaceVariant, CircleShape)
                        .testTag("refresh_ai_analysis_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Audit",
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Quick Preset Inquiry Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 2.dp).testTag("quick_inquiry_row")
            ) {
                items(quickQuestions) { question ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = TerminalSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                        modifier = Modifier
                            .clickable { viewModel.requestAiAnalysis(question) }
                            .testTag("quick_question_${question.take(10)}")
                    ) {
                        Text(
                            text = question,
                            fontSize = 11.sp,
                            color = CyanAccent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // AI Analyst Output Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
                    .testTag("ai_output_card"),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (isAiLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = CyanAccent,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Disciplined Analyst evaluating EMA 50/200, S/R zones & RSI-14...",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (aiAnalysisText != null) {
                        Text(
                            text = aiAnalysisText ?: "",
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = TextPrimary
                        )
                    } else {
                        Text(
                            text = "Tap 'Refresh Audit' or select a quick question to generate a full disciplined Forex structural analysis.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Ask Custom Question to Analyst
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = TerminalSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth().testTag("custom_query_card")
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = userQueryInput,
                        onValueChange = { userQueryInput = it },
                        placeholder = { Text("Ask disciplined analyst about this pair...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f).testTag("custom_query_text_field"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = TerminalBorder
                        )
                    )

                    IconButton(
                        onClick = {
                            if (userQueryInput.isNotBlank()) {
                                viewModel.requestAiAnalysis(userQueryInput)
                                userQueryInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(CyanAccent, RoundedCornerShape(8.dp))
                            .testTag("send_query_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Question",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}
