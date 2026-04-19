package com.betnow.app.ui.watchlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.betnow.app.network.RetrofitClient
import com.betnow.app.network.models.Market
import com.betnow.app.repository.MarketRepository
import com.betnow.app.repository.WatchlistRepository
import com.betnow.app.util.Resource
import kotlinx.coroutines.launch

class WatchlistViewModel(application: Application) : AndroidViewModel(application) {

    private val api = RetrofitClient.getApiService(application)
    private val watchlistRepo = WatchlistRepository(api)
    private val marketRepo = MarketRepository(api)

    private val _markets = MutableLiveData<Resource<List<Market>>>()
    val markets: LiveData<Resource<List<Market>>> = _markets

    init {
        loadWatchlist()
    }

    fun loadWatchlist() {
        _markets.value = Resource.Loading()
        viewModelScope.launch {
            _markets.value = watchlistRepo.getWatchlist()
        }
    }

    fun remove(market: Market) {
        viewModelScope.launch {
            val result = watchlistRepo.remove(market.id)
            if (result is Resource.Success) {
                val current = (_markets.value as? Resource.Success)?.data ?: return@launch
                _markets.postValue(Resource.Success(current.filterNot { it.id == market.id }))
            }
        }
    }

    fun startOddsUpdates() {
        marketRepo.observeOddsUpdates { marketId, newPrices ->
            val currentList = (_markets.value as? Resource.Success)?.data?.toMutableList()
                ?: return@observeOddsUpdates
            val index = currentList.indexOfFirst { it.id == marketId }
            if (index != -1) {
                currentList[index] = currentList[index].copy(outcomePrices = newPrices)
                _markets.postValue(Resource.Success(currentList))
            }
        }
        marketRepo.observeMarketResolved { marketId, winningOutcome ->
            val currentList = (_markets.value as? Resource.Success)?.data?.toMutableList()
                ?: return@observeMarketResolved
            val index = currentList.indexOfFirst { it.id == marketId }
            if (index != -1) {
                currentList[index] = currentList[index].copy(
                    resolved = true,
                    winningOutcome = winningOutcome
                )
                _markets.postValue(Resource.Success(currentList))
            }
        }
    }

    fun stopOddsUpdates() {
        marketRepo.stopObservingOddsUpdates()
        marketRepo.stopObservingMarketResolved()
    }
}
