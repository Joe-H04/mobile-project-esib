package com.betnow.app.network

import android.content.Context
import com.betnow.app.util.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // 10.0.2.2 is the emulator alias for the host machine's localhost.
    const val BASE_URL = "http://10.0.2.2:3000/"

    @Volatile
    private var apiService: ApiService? = null

    fun getApiService(context: Context): ApiService = apiService ?: synchronized(this) {
        apiService ?: build(context.applicationContext).also { apiService = it }
    }

    private fun build(appContext: Context): ApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = TokenManager.getToken(appContext)
                val request = chain.request().newBuilder().apply {
                    if (token != null) addHeader("Authorization", "Bearer $token")
                }.build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
