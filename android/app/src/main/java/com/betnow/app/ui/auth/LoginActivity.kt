package com.betnow.app.ui.auth

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.betnow.app.databinding.ActivityLoginBinding
import com.betnow.app.ui.geo.GeoBlockActivity
import com.betnow.app.ui.main.MainActivity
import com.betnow.app.util.GeoRestrictionManager
import com.betnow.app.util.Resource
import com.betnow.app.util.TokenManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { runGeoCheck() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFormEnabled(false)

        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(binding.root, "Please fill in all fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Snackbar.make(binding.root, "Password must be at least 6 characters", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.login(email, password)
        }

        binding.registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        viewModel.loginResult.observe(this) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.loginButton.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.loginButton.isEnabled = true
                    val data = result.data
                    TokenManager.saveToken(this, data.token)
                    TokenManager.saveUser(this, data.user.id, data.user.email, data.user.balance)
                    navigateToMain()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.loginButton.isEnabled = true
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ensureGeoAllowed()
    }

    private fun ensureGeoAllowed() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }

    private fun runGeoCheck() {
        binding.progressBar.visibility = View.VISIBLE
        setFormEnabled(false)
        lifecycleScope.launch {
            val status = withContext(Dispatchers.IO) { GeoRestrictionManager.check(this@LoginActivity) }
            binding.progressBar.visibility = View.GONE
            when (status) {
                is GeoRestrictionManager.Status.Allowed -> {
                    setFormEnabled(true)
                    if (TokenManager.isLoggedIn(this@LoginActivity)) navigateToMain()
                }
                is GeoRestrictionManager.Status.Blocked -> {
                    TokenManager.clear(this@LoginActivity)
                    startActivity(
                        GeoBlockActivity.newIntent(
                            this@LoginActivity,
                            status.countryCode,
                            status.closeOnly
                        )
                    )
                    finish()
                }
                GeoRestrictionManager.Status.PermissionMissing,
                GeoRestrictionManager.Status.LocationUnavailable -> {
                    startActivity(GeoBlockActivity.newIntent(this@LoginActivity, null, false))
                    finish()
                }
            }
        }
    }

    private fun setFormEnabled(enabled: Boolean) {
        binding.loginButton.isEnabled = enabled
        binding.emailInput.isEnabled = enabled
        binding.passwordInput.isEnabled = enabled
        binding.registerLink.isEnabled = enabled
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
