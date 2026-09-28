package com.example.analysis

import com.example.model.ForexNewsItem
import com.example.model.FundamentalSentiment
import com.example.model.NewsImpact
import java.util.UUID

object ForexNewsRepository {

    fun getInitialHeadlines(): List<ForexNewsItem> {
        val now = System.currentTimeMillis()
        return listOf(
            ForexNewsItem(
                id = "news_us30_gdp",
                headline = "US Q3 GDP Upgraded to 3.0% Annualized: Industrial Dow Components Rally",
                pairSymbol = "US30",
                category = "Economic Growth / GDP",
                impact = NewsImpact.HIGH,
                sentiment = FundamentalSentiment.BULLISH,
                summary = "Consumer spending and business investment beat preliminary estimates, underscoring resilient domestic macro demand and dampening immediate recessionary fears.",
                fundamentalContext = "Strong real growth supports enterprise earnings across blue-chip industrials. Keeps corporate valuations elevated despite higher benchmark borrowing costs.",
                technicalConfluence = "US30 holds firm above the 50-period EMA (43,200). Bullish price rejection at the 43,150 demand order block with RSI-14 rebounding cleanly from 45.",
                source = "Bloomberg Markets",
                timeAgo = "4m ago",
                timestamp = now - 4 * 60 * 1000,
                economicData = mapOf("Actual" to "3.0%", "Forecast" to "2.8%", "Previous" to "2.8%"),
                isBreaking = true
            ),
            ForexNewsItem(
                id = "news_eurusd_ecb",
                headline = "ECB Prepares 25bps Rate Cut as Eurozone Core Inflation Eases to 2.2%",
                pairSymbol = "EUR/USD",
                category = "Central Bank / Policy",
                impact = NewsImpact.HIGH,
                sentiment = FundamentalSentiment.BEARISH,
                summary = "Governing Council remarks reflect growing consensus on disinflation trajectory, widening the policy divergence with the Federal Reserve's neutral holding pattern.",
                fundamentalContext = "Widening real interest rate differential favors USD strength over EUR. Sustained capital outflows into higher-yielding US Dollar short-term paper.",
                technicalConfluence = "EUR/USD retests 1.0845 structural resistance. 50 EMA continues sloping downward below 200 EMA, confirming dominant institutional sell-side pressure.",
                source = "Reuters FX Wire",
                timeAgo = "11m ago",
                timestamp = now - 11 * 60 * 1000,
                economicData = mapOf("Eurozone CPI" to "2.2%", "Forecast" to "2.3%", "Previous" to "2.6%")
            ),
            ForexNewsItem(
                id = "news_gbpusd_boe",
                headline = "Bank of England MPC Votes 5-4 for Rate Pause: Sticky Services Wage Growth Cited",
                pairSymbol = "GBP/USD",
                category = "Employment & Wages",
                impact = NewsImpact.MEDIUM,
                sentiment = FundamentalSentiment.NEUTRAL,
                summary = "A closely divided MPC maintains bank rate at 5.00%. Bailey acknowledges wage moderations but emphasizes underlying persistence in private services inflation.",
                fundamentalContext = "Sterling is caught between hawkish rate support and UK growth deceleration fears. Expect choppy intraday ranges without decisive trend direction.",
                technicalConfluence = "GBP/USD consolidating between 1.2920 demand and 1.3010 resistance. RSI-14 hovering neutrally at 49.3; patience required for breakout confirmation.",
                source = "Financial Times Wire",
                timeAgo = "18m ago",
                timestamp = now - 18 * 60 * 1000,
                economicData = mapOf("UK Wage Growth" to "4.9%", "Forecast" to "5.1%", "Previous" to "5.4%")
            ),
            ForexNewsItem(
                id = "news_usdjpy_boj",
                headline = "BoJ Governor Ueda Hints at Imminent Rate Hike as Tokyo CPI Tops Forecast",
                pairSymbol = "USD/JPY",
                category = "Central Bank / FX Intervention",
                impact = NewsImpact.HIGH,
                sentiment = FundamentalSentiment.BEARISH, // Bearish for USD/JPY = Bullish for Yen
                summary = "Bank of Japan policy normalization signals intensify. Tokyo headline inflation printing at 2.6% sparks speculative unwind of leveraged JPY carry trades.",
                fundamentalContext = "Potential narrowing of the US-Japan 10Y sovereign yield spread. High risk of Japanese MoF currency market intervention above the 154.50 ceiling.",
                technicalConfluence = "USD/JPY forms exhaustive upper wick at 154.20 resistance. RSI-14 overbought at 71.8 with bearish divergence hinting at liquidity flush to 152.00 support.",
                source = "Nikkei Asian Review",
                timeAgo = "27m ago",
                timestamp = now - 27 * 60 * 1000,
                economicData = mapOf("Tokyo CPI" to "2.6%", "Forecast" to "2.4%", "Previous" to "2.2%")
            ),
            ForexNewsItem(
                id = "news_xauusd_gold",
                headline = "Spot Gold Stabilizes Above $2,730 on Sustained Sovereign Reserve Accumulation",
                pairSymbol = "XAU/USD",
                category = "Precious Metals / Reserves",
                impact = NewsImpact.HIGH,
                sentiment = FundamentalSentiment.BULLISH,
                summary = "Global central banks report 4th straight quarter of net bullion additions. Safe-haven bids absorb intraday treasury yield fluctuations.",
                fundamentalContext = "De-dollarization flows and global debt trajectory maintain a structural floor under spot gold prices regardless of short-term Fed hawkishness.",
                technicalConfluence = "Ascending channel intact. Price pullback to $2,725 respects the 50-EMA support dynamic confluence; RSI reset to 52 provides room for bullish extension.",
                source = "World Gold Council / Bloomberg",
                timeAgo = "35m ago",
                timestamp = now - 35 * 60 * 1000,
                economicData = mapOf("CB Net Purchases" to "+48 Tons", "Prior Month" to "+32 Tons")
            ),
            ForexNewsItem(
                id = "news_us30_fed_powell",
                headline = "Fed's Powell Stresses 'No Urgency to Cut': Labor Market Cooling In Orderly Fashion",
                pairSymbol = "US30",
                category = "Federal Reserve / Speeches",
                impact = NewsImpact.HIGH,
                sentiment = FundamentalSentiment.NEUTRAL,
                summary = "Federal Reserve Chair highlights steady real economic conditions and emphasizes that future rate moves will be guided solely by incoming disinflation evidence.",
                fundamentalContext = "Tempered expectations for aggressive 50bps rate cuts. Markets price higher terminal floor, prompting institutional rotations into defensive equity sectors.",
                technicalConfluence = "US30 trading inside 43,500 - 43,900 range. False breakout above previous daily highs was swiftly rejected. Watch for retest of 50 EMA at 43,400.",
                source = "Federal Reserve Wire",
                timeAgo = "44m ago",
                timestamp = now - 44 * 60 * 1000
            ),
            ForexNewsItem(
                id = "news_eurusd_pmi",
                headline = "German Manufacturing PMI Slumps to 42.6: Energy Cost Squeeze Weighs on Production",
                pairSymbol = "EUR/USD",
                category = "Industrial Production / PMI",
                impact = NewsImpact.MEDIUM,
                sentiment = FundamentalSentiment.BEARISH,
                summary = "Flash factory metrics across Germany and France point to prolonged contraction in export orders, dampening euro-area growth projections.",
                fundamentalContext = "Structural economic headwinds reinforce expectations of synchronized ECB interest rate easing through the next calendar quarter.",
                technicalConfluence = "Confirms bearish trend on EUR/USD. Pullback rallies into the 1.0860 supply zone offer favorable 1:2+ R:R short opportunities with SL above 1.0890.",
                source = "S&P Global Market Intelligence",
                timeAgo = "52m ago",
                timestamp = now - 52 * 60 * 1000,
                economicData = mapOf("Germany PMI" to "42.6", "Forecast" to "43.5", "Previous" to "43.0")
            )
        )
    }

    /**
     * Generates a fresh dynamic breaking news alert for market simulation and live analysis.
     */
    fun generateFreshBreakingNews(pairSymbol: String): ForexNewsItem {
        val now = System.currentTimeMillis()
        val id = "news_live_${UUID.randomUUID().toString().take(8)}"
        return when (pairSymbol) {
            "US30" -> ForexNewsItem(
                id = id,
                headline = "Wall Street Bell: US30 Blue-Chips Rally on Resilient ISM Services Print",
                pairSymbol = "US30",
                category = "Services & Industrial Index",
                impact = NewsImpact.HIGH,
                sentiment = FundamentalSentiment.BULLISH,
                summary = "ISM Non-Manufacturing PMI registered 54.9 vs 51.7 consensus, showing robust commercial activity and corporate order pipelines across the US economy.",
                fundamentalContext = "Corporate earnings resilience cushions against restrictive interest rates. Bullish liquidity push favors index long positions.",
                technicalConfluence = "US30 breaks out of the 43,700 consolidation band with strong candle body close. RSI-14 reaches 59.8 with no bearish divergence.",
                source = "Bloomberg Terminal",
                timeAgo = "Just now",
                timestamp = now,
                economicData = mapOf("ISM Services" to "54.9", "Forecast" to "51.7", "Previous" to "51.5"),
                isBreaking = true
            )
            "EUR/USD" -> ForexNewsItem(
                id = id,
                headline = "Eurozone Trade Surplus Narrows to €12.5B Amid Sluggish Global Export Demand",
                pairSymbol = "EUR/USD",
                category = "Trade Balance",
                impact = NewsImpact.MEDIUM,
                sentiment = FundamentalSentiment.BEARISH,
                summary = "Subdued import volumes in key foreign markets reduced European export revenues, signaling muted aggregate momentum for the common currency.",
                fundamentalContext = "Fundamental headwind for EUR; Dollar remains favored on superior growth differentials.",
                technicalConfluence = "EUR/USD rejects 1.0850 intraday pivot. 50 EMA acts as dynamic downward ceiling on 1-hour chart.",
                source = "Eurostat Wire",
                timeAgo = "Just now",
                timestamp = now,
                economicData = mapOf("Trade Surplus" to "€12.5B", "Forecast" to "€14.2B", "Previous" to "€15.1B"),
                isBreaking = true
            )
            "GBP/USD" -> ForexNewsItem(
                id = id,
                headline = "UK Retail Sales Volumes Beat Estimates by +0.8%: Consumer Spending Rebounds",
                pairSymbol = "GBP/USD",
                category = "Consumer Activity",
                impact = NewsImpact.MEDIUM,
                sentiment = FundamentalSentiment.BULLISH,
                summary = "British household retail demand exceeded forecasters' expectations, easing fears of consumer retrenchment ahead of autumn fiscal announcements.",
                fundamentalContext = "Provides modest fundamental relief for Sterling against G10 peers, reducing immediate odds of rapid BoJ/Fed catch-up rate reductions.",
                technicalConfluence = "GBP/USD bounces off 1.2940 key structural support. RSI-14 swings upward to 52.4 confirming bullish momentum shift.",
                source = "Office for National Statistics (UK)",
                timeAgo = "Just now",
                timestamp = now,
                economicData = mapOf("Retail Sales MoM" to "+0.8%", "Forecast" to "-0.1%", "Previous" to "+0.4%"),
                isBreaking = true
            )
            "USD/JPY" -> ForexNewsItem(
                id = id,
                headline = "US 10-Year Treasury Yield Surges to 4.28%: Widening Spread Propels USD/JPY",
                pairSymbol = "USD/JPY",
                category = "Bond Yield Differentials",
                impact = NewsImpact.HIGH,
                sentiment = FundamentalSentiment.BULLISH, // Bullish for USD/JPY
                summary = "Stronger benchmark US yields heighten interest rate disparity with Japanese JGBs, reigniting short-term momentum despite FX intervention warnings.",
                fundamentalContext = "Interest rate parity trade remains heavily tilted toward holding USD assets over JPY funding liabilities.",
                technicalConfluence = "USD/JPY re-tests 153.80 zone. High risk of sudden whipsaw if MoF issues verbal intervention statements; strict 1% risk limit strictly required.",
                source = "CBOE Treasury Index / Reuters",
                timeAgo = "Just now",
                timestamp = now,
                economicData = mapOf("US 10Y Yield" to "4.28%", "Japan 10Y Yield" to "0.96%", "Spread" to "+332 bps"),
                isBreaking = true
            )
            "XAU/USD" -> ForexNewsItem(
                id = id,
                headline = "Middle East Escalation Sparks Safe-Haven Bullion Flight to $2,745",
                pairSymbol = "XAU/USD",
                category = "Geopolitics / Safe Haven",
                impact = NewsImpact.HIGH,
                sentiment = FundamentalSentiment.BULLISH,
                summary = "Heightened diplomatic friction in regional trade corridors triggered immediate institutional flight into physical gold contracts and secure treasury bills.",
                fundamentalContext = "Classic geopolitical safe-haven bid overrides intraday dollar strength and bond yield headwinds.",
                technicalConfluence = "Gold prints strong engulfing bullish candlestick on H1. Key structural breakout confirmed above $2,735 resistance; next objective $2,750.",
                source = "Reuters Geopolitical Alert",
                timeAgo = "Just now",
                timestamp = now,
                isBreaking = true
            )
            else -> ForexNewsItem(
                id = id,
                headline = "Global Central Bank Liquidity Update: Aggregate Balances Expand by $18B",
                pairSymbol = "US30",
                category = "Global Macro / Liquidity",
                impact = NewsImpact.MEDIUM,
                sentiment = FundamentalSentiment.BULLISH,
                summary = "Net liquidity injection across major monetary authorities cushions risk assets and underpins equity index floors into market close.",
                fundamentalContext = "Macro liquidity expansion historically correlates with equity index advances and commodity firming.",
                technicalConfluence = "US30 maintains above 200 EMA baseline. Confluence of rising structural lows on 1-hour timeframe.",
                source = "CrossBorder Capital / Bloomberg",
                timeAgo = "Just now",
                timestamp = now,
                isBreaking = true
            )
        }
    }
}
