package com.betnow.app.repository

import com.betnow.app.network.ApiService
import com.betnow.app.network.SocketManager
import com.betnow.app.network.models.Category
import com.betnow.app.network.models.Market
import com.betnow.app.util.Resource

class MarketRepository(private val api: ApiService) {

    suspend fun getMarkets(
        search: String? = null,
        category: String? = null,
        sort: String? = null
    ): Resource<List<Market>> {
        return try {
            val response = api.getMarkets(
                search = search?.takeIf { it.isNotBlank() },
                category = category?.takeIf { it.isNotBlank() && it != "All" },
                sort = sort?.takeIf { it.isNotBlank() }
            )
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.markets)
            } else {
                Resource.Error("Failed to load markets")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getMarket(id: String): Resource<Market> {
        return try {
            val response = api.getMarket(id)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.market)
            } else {
                Resource.Error("Market not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getCategories(): Resource<List<Category>> {
        return try {
            val response = api.getCategories()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.categories)
            } else {
                Resource.Error("Failed to load categories")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    fun observeOddsUpdates(callback: (String, List<Double>) -> Unit) {
        SocketManager.onOddsUpdate(callback)
    }

    fun stopObservingOddsUpdates() {
        SocketManager.removeOddsUpdateListener()
    }

    fun observeMarketResolved(callback: (String, String) -> Unit) {
        SocketManager.onMarketResolved(callback)
    }

    fun stopObservingMarketResolved() {
        SocketManager.removeMarketResolvedListener()
    }
}
