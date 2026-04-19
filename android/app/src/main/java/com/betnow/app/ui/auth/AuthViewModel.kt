package com.betnow.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.betnow.app.network.RetrofitClient
import com.betnow.app.network.models.AuthResponse
import com.betnow.app.repository.AuthRepository
import com.betnow.app.util.Resource
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepo = AuthRepository(RetrofitClient.getApiService(application))

    private val _loginResult = MutableLiveData<Resource<AuthResponse>>()
    val loginResult: LiveData<Resource<AuthResponse>> = _loginResult

    private val _registerResult = MutableLiveData<Resource<AuthResponse>>()
    val registerResult: LiveData<Resource<AuthResponse>> = _registerResult

    fun login(email: String, password: String) {
        _loginResult.value = Resource.Loading()
        viewModelScope.launch {
            _loginResult.value = authRepo.login(email, password)
        }
    }

    fun register(email: String, password: String) {
        _registerResult.value = Resource.Loading()
        viewModelScope.launch {
            _registerResult.value = authRepo.register(email, password)
        }
    }
}
