package com.betnow.app.repository

import com.betnow.app.network.ApiService
import com.betnow.app.network.models.Category
import com.betnow.app.network.models.Market
import com.betnow.app.util.Resource
import com.betnow.app.util.apiCall
import com.betnow.app.util.map

class MarketRepository(private val api: ApiService) {

    suspend fun getMarkets(
        search: String? = null,
        category: String? = null,
        sort: String? = null
    ): Resource<List<Market>> = apiCall("Failed to load markets") {
        api.getMarkets(
            search = search?.takeIf { it.isNotBlank() },
            category = category?.takeIf { it.isNotBlank() && it != "All" },
            sort = sort?.takeIf { it.isNotBlank() }
        )
    }.map { it.markets }

    suspend fun getMarket(id: String): Resource<Market> =
        apiCall("Market not found") { api.getMarket(id) }.map { it.market }

    suspend fun getCategories(): Resource<List<Category>> =
        apiCall("Failed to load categories") { api.getCategories() }.map { it.categories }
}
