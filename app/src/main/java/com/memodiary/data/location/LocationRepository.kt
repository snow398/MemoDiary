package com.memodiary.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

class LocationRepository(private val context: Context) {

    // Lazy init: some devices throw if GMS is unavailable
    private val fusedClient by lazy {
        try { LocationServices.getFusedLocationProviderClient(context) } catch (_: Exception) { null }
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationInfo? {
        if (!hasLocationPermission()) return null

        // 1. FusedClient: try last known location first (instant, no GPS warm-up)
        fusedClient?.let { client ->
            val last = withTimeoutOrNull(3_000) {
                suspendCancellableCoroutine<android.location.Location?> { cont ->
                    client.lastLocation
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resume(null) }
                }
            }
            if (last != null) return LocationInfo(last.latitude, last.longitude)
        }

        // 2. FusedClient: request fresh location with timeout
        fusedClient?.let { client ->
            val fresh = withTimeoutOrNull(10_000) {
                suspendCancellableCoroutine<android.location.Location?> { cont ->
                    val cts = CancellationTokenSource()
                    client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resume(null) }
                    cont.invokeOnCancellation { cts.cancel() }
                }
            }
            if (fresh != null) return LocationInfo(fresh.latitude, fresh.longitude)
        }

        // 3. Fallback: Android LocationManager (works without GMS)
        return getLocationViaManager()
    }

    @SuppressLint("MissingPermission")
    private fun getLocationViaManager(): LocationInfo? {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER
            ).filter { lm.isProviderEnabled(it) }
            for (provider in providers) {
                val loc = lm.getLastKnownLocation(provider)
                if (loc != null) return LocationInfo(loc.latitude, loc.longitude)
            }
            null
        } catch (_: Exception) { null }
    }

    suspend fun geocode(location: LocationInfo): LocationInfo {
        return withContext(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) {
                    // No geocoding backend: show raw coordinates as address
                    return@withContext location.copy(
                        address = "%.5f, %.5f".format(location.latitude, location.longitude)
                    )
                }
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    location.copy(
                        country = addr.countryName,
                        province = addr.adminArea,
                        city = addr.locality ?: addr.subAdminArea,
                        address = addr.getAddressLine(0)
                    )
                } else {
                    location.copy(address = "%.5f, %.5f".format(location.latitude, location.longitude))
                }
            } catch (_: Exception) {
                location.copy(address = "%.5f, %.5f".format(location.latitude, location.longitude))
            }
        }
    }

    suspend fun geocodeAddress(addressStr: String): LocationInfo? {
        return withContext(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) return@withContext null
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(addressStr, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    LocationInfo(
                        latitude = addr.latitude,
                        longitude = addr.longitude,
                        country = addr.countryName,
                        province = addr.adminArea,
                        city = addr.locality ?: addr.subAdminArea,
                        address = addr.getAddressLine(0)
                    )
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}
