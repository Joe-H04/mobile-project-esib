package com.betnow.app.repository

import com.betnow.app.network.ApiService
import com.betnow.app.network.models.Bet
import com.betnow.app.network.models.PlaceBetRequest
import com.betnow.app.network.models.PlaceBetResponse
import com.betnow.app.network.models.RedeemResponse
import com.betnow.app.util.Resource
import com.betnow.app.util.apiCall
import com.betnow.app.util.map

class BetRepository(private val api: ApiService) {

    suspend fun placeBet(marketId: String, side: String, amount: Double): Resource<PlaceBetResponse> =
        apiCall("Failed to place bet") { api.placeBet(PlaceBetRequest(marketId, side, amount)) }

    suspend fun getMyBets(): Resource<List<Bet>> =
        apiCall("Failed to load bets") { api.getMyBets() }.map { it.bets }

    suspend fun redeemBet(betId: String): Resource<RedeemResponse> =
        apiCall("Failed to redeem bet") { api.redeemBet(betId) }
}
