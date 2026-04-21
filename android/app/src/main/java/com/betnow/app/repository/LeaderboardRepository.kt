package com.betnow.app.repository

import com.betnow.app.network.ApiService
import com.betnow.app.network.models.LeaderboardEntry
import com.betnow.app.util.Resource
import com.betnow.app.util.apiCall
import com.betnow.app.util.map

class LeaderboardRepository(private val api: ApiService) {

    suspend fun getLeaderboard(): Resource<List<LeaderboardEntry>> =
        apiCall("Failed to load leaderboard") { api.getLeaderboard() }.map { it.leaderboard }
}
