package com.cenir.cenirapp.ui.focus

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cenir.cenirapp.data.database.AppDatabase
import com.cenir.cenirapp.data.gamification.GamificationManager
import com.cenir.cenirapp.notifications.NotificationHelper
import kotlinx.coroutines.launch

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val _remainingSeconds = MutableLiveData<Long>()
    val remainingSeconds: LiveData<Long> = _remainingSeconds

    private val _isRunning = MutableLiveData(false)
    val isRunning: LiveData<Boolean> = _isRunning

    private val _sessionDone = MutableLiveData(false)
    val sessionDone: LiveData<Boolean> = _sessionDone

    // ── Gamificación ──────────────────────────────────────────────────────

    private val gamification = GamificationManager(
        AppDatabase.getInstance(application),
        application.applicationContext
    )

    var selectedMinutes: Int = 25
        private set

    var totalSeconds: Long = 25 * 60L
        private set

    private var timer: CountDownTimer? = null
    private var remainingMs: Long = 25 * 60 * 1000L

    init {
        totalSeconds = selectedMinutes * 60L
        _remainingSeconds.value = selectedMinutes * 60L
    }

    fun setMinutes(min: Int) {
        if (_isRunning.value == true) return
        selectedMinutes = min
        totalSeconds    = min * 60L
        remainingMs     = min * 60 * 1000L
        _remainingSeconds.value = min * 60L
    }

    fun start() {
        if (_isRunning.value == true) return
        timer = object : CountDownTimer(remainingMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMs = millisUntilFinished
                _remainingSeconds.value = millisUntilFinished / 1000
            }
            override fun onFinish() {
                _remainingSeconds.value = 0L
                _isRunning.value = false

                // Notificación al sistema
                val ctx = getApplication<Application>().applicationContext
                NotificationHelper.showFocusDoneNotification(ctx, selectedMinutes)

                // ── Gamificación: sesión completada ───────────────────────
                val minutes = selectedMinutes
                viewModelScope.launch {
                    gamification.onFocusSessionCompleted(minutes)
                }

                _sessionDone.value = true
                remainingMs = selectedMinutes * 60 * 1000L  // reset interno
            }
        }.start()
        _isRunning.value = true
    }

    fun pause() {
        timer?.cancel()
        timer = null
        _isRunning.value = false
    }

    fun reset() {
        timer?.cancel()
        timer = null
        remainingMs = selectedMinutes * 60 * 1000L
        _remainingSeconds.value = selectedMinutes * 60L
        _isRunning.value = false
    }

    /** Llamado desde el Fragment tras mostrar el Snackbar */
    fun onSessionDoneHandled() {
        _sessionDone.value = false
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }
}
