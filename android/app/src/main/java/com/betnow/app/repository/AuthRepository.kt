package com.betnow.app.repository

import com.betnow.app.network.ApiService
import com.betnow.app.network.models.AuthResponse
import com.betnow.app.network.models.LoginRequest
import com.betnow.app.network.models.RegisterRequest
import com.betnow.app.util.Resource
import com.betnow.app.util.apiCall

class AuthRepository(private val api: ApiService) {

    suspend fun login(email: String, password: String): Resource<AuthResponse> =
        apiCall { api.login(LoginRequest(email, password)) }

    suspend fun register(email: String, password: String): Resource<AuthResponse> =
        apiCall { api.register(RegisterRequest(email, password)) }
}
