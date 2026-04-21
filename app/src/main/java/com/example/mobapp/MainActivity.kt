package com.example.mobapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    companion object {
        /** Extra on the launch intent to pre-select a tab. One of [TAB_SCREEN], [TAB_WATER], [TAB_HABITS]. */
        const val EXTRA_OPEN_TAB = "com.example.mobapp.EXTRA_OPEN_TAB"
        const val TAB_SCREEN = "screen"
        const val TAB_WATER = "water"
        const val TAB_HABITS = "habits"
    }

    private val notifPermissionCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NotifChannels.ensureAll(this)
        ensureNotificationPermission()

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.setOnItemSelectedListener { item ->
            showFragment(
                when (item.itemId) {
                    R.id.nav_screen -> ScreenTimeFragment()
                    R.id.nav_water -> WaterFragment()
                    R.id.nav_habits -> HabitsFragment()
                    else -> return@setOnItemSelectedListener false
                }
            )
            true
        }

        if (savedInstanceState == null) {
            val targetId = tabIdFromIntent(intent) ?: R.id.nav_screen
            // Populate the container explicitly — assigning selectedItemId when it
            // already matches the default won't re-fire the listener.
            showFragment(
                when (targetId) {
                    R.id.nav_water -> WaterFragment()
                    R.id.nav_habits -> HabitsFragment()
                    else -> ScreenTimeFragment()
                }
            )
            nav.selectedItemId = targetId
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val targetId = tabIdFromIntent(intent) ?: return
        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        if (nav.selectedItemId != targetId) {
            nav.selectedItemId = targetId
        }
    }

    private fun tabIdFromIntent(intent: Intent?): Int? {
        return when (intent?.getStringExtra(EXTRA_OPEN_TAB)) {
            TAB_SCREEN -> R.id.nav_screen
            TAB_WATER -> R.id.nav_water
            TAB_HABITS -> R.id.nav_habits
            else -> null
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
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
