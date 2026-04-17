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
    private val onDone: (Habit) -> Unit,
    private val onDelete: (Habit) -> Unit
) : RecyclerView.Adapter<HabitsAdapter.VH>() {

    private var items: List<Habit> = emptyList()

    fun submit(list: List<Habit>) {
        items = list
        notifyDataSetChanged()
    }

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
        private val deleteBtn: MaterialButton = v.findViewById(R.id.habitDeleteButton)

        fun bind(h: Habit) {
            name.text = h.name
            interval.text = itemView.context.getString(R.string.habit_interval_fmt, h.intervalMinutes)
            lastDone.text = if (h.lastDoneAt == 0L) {
                itemView.context.getString(R.string.habit_last_done_never)
            } else {
                val when_ = DateFormat.getTimeFormat(itemView.context).format(Date(h.lastDoneAt))
                itemView.context.getString(R.string.habit_last_done_at, when_)
            }

            // Avoid re-firing listener during bind.
            enable.setOnCheckedChangeListener(null)
            enable.isChecked = h.enabled
            enable.setOnCheckedChangeListener { _, checked -> onToggle(h, checked) }

            doneBtn.setOnClickListener { onDone(h) }
            deleteBtn.setOnClickListener { onDelete(h) }
        }
    }
}
