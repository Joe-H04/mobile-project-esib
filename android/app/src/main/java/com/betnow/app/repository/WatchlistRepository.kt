package com.betnow.app.repository

import com.betnow.app.network.ApiService
import com.betnow.app.network.models.Market
import com.betnow.app.util.Resource
import org.json.JSONObject

class WatchlistRepository(private val api: ApiService) {

    suspend fun getWatchlist(): Resource<List<Market>> {
        return try {
            val response = api.getWatchlist()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.markets)
            } else {
                Resource.Error("Failed to load watchlist")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun add(marketId: String): Resource<Boolean> {
        return try {
            val response = api.addToWatchlist(marketId)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.watching)
            } else {
                Resource.Error(parseError(response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun remove(marketId: String): Resource<Boolean> {
        return try {
            val response = api.removeFromWatchlist(marketId)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.watching)
            } else {
                Resource.Error(parseError(response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    private fun parseError(body: String?): String {
        return try {
            JSONObject(body ?: "{}").optString("error", "Watchlist error")
        } catch (e: Exception) {
            "Watchlist error"
        }
    }
}
