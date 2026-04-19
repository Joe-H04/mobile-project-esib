package com.betnow.app.ui.markets

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.betnow.app.network.RetrofitClient
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
        _markets.value = Resource.Loading()
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
            val result = marketRepo.getCategories()
            if (result is Resource.Success) {
                _categories.value = result.data
            }
        }
    }

    fun loadWatchlistIds() {
        viewModelScope.launch {
            val result = watchlistRepo.getWatchlist()
            if (result is Resource.Success) {
                _watchlistIds.value = result.data.map { it.id }.toSet()
            }
        }
    }

    fun toggleWatchlist(market: Market) {
        val current = _watchlistIds.value ?: emptySet()
        val isWatching = current.contains(market.id)
        viewModelScope.launch {
            val result = if (isWatching) {
                watchlistRepo.remove(market.id)
            } else {
                watchlistRepo.add(market.id)
            }
            if (result is Resource.Success) {
                val updated = current.toMutableSet()
                if (result.data) updated.add(market.id) else updated.remove(market.id)
                _watchlistIds.value = updated
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
