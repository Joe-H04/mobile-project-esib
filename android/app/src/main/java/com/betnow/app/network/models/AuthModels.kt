package com.betnow.app.network.models

data class LoginRequest(val email: String, val password: String)

data class RegisterRequest(val email: String, val password: String)

data class AuthResponse(val token: String, val user: UserInfo)

data class UserInfo(val id: String, val email: String, val balance: Double)
