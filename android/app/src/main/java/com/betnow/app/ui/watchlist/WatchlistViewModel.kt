package com.betnow.app.ui.watchlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.betnow.app.network.RetrofitClient
import com.betnow.app.network.SocketManager
import com.betnow.app.network.models.Market
import com.betnow.app.repository.WatchlistRepository
import com.betnow.app.util.Resource
import kotlinx.coroutines.launch

class WatchlistViewModel(application: Application) : AndroidViewModel(application) {

    private val watchlistRepo = WatchlistRepository(RetrofitClient.getApiService(application))

    private val _markets = MutableLiveData<Resource<List<Market>>>()
    val markets: LiveData<Resource<List<Market>>> = _markets

    init {
        loadWatchlist()
    }

    fun loadWatchlist() {
        _markets.value = Resource.Loading
        viewModelScope.launch {
            _markets.value = watchlistRepo.getWatchlist()
        }
    }

    fun remove(market: Market) {
        viewModelScope.launch {
            if (watchlistRepo.remove(market.id) is Resource.Success) {
                val current = (_markets.value as? Resource.Success)?.data ?: return@launch
                _markets.postValue(Resource.Success(current.filterNot { it.id == market.id }))
            }
        }
    }

    fun startOddsUpdates() {
        SocketManager.onOddsUpdate { marketId, prices ->
            updateMarket(marketId) { it.copy(outcomePrices = prices) }
        }
        SocketManager.onMarketResolved { marketId, winningOutcome ->
            updateMarket(marketId) { it.copy(resolved = true, winningOutcome = winningOutcome) }
        }
    }

    fun stopOddsUpdates() {
        SocketManager.off("odds-update")
        SocketManager.off("market-resolved")
    }

    private fun updateMarket(marketId: String, transform: (Market) -> Market) {
        val list = (_markets.value as? Resource.Success)?.data ?: return
        val index = list.indexOfFirst { it.id == marketId }
        if (index == -1) return
        _markets.postValue(Resource.Success(list.toMutableList().also { it[index] = transform(it[index]) }))
    }
}
