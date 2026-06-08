package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.VocabApplication
import com.example.data.SettingsManager
import com.example.data.Word
import com.example.data.WordRepository
import com.example.receiver.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WordViewModel(application: Application) : AndroidViewModel(application) {
    private val vocabApp = application as VocabApplication
    private val repository: WordRepository = vocabApp.repository
    private val settings: SettingsManager = vocabApp.settings

    // Expose all words reactively
    val allWords: StateFlow<List<Word>> = repository.allWords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Configuration states
    private val _isLockEnabled = MutableStateFlow(settings.isLockScreenEnabled)
    val isLockEnabled = _isLockEnabled.asStateFlow()

    private val _intervalMinutes = MutableStateFlow(settings.intervalMinutes)
    val intervalMinutes = _intervalMinutes.asStateFlow()

    private val _isSoundEnabled = MutableStateFlow(settings.isSoundEnabled)
    val isSoundEnabled = _isSoundEnabled.asStateFlow()

    fun toggleLockScreen(enabled: Boolean) {
        settings.isLockScreenEnabled = enabled
        _isLockEnabled.value = enabled
        if (enabled) {
            AlarmScheduler.scheduleNextAlarm(vocabApp)
        } else {
            AlarmScheduler.cancelAlarm(vocabApp)
        }
    }

    fun updateInterval(minutes: Int) {
        settings.intervalMinutes = minutes
        _intervalMinutes.value = minutes
        if (settings.isLockScreenEnabled) {
            // Re-schedule alarm with the new interval
            AlarmScheduler.scheduleNextAlarm(vocabApp)
        }
    }

    fun toggleSound(enabled: Boolean) {
        settings.isSoundEnabled = enabled
        _isSoundEnabled.value = enabled
    }

    fun insertWord(word: Word) {
        viewModelScope.launch {
            repository.insert(word)
        }
    }

    fun updateWord(word: Word) {
        viewModelScope.launch {
            repository.update(word)
        }
    }

    fun deleteWord(word: Word) {
        viewModelScope.launch {
            repository.delete(word)
        }
    }

    fun forceShowNextImmediate() {
        viewModelScope.launch {
            // Instantly schedule next alarm in 2 seconds for quick user testing
            val originalInterval = settings.intervalMinutes
            settings.intervalMinutes = 1 // Set to 1 min temp, or trigger immediately
            AlarmScheduler.scheduleNextAlarm(vocabApp)
            settings.intervalMinutes = originalInterval
        }
    }
}
