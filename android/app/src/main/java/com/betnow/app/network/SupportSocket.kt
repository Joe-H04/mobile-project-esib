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
        val options = IO.Options().apply {
            transports = arrayOf("polling", "websocket")
            reconnection = true
            forceNew = true
        }
        val s = IO.socket(serverUrl, options)
        socket = s

        s.on(Socket.EVENT_CONNECT) {
            val payload = JSONObject().put("token", token)
            s.emit("support:auth", payload, Ack { args ->
                val res = args.firstOrNull() as? JSONObject
                if (res != null && res.optBoolean("ok")) {
                    val history = parseMessages(res.optJSONArray("messages"))
                    onReady(history)
                } else {
                    onError(res?.optString("error") ?: "Auth failed")
                }
            })
        }

        s.on("support:message") { args ->
            val event = args.firstOrNull() as? JSONObject ?: return@on
            val msgJson = event.optJSONObject("message") ?: return@on
            messageListener?.invoke(parseMessage(msgJson))
        }

        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val err = args.firstOrNull()?.toString() ?: "Connection error"
            onError(err)
        }

        s.connect()
    }

    fun onMessage(listener: (SupportMessage) -> Unit) {
        messageListener = listener
    }

    fun send(text: String, onResult: (Boolean, String?) -> Unit) {
        val s = socket ?: return onResult(false, "Not connected")
        val payload = JSONObject().put("text", text)
        s.emit("support:send", payload, Ack { args ->
            val res = args.firstOrNull() as? JSONObject
            if (res != null && res.optBoolean("ok")) onResult(true, null)
            else onResult(false, res?.optString("error") ?: "Send failed")
        })
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
        messageListener = null
    }

    private fun parseMessages(array: JSONArray?): List<SupportMessage> {
        if (array == null) return emptyList()
        val out = mutableListOf<SupportMessage>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            out.add(parseMessage(obj))
        }
        return out
    }

    private fun parseMessage(obj: JSONObject): SupportMessage = SupportMessage(
        id = obj.optString("id"),
        from = obj.optString("from"),
        text = obj.optString("text"),
        ts = obj.optString("ts")
    )
}
