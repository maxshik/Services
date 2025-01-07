package com.example.services

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var speedTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var startNotificationButton: Button
    private lateinit var urlInput: EditText
    private lateinit var downloadButton: Button
    private var downloadService: DownloadService? = null
    private var isBound = false

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val speed = intent?.getFloatExtra("speed", 0f) ?: 0f
            val latitude = intent?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val longitude = intent?.getDoubleExtra("longitude", 0.0) ?: 0.0
            val city = intent?.getStringExtra("city")

            speedTextView.text = "Скорость: $speed м/с"
            locationTextView.text = "Местоположение: ($latitude, $longitude) \nГород: $city"
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as DownloadService.LocalBinder
            downloadService = binder.getService()
            Toast.makeText(this@MainActivity, "Сервис подключен", Toast.LENGTH_SHORT).show()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            downloadService = null
            Toast.makeText(this@MainActivity, "Сервис отключен", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        speedTextView = findViewById(R.id.speedTextView)
        locationTextView = findViewById(R.id.locationTextView)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }

        registerReceiver(broadcastReceiver, IntentFilter("SpeedServiceUpdates"))

        startButton.setOnClickListener {
            startService(Intent(this, SpeedService::class.java))
            Toast.makeText(this, "Сервис запущен", Toast.LENGTH_SHORT).show()
        }

        stopButton.setOnClickListener {
            stopService(Intent(this, SpeedService::class.java))
            Toast.makeText(this, "Сервис остановлен", Toast.LENGTH_SHORT).show()
        }

        startNotificationButton = findViewById(R.id.startNotificationButton)

        startNotificationButton.setOnClickListener {
            startService(Intent(this, NotificationService::class.java))
            Toast.makeText(this, "Уведомление будет показано через 3 секунд", Toast.LENGTH_SHORT).show()
        }

        urlInput = findViewById(R.id.urlInput)
        urlInput.setText("https://ru.files.me/u/gtdw9f6p7u")
        downloadButton = findViewById(R.id.downloadButton)

        downloadButton.setOnClickListener {
            val fileUrl = urlInput.text.toString()
            if (fileUrl.isNotEmpty() && downloadService != null) {
                downloadService?.downloadFile(fileUrl)
            } else {
                Toast.makeText(this, "Введите URL файла или подключите сервис", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, DownloadService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver) // Отменяем регистрацию Receiver
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "Разрешение на доступ к местоположению не предоставлено", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
