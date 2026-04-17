package com.example.mobapp

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton

class HabitsFragment : Fragment(R.layout.fragment_habits) {

    private lateinit var list: RecyclerView
    private lateinit var empty: TextView
    private lateinit var fab: FloatingActionButton
    private lateinit var adapter: HabitsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        NotifChannels.ensureAll(ctx)

        list = view.findViewById(R.id.habitsList)
        empty = view.findViewById(R.id.habitsEmpty)
        fab = view.findViewById(R.id.addHabitFab)

        adapter = HabitsAdapter(
            onToggle = { habit, checked ->
                val updated = habit.copy(enabled = checked)
                HabitsStore.update(ctx, updated)
                if (checked) HabitScheduler.schedule(ctx, updated)
                else HabitScheduler.cancel(ctx, habit.id)
                refresh()
            },
            onDone = { habit ->
                val updated = habit.copy(lastDoneAt = System.currentTimeMillis())
                HabitsStore.update(ctx, updated)
                HabitScheduler.cancel(ctx, habit.id)
                if (updated.enabled) HabitScheduler.schedule(ctx, updated)
                // Dismiss any currently-showing reminder for this habit.
                (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .cancel(habit.id)
                refresh()
            },
            onDelete = { habit ->
                HabitScheduler.cancel(ctx, habit.id)
                (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .cancel(habit.id)
                HabitsStore.remove(ctx, habit.id)
                refresh()
            }
        )

        list.layoutManager = LinearLayoutManager(ctx)
        list.adapter = adapter

        fab.setOnClickListener { showAddDialog() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val habits = HabitsStore.getAll(requireContext())
        adapter.submit(habits)
        empty.visibility = if (habits.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAddDialog() {
        val ctx = requireContext()
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_add_habit, null, false)
        val nameField = view.findViewById<EditText>(R.id.dialogHabitName)
        val intervalField = view.findViewById<EditText>(R.id.dialogHabitInterval)

        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.habits_add)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = nameField.text.toString().trim()
                val minutes = intervalField.text.toString().toIntOrNull()
                if (name.isEmpty() || minutes == null || minutes < 1) {
                    Toast.makeText(ctx, R.string.habit_invalid, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val habit = HabitsStore.add(ctx, name, minutes)
                HabitScheduler.schedule(ctx, habit)
                refresh()
            }
            .show()
    }
}
