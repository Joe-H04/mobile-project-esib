package com.betnow.app.network

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONObject

object SocketManager {

    private const val SERVER_URL = "http://10.0.2.2:3000"
    private var socket: Socket? = null

    fun connect() {
        val s = socket ?: IO.socket(SERVER_URL, IO.Options().apply {
            transports = arrayOf("websocket")
            reconnection = true
        }).also { socket = it }
        s.connect()
    }

    fun disconnect() {
        socket?.disconnect()
    }

    fun onOddsUpdate(callback: (marketId: String, prices: List<Double>) -> Unit) {
        socket?.on("odds-update") { args ->
            val data = args.firstOrNull() as? JSONObject ?: return@on
            callback(data.getString("marketId"), data.getJSONArray("outcomePrices").toDoubleList())
        }
    }

    fun onMarketResolved(callback: (marketId: String, winningOutcome: String) -> Unit) {
        socket?.on("market-resolved") { args ->
            val data = args.firstOrNull() as? JSONObject ?: return@on
            callback(data.getString("marketId"), data.optString("winningOutcome", ""))
        }
    }

    fun off(event: String) {
        socket?.off(event)
    }
}

private fun JSONArray.toDoubleList(): List<Double> =
    List(length()) { getDouble(it) }
