package com.example.mobapp

import android.app.Application
import com.google.android.material.color.DynamicColors

/**
 * Applies Material You dynamic color (Android 12+) to every activity and ensures
 * notification channels exist before the first reminder fires.
 */
class MobAppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        NotifChannels.ensureAll(this)
    }
}
