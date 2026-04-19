package com.betnow.app.network.models

data class PlaceBetRequest(val marketId: String, val side: String, val amount: Double)

data class PlaceBetResponse(val bet: Bet, val newBalance: Double)

data class MyBetsResponse(val bets: List<Bet>)

data class RedeemResponse(
    val bet: Bet,
    val payout: Double,
    val won: Boolean,
    val newBalance: Double
)

data class Bet(
    val id: String,
    val marketId: String,
    val marketQuestion: String,
    val side: String,
    val shares: Double,
    val pricePerShare: Double,
    val totalCost: Double,
    val createdAt: String,
    val redeemed: Boolean = false,
    val payout: Double = 0.0,
    val redeemedAt: String? = null,
    val marketResolved: Boolean = false,
    val marketWinningOutcome: String? = null
)
