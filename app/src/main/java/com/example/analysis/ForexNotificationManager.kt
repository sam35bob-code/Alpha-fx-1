package com.example.analysis

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.*
import java.util.UUID

object ForexNotificationManager {
    private const val TAG = "ForexNotificationManager"
    const val CHANNEL_ID = "alphafx_setup_alerts"
    const val CHANNEL_NAME = "AI Strategy Setup Alerts"
    const val CHANNEL_DESCRIPTION = "Real-time alerts when the AI identifies a setup meeting 50/200 EMA and RSI reversal criteria."

    const val ADR_CHANNEL_ID = "alphafx_adr_volatility_alerts"
    const val ADR_CHANNEL_NAME = "ADR Volatility Alerts"
    const val ADR_CHANNEL_DESCRIPTION = "Automated alerts when major currency pairs show abnormal Average Daily Range (ADR) volatility deviations."

    const val EA_CHANNEL_ID = "alphafx_ea_auto_trade_alerts"
    const val EA_CHANNEL_NAME = "Intelligence EA Auto-Trade Alerts"
    const val EA_CHANNEL_DESCRIPTION = "High-priority notifications dispatched when the Intelligence EA automatically takes or exits trades."

    private var channelCreated = false
    private val recentlyNotifiedKeys = mutableMapOf<String, Long>()
    private val adrNotifiedKeys = mutableMapOf<String, Long>()
    private const val DEBOUNCE_INTERVAL_MS = 60_000L // 1 minute per setup key
    private const val ADR_DEBOUNCE_INTERVAL_MS = 120_000L // 2 minutes per pair ADR alert

    fun createNotificationChannel(context: Context) {
        if (channelCreated) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            val setupChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
            }

            val adrChannel = NotificationChannel(ADR_CHANNEL_ID, ADR_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = ADR_CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setShowBadge(true)
            }

            val eaChannel = NotificationChannel(EA_CHANNEL_ID, EA_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = EA_CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 150, 350)
                setShowBadge(true)
            }

            notificationManager?.createNotificationChannel(setupChannel)
            notificationManager?.createNotificationChannel(adrChannel)
            notificationManager?.createNotificationChannel(eaChannel)
        }
        channelCreated = true
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Checks if a setup qualifies and sends a push notification.
     * Criteria: 50/200 EMA trend + RSI 14 reversal/oversold + Key S/R zone + 1:2 R:R.
     */
    fun triggerSetupAlert(
        context: Context,
        pair: ForexPair,
        indicators: TechnicalIndicators,
        setup: TradeSetup,
        forceTest: Boolean = false
    ): SetupAlertNotification? {
        val rule1Met = setup.checklist.getOrNull(0)?.isMet == true
        val rule3Met = setup.checklist.getOrNull(2)?.isMet == true

        // Must meet 50/200 EMA and RSI reversal criteria (or force test)
        if (!forceTest && (!rule1Met || !rule3Met || setup.action == TradeAction.WAIT)) {
            return null
        }

        val setupKey = "${pair.symbol}_${setup.action}_${(setup.entryPrice * 10).toInt()}"
        val now = System.currentTimeMillis()
        val lastNotified = recentlyNotifiedKeys[setupKey] ?: 0L

        if (!forceTest && (now - lastNotified < DEBOUNCE_INTERVAL_MS)) {
            return null // Debounced
        }
        recentlyNotifiedKeys[setupKey] = now

        createNotificationChannel(context)

        val formattedEntry = TechnicalAnalysisEngine.formatPrice(setup.entryPrice, pair)
        val formattedSl = TechnicalAnalysisEngine.formatPrice(setup.stopLoss, pair)
        val formattedTp1 = TechnicalAnalysisEngine.formatPrice(setup.takeProfit1, pair)
        val rsiFormatted = String.format("%.1f", indicators.rsi14)
        val rrFormatted = String.format("%.1f", setup.riskRewardRatio)

        val title = "🚨 AI SETUP DETECTED: ${setup.action} ${pair.symbol}"
        val shortBody = "50/200 EMA & RSI Reversal ($rsiFormatted) met @ $formattedEntry | 1:$rrFormatted R:R"

        val bigText = buildString {
            appendLine("⚡ AI Confirmed 50/200 EMA & RSI(14) Reversal Criteria:")
            appendLine("• Action: ${setup.action} ${pair.symbol} @ $formattedEntry")
            appendLine("• Stop Loss: $formattedSl | Take Profit: $formattedTp1 (1:$rrFormatted R:R)")
            appendLine("• 50/200 EMA Trend: ${if (indicators.ema50 > indicators.ema200) "Bullish (50 > 200)" else "Bearish (50 < 200)"}")
            appendLine("• RSI(14): $rsiFormatted (${indicators.rsiStatus.label})")
            appendLine("• Zone: ${setup.keyZoneLabel}")
            appendLine("• Risk Directive: Max 1.0% equity risk per trade.")
        }

        val alertModel = SetupAlertNotification(
            id = UUID.randomUUID().toString(),
            pairSymbol = pair.symbol,
            action = setup.action,
            entryPrice = setup.entryPrice,
            stopLoss = setup.stopLoss,
            takeProfit1 = setup.takeProfit1,
            riskRewardRatio = setup.riskRewardRatio,
            ema50 = indicators.ema50,
            ema200 = indicators.ema200,
            rsi14 = indicators.rsi14,
            rsiStatus = indicators.rsiStatus.label,
            keyZoneLabel = setup.keyZoneLabel,
            timestamp = now,
            message = shortBody
        )

        // Post system notification if permission is granted
        if (hasNotificationPermission(context)) {
            try {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("EXTRA_SELECTED_PAIR", pair.symbol)
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    pair.symbol.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_forex_logo)
                    .setContentTitle(title)
                    .setContentText(shortBody)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVibrate(longArrayOf(0, 250, 150, 250))

                val notificationId = 1000 + pair.symbol.hashCode() % 1000
                NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                Log.w(TAG, "Notification permission not granted: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to post notification: ${e.message}", e)
            }
        }

        return alertModel
    }

    /**
     * Dispatches a push notification when an abnormal ADR volatility deviation is detected on a currency pair.
     */
    fun triggerAdrVolatilityNotification(
        context: Context,
        alert: AdrVolatilityAlert,
        pair: ForexPair,
        forceTest: Boolean = false
    ): Boolean {
        val now = System.currentTimeMillis()
        val debounceKey = "${alert.pairSymbol}_ADR"
        val lastNotified = adrNotifiedKeys[debounceKey] ?: 0L

        if (!forceTest && (now - lastNotified < ADR_DEBOUNCE_INTERVAL_MS)) {
            return false // Debounced
        }
        adrNotifiedKeys[debounceKey] = now

        createNotificationChannel(context)

        val formattedPrice = TechnicalAnalysisEngine.formatPrice(alert.currentPrice, pair)
        val formattedHigh = TechnicalAnalysisEngine.formatPrice(alert.dayHigh, pair)
        val formattedLow = TechnicalAnalysisEngine.formatPrice(alert.dayLow, pair)

        val title = "⚠️ ADR VOLATILITY ALERT: ${alert.pairSymbol} at ${String.format("%.1f", alert.adrPercentage)}%"
        val shortBody = "${alert.pairSymbol} daily range (${alert.todayRangePips} pips) exceeded 14-day ADR (${alert.adr14Pips} pips) by +${String.format("%.1f", alert.deviationPercentage)}%!"

        val bigText = buildString {
            appendLine("⚡ Automated ADR Volatility Deviation Triggered:")
            appendLine("• Pair: ${alert.pairSymbol} @ $formattedPrice")
            appendLine("• 14-Day Baseline ADR: ${alert.adr14Pips} pips")
            appendLine("• Today's Actual Range: ${alert.todayRangePips} pips (${String.format("%.1f", alert.adrPercentage)}% of ADR)")
            appendLine("• Volatility Deviation: +${String.format("%.1f", alert.deviationPercentage)}% (${alert.volatilityLevel.label})")
            appendLine("• Day Extremes: Low $formattedLow | High $formattedHigh")
            appendLine("• Risk Directive: Extreme market expansion detected. Exercise caution on breakout entries.")
        }

        if (hasNotificationPermission(context)) {
            try {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("EXTRA_SELECTED_PAIR", alert.pairSymbol)
                    putExtra("EXTRA_TAB", "TERMINAL")
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    ("ADR_" + alert.pairSymbol).hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, ADR_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_forex_logo)
                    .setContentTitle(title)
                    .setContentText(shortBody)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVibrate(longArrayOf(0, 300, 200, 300))

                val notificationId = 2000 + alert.pairSymbol.hashCode() % 1000
                NotificationManagerCompat.from(context).notify(notificationId, builder.build())
                return true
            } catch (e: SecurityException) {
                Log.w(TAG, "Notification permission not granted for ADR alert: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to post ADR notification: ${e.message}", e)
            }
        }
        return false
    }

    fun triggerEaAutoTradeNotification(
        context: Context,
        pair: ForexPair,
        action: String,
        entryPrice: Double,
        stopLoss: Double,
        takeProfit: Double,
        lots: Double,
        strategyName: String
    ): Boolean {
        createNotificationChannel(context)
        val formattedEntry = TechnicalAnalysisEngine.formatPrice(entryPrice, pair)
        val formattedSL = TechnicalAnalysisEngine.formatPrice(stopLoss, pair)
        val formattedTP = TechnicalAnalysisEngine.formatPrice(takeProfit, pair)

        val title = "⚡ EA AUTO-TRADE EXECUTED: $action ${pair.symbol}"
        val shortBody = "Intelligence EA entered $action ${pair.symbol} ($lots Lots) @ $formattedEntry | SL: $formattedSL | TP: $formattedTP"

        val bigText = buildString {
            appendLine("🤖 AlphaFX Intelligence EA Auto-Execution:")
            appendLine("• Market: ${pair.symbol} (${pair.name})")
            appendLine("• Strategy: $strategyName")
            appendLine("• Action: $action ($lots Standard Lots)")
            appendLine("• Entry Price: $formattedEntry")
            appendLine("• Stop Loss: $formattedSL")
            appendLine("• Take Profit: $formattedTP")
            appendLine("• Risk Policy: Strict Stop Loss Protection Active")
        }

        if (hasNotificationPermission(context)) {
            try {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("EXTRA_SELECTED_PAIR", pair.symbol)
                    putExtra("EXTRA_TAB", "TERMINAL")
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    ("EA_" + pair.symbol + System.currentTimeMillis()).hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, EA_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_forex_logo)
                    .setContentTitle(title)
                    .setContentText(shortBody)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVibrate(longArrayOf(0, 350, 150, 350))

                val notificationId = 3000 + (System.currentTimeMillis() % 1000).toInt()
                NotificationManagerCompat.from(context).notify(notificationId, builder.build())
                return true
            } catch (e: SecurityException) {
                Log.w(TAG, "Notification permission not granted for EA alert: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to post EA notification: ${e.message}", e)
            }
        }
        return false
    }
}
