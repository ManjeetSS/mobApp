package com.example.mobapp

import android.app.NotificationManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

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
                (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .cancel(habit.id)
                refresh()
            }
        )

        list.layoutManager = LinearLayoutManager(ctx)
        list.adapter = adapter

        attachSwipeToDelete(ctx)

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

    private fun attachSwipeToDelete(ctx: Context) {
        val bg = ContextCompat.getDrawable(ctx, R.drawable.ic_swipe_delete_bg)
        val icon: Drawable? = ContextCompat.getDrawable(ctx, android.R.drawable.ic_menu_delete)

        val callback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val pos = vh.bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return
                val habit = adapter.itemAt(pos)
                deleteWithUndo(ctx, habit, pos)
            }

            override fun onChildDraw(
                c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = vh.itemView
                val left = itemView.left
                val right = itemView.right
                val top = itemView.top
                val bottom = itemView.bottom
                bg?.setBounds(left, top, right, bottom)
                bg?.draw(c)

                icon?.let {
                    val iconSize = it.intrinsicHeight.coerceAtMost((bottom - top) / 2)
                    val iconMargin = (bottom - top - iconSize) / 2
                    val iconTop = top + iconMargin
                    if (dX < 0) {
                        // Swiping left — icon on the right side.
                        val iconLeft = right - iconMargin - iconSize
                        val iconRight = right - iconMargin
                        it.setBounds(iconLeft, iconTop, iconRight, iconTop + iconSize)
                    } else if (dX > 0) {
                        // Swiping right — icon on the left side.
                        val iconLeft = left + iconMargin
                        val iconRight = left + iconMargin + iconSize
                        it.setBounds(iconLeft, iconTop, iconRight, iconTop + iconSize)
                    } else {
                        it.setBounds(0, 0, 0, 0)
                    }
                    it.setTint(android.graphics.Color.WHITE)
                    it.draw(c)
                }

                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(list)
    }

    private fun deleteWithUndo(ctx: Context, habit: Habit, originalPosition: Int) {
        HabitScheduler.cancel(ctx, habit.id)
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(habit.id)
        HabitsStore.remove(ctx, habit.id)
        refresh()

        val root = view ?: return
        Snackbar.make(root, getString(R.string.habit_deleted, habit.name), Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) {
                HabitsStore.insertAt(ctx, habit, originalPosition)
                if (habit.enabled) HabitScheduler.schedule(ctx, habit)
                refresh()
            }
            .setAnchorView(fab)
            .show()
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
