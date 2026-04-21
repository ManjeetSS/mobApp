package com.example.mobapp

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Date

class HabitsAdapter(
    private val onToggle: (Habit, Boolean) -> Unit,
    private val onDone: (Habit) -> Unit
) : RecyclerView.Adapter<HabitsAdapter.VH>() {

    private var items: List<Habit> = emptyList()

    fun submit(list: List<Habit>) {
        items = list
        notifyDataSetChanged()
    }

    fun itemAt(position: Int): Habit = items[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_habit, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val name: TextView = v.findViewById(R.id.habitName)
        private val interval: TextView = v.findViewById(R.id.habitInterval)
        private val lastDone: TextView = v.findViewById(R.id.habitLastDone)
        private val enable: SwitchMaterial = v.findViewById(R.id.habitEnableSwitch)
        private val doneBtn: MaterialButton = v.findViewById(R.id.habitDoneButton)
        private val chart: SimpleBarChartView = v.findViewById(R.id.habitChart)
        private val insights: TextView = v.findViewById(R.id.habitInsights)

        fun bind(h: Habit) {
            val ctx = itemView.context
            name.text = h.name
            interval.text = ctx.getString(R.string.habit_interval_fmt, h.intervalMinutes)
            lastDone.text = if (h.lastDoneAt == 0L) {
                ctx.getString(R.string.habit_last_done_never)
            } else {
                val when_ = DateFormat.getTimeFormat(ctx).format(Date(h.lastDoneAt))
                ctx.getString(R.string.habit_last_done_at, when_)
            }

            // Avoid re-firing listener during bind.
            enable.setOnCheckedChangeListener(null)
            enable.isChecked = h.enabled
            enable.setOnCheckedChangeListener { _, checked -> onToggle(h, checked) }

            doneBtn.setOnClickListener { onDone(h) }

            chart.barColor = androidx.core.content.ContextCompat.getColor(ctx, R.color.brand_habits)
            chart.trackColor = 0x22888888
            chart.labelColor = ctx.resolveThemeColor(
                android.R.attr.textColorPrimary, 0xDD000000.toInt()
            )
            val raw = HabitHistory.lastNDays(ctx, h.id, 7)
            val entries = raw.map { (day, count) ->
                SimpleBarChartView.Entry(
                    label = DailyStats.shortLabel(day),
                    value = count.toFloat(),
                    valueLabel = count.toString()
                )
            }
            chart.setEntries(entries)

            val labels = raw.map { (day, _) -> DailyStats.shortLabel(day) }
            val values = raw.map { (_, count) -> count.toDouble() }
            val lines = Insights.summarize(
                labels = labels,
                values = values,
                noun = "completions",
                formatValue = { v ->
                    val n = v.toInt()
                    if (n == 1) "1 completion" else "$n completions"
                }
            )
            insights.text = Insights.asBulletText(lines)
        }
    }
}
