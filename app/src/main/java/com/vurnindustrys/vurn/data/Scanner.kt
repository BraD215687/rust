package com.vurnindustrys.vurn.data

import com.vurnindustrys.vurn.model.NewsItem
import com.vurnindustrys.vurn.model.Quote
import com.vurnindustrys.vurn.model.StockPick
import kotlinx.coroutines.delay

class Scanner(private val client: FinnhubClient) {
    private val universe = listOf(
        "ACHR","ADTX","AMTX","ATER","ATOS","BBAI","BITF","BLNK","CLOV","CENN",
        "DNN","EVGO","FFIE","GFAI","HIVE","HUT","KULR","LUMN","MVIS","NNDM",
        "OPEN","OPTT","PLUG","QBTS","RGTI","SENS","SIRI","SOUN","TLRY","WULF","ZOM"
    )

    suspend fun scan(onProgress: (Int, Int) -> Unit = { _, _ -> }): List<StockPick> {
        val candidates = mutableListOf<Pair<String, Quote>>()
        universe.forEachIndexed { index, symbol ->
            runCatching { client.quote(symbol) }.getOrNull()?.let { q ->
                if (q.c in 0.20..5.00 && q.pc > 0.0) candidates += symbol to q
            }
            onProgress(index + 1, universe.size)
            delay(1050)
        }
        val preRanked = candidates.sortedByDescending { (_, q) -> preliminaryScore(q) }.take(14)
        return preRanked.mapIndexed { index, (symbol, quote) ->
            val news = runCatching { client.news(symbol) }.getOrDefault(emptyList())
            onProgress(universe.size + index + 1, universe.size + preRanked.size)
            delay(1050)
            score(symbol, quote, news)
        }.sortedByDescending { it.score }.take(10)
    }

    private fun preliminaryScore(q: Quote): Double {
        val range = (q.h - q.l).takeIf { it > 0 } ?: 0.01
        val pos = ((q.c - q.l) / range).coerceIn(0.0, 1.0)
        val gap = if (q.pc > 0) ((q.o - q.pc) / q.pc) * 100 else 0.0
        return q.dp * 2.2 + pos * 7.0 + gap.coerceIn(-8.0, 8.0)
    }

    private fun score(symbol: String, q: Quote, news: List<NewsItem>): StockPick {
        val range = (q.h - q.l).takeIf { it > 0 } ?: 0.01
        val pos = ((q.c - q.l) / range).coerceIn(0.0, 1.0)
        val gap = if (q.pc > 0) ((q.o - q.pc) / q.pc) * 100 else 0.0
        val sent = news.take(8).sumOf { sentiment(it.headline + " " + it.summary) }
        var raw = 48.0 + q.dp.coerceIn(-12.0, 12.0) * 1.45 + (pos - .5) * 18.0 + gap.coerceIn(-8.0, 8.0) * .8 + sent.coerceIn(-8, 8) * 2.2
        if (q.c < .50) raw -= 8.0
        if ((q.h - q.l) / q.c > .35) raw -= 5.0
        val final = raw.toInt().coerceIn(5, 95)
        val catalysts = news.asSequence().filter { sentiment(it.headline + " " + it.summary) > 0 }.map { it.headline }.filter { it.isNotBlank() }.distinct().take(2).toList()
        val thesis = (if (q.dp > 0) "Positive momentum" else "Contrarian setup") + if (sent > 0) " with supportive recent headlines." else " with limited news support."
        return StockPick(symbol, q.c, q.dp, final, if (final >= 78) "High" else if (final >= 63) "Medium" else "Speculative", thesis, catalysts.ifEmpty { listOf("No strong positive catalyst detected") }, if (q.c < .50) "Extreme micro-price volatility" else "High-risk penny stock")
    }

    private fun sentiment(text: String): Int {
        val t = text.lowercase()
        val positive = listOf("approval","approved","contract","award","partnership","launch","record","growth","profit","surge","breakthrough","acquisition","beats","upgrade","expands")
        val negative = listOf("offering","dilution","bankruptcy","delisting","warning","loss","lawsuit","investigation","misses","downgrade","reverse split","default")
        return positive.count { it in t } - negative.count { it in t }
    }
}
