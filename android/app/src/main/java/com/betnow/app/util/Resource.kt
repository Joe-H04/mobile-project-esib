package com.betnow.app.util

import org.json.JSONObject
import retrofit2.Response

sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
}

inline fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> = when (this) {
    is Resource.Success -> Resource.Success(transform(data))
    is Resource.Error -> this
    Resource.Loading -> this
}

suspend fun <T> apiCall(
    fallbackError: String = "Something went wrong",
    call: suspend () -> Response<T>
): Resource<T> = try {
    val response = call()
    val body = response.body()
    if (response.isSuccessful && body != null) {
        Resource.Success(body)
    } else {
        Resource.Error(parseError(response.errorBody()?.string(), fallbackError))
    }
} catch (e: Exception) {
    Resource.Error(e.message ?: "Network error")
}

private fun parseError(body: String?, fallback: String): String =
    try {
        JSONObject(body ?: "{}").optString("error", fallback)
    } catch (_: Exception) {
        fallback
    }
