package com.yperez.turnoya.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class LatLng(val lat: Double, val lng: Double)

/** Bogotá como punto de partida cuando no hay permiso o GPS. */
val DEFAULT_LOCATION = LatLng(4.6533, -74.0836)

fun hasLocationPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** Ubicación actual usando el LocationManager de Android (sin servicios de Google). */
@SuppressLint("MissingPermission")
suspend fun currentLocation(context: Context): LatLng? {
    if (!hasLocationPermission(context)) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = lm.getProviders(true)
    val last = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
    if (last != null && System.currentTimeMillis() - last.time < 5 * 60_000) return LatLng(last.latitude, last.longitude)
    val provider = when {
        LocationManager.NETWORK_PROVIDER in providers -> LocationManager.NETWORK_PROVIDER
        LocationManager.GPS_PROVIDER in providers -> LocationManager.GPS_PROVIDER
        else -> return last?.let { LatLng(it.latitude, it.longitude) }
    }
    val fresh = withTimeoutOrNull(8_000) {
        suspendCancellableCoroutine<Location?> { cont ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    lm.removeUpdates(this)
                    if (cont.isActive) cont.resume(location)
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            cont.invokeOnCancellation { lm.removeUpdates(listener) }
        }
    }
    val best = fresh ?: last
    return best?.let { LatLng(it.latitude, it.longitude) }
}
