package com.betnow.app.ui.bets

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.betnow.app.network.RetrofitClient
import com.betnow.app.network.models.Bet
import com.betnow.app.network.models.RedeemResponse
import com.betnow.app.repository.BetRepository
import com.betnow.app.util.Resource
import kotlinx.coroutines.launch

class MyBetsViewModel(application: Application) : AndroidViewModel(application) {

    private val betRepository = BetRepository(RetrofitClient.getApiService(application))

    private val _bets = MutableLiveData<Resource<List<Bet>>>()
    val bets: LiveData<Resource<List<Bet>>> = _bets

    private val _redeemResult = MutableLiveData<Resource<RedeemResponse>?>()
    val redeemResult: LiveData<Resource<RedeemResponse>?> = _redeemResult

    init {
        loadBets()
    }

    fun loadBets() {
        _bets.value = Resource.Loading
        viewModelScope.launch {
            _bets.value = betRepository.getMyBets()
        }
    }

    fun redeem(betId: String) {
        _redeemResult.value = Resource.Loading
        viewModelScope.launch {
            val result = betRepository.redeemBet(betId)
            _redeemResult.value = result
            if (result is Resource.Success) {
                loadBets()
            }
        }
    }

    fun clearRedeemResult() {
        _redeemResult.value = null
    }
}
