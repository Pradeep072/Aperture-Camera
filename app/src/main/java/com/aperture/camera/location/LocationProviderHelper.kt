package com.aperture.camera.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log

class LocationProviderHelper(private val context: Context) : LocationListener {

    private val tag = "LocationProviderHelper"
    private val locationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    @Volatile
    private var lastKnownLocation: Location? = null
    private var isTracking = false

    /**
     * Starts listening for location updates from GPS and Network providers.
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val lm = locationManager ?: return
        if (isTracking) return

        try {
            // Retrieve cached location from available providers
            val gpsLoc = try { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
            val netLoc = try { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
            val passLoc = try { lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } catch (_: Exception) { null }

            lastKnownLocation = getBestLocation(gpsLoc, getBestLocation(netLoc, passLoc))

            // Register for active updates
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L, // 5 seconds
                    5f,    // 5 meters
                    this,
                    Looper.getMainLooper()
                )
            }

            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000L,
                    10f,
                    this,
                    Looper.getMainLooper()
                )
            }

            isTracking = true
            Log.d(tag, "Location updates started. Initial location: $lastKnownLocation")
        } catch (e: SecurityException) {
            Log.w(tag, "Location permission not granted: ${e.message}")
        } catch (e: Exception) {
            Log.e(tag, "Error starting location updates: ${e.message}")
        }
    }

    /**
     * Stops location listening to save battery when camera is closed.
     */
    fun stopLocationUpdates() {
        if (!isTracking) return
        try {
            locationManager?.removeUpdates(this)
            isTracking = false
            Log.d(tag, "Location updates stopped")
        } catch (e: Exception) {
            Log.e(tag, "Error stopping location updates: ${e.message}")
        }
    }

    /**
     * Gets the latest, most accurate location available.
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(): Location? {
        val lm = locationManager ?: return lastKnownLocation
        try {
            val gpsLoc = try { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
            val netLoc = try { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
            val freshBest = getBestLocation(gpsLoc, netLoc)
            if (freshBest != null) {
                lastKnownLocation = getBestLocation(lastKnownLocation, freshBest)
            }
        } catch (_: Exception) { }
        return lastKnownLocation
    }

    override fun onLocationChanged(location: Location) {
        if (isBetterLocation(location, lastKnownLocation)) {
            lastKnownLocation = location
            Log.d(tag, "Updated location: ${location.latitude}, ${location.longitude} (Accuracy: ${location.accuracy}m)")
        }
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    private fun getBestLocation(loc1: Location?, loc2: Location?): Location? {
        if (loc1 == null) return loc2
        if (loc2 == null) return loc1
        return if (isBetterLocation(loc1, loc2)) loc1 else loc2
    }

    private fun isBetterLocation(location: Location, currentBestLocation: Location?): Boolean {
        if (currentBestLocation == null) return true

        val timeDelta = location.time - currentBestLocation.time
        val isSignificantlyNewer = timeDelta > 60000L
        val isSignificantlyOlder = timeDelta < -60000L
        val isNewer = timeDelta > 0

        if (isSignificantlyNewer) return true
        if (isSignificantlyOlder) return false

        val accuracyDelta = (location.accuracy - currentBestLocation.accuracy).toInt()
        val isLessAccurate = accuracyDelta > 0
        val isMoreAccurate = accuracyDelta < 0
        val isSignificantlyLessAccurate = accuracyDelta > 200

        val isSameProvider = location.provider == currentBestLocation.provider

        return when {
            isMoreAccurate -> true
            isNewer && !isLessAccurate -> true
            isNewer && !isSignificantlyLessAccurate && isSameProvider -> true
            else -> false
        }
    }
}
