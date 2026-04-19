package com.betnow.app.util

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object UiFormatters {

    private val currencyFormatter: NumberFormat
        get() = NumberFormat.getCurrencyInstance(Locale.US)

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US)

    fun currency(value: Double): String = currencyFormatter.format(value)

    fun currencyFromString(value: String?): String {
        if (value.isNullOrBlank()) return currency(0.0)
        return value.toDoubleOrNull()?.let(::currency) ?: value
    }

    fun cents(probability: Double): String {
        val centsValue = (probability * 100).roundToInt().coerceIn(0, 100)
        return "${centsValue}c"
    }

    fun sidePrice(side: String, probability: Double): String =
        "${side.uppercase(Locale.US)} ${cents(probability)}"

    fun shares(value: Double): String {
        val pattern = if (value >= 100) "%.1f" else "%.2f"
        return "${String.format(Locale.US, pattern, value)} shares"
    }

    fun dateTime(isoValue: String?): String {
        if (isoValue.isNullOrBlank()) return "Unknown date"
        return try {
            val instant = Instant.parse(isoValue)
            dateFormatter.format(instant.atZone(ZoneId.systemDefault()))
        } catch (_: Exception) {
            isoValue
        }
    }
}
