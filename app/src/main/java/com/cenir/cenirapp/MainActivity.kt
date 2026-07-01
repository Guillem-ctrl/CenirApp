package com.cenir.cenirapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cenir.cenirapp.data.database.AppDatabase
import com.cenir.cenirapp.data.gamification.GamificationManager
import com.cenir.cenirapp.notifications.DailyMissionsWorker
import com.cenir.cenirapp.notifications.DailyResetWorker
import com.cenir.cenirapp.notifications.NotificationHelper
import com.cenir.cenirapp.notifications.StreakWarningWorker
import com.cenir.cenirapp.ui.FocusFragment
import com.cenir.cenirapp.ui.HomeFragment
import com.cenir.cenirapp.ui.StatsFragment
import com.cenir.cenirapp.ui.TasksFragment
import com.cenir.cenirapp.ui.achievements.AchievementsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* resultado ignorado */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar tema ANTES de super.onCreate para evitar flash visual
        val prefs = getSharedPreferences("cenir_prefs", Context.MODE_PRIVATE)
        val savedMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedMode)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Crear canal de notificaciones
        NotificationHelper.createChannel(this)

        // Programar workers periódicos
        scheduleDailyReset()
        scheduleGamificationNotifications()

        // Inicializar gamificación en background (nunca en el hilo principal)
        lifecycleScope.launch(Dispatchers.IO) {
            GamificationManager(
                AppDatabase.getInstance(this@MainActivity),
                this@MainActivity
            ).initIfNeeded()
        }

        // Pedir permiso de notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            // Primera creación: añadir todos los fragments y ocultar los que no son Home
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, HomeFragment(),         TAG_HOME)
                .add(R.id.fragment_container, FocusFragment(),        TAG_FOCUS)
                .add(R.id.fragment_container, TasksFragment(),        TAG_TASKS)
                .add(R.id.fragment_container, StatsFragment(),        TAG_STATS)
                .add(R.id.fragment_container, AchievementsFragment(), TAG_ACHIEVEMENTS)
                .commit()

            // Ocultar todos excepto Home (necesita un segundo commit para que estén añadidos)
            supportFragmentManager.executePendingTransactions()
            supportFragmentManager.beginTransaction()
                .hide(supportFragmentManager.findFragmentByTag(TAG_FOCUS)!!)
                .hide(supportFragmentManager.findFragmentByTag(TAG_TASKS)!!)
                .hide(supportFragmentManager.findFragmentByTag(TAG_STATS)!!)
                .hide(supportFragmentManager.findFragmentByTag(TAG_ACHIEVEMENTS)!!)
                .commit()

            // Si se abre desde notificación de logro, ir a Achievements
            val openTab = intent.getStringExtra("open_tab")
            if (openTab == "achievements") {
                bottomNav.selectedItemId = R.id.nav_achievements
                showFragmentByTag(TAG_ACHIEVEMENTS)
            }

        } else {
            // Recreación (cambio de tema, rotación, etc.): restaurar pestaña activa
            val openTab = intent.getStringExtra("open_tab")
            if (openTab == "achievements") {
                bottomNav.selectedItemId = R.id.nav_achievements
                showFragmentByTag(TAG_ACHIEVEMENTS)
            } else {
                val selectedId = savedInstanceState.getInt("selected_nav", R.id.nav_home)
                val tag = when (selectedId) {
                    R.id.nav_focus        -> TAG_FOCUS
                    R.id.nav_tasks        -> TAG_TASKS
                    R.id.nav_stats        -> TAG_STATS
                    R.id.nav_achievements -> TAG_ACHIEVEMENTS
                    else                  -> TAG_HOME
                }
                showFragmentByTag(tag)
                bottomNav.selectedItemId = selectedId
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            val tag = when (item.itemId) {
                R.id.nav_home         -> TAG_HOME
                R.id.nav_focus        -> TAG_FOCUS
                R.id.nav_tasks        -> TAG_TASKS
                R.id.nav_stats        -> TAG_STATS
                R.id.nav_achievements -> TAG_ACHIEVEMENTS
                else                  -> TAG_HOME
            }
            showFragmentByTag(tag)
            true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        outState.putInt("selected_nav", bottomNav.selectedItemId)
    }

    /**
     * Muestra el fragment con [targetTag] y oculta el resto.
     * Solo trabaja con fragments ya añadidos al FragmentManager.
     */
    private fun showFragmentByTag(targetTag: String) {
        val allTags = listOf(TAG_HOME, TAG_FOCUS, TAG_TASKS, TAG_STATS, TAG_ACHIEVEMENTS)
        val tx = supportFragmentManager.beginTransaction()
        allTags.forEach { tag ->
            val fragment = supportFragmentManager.findFragmentByTag(tag) ?: return@forEach
            if (tag == targetTag) tx.show(fragment) else tx.hide(fragment)
        }
        tx.commit()
    }

    // ── Workers ──────────────────────────────────────────────────────────────

    private fun scheduleDailyReset() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val delayUntilMidnight = cal.timeInMillis - now

        val request = PeriodicWorkRequestBuilder<DailyResetWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntilMidnight, TimeUnit.MILLISECONDS)
            .addTag(DailyResetWorker.TAG)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DailyResetWorker.TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun scheduleGamificationNotifications() {
        val workManager = WorkManager.getInstance(this)

        // Misiones diarias — cada día a las 08:00
        val missionRequest = PeriodicWorkRequestBuilder<DailyMissionsWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(minutesUntil(8, 0), TimeUnit.MILLISECONDS)
            .addTag(DailyMissionsWorker.TAG)
            .build()

        // Aviso de racha — cada día a las 20:00
        val streakRequest = PeriodicWorkRequestBuilder<StreakWarningWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(minutesUntil(20, 0), TimeUnit.MILLISECONDS)
            .addTag(StreakWarningWorker.TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            DailyMissionsWorker.TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            missionRequest
        )
        workManager.enqueueUniquePeriodicWork(
            StreakWarningWorker.TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            streakRequest
        )
    }

    /**
     * Calcula los milisegundos que faltan hasta la próxima ocurrencia de [hour]:[minute].
     * Si ya pasó hoy, apunta al día siguiente.
     */
    private fun minutesUntil(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    companion object {
        private const val TAG_HOME         = "home"
        private const val TAG_FOCUS        = "focus"
        private const val TAG_TASKS        = "tasks"
        private const val TAG_STATS        = "stats"
        private const val TAG_ACHIEVEMENTS = "achievements"
    }
}
