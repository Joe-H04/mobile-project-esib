package com.betnow.app.ui.support

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.betnow.app.R
import com.betnow.app.databinding.ActivitySupportBinding
import com.betnow.app.network.RetrofitClient
import com.betnow.app.network.SupportSocket
import com.betnow.app.network.models.SupportMessage
import com.betnow.app.util.TokenManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SupportActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupportBinding
    private lateinit var adapter: SupportAdapter
    private var supportSocket: SupportSocket? = null
    private val messages = mutableListOf<SupportMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = SupportAdapter()
        binding.messagesRecycler.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.messagesRecycler.adapter = adapter

        binding.messageInput.doAfterTextChanged {
            binding.sendButton.isEnabled = !it.isNullOrBlank()
        }
        binding.sendButton.setOnClickListener { sendCurrentInput() }

        val token = TokenManager.getToken(this)
        if (token == null) {
            finish()
            return
        }

        connect(token)
        loadHistory()
    }

    private fun connect(token: String) {
        val baseUrl = "http://10.0.2.2:3000"
        binding.statusText.text = getString(R.string.support_status_connecting)
        val socket = SupportSocket(baseUrl, token)
        supportSocket = socket
        socket.onMessage { msg ->
            runOnUiThread { appendMessage(msg) }
        }
        socket.connect(
            onReady = { history ->
                runOnUiThread {
                    setMessages(history)
                    binding.statusText.text = getString(R.string.support_status_online)
                    binding.statusText.visibility = View.GONE
                }
            },
            onError = { err ->
                runOnUiThread {
                    binding.statusText.text = getString(R.string.support_status_offline)
                    binding.statusText.visibility = View.VISIBLE
                    Snackbar.make(binding.root, err, Snackbar.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun loadHistory() {
        binding.progressBar.isVisible = messages.isEmpty()
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.getApiService(applicationContext).getSupportHistory()
                }
                if (response.isSuccessful) {
                    val history = response.body()?.messages.orEmpty()
                    if (messages.isEmpty()) setMessages(history)
                }
            } catch (_: Exception) {
                // socket will still deliver history on auth; ignore REST failures
            } finally {
                binding.progressBar.isVisible = false
            }
        }
    }

    private fun setMessages(list: List<SupportMessage>) {
        messages.clear()
        messages.addAll(list)
        adapter.submitList(messages.toList())
        binding.emptyText.isVisible = messages.isEmpty()
        scrollToBottom()
    }

    private fun appendMessage(m: SupportMessage) {
        if (messages.any { it.id == m.id }) return
        messages.add(m)
        adapter.submitList(messages.toList())
        binding.emptyText.isVisible = false
        scrollToBottom()
    }

    private fun scrollToBottom() {
        if (messages.isEmpty()) return
        binding.messagesRecycler.post {
            binding.messagesRecycler.scrollToPosition(messages.size - 1)
        }
    }

    private fun sendCurrentInput() {
        val text = binding.messageInput.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        val socket = supportSocket ?: return
        binding.sendButton.isEnabled = false
        socket.send(text) { ok, err ->
            runOnUiThread {
                if (ok) {
                    binding.messageInput.setText("")
                } else {
                    binding.sendButton.isEnabled = true
                    Snackbar.make(
                        binding.root,
                        err ?: "Failed to send",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        supportSocket?.disconnect()
        supportSocket = null
        super.onDestroy()
    }
}
