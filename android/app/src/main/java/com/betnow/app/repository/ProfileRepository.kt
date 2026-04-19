package com.betnow.app.repository

import com.betnow.app.network.ApiService
import com.betnow.app.network.models.MeResponse
import com.betnow.app.util.Resource

class ProfileRepository(private val api: ApiService) {

    suspend fun getMe(): Resource<MeResponse> {
        return try {
            val response = api.getMe()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Failed to load profile")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }
}
