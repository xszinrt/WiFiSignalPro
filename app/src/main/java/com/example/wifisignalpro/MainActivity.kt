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
    private val uiHandler = Handler(Looper.getMainLooper())
    private var isScanning = false

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val SCAN_INTERVAL_MS = 2000L
    }

    private val scanResultsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                updateSignalInfo()
                scheduleNextScan()
            }
        }
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
                    startActiveScanning()
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
            startActiveScanning()
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
                startActiveScanning()
            } else {
                Toast.makeText(this, "Location permission is required for WiFi monitoring", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startActiveScanning() {
        val filter = IntentFilter().apply {
            addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        }
        registerReceiver(scanResultsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        performScan()
        updateSignalInfo()
    }

    private fun performScan() {
        wifiManager?.let { wm ->
            if (!isScanning) {
                try {
                    isScanning = true
                    wm.startScan()
                } catch (e: Exception) {
                    isScanning = false
                    scheduleNextScan()
                }
            }
        } ?: scheduleNextScan()
    }

    private fun scheduleNextScan() {
        isScanning = false
        uiHandler.postDelayed({
            if (!isFinishing && !isDestroyed) {
                performScan()
            }
        }, SCAN_INTERVAL_MS)
    }

    private fun updateSignalInfo() {
        wifiManager?.let { wm ->
            try {
                val wifiInfo = wm.connectionInfo
                val rssi = wifiInfo.rssi
                val level = WifiManager.calculateSignalLevel(rssi, 101)
                val percentage = level.toDouble()

                val (qualityText, color) = when {
                    percentage >= 80 -> "Excellent" to R.color.signal_excellent
                    percentage >= 60 -> "Good" to R.color.signal_good
                    percentage >= 40 -> "Fair" to R.color.signal_fair
                    percentage >= 20 -> "Poor" to R.color.signal_poor
                    else -> "Very Poor" to R.color.signal_very_poor
                }

                tvSignalStrength.text = "$rssi dBm"
                tvSignalStrength.setTextColor(getColor(color))
                tvSignalPercent.text = String.format("%.0f%%", percentage)
                tvSignalQuality.text = qualityText
                tvSignalQuality.setTextColor(getColor(color))
                progressBar.progress = percentage.toInt()
                progressBar.progressTintList = android.content.res.ColorStateList.valueOf(getColor(color))

                // SSID (إزالة علامات الاقتباس)
                var ssid = wifiInfo.ssid
                if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length - 1)  // ✅ length (بدون أقواس)
                }
                tvSSID.text = if (ssid.isNotEmpty()) ssid else "Unknown"

                tvBSSID.text = wifiInfo.bssid ?: "Unknown"

                val frequency = wifiInfo.frequency
                tvFrequency.text = when {
                    frequency > 4900 -> "5 GHz ($frequency MHz)"
                    frequency > 0 -> "2.4 GHz ($frequency MHz)"
                    else -> "Unknown"
                }

                tvLinkSpeed.text = "${wifiInfo.linkSpeed} Mbps"

            } catch (e: Exception) {
                setDisconnectedState()
            }
        } ?: setDisconnectedState()
    }

    private fun setDisconnectedState() {
        tvSignalStrength.text = "Not Connected"
        tvSignalPercent.text = "0%"
        tvSignalQuality.text = "No Connection"
        tvSSID.text = "Not connected"
        tvBSSID.text = "---"
        tvFrequency.text = "---"
        tvLinkSpeed.text = "---"
        progressBar.progress = 0
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(scanResultsReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver wasn't registered
        }
        uiHandler.removeCallbacksAndMessages(null)
    }
}
