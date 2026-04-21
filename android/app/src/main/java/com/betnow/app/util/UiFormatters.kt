package com.betnow.app.util

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object UiFormatters {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private val dateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US)

    fun currency(value: Double): String = currencyFormat.format(value)

    fun currencyFromString(value: String?): String =
        value?.toDoubleOrNull()?.let(::currency) ?: value ?: currency(0.0)

    fun cents(probability: Double): String =
        "${(probability * 100).roundToInt().coerceIn(0, 100)}c"

    fun sidePrice(side: String, probability: Double): String =
        "${side.uppercase(Locale.US)} ${cents(probability)}"

    fun shares(value: Double): String {
        val pattern = if (value >= 100) "%.1f" else "%.2f"
        return "${String.format(Locale.US, pattern, value)} shares"
    }

    fun dateTime(iso: String?): String {
        if (iso.isNullOrBlank()) return "Unknown date"
        return try {
            dateFormat.format(Instant.parse(iso).atZone(ZoneId.systemDefault()))
        } catch (_: Exception) {
            iso
        }
    }
}
