package com.example.analysis

import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Random

class MarketDataRepository {
    private val random = Random(42)

    enum class MarketScenario(val title: String, val description: String) {
        PRIME_EURUSD_BUY(
            "EUR/USD Bullish Pullback (Prime Buy)",
            "50 EMA > 200 EMA, clean pullback into key demand zone, RSI-14 oversold at 28.5. 1:2.4 R:R."
        ),
        PRIME_US30_SELL(
            "US30 Bearish Reversal (Prime Sell)",
            "50 EMA < 200 EMA, price testing key institutional resistance, RSI-14 overbought at 72.8 reversing down. 1:2.8 R:R."
        ),
        GBPUSD_NO_TRADE(
            "GBP/USD Mid-Range (Strict Discipline - No Trade)",
            "Price hovering in middle of range between EMAs, RSI at 52 neutral. Zero setups permitted."
        ),
        LIVE_MARKET(
            "Live Dynamic Volatility",
            "Simulated real-time institutional tick stream with structural swings."
        )
    }

    private val _currentScenario = MutableStateFlow(MarketScenario.PRIME_EURUSD_BUY)
    val currentScenario: StateFlow<MarketScenario> = _currentScenario.asStateFlow()

    fun setScenario(scenario: MarketScenario) {
        _currentScenario.value = scenario
    }

    /**
     * Generates a realistic 60-candle sequence matching the pair, timeframe, and active scenario.
     */
    fun generateCandles(pair: ForexPair, timeframe: Timeframe, scenario: MarketScenario): List<Candle> {
        val count = 65
        val list = ArrayList<Candle>(count)
        val stepMillis = timeframe.minutes * 60 * 1000L
        var currentTs = System.currentTimeMillis() - (count * stepMillis)

        val base = pair.basePrice
        val pip = pair.pipSize

        when (scenario) {
            MarketScenario.PRIME_EURUSD_BUY -> {
                // Generate a strong upward trend where 50 EMA > 200 EMA,
                // then recent candles retrace sharply to key support and RSI drops to oversold (~28)
                var p = base * 0.985
                for (i in 0 until count) {
                    val progress = i.toDouble() / count.toDouble()
                    val candleVolatility = pip * (if (pair.symbol == "US30") 30.0 else 8.0)

                    val target = when {
                        progress < 0.65 -> base * (0.985 + (progress * 0.035)) // Strong uptrend
                        progress < 0.92 -> base * (1.008 - ((progress - 0.65) * 0.05)) // Deep pullback to support
                        else -> base * 0.995 + ((progress - 0.92) * 0.005) // Stalling at support / oversold
                    }

                    val delta = (target - p) * 0.2 + ((random.nextDouble() - 0.48) * candleVolatility)
                    val open = p
                    val close = open + delta
                    val high = maxOf(open, close) + (random.nextDouble() * candleVolatility * 0.5)
                    val low = minOf(open, close) - (random.nextDouble() * candleVolatility * 0.5)

                    list.add(Candle(currentTs, open, high, low, close))
                    p = close
                    currentTs += stepMillis
                }
            }
            MarketScenario.PRIME_US30_SELL -> {
                // Generate a structural downtrend (50 EMA < 200 EMA),
                // then recent candles rally up to retest key resistance and RSI reaches overbought (~72)
                var p = base * 1.015
                for (i in 0 until count) {
                    val progress = i.toDouble() / count.toDouble()
                    val candleVolatility = pip * (if (pair.symbol == "US30") 40.0 else 10.0)

                    val target = when {
                        progress < 0.60 -> base * (1.015 - (progress * 0.035)) // Downtrend
                        progress < 0.90 -> base * (0.994 + ((progress - 0.60) * 0.045)) // Retrace to resistance
                        else -> base * 1.0075 - ((progress - 0.90) * 0.006) // Rejection wick forming
                    }

                    val delta = (target - p) * 0.2 + ((random.nextDouble() - 0.52) * candleVolatility)
                    val open = p
                    val close = open + delta
                    val high = maxOf(open, close) + (random.nextDouble() * candleVolatility * 0.6)
                    val low = minOf(open, close) - (random.nextDouble() * candleVolatility * 0.4)

                    list.add(Candle(currentTs, open, high, low, close))
                    p = close
                    currentTs += stepMillis
                }
            }
            MarketScenario.GBPUSD_NO_TRADE -> {
                // Choppy horizontal oscillation around EMAs, RSI strictly neutral between 48 and 54
                var p = base
                for (i in 0 until count) {
                    val candleVolatility = pip * (if (pair.symbol == "US30") 25.0 else 6.0)
                    val wave = Math.sin(i * 0.4) * (candleVolatility * 2)
                    val open = p
                    val close = base + wave + ((random.nextDouble() - 0.5) * candleVolatility)
                    val high = maxOf(open, close) + (random.nextDouble() * candleVolatility * 0.4)
                    val low = minOf(open, close) - (random.nextDouble() * candleVolatility * 0.4)

                    list.add(Candle(currentTs, open, high, low, close))
                    p = close
                    currentTs += stepMillis
                }
            }
            MarketScenario.LIVE_MARKET -> {
                // Natural realistic drift
                var p = base
                for (i in 0 until count) {
                    val candleVolatility = pip * (if (pair.symbol == "US30") 30.0 else 7.0)
                    val delta = (random.nextDouble() - 0.495) * candleVolatility
                    val open = p
                    val close = open + delta
                    val high = maxOf(open, close) + (random.nextDouble() * candleVolatility * 0.5)
                    val low = minOf(open, close) - (random.nextDouble() * candleVolatility * 0.5)

                    list.add(Candle(currentTs, open, high, low, close))
                    p = close
                    currentTs += stepMillis
                }
            }
        }

        return list
    }

    /**
     * Simulates a new real-time tick on the latest candle.
     */
    fun tickCandle(latestCandle: Candle, pair: ForexPair): Candle {
        val candleVolatility = pair.pipSize * (if (pair.symbol == "US30") 5.0 else 0.8)
        val tickDelta = (random.nextDouble() - 0.498) * candleVolatility
        val newClose = latestCandle.close + tickDelta
        val newHigh = maxOf(latestCandle.high, newClose)
        val newLow = minOf(latestCandle.low, newClose)
        return latestCandle.copy(
            high = newHigh,
            low = newLow,
            close = newClose
        )
    }
}
