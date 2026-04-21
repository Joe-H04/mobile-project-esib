package com.betnow.app.repository

import com.betnow.app.data.local.CachedProfileEntity
import com.betnow.app.data.local.ProfileDao
import com.betnow.app.network.ApiService
import com.betnow.app.network.models.MeResponse
import com.betnow.app.util.Resource
import com.betnow.app.util.apiCall

class ProfileRepository(
    private val api: ApiService,
    private val profileDao: ProfileDao,
    private val currentUserId: () -> String?
) {

    suspend fun getMe(): Resource<MeResponse> {
        val result = apiCall("Failed to load profile") { api.getMe() }
        return when (result) {
            is Resource.Success -> {
                profileDao.upsert(CachedProfileEntity.from(result.data))
                result
            }
            is Resource.Error -> cachedOr(result)
            Resource.Loading -> result
        }
    }

    private suspend fun cachedOr(error: Resource.Error): Resource<MeResponse> {
        val userId = currentUserId() ?: return error
        val cached = profileDao.getProfile(userId) ?: return error
        return Resource.Success(cached.toMeResponse())
    }
}
