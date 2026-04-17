package com.example.mobapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Date

class WaterFragment : Fragment(R.layout.fragment_water) {

    private lateinit var interval: EditText
    private lateinit var enableSwitch: SwitchMaterial
    private lateinit var status: TextView
    private lateinit var intake: TextView
    private lateinit var glassButton: Button
    private lateinit var customMl: EditText
    private lateinit var logCustomButton: Button
    private var suppressListeners = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        NotifChannels.ensureAll(ctx)

        interval = view.findViewById(R.id.waterInterval)
        enableSwitch = view.findViewById(R.id.waterEnableSwitch)
        status = view.findViewById(R.id.waterStatus)
        intake = view.findViewById(R.id.waterIntake)
        glassButton = view.findViewById(R.id.waterGlassButton)
        customMl = view.findViewById(R.id.waterCustomMl)
        logCustomButton = view.findViewById(R.id.waterLogCustom)

        interval.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (suppressListeners) return
                val n = interval.text.toString().toIntOrNull() ?: return
                if (n < 1) return
                WaterPrefs.setIntervalMinutes(ctx, n)
                if (WaterPrefs.isEnabled(ctx)) {
                    WaterScheduler.cancel(ctx)
                    WaterScheduler.schedule(ctx)
                }
                refreshStatus()
            }
        })

        enableSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressListeners) return@setOnCheckedChangeListener
            val n = interval.text.toString().toIntOrNull()
            if (checked && (n == null || n < 1)) {
                Toast.makeText(ctx, R.string.invalid_threshold, Toast.LENGTH_SHORT).show()
                suppressListeners = true
                enableSwitch.isChecked = false
                suppressListeners = false
                return@setOnCheckedChangeListener
            }
            WaterPrefs.setEnabled(ctx, checked)
            if (checked) WaterScheduler.schedule(ctx) else WaterScheduler.cancel(ctx)
            refreshStatus()
        }

        glassButton.setOnClickListener {
            WaterPrefs.logMl(ctx, WaterPrefs.GLASS_ML)
            if (WaterPrefs.isEnabled(ctx)) {
                WaterScheduler.cancel(ctx)
                WaterScheduler.schedule(ctx)
            }
            refreshStatus()
        }

        logCustomButton.setOnClickListener {
            val ml = customMl.text.toString().toIntOrNull()
            if (ml == null || ml <= 0) {
                Toast.makeText(ctx, R.string.water_custom_hint, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            WaterPrefs.logMl(ctx, ml)
            customMl.setText("")
            if (WaterPrefs.isEnabled(ctx)) {
                WaterScheduler.cancel(ctx)
                WaterScheduler.schedule(ctx)
            }
            refreshStatus()
        }
    }

    override fun onResume() {
        super.onResume()
        val ctx = requireContext()
        suppressListeners = true
        interval.setText(WaterPrefs.getIntervalMinutes(ctx).toString())
        enableSwitch.isChecked = WaterPrefs.isEnabled(ctx)
        suppressListeners = false
        refreshStatus()
    }

    private fun refreshStatus() {
        val ctx = requireContext()
        if (WaterPrefs.isEnabled(ctx)) {
            val nextAt = WaterPrefs.getNextAlarmAt(ctx)
            val timeStr = if (nextAt > System.currentTimeMillis()) {
                DateFormat.getTimeFormat(ctx).format(Date(nextAt))
            } else "soon"
            status.text = getString(R.string.water_next, timeStr)
        } else {
            status.setText(R.string.water_off)
        }
        val ml = WaterPrefs.getTodayMl(ctx)
        val glasses = ml / WaterPrefs.GLASS_ML
        intake.text = getString(R.string.water_intake_format, glasses, ml)
    }
}
