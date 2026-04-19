package com.betnow.app.repository

import com.betnow.app.network.ApiService
import com.betnow.app.network.models.AuthResponse
import com.betnow.app.network.models.LoginRequest
import com.betnow.app.network.models.RegisterRequest
import com.betnow.app.util.Resource
import org.json.JSONObject

class AuthRepository(private val api: ApiService) {

    suspend fun login(email: String, password: String): Resource<AuthResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(parseError(response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun register(email: String, password: String): Resource<AuthResponse> {
        return try {
            val response = api.register(RegisterRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(parseError(response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    private fun parseError(errorBody: String?): String {
        return try {
            val json = JSONObject(errorBody ?: "{}")
            json.optString("error", "Something went wrong")
        } catch (e: Exception) {
            "Something went wrong"
        }
    }
}
