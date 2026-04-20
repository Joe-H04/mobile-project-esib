package com.betnow.app.network.models

data class SupportMessage(
    val id: String,
    val from: String,
    val text: String,
    val ts: String
)

data class SupportHistoryResponse(
    val messages: List<SupportMessage>
)
