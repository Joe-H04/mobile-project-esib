package com.betnow.app.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.betnow.app.network.RetrofitClient
import com.betnow.app.network.models.MeResponse
import com.betnow.app.repository.ProfileRepository
import com.betnow.app.util.Resource
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ProfileRepository(RetrofitClient.getApiService(application))

    private val _me = MutableLiveData<Resource<MeResponse>>()
    val me: LiveData<Resource<MeResponse>> = _me

    init {
        load()
    }

    fun load() {
        _me.value = Resource.Loading()
        viewModelScope.launch {
            _me.value = repo.getMe()
        }
    }
}
