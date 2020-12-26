package com.icov.app.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class GetCurrentLocation() {

    companion object {
        @SuppressLint("MissingPermission")
        fun getLocation(context: Context): Location? {
            var locationGps: Location? = null
            var locationNetwork: Location? = null


            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            Log.d("Loc", "has GPS $hasGps   hasNetwork $hasNetwork")

            if (hasGps || hasNetwork) {

                if (hasGps) {

                    val locationListener = LocationListener { location ->
                        if (location != null) {
                            locationGps = location
                            Log.d("Loc", "Gps  ${location.latitude}   ${location.longitude}")
                        }
                    }

                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        5000,
                        0F,
                        locationListener
                    )

                    val localGpsLocation: Location? =
                        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (localGpsLocation != null) {
                        locationGps = localGpsLocation
                    }
                }

                if (hasNetwork) {

                    val locationListener = LocationListener { location ->
                        if (location != null) {
                            locationNetwork = location
                            Log.d("Loc", "Net  ${location.latitude}   ${location.longitude}")
                        }
                    }

                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000,
                        0F,
                        locationListener
                    )

                    val localNetworkLocation: Location? =
                        locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (localNetworkLocation != null) {
                        locationNetwork = localNetworkLocation
                    }
                }

                Log.d("Loc", "locGPS $locationGps   locNet $locationNetwork")


                return if (locationGps != null && locationNetwork != null) {
                    if (locationGps!!.accuracy > locationNetwork!!.accuracy) {
                        locationGps!!
                    } else {
                        locationNetwork!!
                    }
                } else {
                    when {
                        locationGps != null -> {
                            locationGps!!
                        }
                        locationNetwork != null -> {
                            locationNetwork!!
                        }
                        else -> {
                            null
                        }
                    }
                }

            } else {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                return null
            }
        }

    }

}