package com.betnow.app.ui.leaderboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.betnow.app.network.RetrofitClient
import com.betnow.app.network.models.LeaderboardEntry
import com.betnow.app.repository.LeaderboardRepository
import com.betnow.app.util.Resource
import kotlinx.coroutines.launch

class LeaderboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = LeaderboardRepository(RetrofitClient.getApiService(application))

    private val _entries = MutableLiveData<Resource<List<LeaderboardEntry>>>()
    val entries: LiveData<Resource<List<LeaderboardEntry>>> = _entries

    init {
        load()
    }

    fun load() {
        _entries.value = Resource.Loading
        viewModelScope.launch {
            _entries.value = repo.getLeaderboard()
        }
    }
}
