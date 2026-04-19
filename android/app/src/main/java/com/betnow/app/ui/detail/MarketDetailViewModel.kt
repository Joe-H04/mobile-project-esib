package com.betnow.app.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.betnow.app.network.RetrofitClient
import com.betnow.app.network.models.Market
import com.betnow.app.network.models.PlaceBetResponse
import com.betnow.app.repository.BetRepository
import com.betnow.app.repository.MarketRepository
import com.betnow.app.repository.WatchlistRepository
import com.betnow.app.util.Resource
import kotlinx.coroutines.launch

class MarketDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val api = RetrofitClient.getApiService(application)
    private val marketRepo = MarketRepository(api)
    private val betRepo = BetRepository(api)
    private val watchlistRepo = WatchlistRepository(api)

    private val _market = MutableLiveData<Resource<Market>>()
    val market: LiveData<Resource<Market>> = _market

    private val _betResult = MutableLiveData<Resource<PlaceBetResponse>?>()
    val betResult: LiveData<Resource<PlaceBetResponse>?> = _betResult

    private val _isWatching = MutableLiveData<Boolean>(false)
    val isWatching: LiveData<Boolean> = _isWatching

    fun loadMarket(marketId: String) {
        _market.value = Resource.Loading()
        viewModelScope.launch {
            _market.value = marketRepo.getMarket(marketId)
            refreshWatchingState(marketId)
        }
    }

    private suspend fun refreshWatchingState(marketId: String) {
        val result = watchlistRepo.getWatchlist()
        if (result is Resource.Success) {
            _isWatching.postValue(result.data.any { it.id == marketId })
        }
    }

    fun toggleWatchlist(marketId: String) {
        val watching = _isWatching.value == true
        viewModelScope.launch {
            val result = if (watching) watchlistRepo.remove(marketId)
                         else watchlistRepo.add(marketId)
            if (result is Resource.Success) {
                _isWatching.postValue(result.data)
            }
        }
    }

    fun placeBet(marketId: String, side: String, amount: Double) {
        _betResult.value = Resource.Loading()
        viewModelScope.launch {
            _betResult.value = betRepo.placeBet(marketId, side, amount)
        }
    }

    fun observePriceUpdates(marketId: String) {
        marketRepo.observeOddsUpdates { updatedId, newPrices ->
            if (updatedId == marketId) {
                val current = (_market.value as? Resource.Success)?.data ?: return@observeOddsUpdates
                _market.postValue(Resource.Success(current.copy(outcomePrices = newPrices)))
            }
        }
        marketRepo.observeMarketResolved { resolvedId, winningOutcome ->
            if (resolvedId == marketId) {
                val current = (_market.value as? Resource.Success)?.data ?: return@observeMarketResolved
                _market.postValue(
                    Resource.Success(
                        current.copy(resolved = true, winningOutcome = winningOutcome)
                    )
                )
            }
        }
    }

    fun stopPriceUpdates() {
        marketRepo.stopObservingOddsUpdates()
        marketRepo.stopObservingMarketResolved()
    }

    fun clearBetResult() {
        _betResult.value = null
    }
}
