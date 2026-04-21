package com.betnow.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.betnow.app.databinding.ActivityRegisterBinding
import com.betnow.app.ui.main.MainActivity
import com.betnow.app.util.Resource
import com.betnow.app.util.TokenManager
import com.google.android.material.snackbar.Snackbar

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registerButton.setOnClickListener { submit() }
        binding.loginLink.setOnClickListener { finish() }

        viewModel.registerResult.observe(this) { result ->
            val loading = result is Resource.Loading
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.registerButton.isEnabled = !loading
            when (result) {
                is Resource.Success -> {
                    TokenManager.saveToken(this, result.data.token)
                    TokenManager.saveUser(this, result.data.user.id, result.data.user.email, result.data.user.balance)
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
                is Resource.Error -> Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                Resource.Loading -> Unit
            }
        }
    }

    private fun submit() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()
        val confirm = binding.confirmPasswordInput.text.toString()

        val error = when {
            email.isEmpty() || password.isEmpty() || confirm.isEmpty() -> "Please fill in all fields"
            password.length < 6 -> "Password must be at least 6 characters"
            password != confirm -> "Passwords do not match"
            else -> null
        }
        if (error != null) {
            Snackbar.make(binding.root, error, Snackbar.LENGTH_SHORT).show()
            return
        }
        viewModel.register(email, password)
    }
}
