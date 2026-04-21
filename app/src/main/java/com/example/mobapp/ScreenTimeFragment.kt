package com.example.mobapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.switchmaterial.SwitchMaterial

class ScreenTimeFragment : Fragment(R.layout.fragment_screen_time) {

    private lateinit var enabledSwitch: SwitchMaterial
    private lateinit var status: TextView
    private lateinit var thresholdValue: EditText
    private lateinit var unitToggle: MaterialButtonToggleGroup
    private lateinit var soundName: TextView
    private lateinit var pickSound: MaterialButton
    private lateinit var todayValue: TextView
    private lateinit var chart: SimpleBarChartView
    private var suppressListeners = false
    private val refreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val liveRefresh = object : Runnable {
        override fun run() {
            refreshTodayAndChart()
            // Re-post while the fragment is visible so the live counter ticks forward.
            refreshHandler.postDelayed(this, 60_000L)
        }
    }

    private lateinit var soundPickerLauncher: ActivityResultLauncher<Intent>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        NotifChannels.ensureAll(ctx)

        status = view.findViewById(R.id.status)
        enabledSwitch = view.findViewById(R.id.enabledSwitch)
        thresholdValue = view.findViewById(R.id.thresholdValue)
        unitToggle = view.findViewById(R.id.unitToggle)
        soundName = view.findViewById(R.id.screenSoundName)
        pickSound = view.findViewById(R.id.screenPickSound)
        todayValue = view.findViewById(R.id.screenTodayValue)
        chart = view.findViewById(R.id.screenChart)
        chart.barColor = androidx.core.content.ContextCompat.getColor(ctx, R.color.brand_indigo)
        chart.trackColor = 0x22888888
        chart.labelColor = ctx.resolveThemeColor(
            android.R.attr.textColorPrimary, 0xDD000000.toInt()
        )

        soundPickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val picked = SoundPicker.extractPickedUri(result.data)
            Prefs.setSoundUri(ctx, picked)
            NotifChannels.recreateScreenAlertChannel(ctx)
            refreshSoundLabel()
        }

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

        pickSound.setOnClickListener {
            val intent = SoundPicker.buildIntent(
                ctx,
                Prefs.getSoundUri(ctx),
                getString(R.string.picker_title_screen)
            )
            soundPickerLauncher.launch(intent)
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
        refreshSoundLabel()
        refreshTodayAndChart()
        // Start the live ticker (updates the "Today" number every minute).
        refreshHandler.removeCallbacks(liveRefresh)
        refreshHandler.postDelayed(liveRefresh, 60_000L)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(liveRefresh)
    }

    private fun refreshTodayAndChart() {
        val ctx = context ?: return
        val todayMs = ScreenHistory.getMsTodayWithLive(ctx)
        todayValue.text = formatDuration(todayMs)

        val entries = ScreenHistory.lastNDaysWithLive(ctx, 7).map { (day, ms) ->
            val minutes = (ms / 60000L).toInt()
            SimpleBarChartView.Entry(
                label = DailyStats.shortLabel(day),
                value = minutes.toFloat(),
                valueLabel = formatShortDuration(ms)
            )
        }
        chart.setEntries(entries)
    }

    private fun formatDuration(ms: Long): String {
        val totalMin = (ms / 60000L).toInt()
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) getString(R.string.duration_h_m, h, m)
        else getString(R.string.duration_m, m)
    }

    private fun formatShortDuration(ms: Long): String {
        val totalMin = (ms / 60000L).toInt()
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) "${h}h" else "${m}m"
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

    private fun refreshSoundLabel() {
        val ctx = requireContext()
        soundName.text = SoundPicker.displayName(ctx, Prefs.getSoundUri(ctx))
    }
}
