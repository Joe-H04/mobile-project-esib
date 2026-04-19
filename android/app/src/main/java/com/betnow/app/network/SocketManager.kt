package com.betnow.app.network

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

object SocketManager {

    private const val SERVER_URL = "http://10.0.2.2:3000"
    private var socket: Socket? = null

    fun connect() {
        if (socket == null) {
            val options = IO.Options().apply {
                transports = arrayOf("websocket")
                reconnection = true
            }
            socket = IO.socket(SERVER_URL, options)
        }
        socket?.connect()
    }

    fun disconnect() {
        socket?.disconnect()
    }

    fun onOddsUpdate(callback: (marketId: String, prices: List<Double>) -> Unit) {
        socket?.on("odds-update") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val marketId = data.getString("marketId")
                val pricesArray = data.getJSONArray("outcomePrices")
                val prices = mutableListOf<Double>()
                for (i in 0 until pricesArray.length()) {
                    prices.add(pricesArray.getDouble(i))
                }
                callback(marketId, prices)
            }
        }
    }

    fun removeOddsUpdateListener() {
        socket?.off("odds-update")
    }

    fun onMarketResolved(callback: (marketId: String, winningOutcome: String) -> Unit) {
        socket?.on("market-resolved") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val marketId = data.getString("marketId")
                val winningOutcome = data.optString("winningOutcome", "")
                callback(marketId, winningOutcome)
            }
        }
    }

    fun removeMarketResolvedListener() {
        socket?.off("market-resolved")
    }
}
