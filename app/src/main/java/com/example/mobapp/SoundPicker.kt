package com.example.mobapp

import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri

/** Helpers for invoking the system ringtone picker and rendering selected URIs. */
object SoundPicker {

    fun buildIntent(ctx: Context, current: Uri?, title: String): Intent =
        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, title)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current)
        }

    fun extractPickedUri(data: Intent?): Uri? =
        data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)

    /** Human-readable name for a sound URI (e.g. "Cesium" or "Default alarm"). */
    fun displayName(ctx: Context, uri: Uri?): String {
        if (uri == null) return ctx.getString(R.string.sound_default)
        val r: Ringtone? = RingtoneManager.getRingtone(ctx, uri)
        return r?.getTitle(ctx) ?: ctx.getString(R.string.sound_default)
    }
}
