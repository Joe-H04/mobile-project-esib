package com.betnow.app.repository

import com.betnow.app.network.ApiService
import com.betnow.app.network.models.LeaderboardEntry
import com.betnow.app.util.Resource

class LeaderboardRepository(private val api: ApiService) {

    suspend fun getLeaderboard(): Resource<List<LeaderboardEntry>> {
        return try {
            val response = api.getLeaderboard()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.leaderboard)
            } else {
                Resource.Error("Failed to load leaderboard")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }
}
