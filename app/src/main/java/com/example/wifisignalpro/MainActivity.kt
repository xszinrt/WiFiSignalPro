package com.example.wifisignalpro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvSignalStrength: TextView
    private lateinit var tvSignalPercent: TextView
    private lateinit var tvSignalQuality: TextView
    private lateinit var tvSSID: TextView
    private lateinit var tvBSSID: TextView
    private lateinit var tvFrequency: TextView
    private lateinit var tvLinkSpeed: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var viewModel: SignalViewModel

    // طلب إذن الموقع
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startMonitoring()
        } else {
            Toast.makeText(this, "Location permission is required to read WiFi signal", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        
        // إنشاء ViewModel
        viewModel = SignalViewModel(application)
        
        checkPermissionAndStart()
        
        // جمع البيانات من ViewModel وتحديث الواجهة
        lifecycleScope.launch {
            viewModel.rssi.collectLatest { rssi ->
                updateSignalDisplay(rssi)
            }
        }
        
        lifecycleScope.launch {
            viewModel.ssid.collectLatest { ssid ->
                tvSSID.text = if (ssid.isNotEmpty()) ssid else "Not connected"
            }
        }
        
        lifecycleScope.launch {
            viewModel.linkSpeed.collectLatest { speed ->
                tvLinkSpeed.text = if (speed > 0) "$speed Mbps" else "--"
            }
        }
        
        lifecycleScope.launch {
            viewModel.frequency.collectLatest { freq ->
                tvFrequency.text = when {
                    freq > 4900 -> "5 GHz ($freq MHz)"
                    freq > 0 -> "2.4 GHz ($freq MHz)"
                    else -> "--"
                }
            }
        }
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
    
    private fun checkPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED -> {
                    startMonitoring()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        } else {
            startMonitoring()
        }
    }
    
    private fun startMonitoring() {
        viewModel.startMonitoring()
    }
    
    private fun updateSignalDisplay(rssi: Int) {
        // حساب النسبة المئوية (dBm يتراوح بين -100 و -50)
        val percentage = when {
            rssi >= -50 -> 100
            rssi <= -100 -> 0
            else -> ((rssi + 100) * 100 / 50).coerceIn(0, 100)
        }
        
        // تحديد اللون والجودة
        val (colorRes, quality) = when {
            rssi >= -50 -> R.color.signal_excellent to "Excellent"
            rssi >= -60 -> R.color.signal_good to "Good"
            rssi >= -70 -> R.color.signal_fair to "Fair"
            rssi >= -80 -> R.color.signal_poor to "Poor"
            else -> R.color.signal_very_poor to "Very Poor"
        }
        
        val color = getColor(colorRes)
        
        tvSignalStrength.text = "$rssi dBm"
        tvSignalStrength.setTextColor(color)
        tvSignalPercent.text = "$percentage%"
        tvSignalQuality.text = quality
        tvSignalQuality.setTextColor(color)
        progressBar.progress = percentage
        progressBar.progressTintList = android.content.res.ColorStateList.valueOf(color)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopMonitoring()
    }
}
