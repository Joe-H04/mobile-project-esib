package com.betnow.app.network

import com.betnow.app.network.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("api/auth/me")
    suspend fun getMe(): Response<MeResponse>

    @GET("api/markets")
    suspend fun getMarkets(
        @Query("search") search: String? = null,
        @Query("category") category: String? = null,
        @Query("sort") sort: String? = null
    ): Response<MarketsResponse>

    @GET("api/markets/{id}")
    suspend fun getMarket(@Path("id") id: String): Response<MarketResponse>

    @GET("api/markets/categories")
    suspend fun getCategories(): Response<CategoriesResponse>

    @POST("api/bets/place")
    suspend fun placeBet(@Body request: PlaceBetRequest): Response<PlaceBetResponse>

    @GET("api/bets/my")
    suspend fun getMyBets(): Response<MyBetsResponse>

    @POST("api/bets/{id}/redeem")
    suspend fun redeemBet(@Path("id") id: String): Response<RedeemResponse>

    @GET("api/watchlist")
    suspend fun getWatchlist(): Response<MarketsResponse>

    @POST("api/watchlist/{marketId}")
    suspend fun addToWatchlist(@Path("marketId") marketId: String): Response<WatchlistToggleResponse>

    @DELETE("api/watchlist/{marketId}")
    suspend fun removeFromWatchlist(@Path("marketId") marketId: String): Response<WatchlistToggleResponse>

    @GET("api/leaderboard")
    suspend fun getLeaderboard(): Response<LeaderboardResponse>

    @GET("api/support/me")
    suspend fun getSupportHistory(): Response<SupportHistoryResponse>
}
