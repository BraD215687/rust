package com.vurnindustrys.vurn.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vurnindustrys.vurn.model.NewsItem
import com.vurnindustrys.vurn.model.Quote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate

class FinnhubClient(private val token: String) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val base = "https://finnhub.io/api/v1"

    suspend fun quote(symbol: String): Quote = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$base/quote?symbol=$symbol&token=$token").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Quote request failed: ${response.code}")
            gson.fromJson(response.body?.string(), Quote::class.java)
        }
    }

    suspend fun news(symbol: String): List<NewsItem> = withContext(Dispatchers.IO) {
        val to = LocalDate.now()
        val from = to.minusDays(8)
        val request = Request.Builder().url("$base/company-news?symbol=$symbol&from=$from&to=$to&token=$token").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val type = object : TypeToken<List<NewsItem>>() {}.type
            gson.fromJson<List<NewsItem>>(response.body?.string(), type) ?: emptyList()
        }
    }
}
