package com.betnow.app.network.models

data class LeaderboardResponse(val leaderboard: List<LeaderboardEntry>)

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val email: String,
    val balance: Double,
    val betsCount: Int,
    val wagered: Double,
    val redeemed: Double,
    val netProfit: Double
)
