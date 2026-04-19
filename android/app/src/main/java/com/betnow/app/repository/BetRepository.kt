package com.betnow.app.repository

import com.betnow.app.network.ApiService
import com.betnow.app.network.models.Bet
import com.betnow.app.network.models.PlaceBetRequest
import com.betnow.app.network.models.PlaceBetResponse
import com.betnow.app.network.models.RedeemResponse
import com.betnow.app.util.Resource
import org.json.JSONObject

class BetRepository(private val api: ApiService) {

    suspend fun placeBet(marketId: String, side: String, amount: Double): Resource<PlaceBetResponse> {
        return try {
            val response = api.placeBet(PlaceBetRequest(marketId, side, amount))
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(parseError(response.errorBody()?.string(), "Failed to place bet"))
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getMyBets(): Resource<List<Bet>> {
        return try {
            val response = api.getMyBets()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.bets)
            } else {
                Resource.Error("Failed to load bets")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun redeemBet(betId: String): Resource<RedeemResponse> {
        return try {
            val response = api.redeemBet(betId)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(parseError(response.errorBody()?.string(), "Failed to redeem bet"))
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    private fun parseError(errorBody: String?, fallback: String): String {
        return try {
            JSONObject(errorBody ?: "{}").optString("error", fallback)
        } catch (e: Exception) {
            fallback
        }
    }
}
