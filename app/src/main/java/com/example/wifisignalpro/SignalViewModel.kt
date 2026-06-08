package com.example.wifisignalpro

import android.app.Application
import android.net.wifi.WifiManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignalViewModel(application: Application) : AndroidViewModel(application) {
    
    private val wifiManager: WifiManager = application.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager
    
    private val _rssi = MutableStateFlow(-100)
    val rssi: StateFlow<Int> = _rssi.asStateFlow()
    
    private val _ssid = MutableStateFlow("")
    val ssid: StateFlow<String> = _ssid.asStateFlow()
    
    private val _linkSpeed = MutableStateFlow(0)
    val linkSpeed: StateFlow<Int> = _linkSpeed.asStateFlow()
    
    private val _frequency = MutableStateFlow(0)
    val frequency: StateFlow<Int> = _frequency.asStateFlow()
    
    private var isMonitoring = false
    
    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        
        viewModelScope.launch {
            while (isMonitoring) {
                try {
                    val wifiInfo = wifiManager.connectionInfo
                    _rssi.value = wifiInfo.rssi
                    _ssid.value = wifiInfo.ssid?.trim('"') ?: "Unknown"
                    _linkSpeed.value = wifiInfo.linkSpeed
                    _frequency.value = wifiInfo.frequency
                } catch (e: Exception) {
                    // إذا فشلت القراءة، نحتفظ بالقيم السابقة
                }
                delay(1000) // تحديث كل ثانية
            }
        }
    }
    
    fun stopMonitoring() {
        isMonitoring = false
    }
    
    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}
