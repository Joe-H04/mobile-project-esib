package com.betnow.app.ui.markets

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.betnow.app.network.RetrofitClient
import com.betnow.app.network.SocketManager
import com.betnow.app.network.models.Category
import com.betnow.app.network.models.Market
import com.betnow.app.repository.MarketRepository
import com.betnow.app.repository.WatchlistRepository
import com.betnow.app.util.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MarketListViewModel(application: Application) : AndroidViewModel(application) {

    private val api = RetrofitClient.getApiService(application)
    private val marketRepo = MarketRepository(api)
    private val watchlistRepo = WatchlistRepository(api)

    private val _markets = MutableLiveData<Resource<List<Market>>>()
    val markets: LiveData<Resource<List<Market>>> = _markets

    private val _categories = MutableLiveData<List<Category>>(emptyList())
    val categories: LiveData<List<Category>> = _categories

    private val _watchlistIds = MutableLiveData<Set<String>>(emptySet())
    val watchlistIds: LiveData<Set<String>> = _watchlistIds

    var search: String = ""
    var sort: String = ""
    var category: String = "All"

    private var searchJob: Job? = null

    init {
        loadCategories()
        loadWatchlistIds()
        loadMarkets()
    }

    fun loadMarkets() {
        _markets.value = Resource.Loading
        viewModelScope.launch {
            _markets.value = marketRepo.getMarkets(search, category, sort)
        }
    }

    fun onSearchChanged(query: String) {
        search = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadMarkets()
        }
    }

    fun onSortChanged(newSort: String) {
        if (sort == newSort) return
        sort = newSort
        loadMarkets()
    }

    fun onCategoryChanged(newCategory: String) {
        if (category == newCategory) return
        category = newCategory
        loadMarkets()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            (marketRepo.getCategories() as? Resource.Success)?.let {
                _categories.value = it.data
            }
        }
    }

    fun loadWatchlistIds() {
        viewModelScope.launch {
            (watchlistRepo.getWatchlist() as? Resource.Success)?.let { result ->
                _watchlistIds.value = result.data.map { it.id }.toSet()
            }
        }
    }

    fun toggleWatchlist(market: Market) {
        val current = _watchlistIds.value ?: emptySet()
        val watching = market.id in current
        viewModelScope.launch {
            val result = if (watching) watchlistRepo.remove(market.id) else watchlistRepo.add(market.id)
            if (result is Resource.Success) {
                _watchlistIds.value = if (result.data) current + market.id else current - market.id
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
