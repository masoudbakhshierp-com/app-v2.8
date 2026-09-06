package com.example.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RealGpsManager(private val context: Context) {

    private val locationManager: LocationManager? by lazy {
        try {
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        } catch (e: Exception) {
            null
        }
    }

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    private val _isGpsActive = MutableStateFlow(false)
    val isGpsActive: StateFlow<Boolean> = _isGpsActive

    private var onLocationReceived: ((lat: Double, lng: Double, speedKmh: Float) -> Unit)? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _currentLocation.value = location
            val speedKmh = location.speed * 3.6f
            onLocationReceived?.invoke(location.latitude, location.longitude, speedKmh)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    fun setLocationCallback(callback: (lat: Double, lng: Double, speedKmh: Float) -> Unit) {
        this.onLocationReceived = callback
    }

    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocation || coarseLocation
    }

    @SuppressLint("MissingPermission")
    fun startTracking(): Boolean {
        if (!hasLocationPermission()) {
            Log.d("RealGpsManager", "Location permission is not yet granted. Waiting for user approval.")
            _isGpsActive.value = false
            return false
        }

        try {
            val lm = locationManager ?: return false
            val isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                _isGpsActive.value = false
                return false
            }

            if (isGpsEnabled) {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L, // 5 seconds interval for real updates
                    5f,   // 5 meters min distance
                    locationListener
                )
            }

            if (isNetworkEnabled) {
                lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000L,
                    5f,
                    locationListener
                )
            }

            // Get last known location immediately
            val lastGps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNetwork = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLocation = lastGps ?: lastNetwork
            if (bestLocation != null) {
                _currentLocation.value = bestLocation
                val speedKmh = bestLocation.speed * 3.6f
                onLocationReceived?.invoke(bestLocation.latitude, bestLocation.longitude, speedKmh)
            }

            _isGpsActive.value = true
            return true
        } catch (e: SecurityException) {
            Log.d("RealGpsManager", "Location permission missing or revoked: ${e.message}")
            _isGpsActive.value = false
            return false
        } catch (e: Exception) {
            Log.w("RealGpsManager", "Error starting location tracking: ${e.message}")
            _isGpsActive.value = false
            return false
        }
    }

    fun stopTracking() {
        try {
            locationManager?.removeUpdates(locationListener)
        } catch (e: Exception) {
            Log.w("RealGpsManager", "Error stopping location tracking: ${e.message}")
        } finally {
            _isGpsActive.value = false
        }
    }
}
