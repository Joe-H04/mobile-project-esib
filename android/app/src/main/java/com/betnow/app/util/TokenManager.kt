package com.betnow.app.util

import android.content.Context
import androidx.core.content.edit

object TokenManager {

    private const val PREFS = "betnow_prefs"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_BALANCE = "user_balance"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveToken(context: Context, token: String) =
        prefs(context).edit { putString(KEY_TOKEN, token) }

    fun saveUser(context: Context, id: String, email: String, balance: Double) =
        prefs(context).edit {
            putString(KEY_USER_ID, id)
            putString(KEY_USER_EMAIL, email)
            putFloat(KEY_BALANCE, balance.toFloat())
        }

    fun updateBalance(context: Context, balance: Double) =
        prefs(context).edit { putFloat(KEY_BALANCE, balance.toFloat()) }

    fun getToken(context: Context): String? = prefs(context).getString(KEY_TOKEN, null)
    fun getUserId(context: Context): String? = prefs(context).getString(KEY_USER_ID, null)
    fun getUserEmail(context: Context): String? = prefs(context).getString(KEY_USER_EMAIL, null)
    fun getBalance(context: Context): Double = prefs(context).getFloat(KEY_BALANCE, 0f).toDouble()
    fun isLoggedIn(context: Context): Boolean = getToken(context) != null

    fun clear(context: Context) = prefs(context).edit { clear() }
}
