package com.example.domain.location

import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission
import com.example.domain.repository.LocationResult
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

import android.location.Geocoder
import java.util.Locale

class GpsLocationTracker(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private fun getCityName(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                address.locality ?: address.subAdminArea ?: address.adminArea ?: "Current Location"
            } else {
                "Current Location"
            }
        } catch (e: Exception) {
            "Current Location"
        }
    }

    @RequiresPermission(
        anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"]
    )
    suspend fun getCurrentLocation(): LocationResult? = suspendCancellableCoroutine { continuation ->
        try {
            fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        continuation.resume(LocationResult(getCityName(location.latitude, location.longitude), location.latitude, location.longitude))
                    } else {
                        // Fallback to last location
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc -> 
                            if (lastLoc != null) {
                                continuation.resume(LocationResult(getCityName(lastLoc.latitude, lastLoc.longitude), lastLoc.latitude, lastLoc.longitude))
                            } else {
                                continuation.resume(null)
                            }
                        }.addOnFailureListener {
                            continuation.resume(null)
                        }
                    }
                }.addOnFailureListener {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc -> 
                        if (lastLoc != null) {
                            continuation.resume(LocationResult(getCityName(lastLoc.latitude, lastLoc.longitude), lastLoc.latitude, lastLoc.longitude))
                        } else {
                            continuation.resume(null)
                        }
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }
                }
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }
}
