package com.betnow.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

// Mirrors docs.polymarket.com/api-reference/geoblock. ISO 3166-1 alpha-2.
object GeoRestrictionManager {

    private val FULLY_BLOCKED = setOf(
        "AU", "BE", "BY", "BI", "CF", "CD", "CU", "DE", "ET", "FR",
        "GB", "IR", "IQ", "IT", "KP", "LB", "LY", "MM", "NI", "NL",
        "RU", "SO", "SS", "SD", "SY", "UM", "US", "VE", "YE", "ZW"
    )
    private val CLOSE_ONLY = setOf("PL", "SG", "TH", "TW")

    private const val PREFS = "betnow_geo"
    private const val KEY_SIMULATED = "simulated_country"
    private const val KEY_LAST = "last_country"
    private const val LOCATION_TIMEOUT_MS = 5_000L
    private const val LAST_KNOWN_MAX_AGE_MS = 30_000L

    sealed class Status {
        data class Allowed(val countryCode: String) : Status()
        data class Blocked(val countryCode: String, val closeOnly: Boolean) : Status()
        object PermissionMissing : Status()
        object LocationUnavailable : Status()
    }

    fun setSimulatedCountry(context: Context, code: String?) = prefs(context).edit {
        if (code.isNullOrBlank()) remove(KEY_SIMULATED)
        else putString(KEY_SIMULATED, code.uppercase(Locale.US))
    }

    fun getLastKnownCountry(context: Context): String? =
        prefs(context).getString(KEY_LAST, null)

    suspend fun check(context: Context): Status {
        prefs(context).getString(KEY_SIMULATED, null)?.takeIf { it.isNotBlank() }?.let {
            return classify(it)
        }
        if (!hasLocationPermission(context)) return Status.PermissionMissing

        val location = withTimeoutOrNull(LOCATION_TIMEOUT_MS) { requestLocation(context) }
            ?: return Status.LocationUnavailable

        val code = withContext(Dispatchers.IO) { resolveCountry(context, location) }
            ?: return Status.LocationUnavailable

        prefs(context).edit { putString(KEY_LAST, code) }
        return classify(code)
    }

    fun classify(code: String): Status {
        val upper = code.uppercase(Locale.US)
        return when (upper) {
            in FULLY_BLOCKED -> Status.Blocked(upper, closeOnly = false)
            in CLOSE_ONLY -> Status.Blocked(upper, closeOnly = true)
            else -> Status.Allowed(upper)
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun hasLocationPermission(context: Context): Boolean =
        listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    @Suppress("MissingPermission")
    private suspend fun requestLocation(context: Context): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).filter { lm.isProviderEnabled(it) }

        val now = System.currentTimeMillis()
        providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .filter { now - it.time <= LAST_KNOWN_MAX_AGE_MS }
            .maxByOrNull { it.time }
            ?.let { return it }

        val provider = providers.firstOrNull() ?: return null
        return suspendCancellableCoroutine { cont ->
            val listener = android.location.LocationListener { if (cont.isActive) cont.resume(it) }
            try {
                lm.requestSingleUpdate(provider, listener, context.mainLooper)
                cont.invokeOnCancellation { lm.removeUpdates(listener) }
            } catch (_: SecurityException) {
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    private fun resolveCountry(context: Context, location: Location): String? = try {
        @Suppress("DEPRECATION")
        Geocoder(context, Locale.US)
            .getFromLocation(location.latitude, location.longitude, 1)
            ?.firstOrNull()
            ?.countryCode
    } catch (_: Exception) {
        null
    }
}
