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
    private val adapter = SupportAdapter()
    private val messages = mutableListOf<SupportMessage>()
    private var supportSocket: SupportSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val token = TokenManager.getToken(this)
        if (token == null) {
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.messagesRecycler.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.messagesRecycler.adapter = adapter
        binding.messageInput.doAfterTextChanged { binding.sendButton.isEnabled = !it.isNullOrBlank() }
        binding.sendButton.setOnClickListener { sendCurrentInput() }

        connect(token)
        loadHistory()
    }

    private fun connect(token: String) {
        binding.statusText.text = getString(R.string.support_status_connecting)
        supportSocket = SupportSocket(RetrofitClient.BASE_URL.trimEnd('/'), token).apply {
            onMessage { m -> runOnUiThread { appendMessage(m) } }
            connect(
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
    }

    private fun loadHistory() {
        binding.progressBar.isVisible = messages.isEmpty()
        lifecycleScope.launch {
            val history = runCatching {
                withContext(Dispatchers.IO) {
                    RetrofitClient.getApiService(applicationContext).getSupportHistory()
                }
            }.getOrNull()?.takeIf { it.isSuccessful }?.body()?.messages

            if (history != null && messages.isEmpty()) setMessages(history)
            binding.progressBar.isVisible = false
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
                    Snackbar.make(binding.root, err ?: "Failed to send", Snackbar.LENGTH_SHORT).show()
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
