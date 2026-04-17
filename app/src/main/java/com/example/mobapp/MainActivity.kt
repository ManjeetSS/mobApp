package com.example.mobapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private val notifPermissionCode = 1001

    private lateinit var enabledSwitch: SwitchMaterial
    private lateinit var status: TextView
    private lateinit var thresholdValue: EditText
    private lateinit var unitToggle: MaterialButtonToggleGroup

    /** Used to prevent our own programmatic UI updates from re-triggering listeners. */
    private var suppressListeners = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        enabledSwitch = findViewById(R.id.enabledSwitch)
        thresholdValue = findViewById(R.id.thresholdValue)
        unitToggle = findViewById(R.id.unitToggle)

        enabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (suppressListeners) return@setOnCheckedChangeListener

            if (isChecked) {
                val parsed = thresholdValue.text.toString().toIntOrNull()
                if (parsed == null || parsed < 1) {
                    Toast.makeText(this, R.string.invalid_threshold, Toast.LENGTH_SHORT).show()
                    suppressListeners = true
                    enabledSwitch.isChecked = false
                    suppressListeners = false
                    return@setOnCheckedChangeListener
                }
                persistThreshold()
                ensureNotificationPermission()
                Prefs.setEnabled(this, true)
                ContextCompat.startForegroundService(
                    this, Intent(this, ScreenTimeService::class.java)
                )
            } else {
                Prefs.setEnabled(this, false)
                stopService(Intent(this, ScreenTimeService::class.java))
            }
            refreshStatus()
        }

        thresholdValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (suppressListeners) return
                onThresholdChanged()
            }
        })

        unitToggle.addOnButtonCheckedListener { _, _, isChecked ->
            if (suppressListeners || !isChecked) return@addOnButtonCheckedListener
            onThresholdChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        suppressListeners = true

        val value = Prefs.getThresholdValue(this)
        val unit = Prefs.getThresholdUnit(this)
        thresholdValue.setText(value.toString())
        unitToggle.check(if (unit == Prefs.UNIT_HOURS) R.id.btnHours else R.id.btnMinutes)
        enabledSwitch.isChecked = Prefs.isEnabled(this)

        suppressListeners = false
        refreshStatus()
    }

    private fun onThresholdChanged() {
        val parsed = thresholdValue.text.toString().toIntOrNull() ?: return
        if (parsed < 1) return
        persistThreshold()
        refreshStatus()

        // If monitoring is running, ask the service to pick up the new value now.
        if (Prefs.isEnabled(this)) {
            val reload = Intent(this, ScreenTimeService::class.java).apply {
                action = ScreenTimeService.ACTION_RELOAD
            }
            ContextCompat.startForegroundService(this, reload)
        }
    }

    private fun persistThreshold() {
        val parsed = thresholdValue.text.toString().toIntOrNull() ?: return
        val unit = if (unitToggle.checkedButtonId == R.id.btnHours)
            Prefs.UNIT_HOURS else Prefs.UNIT_MINUTES
        Prefs.setThreshold(this, parsed, unit)
    }

    private fun refreshStatus() {
        if (Prefs.isEnabled(this)) {
            val durationText = DurationFormat.format(
                this, Prefs.getThresholdValue(this), Prefs.getThresholdUnit(this)
            )
            status.text = getString(R.string.status_running, durationText)
        } else {
            status.setText(R.string.status_stopped)
        }
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
