package com.betnow.app.repository

import com.betnow.app.network.ApiService
import com.betnow.app.network.models.Market
import com.betnow.app.util.Resource
import com.betnow.app.util.apiCall
import com.betnow.app.util.map

class WatchlistRepository(private val api: ApiService) {

    suspend fun getWatchlist(): Resource<List<Market>> =
        apiCall("Failed to load watchlist") { api.getWatchlist() }.map { it.markets }

    suspend fun add(marketId: String): Resource<Boolean> =
        apiCall("Watchlist error") { api.addToWatchlist(marketId) }.map { it.watching }

    suspend fun remove(marketId: String): Resource<Boolean> =
        apiCall("Watchlist error") { api.removeFromWatchlist(marketId) }.map { it.watching }
}
