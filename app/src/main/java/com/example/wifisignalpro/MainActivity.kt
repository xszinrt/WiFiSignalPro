package com.example.wifisignalpro

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvSignalStrength: TextView
    private lateinit var tvSignalPercent: TextView
    private lateinit var tvSignalQuality: TextView
    private lateinit var tvSSID: TextView
    private lateinit var tvBSSID: TextView
    private lateinit var tvFrequency: TextView
    private lateinit var tvLinkSpeed: TextView
    private lateinit var progressBar: ProgressBar

    private var wifiManager: WifiManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isUpdating = false
    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val UPDATE_INTERVAL_MS = 1000L // 1 second
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        
        checkPermissions()
    }

    private fun initViews() {
        tvSignalStrength = findViewById(R.id.tvSignalStrength)
        tvSignalPercent = findViewById(R.id.tvSignalPercent)
        tvSignalQuality = findViewById(R.id.tvSignalQuality)
        tvSSID = findViewById(R.id.tvSSID)
        tvBSSID = findViewById(R.id.tvBSSID)
        tvFrequency = findViewById(R.id.tvFrequency)
        tvLinkSpeed = findViewById(R.id.tvLinkSpeed)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED -> {
                    startUpdating()
                }
                else -> {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        PERMISSION_REQUEST_CODE
                    )
                }
            }
        } else {
            startUpdating()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startUpdating()
            } else {
                Toast.makeText(this, "Location permission required for WiFi signal", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startUpdating() {
        if (isUpdating) return
        isUpdating = true
        
        scope.launch {
            while (isUpdating && !isFinishing && !isDestroyed) {
                updateSignalInfo()
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun updateSignalInfo() {
        wifiManager?.let { wm ->
            try {
                val wifiInfo = wm.connectionInfo
                val rssi = wifiInfo.rssi
                
                // حساب النسبة المئوية (dBm يتراوح بين -100 و -50 تقريباً)
                val percentage = when {
                    rssi >= -50 -> 100.0
                    rssi <= -100 -> 0.0
                    else -> ((rssi + 100) * 100.0 / 50.0).coerceIn(0.0, 100.0)
                }
                
                val (qualityText, colorRes) = when {
                    percentage >= 80 -> "Excellent" to R.color.signal_excellent
                    percentage >= 60 -> "Good" to R.color.signal_good
                    percentage >= 40 -> "Fair" to R.color.signal_fair
                    percentage >= 20 -> "Poor" to R.color.signal_poor
                    else -> "Very Poor" to R.color.signal_very_poor
                }
                
                val color = getColor(colorRes)
                
                tvSignalStrength.text = "$rssi dBm"
                tvSignalStrength.setTextColor(color)
                tvSignalPercent.text = "${percentage.toInt()}%"
                tvSignalQuality.text = qualityText
                tvSignalQuality.setTextColor(color)
                progressBar.progress = percentage.toInt()
                progressBar.progressTintList = android.content.res.ColorStateList.valueOf(color)
                
                // SSID
                var ssid = wifiInfo.ssid
                if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length - 1)
                }
                tvSSID.text = if (ssid.isNotEmpty() && ssid != "<unknown ssid>") ssid else "Not connected"
                
                // BSSID
                tvBSSID.text = wifiInfo.bssid ?: "---"
                
                // Frequency
                val frequency = wifiInfo.frequency
                tvFrequency.text = when {
                    frequency > 4900 -> "5 GHz ($frequency MHz)"
                    frequency > 0 -> "2.4 GHz ($frequency MHz)"
                    else -> "Unknown"
                }
                
                // Link Speed
                tvLinkSpeed.text = "${wifiInfo.linkSpeed} Mbps"
                
            } catch (e: Exception) {
                // خطأ في القراءة
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isUpdating = false
        scope.cancel()
    }
}
