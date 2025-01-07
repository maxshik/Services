package com.example.services

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.Locale

class SpeedService : Service() {

    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val speed = location.speed
                val latitude = location.latitude
                val longitude = location.longitude

                val geocoder = Geocoder(this@SpeedService, Locale.getDefault())
                val addresses: List<Address>?

                try {
                    addresses = geocoder.getFromLocation(latitude, longitude, 1)
                } catch (e: IOException) {
                    Log.e("SpeedService", "Geocoder service not available", e)
                    return
                }

                var cityName = "Неизвестно"
                if (addresses != null && addresses.isNotEmpty()) {
                    cityName = addresses[0].locality ?: "Неизвестно"
                }

                Log.d("SpeedService", "Текущая скорость: $speed м/с, Город: $cityName")

                val intent = Intent("SpeedServiceUpdates")
                intent.putExtra("speed", speed)
                intent.putExtra("latitude", latitude)
                intent.putExtra("longitude", longitude)
                intent.putExtra("city", cityName)
                sendBroadcast(intent)
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1f, locationListener)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(locationListener)
    }
}