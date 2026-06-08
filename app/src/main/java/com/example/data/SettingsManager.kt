package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "vocab_lock_settings",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_INTERVAL = "key_interval_minutes"
        private const val KEY_ENABLED = "key_lock_screen_enabled"
        private const val KEY_SOUND = "key_sound_enabled"
        private const val KEY_LAST_WORD_ID = "key_last_word_id"
    }

    var intervalMinutes: Int
        get() = prefs.getInt(KEY_INTERVAL, 15) // Default: 15 minutes
        set(value) = prefs.edit().putInt(KEY_INTERVAL, value).apply()

    var isLockScreenEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true) // Default: Enabled
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true) // Default: Enabled
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var lastWordId: Int
        get() = prefs.getInt(KEY_LAST_WORD_ID, 1)
        set(value) = prefs.edit().putInt(KEY_LAST_WORD_ID, value).apply()
}
