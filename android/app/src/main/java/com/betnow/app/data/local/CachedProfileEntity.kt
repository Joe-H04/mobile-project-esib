package com.betnow.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.betnow.app.network.models.MeResponse
import com.betnow.app.network.models.ProfileUser
import com.betnow.app.network.models.UserStats

@Entity(tableName = "cached_profile")
data class CachedProfileEntity(
    @PrimaryKey val userId: String,
    val email: String,
    val balance: Double,
    val createdAt: String,
    val betsCount: Int,
    val openBets: Int,
    val wagered: Double,
    val redeemed: Double,
    val wins: Int,
    val netProfit: Double,
    val cachedAt: Long
) {
    fun toMeResponse(): MeResponse = MeResponse(
        user = ProfileUser(
            id = userId,
            email = email,
            balance = balance,
            createdAt = createdAt
        ),
        stats = UserStats(
            betsCount = betsCount,
            openBets = openBets,
            wagered = wagered,
            redeemed = redeemed,
            wins = wins,
            netProfit = netProfit
        )
    )

    companion object {
        fun from(response: MeResponse): CachedProfileEntity = CachedProfileEntity(
            userId = response.user.id,
            email = response.user.email,
            balance = response.user.balance,
            createdAt = response.user.createdAt,
            betsCount = response.stats.betsCount,
            openBets = response.stats.openBets,
            wagered = response.stats.wagered,
            redeemed = response.stats.redeemed,
            wins = response.stats.wins,
            netProfit = response.stats.netProfit,
            cachedAt = System.currentTimeMillis()
        )
    }
}
