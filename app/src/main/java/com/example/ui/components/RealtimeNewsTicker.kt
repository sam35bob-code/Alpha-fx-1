package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun RealtimeNewsTicker(
    newsItems: List<ForexNewsItem>,
    currentIndex: Int,
    isPaused: Boolean,
    isFilteredByPair: Boolean,
    selectedPair: ForexPair,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleFilter: () -> Unit,
    onRefreshNews: () -> Unit,
    onSelectNews: (ForexNewsItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (newsItems.isEmpty()) return

    val safeIndex = currentIndex.coerceIn(0, newsItems.size - 1)
    val activeItem = newsItems[safeIndex]

    // Pulsing live dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_live")
    val liveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveAlpha"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = TerminalSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("realtime_news_ticker_container")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Top Bar: Live indicator, Category, Counter, Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Pulse + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (activeItem.isBreaking) BearishRed.copy(alpha = liveAlpha)
                                else CyanAccent.copy(alpha = liveAlpha)
                            )
                    )
                    Text(
                        text = if (activeItem.isBreaking) "BREAKING WIRE" else "MACRO WIRE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                        color = if (activeItem.isBreaking) BearishRed else CyanAccent
                    )
                    Text(
                        text = "• ${activeItem.category}",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Controls: Filter Chip + Navigation Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Filter Toggle Chip
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isFilteredByPair) CyanAccent.copy(alpha = 0.18f) else TerminalSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isFilteredByPair) CyanAccent.copy(alpha = 0.5f) else TerminalBorder
                        ),
                        modifier = Modifier
                            .clickable { onToggleFilter() }
                            .testTag("news_filter_chip")
                    ) {
                        Text(
                            text = if (isFilteredByPair) selectedPair.symbol else "All Pairs",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFilteredByPair) CyanAccent else TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    // Index indicator
                    Text(
                        text = "${safeIndex + 1}/${newsItems.size}",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )

                    // Prev Button
                    IconButton(
                        onClick = onPrev,
                        modifier = Modifier.size(24.dp).testTag("news_prev_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous News",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Play / Pause Button
                    IconButton(
                        onClick = onTogglePause,
                        modifier = Modifier.size(24.dp).testTag("news_pause_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume Ticker" else "Pause Ticker",
                            tint = if (isPaused) GoldAccent else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Next Button
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(24.dp).testTag("news_next_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next News",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Refresh Button
                    IconButton(
                        onClick = onRefreshNews,
                        modifier = Modifier.size(24.dp).testTag("news_refresh_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Market News",
                            tint = CyanAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Clickable Headline Body with Animated Content
            AnimatedContent(
                targetState = activeItem,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "news_ticker_anim",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectNews(activeItem) }
                    .testTag("news_ticker_item_card")
            ) { item ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Badges Row: Pair + Impact + Sentiment + TimeAgo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Pair Pill
                        val pairColor = when (item.pairSymbol) {
                            "US30" -> GoldAccent
                            "EUR/USD" -> CyanAccent
                            "GBP/USD" -> PurpleAccent
                            "USD/JPY" -> Color(0xFFFF80AB)
                            "XAU/USD" -> GoldAccent
                            else -> CyanAccent
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = pairColor.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, pairColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = item.pairSymbol,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = pairColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Impact Pill
                        val impactColor = when (item.impact) {
                            NewsImpact.HIGH -> BearishRed
                            NewsImpact.MEDIUM -> GoldAccent
                            NewsImpact.LOW -> NeutralGray
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = impactColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = item.impact.label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = impactColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }

                        // Sentiment Pill
                        val (sentimentColor, sentimentText) = when (item.sentiment) {
                            FundamentalSentiment.BULLISH -> BullishGreen to "▲ BULLISH"
                            FundamentalSentiment.BEARISH -> BearishRed to "▼ BEARISH"
                            FundamentalSentiment.NEUTRAL -> TextSecondary to "● NEUTRAL"
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = sentimentColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = sentimentText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = sentimentColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = "${item.source} • ${item.timeAgo}",
                            fontSize = 9.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Headline
                    Text(
                        text = item.headline,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                        lineHeight = 17.sp,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Context Snippet / Confluence
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "💡 ${item.fundamentalContext}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Analysis",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyanAccent
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View Fundamental Analysis",
                                tint = CyanAccent,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Detailed Fundamental Intelligence & Technical Confluence Modal Sheet / Dialog
 */
@Composable
fun FundamentalIntelligenceDialog(
    newsItem: ForexNewsItem,
    currentIndicators: TechnicalIndicators?,
    currentPair: ForexPair,
    aiBriefing: String?,
    isAiLoading: Boolean,
    onDismiss: () -> Unit,
    onSelectPair: (ForexPair) -> Unit,
    onRequestAiBriefing: (ForexNewsItem) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = TerminalSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .testTag("fundamental_intelligence_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(CyanAccent.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Fundamental Analysis",
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "FUNDAMENTAL INTELLIGENCE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = CyanAccent
                            )
                            Text(
                                text = "${newsItem.pairSymbol} • ${newsItem.category}",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp).testTag("dialog_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Dialog",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = TerminalBorder,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Headline Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = TerminalSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val (sentimentColor, sentimentText) = when (newsItem.sentiment) {
                                    FundamentalSentiment.BULLISH -> BullishGreen to "▲ BULLISH BIAS"
                                    FundamentalSentiment.BEARISH -> BearishRed to "▼ BEARISH BIAS"
                                    FundamentalSentiment.NEUTRAL -> TextSecondary to "● NEUTRAL BIAS"
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = sentimentColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = sentimentText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = sentimentColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Text(
                                    text = "${newsItem.source} • ${newsItem.timeAgo}",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = newsItem.headline,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = newsItem.summary,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Economic Data Indicators (if available)
                    if (newsItem.economicData.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = TerminalSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "📊 ECONOMIC DATA METRICS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    newsItem.economicData.forEach { (metricName, metricValue) ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = TerminalSurface,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = metricName,
                                                    fontSize = 9.sp,
                                                    color = TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = metricValue,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = TextPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Fundamental Context Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = TerminalSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = "Macro",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "1. MACRO ECONOMIC CONTEXT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = newsItem.fundamentalContext,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    // Technical Confluence Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = TerminalSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CandlestickChart,
                                    contentDescription = "Technicals",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "2. TECHNICAL CONFLUENCE (50/200 EMA & RSI-14)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = newsItem.technicalConfluence,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 17.sp
                            )

                            currentIndicators?.let { ind ->
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = TerminalBorder)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Current Pair: ${currentPair.symbol}",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "RSI(14): ${String.format("%.1f", ind.rsi14)}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ind.rsi14 < 30 || ind.rsi14 > 70) GoldAccent else CyanAccent
                                    )
                                    Text(
                                        text = if (ind.ema50 > ind.ema200) "EMA: Bullish" else "EMA: Bearish",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ind.ema50 > ind.ema200) BullishGreen else BearishRed
                                    )
                                }
                            }
                        }
                    }

                    // Strict Risk Management Rule
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BearishRed.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BearishRed.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Risk Discipline",
                                tint = BearishRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "3. STRICT 1% - 2% RISK DISCIPLINE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BearishRed
                                )
                                Text(
                                    text = "Never trade high-impact news impulses without established stop loss and structural support/resistance invalidation levels.",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // AI Strategic Briefing Section
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = TerminalSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.4f))
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
                                        contentDescription = "AI",
                                        tint = PurpleAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "AI FUNDAMENTAL SYNTHESIS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PurpleAccent
                                    )
                                }

                                if (!isAiLoading && aiBriefing == null) {
                                    Button(
                                        onClick = { onRequestAiBriefing(newsItem) },
                                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp).testTag("generate_ai_briefing_btn")
                                    ) {
                                        Text("Generate Briefing", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (isAiLoading) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = PurpleAccent,
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "Synthesizing fundamental driver with technical indicators...",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            aiBriefing?.let { text ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = text,
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val matchingPair = ForexPair.ALL_PAIRS.firstOrNull {
                        it.symbol.equals(newsItem.pairSymbol, ignoreCase = true)
                    }

                    if (matchingPair != null && matchingPair.symbol != currentPair.symbol) {
                        Button(
                            onClick = {
                                onSelectPair(matchingPair)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            modifier = Modifier.weight(1f).testTag("switch_to_news_pair_btn")
                        ) {
                            Text(
                                text = "Switch Chart to ${matchingPair.symbol}",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                        modifier = Modifier.weight(1f).testTag("dismiss_dialog_btn")
                    ) {
                        Text("Dismiss", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
