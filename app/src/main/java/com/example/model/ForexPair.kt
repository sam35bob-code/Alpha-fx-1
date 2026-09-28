package com.example.model

enum class PairCategory {
    FOREX_MAJOR, FOREX_CROSS, FOREX_EXOTIC, INDICES, METALS, CRYPTO
}

data class ForexPair(
    val symbol: String,
    val name: String,
    val category: PairCategory,
    val basePrice: Double,
    val pipSize: Double, // 1 pip (e.g. 0.0001 for EUR/USD, 0.01 for USD/JPY, 1.0 for US30)
    val pipValuePerStandardLot: Double, // $10 for EUR/USD, $1 for US30, etc.
    val decimalDigits: Int,
    val description: String
) {
    companion object {
        val ALL_PAIRS = listOf(
            // FOREX MAJORS
            ForexPair(
                symbol = "EUR/USD",
                name = "Euro / US Dollar",
                category = PairCategory.FOREX_MAJOR,
                basePrice = 1.08450,
                pipSize = 0.0001,
                pipValuePerStandardLot = 10.0,
                decimalDigits = 5,
                description = "Primary world currency pair with clean technical support & resistance response."
            ),
            ForexPair(
                symbol = "GBP/USD",
                name = "British Pound / US Dollar",
                category = PairCategory.FOREX_MAJOR,
                basePrice = 1.29800,
                pipSize = 0.0001,
                pipValuePerStandardLot = 10.0,
                decimalDigits = 5,
                description = "The 'Cable'. Clean structural swings during London and New York overlaps."
            ),
            ForexPair(
                symbol = "USD/JPY",
                name = "US Dollar / Japanese Yen",
                category = PairCategory.FOREX_MAJOR,
                basePrice = 153.250,
                pipSize = 0.01,
                pipValuePerStandardLot = 6.5,
                decimalDigits = 3,
                description = "Momentum mover driven by yield differentials and clean EMA trends."
            ),
            ForexPair(
                symbol = "AUD/USD",
                name = "Australian Dollar / US Dollar",
                category = PairCategory.FOREX_MAJOR,
                basePrice = 0.65820,
                pipSize = 0.0001,
                pipValuePerStandardLot = 10.0,
                decimalDigits = 5,
                description = "Aussie Dollar commodity currency sensitive to Asian session volume & risk appetite."
            ),
            ForexPair(
                symbol = "USD/CAD",
                name = "US Dollar / Canadian Dollar",
                category = PairCategory.FOREX_MAJOR,
                basePrice = 1.38950,
                pipSize = 0.0001,
                pipValuePerStandardLot = 7.2,
                decimalDigits = 5,
                description = "The 'Loonie'. Highly correlated with crude oil and North American macro trends."
            ),
            ForexPair(
                symbol = "USD/CHF",
                name = "US Dollar / Swiss Franc",
                category = PairCategory.FOREX_MAJOR,
                basePrice = 0.86740,
                pipSize = 0.0001,
                pipValuePerStandardLot = 11.5,
                decimalDigits = 5,
                description = "The 'Swissie'. Classic European safe-haven market with sharp institutional retests."
            ),
            ForexPair(
                symbol = "NZD/USD",
                name = "New Zealand Dollar / US Dollar",
                category = PairCategory.FOREX_MAJOR,
                basePrice = 0.59760,
                pipSize = 0.0001,
                pipValuePerStandardLot = 10.0,
                decimalDigits = 5,
                description = "The 'Kiwi'. Fast-moving Pacific currency with high volatility during Tokyo open."
            ),

            // FOREX MINORS & CROSSES
            ForexPair(
                symbol = "EUR/GBP",
                name = "Euro / British Pound",
                category = PairCategory.FOREX_CROSS,
                basePrice = 0.83550,
                pipSize = 0.0001,
                pipValuePerStandardLot = 13.0,
                decimalDigits = 5,
                description = "European cross pair with high mean-reversion characteristics and tight spreads."
            ),
            ForexPair(
                symbol = "EUR/JPY",
                name = "Euro / Japanese Yen",
                category = PairCategory.FOREX_CROSS,
                basePrice = 166.200,
                pipSize = 0.01,
                pipValuePerStandardLot = 6.5,
                decimalDigits = 3,
                description = "High beta cross pair. Exceptional trending moves aligned with 50/200 EMAs."
            ),
            ForexPair(
                symbol = "GBP/JPY",
                name = "British Pound / Japanese Yen",
                category = PairCategory.FOREX_CROSS,
                basePrice = 198.920,
                pipSize = 0.01,
                pipValuePerStandardLot = 6.5,
                decimalDigits = 3,
                description = "The 'Dragon' / 'Geppy'. Legendary for explosive multi-hundred pip breakout runs."
            ),
            ForexPair(
                symbol = "AUD/JPY",
                name = "Australian Dollar / Japanese Yen",
                category = PairCategory.FOREX_CROSS,
                basePrice = 100.850,
                pipSize = 0.01,
                pipValuePerStandardLot = 6.5,
                decimalDigits = 3,
                description = "Classic global risk-on/risk-off sentiment barometer."
            ),
            ForexPair(
                symbol = "CAD/JPY",
                name = "Canadian Dollar / Japanese Yen",
                category = PairCategory.FOREX_CROSS,
                basePrice = 110.300,
                pipSize = 0.01,
                pipValuePerStandardLot = 6.5,
                decimalDigits = 3,
                description = "Commodity-energy driven yen cross with sharp institutional trend legs."
            ),
            ForexPair(
                symbol = "CHF/JPY",
                name = "Swiss Franc / Japanese Yen",
                category = PairCategory.FOREX_CROSS,
                basePrice = 176.650,
                pipSize = 0.01,
                pipValuePerStandardLot = 6.5,
                decimalDigits = 3,
                description = "Dual safe-haven cross exhibiting clean structural Fibonacci pullbacks."
            ),
            ForexPair(
                symbol = "NZD/JPY",
                name = "New Zealand Dollar / Japanese Yen",
                category = PairCategory.FOREX_CROSS,
                basePrice = 91.550,
                pipSize = 0.01,
                pipValuePerStandardLot = 6.5,
                decimalDigits = 3,
                description = "High-carry Pacific yen cross active during Asian trading sessions."
            ),
            ForexPair(
                symbol = "EUR/AUD",
                name = "Euro / Australian Dollar",
                category = PairCategory.FOREX_CROSS,
                basePrice = 1.64750,
                pipSize = 0.0001,
                pipValuePerStandardLot = 6.6,
                decimalDigits = 5,
                description = "Volatile European-Pacific cross offering wide daily trading ranges."
            ),
            ForexPair(
                symbol = "EUR/CAD",
                name = "Euro / Canadian Dollar",
                category = PairCategory.FOREX_CROSS,
                basePrice = 1.50650,
                pipSize = 0.0001,
                pipValuePerStandardLot = 7.3,
                decimalDigits = 5,
                description = "Cross sensitive to ECB policy shifts and global energy demand."
            ),
            ForexPair(
                symbol = "GBP/AUD",
                name = "British Pound / Australian Dollar",
                category = PairCategory.FOREX_CROSS,
                basePrice = 1.97200,
                pipSize = 0.0001,
                pipValuePerStandardLot = 6.6,
                decimalDigits = 5,
                description = "Fast-moving cross renowned for strong London session trends."
            ),
            ForexPair(
                symbol = "AUD/CAD",
                name = "Australian Dollar / Canadian Dollar",
                category = PairCategory.FOREX_CROSS,
                basePrice = 0.91500,
                pipSize = 0.0001,
                pipValuePerStandardLot = 7.3,
                decimalDigits = 5,
                description = "Dual commodity cross with high mean-reversion and key support/resistance levels."
            ),

            // FOREX EXOTICS
            ForexPair(
                symbol = "USD/ZAR",
                name = "US Dollar / South African Rand",
                category = PairCategory.FOREX_EXOTIC,
                basePrice = 17.6500,
                pipSize = 0.001,
                pipValuePerStandardLot = 5.6,
                decimalDigits = 4,
                description = "Emerging market high-beta forex pair with powerful trend momentum."
            ),
            ForexPair(
                symbol = "USD/MXN",
                name = "US Dollar / Mexican Peso",
                category = PairCategory.FOREX_EXOTIC,
                basePrice = 19.8500,
                pipSize = 0.001,
                pipValuePerStandardLot = 5.0,
                decimalDigits = 4,
                description = "Liquid Latin American exotic pair responsive to US economic data."
            ),
            ForexPair(
                symbol = "USD/SGD",
                name = "US Dollar / Singapore Dollar",
                category = PairCategory.FOREX_EXOTIC,
                basePrice = 1.3210,
                pipSize = 0.0001,
                pipValuePerStandardLot = 7.5,
                decimalDigits = 4,
                description = "Asian financial hub currency with low volatility and disciplined trends."
            ),

            // INDICES, METALS & CRYPTO
            ForexPair(
                symbol = "US30",
                name = "Dow Jones 30 Index",
                category = PairCategory.INDICES,
                basePrice = 43850.0,
                pipSize = 1.0,
                pipValuePerStandardLot = 1.0,
                decimalDigits = 1,
                description = "US Wall Street 30 Cash Index. High structural volatility & institutional flow."
            ),
            ForexPair(
                symbol = "NAS100",
                name = "Nasdaq 100 Index",
                category = PairCategory.INDICES,
                basePrice = 20380.0,
                pipSize = 1.0,
                pipValuePerStandardLot = 1.0,
                decimalDigits = 1,
                description = "US Tech 100 Index. Hyper-liquid index with precise Fair Value Gap (FVG) respect."
            ),
            ForexPair(
                symbol = "SPX500",
                name = "S&P 500 Index",
                category = PairCategory.INDICES,
                basePrice = 5865.0,
                pipSize = 0.1,
                pipValuePerStandardLot = 10.0,
                decimalDigits = 2,
                description = "Benchmark US Equity Index representing institutional global capital flow."
            ),
            ForexPair(
                symbol = "XAU/USD",
                name = "Gold / US Dollar",
                category = PairCategory.METALS,
                basePrice = 2735.50,
                pipSize = 0.10,
                pipValuePerStandardLot = 10.0,
                decimalDigits = 2,
                description = "Precious safe-haven metal respecting macro S/R zones and RSI divergence."
            ),
            ForexPair(
                symbol = "BTC/USD",
                name = "Bitcoin / US Dollar",
                category = PairCategory.CRYPTO,
                basePrice = 67800.0,
                pipSize = 1.0,
                pipValuePerStandardLot = 1.0,
                decimalDigits = 1,
                description = "24/7 Digital Asset benchmark with high liquidity and order block breakouts."
            )
        )

        fun defaultPair() = ALL_PAIRS.first { it.symbol == "EUR/USD" }
    }
}
