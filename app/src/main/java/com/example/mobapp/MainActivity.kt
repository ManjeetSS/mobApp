package com.example.mobapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private val notifPermissionCode = 1001
    private lateinit var enabledSwitch: SwitchMaterial
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        enabledSwitch = findViewById(R.id.enabledSwitch)

        enabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ensureNotificationPermission()
                Prefs.setEnabled(this, true)
                ContextCompat.startForegroundService(
                    this, Intent(this, ScreenTimeService::class.java)
                )
                status.setText(R.string.status_running)
            } else {
                Prefs.setEnabled(this, false)
                stopService(Intent(this, ScreenTimeService::class.java))
                status.setText(R.string.status_stopped)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val enabled = Prefs.isEnabled(this)
        enabledSwitch.isChecked = enabled
        status.setText(if (enabled) R.string.status_running else R.string.status_stopped)
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    notifPermissionCode
                )
            }
        }
    }
}
