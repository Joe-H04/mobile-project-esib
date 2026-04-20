package com.betnow.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

object GeoRestrictionManager {

    // Polymarket-equivalent blocklist (docs.polymarket.com/api-reference/geoblock).
    // ISO 3166-1 alpha-2 codes.
    private val FULLY_BLOCKED = setOf(
        "AU", "BE", "BY", "BI", "CF", "CD", "CU", "DE", "ET", "FR",
        "GB", "IR", "IQ", "IT", "KP", "LB", "LY", "MM", "NI", "NL",
        "RU", "SO", "SS", "SD", "SY", "UM", "US", "VE", "YE", "ZW"
    )

    // "Close-only" — treated as restricted for this simulation (no new bets).
    private val CLOSE_ONLY = setOf("PL", "SG", "TH", "TW")

    private const val PREF_NAME = "betnow_geo"
    private const val KEY_SIMULATED_COUNTRY = "simulated_country"
    private const val KEY_LAST_COUNTRY = "last_country"

    sealed class Status {
        data class Allowed(val countryCode: String) : Status()
        data class Blocked(val countryCode: String, val closeOnly: Boolean) : Status()
        object PermissionMissing : Status()
        object LocationUnavailable : Status()
    }

    fun setSimulatedCountry(context: Context, code: String?) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (code.isNullOrBlank()) {
            prefs.edit().remove(KEY_SIMULATED_COUNTRY).apply()
        } else {
            prefs.edit().putString(KEY_SIMULATED_COUNTRY, code.uppercase(Locale.US)).apply()
        }
    }

    fun getLastKnownCountry(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_COUNTRY, null)

    suspend fun check(context: Context): Status {
        val simulated = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SIMULATED_COUNTRY, null)
        if (!simulated.isNullOrBlank()) {
            return classify(simulated)
        }

        if (!hasLocationPermission(context)) return Status.PermissionMissing

        val location = withTimeoutOrNull(5_000) { requestLocation(context) }
            ?: return Status.LocationUnavailable

        val code = withContext(Dispatchers.IO) {
            resolveCountry(context, location)
        } ?: return Status.LocationUnavailable

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST_COUNTRY, code).apply()

        return classify(code)
    }

    fun classify(code: String): Status {
        val upper = code.uppercase(Locale.US)
        return when {
            FULLY_BLOCKED.contains(upper) -> Status.Blocked(upper, closeOnly = false)
            CLOSE_ONLY.contains(upper) -> Status.Blocked(upper, closeOnly = true)
            else -> Status.Allowed(upper)
        }
    }

    private fun hasLocationPermission(context: Context): Boolean {
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    private const val LAST_KNOWN_MAX_AGE_MS = 30_000L

    @SuppressWarnings("MissingPermission")
    private suspend fun requestLocation(context: Context): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).filter { lm.isProviderEnabled(it) }

        val now = System.currentTimeMillis()
        var freshest: Location? = null
        for (provider in providers) {
            try {
                @Suppress("MissingPermission")
                val last = lm.getLastKnownLocation(provider) ?: continue
                if (now - last.time <= LAST_KNOWN_MAX_AGE_MS &&
                    (freshest == null || last.time > freshest.time)
                ) {
                    freshest = last
                }
            } catch (_: SecurityException) {
                return null
            }
        }
        if (freshest != null) return freshest

        val activeProvider = providers.firstOrNull() ?: return null
        return suspendCancellableCoroutine { cont ->
            val listener = android.location.LocationListener { location ->
                if (cont.isActive) cont.resume(location)
            }
            try {
                @Suppress("MissingPermission")
                lm.requestSingleUpdate(activeProvider, listener, context.mainLooper)
                cont.invokeOnCancellation { lm.removeUpdates(listener) }
            } catch (_: SecurityException) {
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    private fun resolveCountry(context: Context, location: Location): String? {
        return try {
            val geocoder = Geocoder(context, Locale.US)
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            results?.firstOrNull()?.countryCode
        } catch (_: Exception) {
            null
        }
    }
}
