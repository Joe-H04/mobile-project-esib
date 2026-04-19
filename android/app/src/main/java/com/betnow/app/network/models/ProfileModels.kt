package com.betnow.app.network.models

data class MeResponse(val user: ProfileUser, val stats: UserStats)

data class ProfileUser(
    val id: String,
    val email: String,
    val balance: Double,
    val createdAt: String
)

data class UserStats(
    val betsCount: Int,
    val openBets: Int,
    val wagered: Double,
    val redeemed: Double,
    val wins: Int,
    val netProfit: Double
)
