package com.example

import android.app.Application
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.SettingsManager
import com.example.data.WordRepository
import com.example.receiver.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VocabApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var database: AppDatabase
        private set

    lateinit var repository: WordRepository
        private set

    lateinit var settings: SettingsManager
        private set

    override fun onCreate() {
        super.onCreate()
        Log.d("VocabApplication", "VocabApplication created.")
        
        database = AppDatabase.getDatabase(this, applicationScope)
        repository = WordRepository(database.wordDao())
        settings = SettingsManager(this)

        // Bootstrap alarms if enabled on startup
        applicationScope.launch {
            if (settings.isLockScreenEnabled) {
                Log.d("VocabApplication", "Alarm schedule is enabled. Triggering setup.")
                AlarmScheduler.scheduleNextAlarm(this@VocabApplication)
            }
        }
    }
}
