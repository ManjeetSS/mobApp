package com.example.mobapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.switchmaterial.SwitchMaterial

class ScreenTimeFragment : Fragment(R.layout.fragment_screen_time) {

    private lateinit var enabledSwitch: SwitchMaterial
    private lateinit var status: TextView
    private lateinit var thresholdValue: EditText
    private lateinit var unitToggle: MaterialButtonToggleGroup
    private var suppressListeners = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()

        status = view.findViewById(R.id.status)
        enabledSwitch = view.findViewById(R.id.enabledSwitch)
        thresholdValue = view.findViewById(R.id.thresholdValue)
        unitToggle = view.findViewById(R.id.unitToggle)

        enabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (suppressListeners) return@setOnCheckedChangeListener
            if (isChecked) {
                val parsed = thresholdValue.text.toString().toIntOrNull()
                if (parsed == null || parsed < 1) {
                    Toast.makeText(ctx, R.string.invalid_threshold, Toast.LENGTH_SHORT).show()
                    suppressListeners = true
                    enabledSwitch.isChecked = false
                    suppressListeners = false
                    return@setOnCheckedChangeListener
                }
                persistThreshold()
                Prefs.setEnabled(ctx, true)
                ContextCompat.startForegroundService(
                    ctx, Intent(ctx, ScreenTimeService::class.java)
                )
            } else {
                Prefs.setEnabled(ctx, false)
                ctx.stopService(Intent(ctx, ScreenTimeService::class.java))
            }
            refreshStatus()
        }

        thresholdValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!suppressListeners) onThresholdChanged()
            }
        })

        unitToggle.addOnButtonCheckedListener { _, _, isChecked ->
            if (!suppressListeners && isChecked) onThresholdChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        val ctx = requireContext()
        suppressListeners = true

        val value = Prefs.getThresholdValue(ctx)
        val unit = Prefs.getThresholdUnit(ctx)
        thresholdValue.setText(value.toString())
        unitToggle.check(if (unit == Prefs.UNIT_HOURS) R.id.btnHours else R.id.btnMinutes)
        enabledSwitch.isChecked = Prefs.isEnabled(ctx)

        suppressListeners = false
        refreshStatus()
    }

    private fun onThresholdChanged() {
        val ctx = requireContext()
        val parsed = thresholdValue.text.toString().toIntOrNull() ?: return
        if (parsed < 1) return
        persistThreshold()
        refreshStatus()
        if (Prefs.isEnabled(ctx)) {
            val reload = Intent(ctx, ScreenTimeService::class.java).apply {
                action = ScreenTimeService.ACTION_RELOAD
            }
            ContextCompat.startForegroundService(ctx, reload)
        }
    }

    private fun persistThreshold() {
        val ctx = requireContext()
        val parsed = thresholdValue.text.toString().toIntOrNull() ?: return
        val unit = if (unitToggle.checkedButtonId == R.id.btnHours)
            Prefs.UNIT_HOURS else Prefs.UNIT_MINUTES
        Prefs.setThreshold(ctx, parsed, unit)
    }

    private fun refreshStatus() {
        val ctx = requireContext()
        if (Prefs.isEnabled(ctx)) {
            val durationText = DurationFormat.format(
                ctx, Prefs.getThresholdValue(ctx), Prefs.getThresholdUnit(ctx)
            )
            status.text = getString(R.string.status_running, durationText)
        } else {
            status.setText(R.string.status_stopped)
        }
    }
}
