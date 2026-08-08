package com.vurnindustrys.vurn.model

data class Quote(
    val c: Double = 0.0,
    val d: Double = 0.0,
    val dp: Double = 0.0,
    val h: Double = 0.0,
    val l: Double = 0.0,
    val o: Double = 0.0,
    val pc: Double = 0.0,
    val t: Long = 0L
)

data class NewsItem(
    val category: String = "",
    val datetime: Long = 0L,
    val headline: String = "",
    val id: Long = 0L,
    val image: String = "",
    val related: String = "",
    val source: String = "",
    val summary: String = "",
    val url: String = ""
)

data class StockPick(
    val symbol: String,
    val price: Double,
    val changePercent: Double,
    val score: Int,
    val confidence: String,
    val thesis: String,
    val catalysts: List<String>,
    val risk: String
)
