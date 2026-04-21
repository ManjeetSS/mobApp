package com.example.mobapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Date

class WaterFragment : Fragment(R.layout.fragment_water) {

    private lateinit var interval: EditText
    private lateinit var goal: EditText
    private lateinit var enableSwitch: SwitchMaterial
    private lateinit var status: TextView
    private lateinit var heroMl: TextView
    private lateinit var heroGlasses: TextView
    private lateinit var goalSummary: TextView
    private lateinit var progress: LinearProgressIndicator
    private lateinit var glassButton: MaterialButton
    private lateinit var customMl: EditText
    private lateinit var logCustomButton: MaterialButton
    private lateinit var soundName: TextView
    private lateinit var pickSound: MaterialButton
    private lateinit var chart: SimpleBarChartView
    private lateinit var insights: TextView
    private var suppressListeners = false

    private lateinit var soundPickerLauncher: ActivityResultLauncher<Intent>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        NotifChannels.ensureAll(ctx)

        interval = view.findViewById(R.id.waterInterval)
        goal = view.findViewById(R.id.waterGoal)
        enableSwitch = view.findViewById(R.id.waterEnableSwitch)
        status = view.findViewById(R.id.waterStatus)
        heroMl = view.findViewById(R.id.waterHeroMl)
        heroGlasses = view.findViewById(R.id.waterHeroGlasses)
        goalSummary = view.findViewById(R.id.waterGoalSummary)
        progress = view.findViewById(R.id.waterProgress)
        glassButton = view.findViewById(R.id.waterGlassButton)
        customMl = view.findViewById(R.id.waterCustomMl)
        logCustomButton = view.findViewById(R.id.waterLogCustom)
        soundName = view.findViewById(R.id.waterSoundName)
        pickSound = view.findViewById(R.id.waterPickSound)
        chart = view.findViewById(R.id.waterChart)
        insights = view.findViewById(R.id.waterInsights)
        chart.barColor = androidx.core.content.ContextCompat.getColor(ctx, R.color.brand_water)
        chart.trackColor = 0x22888888
        chart.labelColor = ctx.resolveThemeColor(
            android.R.attr.textColorPrimary, 0xDD000000.toInt()
        )

        soundPickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val picked = SoundPicker.extractPickedUri(result.data)
            WaterPrefs.setSoundUri(ctx, picked)
            NotifChannels.recreateWaterChannel(ctx)
            refreshSoundLabel()
        }

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

        goal.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (suppressListeners) return
                val n = goal.text.toString().toIntOrNull() ?: return
                if (n < 1) return
                WaterPrefs.setDailyGoal(ctx, n)
                refreshHero()
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
            refreshHero()
            refreshStatus()
            refreshChart()
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
            refreshHero()
            refreshStatus()
            refreshChart()
        }

        pickSound.setOnClickListener {
            val intent = SoundPicker.buildIntent(
                ctx,
                WaterPrefs.getSoundUri(ctx),
                getString(R.string.picker_title_water)
            )
            soundPickerLauncher.launch(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val ctx = requireContext()
        suppressListeners = true
        interval.setText(WaterPrefs.getIntervalMinutes(ctx).toString())
        goal.setText(WaterPrefs.getDailyGoal(ctx).toString())
        enableSwitch.isChecked = WaterPrefs.isEnabled(ctx)
        suppressListeners = false
        refreshHero()
        refreshStatus()
        refreshSoundLabel()
        refreshChart()
    }

    private fun refreshChart() {
        val ctx = requireContext()
        val raw = WaterHistory.lastNDays(ctx, 7)
        val entries = raw.map { (day, ml) ->
            val glasses = ml / WaterPrefs.GLASS_ML
            SimpleBarChartView.Entry(
                label = DailyStats.shortLabel(day),
                value = glasses.toFloat(),
                valueLabel = glasses.toString()
            )
        }
        chart.setEntries(entries)

        val labels = raw.map { (day, _) -> DailyStats.shortLabel(day) }
        val values = raw.map { (_, ml) -> (ml.toDouble() / WaterPrefs.GLASS_ML) }
        val lines = Insights.summarize(
            labels = labels,
            values = values,
            noun = "hydration data",
            formatValue = { v ->
                val g = v.toInt()
                if (g == 1) "1 glass" else "$g glasses"
            }
        )
        insights.text = Insights.asBulletText(lines)
    }

    private fun refreshHero() {
        val ctx = requireContext()
        val ml = WaterPrefs.getTodayMl(ctx)
        val dailyGoal = WaterPrefs.getDailyGoal(ctx)
        val glasses = ml / WaterPrefs.GLASS_ML
        heroMl.text = "$ml ml"
        heroGlasses.text = getString(R.string.water_today_glasses, glasses)
        goalSummary.text = getString(R.string.water_goal_progress, ml, dailyGoal)
        val pct = if (dailyGoal <= 0) 0 else ((ml.toLong() * 100L) / dailyGoal).toInt().coerceIn(0, 100)
        progress.setProgressCompat(pct, true)
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
    }

    private fun refreshSoundLabel() {
        val ctx = requireContext()
        soundName.text = SoundPicker.displayName(ctx, WaterPrefs.getSoundUri(ctx))
    }
}
