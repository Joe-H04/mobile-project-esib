package com.betnow.app.util

import android.content.Context
import android.content.SharedPreferences

object TokenManager {

    private const val PREF_NAME = "betnow_prefs"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_BALANCE = "user_balance"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveToken(context: Context, token: String) {
        prefs(context).edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(context: Context): String? =
        prefs(context).getString(KEY_TOKEN, null)

    fun saveUser(context: Context, id: String, email: String, balance: Double) {
        prefs(context).edit()
            .putString(KEY_USER_ID, id)
            .putString(KEY_USER_EMAIL, email)
            .putFloat(KEY_USER_BALANCE, balance.toFloat())
            .apply()
    }

    fun getBalance(context: Context): Double =
        prefs(context).getFloat(KEY_USER_BALANCE, 0f).toDouble()

    fun updateBalance(context: Context, newBalance: Double) {
        prefs(context).edit().putFloat(KEY_USER_BALANCE, newBalance.toFloat()).apply()
    }

    fun getUserEmail(context: Context): String? =
        prefs(context).getString(KEY_USER_EMAIL, null)

    fun isLoggedIn(context: Context): Boolean =
        getToken(context) != null

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
