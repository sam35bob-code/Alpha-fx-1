package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.model.Candle
import com.example.model.Timeframe
import com.example.model.TradeAction
import com.example.model.TradeSetup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomDatabaseOfflineTest {

    private lateinit var database: AlphaFxDatabase
    private lateinit var historicalPriceDao: HistoricalPriceDao
    private lateinit var tradeSignalDao: TradeSignalDao
    private lateinit var offlineRepository: OfflineMarketRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AlphaFxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        historicalPriceDao = database.historicalPriceDao()
        tradeSignalDao = database.tradeSignalDao()
        offlineRepository = OfflineMarketRepository(historicalPriceDao, tradeSignalDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `test historical price dao insert and query operations`() = runBlocking {
        val baseTime = 1700000000000L
        val prices = listOf(
            HistoricalPriceEntity(pairSymbol = "EUR/USD", timeframe = "H1", timestamp = baseTime, open = 1.0800, high = 1.0850, low = 1.0790, close = 1.0840, volume = 5000L),
            HistoricalPriceEntity(pairSymbol = "EUR/USD", timeframe = "H1", timestamp = baseTime + 3600000L, open = 1.0840, high = 1.0880, low = 1.0830, close = 1.0870, volume = 6200L),
            HistoricalPriceEntity(pairSymbol = "EUR/USD", timeframe = "H1", timestamp = baseTime + 7200000L, open = 1.0870, high = 1.0895, low = 1.0860, close = 1.0890, volume = 7100L),
            HistoricalPriceEntity(pairSymbol = "US30", timeframe = "H1", timestamp = baseTime, open = 40000.0, high = 40200.0, low = 39950.0, close = 40150.0, volume = 15000L)
        )

        historicalPriceDao.insertPrices(prices)

        // Check count
        val eurCount = historicalPriceDao.getPriceCount("EUR/USD", "H1")
        assertEquals(3, eurCount)

        val us30Count = historicalPriceDao.getPriceCount("US30", "H1")
        assertEquals(1, us30Count)

        // Check chronological retrieval
        val eurusdPrices = historicalPriceDao.getHistoricalPricesList("EUR/USD", "H1")
        assertEquals(3, eurusdPrices.size)
        assertEquals(1.0800, eurusdPrices.first().open, 0.0001)
        assertEquals(1.0890, eurusdPrices.last().close, 0.0001)

        // Check latest price
        val latestEur = historicalPriceDao.getLatestPrice("EUR/USD", "H1")
        assertNotNull(latestEur)
        assertEquals(baseTime + 7200000L, latestEur!!.timestamp)
        assertEquals(1.0890, latestEur.close, 0.0001)

        // Check recent prices with limit
        val recent2 = historicalPriceDao.getRecentPricesList("EUR/USD", "H1", limit = 2)
        assertEquals(2, recent2.size)
        assertEquals(baseTime + 7200000L, recent2.first().timestamp)

        // Check prices between range
        val range = historicalPriceDao.getPricesBetween("EUR/USD", "H1", baseTime, baseTime + 3600000L)
        assertEquals(2, range.size)

        // Test delete for pair
        historicalPriceDao.deletePricesForPair("US30", "H1")
        assertEquals(0, historicalPriceDao.getPriceCount("US30", "H1"))
        assertEquals(3, historicalPriceDao.getPriceCount("EUR/USD", "H1"))
    }

    @Test
    fun `test trade signal dao insert update and filtering`() = runBlocking {
        val now = System.currentTimeMillis()
        val signal1 = TradeSignalEntity(
            signalId = "sig-101",
            pairSymbol = "EUR/USD",
            timeframe = "H1",
            action = "BUY",
            isValid = true,
            entryPrice = 1.0850,
            stopLoss = 1.0820,
            takeProfit1 = 1.0910,
            takeProfit2 = 1.0950,
            riskRewardRatio = 2.0,
            confidenceScore = 0.92,
            signalType = "AI_CONFLUENCE",
            status = "ACTIVE",
            statusSummary = "50 > 200 EMA uptrend with oversold RSI-14",
            detailedReasoning = "Clear bullish pullback into key demand zone.",
            ema50 = 1.0845,
            ema200 = 1.0810,
            rsi14 = 28.5,
            keyZoneLabel = "Key H1 Demand",
            timestamp = now
        )

        val signal2 = TradeSignalEntity(
            signalId = "sig-102",
            pairSymbol = "US30",
            timeframe = "H4",
            action = "SELL",
            isValid = true,
            entryPrice = 40100.0,
            stopLoss = 40250.0,
            takeProfit1 = 39800.0,
            takeProfit2 = 39500.0,
            riskRewardRatio = 2.0,
            confidenceScore = 0.88,
            signalType = "AI_CONFLUENCE",
            status = "ACTIVE",
            statusSummary = "50 < 200 EMA rejection at resistance",
            detailedReasoning = "Overbought RSI-14 reversal.",
            ema50 = 40050.0,
            ema200 = 40300.0,
            rsi14 = 74.2,
            keyZoneLabel = "Institutional Resistance",
            timestamp = now - 10000L
        )

        val id1 = tradeSignalDao.insertSignal(signal1)
        val id2 = tradeSignalDao.insertSignal(signal2)
        assertTrue(id1 > 0)
        assertTrue(id2 > 0)

        // Query all signals
        val allSignals = tradeSignalDao.getAllSignals().first()
        assertEquals(2, allSignals.size)

        // Query by pair
        val eurSignals = tradeSignalDao.getSignalsForPair("EUR/USD").first()
        assertEquals(1, eurSignals.size)
        assertEquals("BUY", eurSignals.first().action)

        // Query by action
        val sellSignals = tradeSignalDao.getSignalsByAction("SELL").first()
        assertEquals(1, sellSignals.size)
        assertEquals("US30", sellSignals.first().pairSymbol)

        // Update status to TP1_HIT
        tradeSignalDao.updateSignalStatus(id1, "TP1_HIT")
        val updatedSig1 = tradeSignalDao.getSignalById(id1)
        assertNotNull(updatedSig1)
        assertEquals("TP1_HIT", updatedSig1!!.status)

        // Check active signals (should now only be signal2)
        val activeSignals = tradeSignalDao.getActiveSignals().first()
        assertEquals(1, activeSignals.size)
        assertEquals("US30", activeSignals.first().pairSymbol)
    }

    @Test
    fun `test offline market repository candle and signal caching`() = runBlocking {
        val candles = listOf(
            Candle(timestamp = 1000L, open = 1.0800, high = 1.0850, low = 1.0790, close = 1.0840),
            Candle(timestamp = 2000L, open = 1.0840, high = 1.0890, low = 1.0830, close = 1.0880)
        )

        offlineRepository.saveHistoricalPrices("GBP/USD", "H1", candles)
        assertTrue(offlineRepository.hasOfflinePrices("GBP/USD", "H1"))

        val cachedCandles = offlineRepository.getHistoricalPrices("GBP/USD", "H1")
        assertEquals(2, cachedCandles.size)
        assertEquals(1.0800, cachedCandles.first().open, 0.0001)
        assertEquals(1.0880, cachedCandles.last().close, 0.0001)

        // Test saving TradeSetup
        val setup = TradeSetup(
            pairSymbol = "GBP/USD",
            timeframe = Timeframe.H1,
            action = TradeAction.BUY,
            isValid = true,
            statusSummary = "Valid Setup",
            detailedReasoning = "All 4 criteria met",
            entryPrice = 1.2750,
            stopLoss = 1.2720,
            takeProfit1 = 1.2810,
            takeProfit2 = 1.2870,
            riskPipsOrPoints = 30.0,
            rewardPipsOrPoints = 60.0,
            riskRewardRatio = 2.0,
            ema50 = 1.2740,
            ema200 = 1.2700,
            rsi14 = 32.0,
            keyZoneLabel = "Support",
            checklist = emptyList()
        )

        val signalId = offlineRepository.saveTradeSignal(setup)
        assertTrue(signalId > 0)

        val active = offlineRepository.observeActiveSignals().first()
        assertEquals(1, active.size)
        assertEquals("GBP/USD", active.first().pairSymbol)
        assertEquals("BUY", active.first().action)
        assertEquals(2.0, active.first().riskRewardRatio, 0.01)
    }
}
