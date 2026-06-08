package com.example.wifisignalpro;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private TextView tvSignalStrength, tvSignalPercent, tvSignalQuality;
    private TextView tvSSID, tvBSSID, tvFrequency, tvLinkSpeed;
    private ProgressBar progressBar;
    private WifiManager wifiManager;
    
    private static final int PERMISSION_REQUEST_CODE = 100;

    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.RSSI_CHANGED_ACTION.equals(action) || 
                WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action) ||
                WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                updateSignalInfo();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        tvSignalStrength = findViewById(R.id.tvSignalStrength);
        tvSignalPercent = findViewById(R.id.tvSignalPercent);
        tvSignalQuality = findViewById(R.id.tvSignalQuality);
        tvSSID = findViewById(R.id.tvSSID);
        tvBSSID = findViewById(R.id.tvBSSID);
        tvFrequency = findViewById(R.id.tvFrequency);
        tvLinkSpeed = findViewById(R.id.tvLinkSpeed);
        progressBar = findViewById(R.id.progressBar);
        
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        
        checkPermissions();
    }
    
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                    PERMISSION_REQUEST_CODE);
            } else {
                registerWifiReceiver();
                updateSignalInfo();
            }
        } else {
            registerWifiReceiver();
            updateSignalInfo();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                registerWifiReceiver();
                updateSignalInfo();
            } else {
                Toast.makeText(this, "Location permission is required for WiFi signal monitoring", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void registerWifiReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(wifiReceiver, filter);
    }
    
    private void updateSignalInfo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            
            if (wifiInfo != null && wifiInfo.getNetworkId() != -1) {
                int rssi = wifiInfo.getRssi();
                int level = WifiManager.calculateSignalLevel(rssi, 101);
                double percentage = (level * 100.0) / 100.0;
                
                String qualityText;
                int color;
                
                if (percentage >= 80) {
                    qualityText = "Excellent";
                    color = getColor(R.color.signal_excellent);
                } else if (percentage >= 60) {
                    qualityText = "Good";
                    color = getColor(R.color.signal_good);
                } else if (percentage >= 40) {
                    qualityText = "Fair";
                    color = getColor(R.color.signal_fair);
                } else if (percentage >= 20) {
                    qualityText = "Poor";
                    color = getColor(R.color.signal_poor);
                } else {
                    qualityText = "Very Poor";
                    color = getColor(R.color.signal_very_poor);
                }
                
                tvSignalStrength.setText(rssi + " dBm");
                tvSignalStrength.setTextColor(color);
                tvSignalPercent.setText(String.format("%.0f%%", percentage));
                tvSignalQuality.setText(qualityText);
                tvSignalQuality.setTextColor(color);
                progressBar.setProgress((int) percentage);
                progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
                
                String ssid = wifiInfo.getSSID();
                if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length() - 1);
                }
                tvSSID.setText(ssid != null ? ssid : "Unknown");
                tvBSSID.setText(wifiInfo.getBSSID() != null ? wifiInfo.getBSSID() : "Unknown");
                
                int frequency = wifiInfo.getFrequency();
                if (frequency > 4900) {
                    tvFrequency.setText("5 GHz (" + frequency + " MHz)");
                } else {
                    tvFrequency.setText("2.4 GHz (" + frequency + " MHz)");
                }
                
                tvLinkSpeed.setText(wifiInfo.getLinkSpeed() + " Mbps");
            } else {
                tvSignalStrength.setText("Not Connected");
                tvSignalPercent.setText("0%");
                tvSignalQuality.setText("No Connection");
                tvSSID.setText("Not connected");
                tvBSSID.setText("---");
                tvFrequency.setText("---");
                tvLinkSpeed.setText("---");
                progressBar.setProgress(0);
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(wifiReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }
    }
}
