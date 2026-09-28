package com.example.ui.components

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.analysis.ForexNotificationManager
import com.example.model.*
import com.example.ui.theme.*

/**
 * Animated In-App Heads-Up Alert Banner displayed at top of screen when an AI setup triggers.
 */
@Composable
fun HeadsUpSetupAlertBanner(
    alert: SetupAlertNotification?,
    onDismiss: () -> Unit,
    onNavigateToPair: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = alert != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("heads_up_alert_banner")
    ) {
        if (alert != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardBackground,
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (alert.action == TradeAction.BUY) BullishGreen else BearishRed
                ),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (alert.action == TradeAction.BUY) BullishGreen.copy(alpha = 0.2f)
                                    else BearishRed.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Setup Alert",
                                tint = if (alert.action == TradeAction.BUY) BullishGreen else BearishRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "AI SETUP ALERT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (alert.action == TradeAction.BUY) BullishGreen else BearishRed,
                                    letterSpacing = 0.6.sp
                                )
                                Text(
                                    text = "${alert.action} ${alert.pairSymbol}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "50/200 EMA & RSI-14 Reversal @ ${alert.entryPrice} | 1:${String.format("%.1f", alert.riskRewardRatio)} R:R",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = {
                                onNavigateToPair(alert.pairSymbol)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanAccent,
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp).testTag("alert_view_btn")
                        ) {
                            Text("View", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp).testTag("alert_dismiss_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss Alert",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Settings and History Dialog for AI Strategy Push Notifications.
 */
@Composable
fun SetupNotificationDialog(
    notificationsEnabled: Boolean,
    alertHistory: List<SetupAlertNotification>,
    onToggleNotifications: () -> Unit,
    onSendTestAlert: () -> Unit,
    onDismiss: () -> Unit,
    onSelectAlertPair: (String) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(ForexNotificationManager.hasNotificationPermission(context)) }

    // Android 13+ Runtime Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = TerminalSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .testTag("setup_notification_dialog")
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
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Notifications",
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "AI SETUP PUSH ALERTS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = "50/200 EMA & RSI-14 Reversal Criteria",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp).testTag("close_notification_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = TerminalBorder, modifier = Modifier.padding(vertical = 12.dp))

                // Master Toggle & Permission Status Card
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Real-Time Push Alerts",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Send push notification when technical criteria are satisfied",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { onToggleNotifications() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = CyanAccent
                                ),
                                modifier = Modifier.testTag("push_notifications_toggle")
                            )
                        }

                        // Permission Check on Android 13+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BearishRed.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BearishRed.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Permission Warning",
                                            tint = BearishRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "OS Notification permission needed",
                                            fontSize = 10.sp,
                                            color = BearishRed
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BearishRed),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(24.dp).testTag("grant_notification_permission_btn")
                                    ) {
                                        Text("Grant", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Alert Trigger Criteria Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TerminalSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🎯 CRITERIA CHECKLIST REQUIRED FOR ALERT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        CriteriaRow(label = "1. 50 & 200 EMA Alignment", desc = "Bullish (50 > 200) or Bearish (50 < 200)")
                        CriteriaRow(label = "2. S/R Zone Pullback", desc = "Price tested 4H/Daily value area")
                        CriteriaRow(label = "3. RSI(14) Extreme Reversal", desc = "Oversold (<32) or Overbought (>68)")
                        CriteriaRow(label = "4. Strict Risk Management", desc = "Minimum 1:2.0 Risk-to-Reward ratio")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Test Trigger Button
                Button(
                    onClick = onSendTestAlert,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().testTag("send_test_notification_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Test Alert",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trigger Instant Test Push Alert", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "RECENT SETUP NOTIFICATIONS (${alertHistory.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Alert History List
                if (alertHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No alerts fired yet. When the 4 criteria converge on any pair, push notifications will appear here and in your system drawer.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(alertHistory) { alertItem ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = TerminalSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectAlertPair(alertItem.pairSymbol)
                                        onDismiss()
                                    }
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
                                                color = if (alertItem.action == TradeAction.BUY) BullishGreen.copy(alpha = 0.2f) else BearishRed.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "${alertItem.action} ${alertItem.pairSymbol}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (alertItem.action == TradeAction.BUY) BullishGreen else BearishRed,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = "RSI: ${String.format("%.1f", alertItem.rsi14)}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CyanAccent
                                            )
                                            Text(
                                                text = "1:${String.format("%.1f", alertItem.riskRewardRatio)} R:R",
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Entry: ${alertItem.entryPrice} | SL: ${alertItem.stopLoss} | TP: ${alertItem.takeProfit1}",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextPrimary
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "View",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CriteriaRow(label: String, desc: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Passed",
            tint = BullishGreen,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "$label: ",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = desc,
            fontSize = 10.sp,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
