package com.vurnindustrys.vurn.data

import android.text.Html
import com.google.gson.JsonParser
import com.vurnindustrys.vurn.model.NewsItem
import com.vurnindustrys.vurn.model.Quote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class VurnDataClient {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    suspend fun quote(symbol: String): Quote = withContext(Dispatchers.IO) {
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol".toHttpUrl().newBuilder()
            .addQueryParameter("range", "5d")
            .addQueryParameter("interval", "1d")
            .addQueryParameter("includePrePost", "false")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 VurnIndustrys/1.0")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Price feed unavailable: ${response.code}")
            val root = JsonParser.parseString(response.body?.string().orEmpty()).asJsonObject
            val result = root.getAsJsonObject("chart")?.getAsJsonArray("result")?.firstOrNull()?.asJsonObject
                ?: error("No price data for $symbol")
            val meta = result.getAsJsonObject("meta")
            val quote = result.getAsJsonObject("indicators")?.getAsJsonArray("quote")?.firstOrNull()?.asJsonObject
            val closes = quote?.getAsJsonArray("close")?.mapNotNull { if (it.isJsonNull) null else it.asDouble }.orEmpty()
            val highs = quote?.getAsJsonArray("high")?.mapNotNull { if (it.isJsonNull) null else it.asDouble }.orEmpty()
            val lows = quote?.getAsJsonArray("low")?.mapNotNull { if (it.isJsonNull) null else it.asDouble }.orEmpty()
            val opens = quote?.getAsJsonArray("open")?.mapNotNull { if (it.isJsonNull) null else it.asDouble }.orEmpty()
            val current = meta?.get("regularMarketPrice")?.takeUnless { it.isJsonNull }?.asDouble
                ?: closes.lastOrNull() ?: 0.0
            val previous = meta?.get("previousClose")?.takeUnless { it.isJsonNull }?.asDouble
                ?: closes.dropLast(1).lastOrNull() ?: current
            val change = if (previous > 0) ((current - previous) / previous) * 100.0 else 0.0
            Quote(
                c = current,
                d = current - previous,
                dp = change,
                h = highs.lastOrNull() ?: current,
                l = lows.lastOrNull() ?: current,
                o = opens.lastOrNull() ?: current,
                pc = previous,
                t = System.currentTimeMillis() / 1000
            )
        }
    }

    suspend fun news(symbol: String): List<NewsItem> = withContext(Dispatchers.IO) {
        val query = URLEncoder.encode("$symbol stock when:7d", StandardCharsets.UTF_8.toString())
        val request = Request.Builder()
            .url("https://news.google.com/rss/search?q=$query&hl=en-US&gl=US&ceid=US:en")
            .header("User-Agent", "Mozilla/5.0 VurnIndustrys/1.0")
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val xml = response.body?.string().orEmpty()
                Regex("<item>(.*?)</item>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                    .findAll(xml)
                    .mapNotNull { match ->
                        val block = match.groupValues[1]
                        val rawTitle = Regex("<title>(.*?)</title>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                            .find(block)?.groupValues?.getOrNull(1) ?: return@mapNotNull null
                        val title = Html.fromHtml(rawTitle, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        if (title.isBlank()) null else NewsItem(headline = title, summary = "", source = "Google News")
                    }
                    .distinctBy { it.headline }
                    .take(12)
                    .toList()
            }
        }.getOrDefault(emptyList())
    }
}
