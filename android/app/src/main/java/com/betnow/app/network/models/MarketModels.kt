package com.betnow.app.network.models

data class MarketsResponse(val markets: List<Market>)

data class MarketResponse(val market: Market)

data class CategoriesResponse(val categories: List<Category>)

data class Category(val name: String, val count: Int)

data class Market(
    val id: String,
    val question: String,
    val description: String,
    val slug: String,
    val image: String,
    val outcomes: List<String>,
    val outcomePrices: List<Double>,
    val volume: String,
    val liquidity: String,
    val endDate: String?,
    val active: Boolean,
    val closed: Boolean,
    val lastUpdated: String?,
    val category: String?,
    val resolved: Boolean = false,
    val winningOutcome: String? = null
)

data class WatchlistToggleResponse(val watching: Boolean)
