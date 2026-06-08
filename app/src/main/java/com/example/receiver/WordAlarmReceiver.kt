package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.LockScreenWordActivity
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.SettingsManager
import com.example.data.Word
import com.example.data.WordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WordAlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "WordAlarmReceiver"
        private const val CHANNEL_ID = "com.example.vocablock.WORD_NOTIFICATION_CHANNEL"
        private const val NOTIFICATION_ID = 8821
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm received. Action: ${intent.action}")

        val settings = SettingsManager(context)
        if (!settings.isLockScreenEnabled) {
            Log.d(TAG, "Lock screen notifications disabled in settings. Skipping.")
            return
        }

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context, scope)
                val repository = WordRepository(db.wordDao())
                
                // Get next word, or fallback to a default if db empty
                var word = repository.getNextWordToLearn()
                if (word == null) {
                    // Try to get any random word
                    word = repository.getRandomWord()
                }

                if (word != null) {
                    repository.incrementShownCount(word.id)
                    settings.lastWordId = word.id
                    
                    withContext(Dispatchers.Main) {
                        showNotification(context, word)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching word or showing notification: ${e.message}", e)
            } finally {
                // Schedule the next flashcard
                AlarmScheduler.scheduleNextAlarm(context)
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, word: Word) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel for O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vocab Lockscreens",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows new words dynamically on lock screen"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Full Screen Intent (Activity)
        val fullScreenIntent = Intent(context, LockScreenWordActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("word_id", word.id)
            putExtra("is_from_notification", true)
        }

        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            word.id,
            fullScreenIntent,
            pendingFlags
        )

        // General Click Intent (Normal click expands app)
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            pendingFlags
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon) // Fallback to app icon
            .setContentTitle("New Word / Yangi so'z")
            .setContentText("${word.english} = ${word.uzbek} (RU: ${word.russian})")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true) // Essential lockscreen override
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        try {
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
            Log.d(TAG, "Notification issued for word ID: ${word.id}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission missing to display notification: ${e.message}")
        }
    }
}
