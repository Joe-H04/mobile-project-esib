package com.betnow.app.network

import com.betnow.app.network.models.SupportMessage
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONObject

class SupportSocket(
    private val serverUrl: String,
    private val token: String
) {
    private var socket: Socket? = null
    private var messageListener: ((SupportMessage) -> Unit)? = null

    fun connect(
        onReady: (List<SupportMessage>) -> Unit,
        onError: (String) -> Unit
    ) {
        val s = IO.socket(serverUrl, IO.Options().apply {
            transports = arrayOf("polling", "websocket")
            reconnection = true
            forceNew = true
        })
        socket = s

        s.on(Socket.EVENT_CONNECT) {
            s.emit("support:auth", JSONObject().put("token", token), Ack { args ->
                val res = args.firstOrNull() as? JSONObject
                if (res?.optBoolean("ok") == true) {
                    onReady(res.optJSONArray("messages").toMessages())
                } else {
                    onError(res?.optString("error") ?: "Auth failed")
                }
            })
        }

        s.on("support:message") { args ->
            val msgJson = (args.firstOrNull() as? JSONObject)?.optJSONObject("message") ?: return@on
            messageListener?.invoke(msgJson.toMessage())
        }

        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            onError(args.firstOrNull()?.toString() ?: "Connection error")
        }

        s.connect()
    }

    fun onMessage(listener: (SupportMessage) -> Unit) {
        messageListener = listener
    }

    fun send(text: String, onResult: (Boolean, String?) -> Unit) {
        val s = socket ?: return onResult(false, "Not connected")
        s.emit("support:send", JSONObject().put("text", text), Ack { args ->
            val res = args.firstOrNull() as? JSONObject
            if (res?.optBoolean("ok") == true) onResult(true, null)
            else onResult(false, res?.optString("error") ?: "Send failed")
        })
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
        messageListener = null
    }
}

private fun JSONArray?.toMessages(): List<SupportMessage> {
    if (this == null) return emptyList()
    return List(length()) { optJSONObject(it)?.toMessage() }.filterNotNull()
}

private fun JSONObject.toMessage() = SupportMessage(
    id = optString("id"),
    from = optString("from"),
    text = optString("text"),
    ts = optString("ts")
)
