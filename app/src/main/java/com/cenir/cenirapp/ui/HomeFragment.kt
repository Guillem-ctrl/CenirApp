package com.cenir.cenirapp.ui

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cenir.cenirapp.R
import com.cenir.cenirapp.data.gamification.LevelSystem
import com.cenir.cenirapp.data.gamification.StreakResult
import com.cenir.cenirapp.data.model.Task
import com.cenir.cenirapp.ui.home.AddTaskSheet
import com.cenir.cenirapp.ui.home.HomeViewModel
import com.cenir.cenirapp.ui.home.TaskAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import android.widget.ImageView
import android.content.pm.PackageManager

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var adapterObjectives: TaskAdapter
    private lateinit var adapterOverdue:    TaskAdapter
    private lateinit var adapterDaily:      TaskAdapter
    private lateinit var adapterToday:      TaskAdapter
    private lateinit var adapterUpcoming:   TaskAdapter

    // ── Tema ──────────────────────────────────────────────────────────────
    private fun saveThemePref(mode: Int) =
        requireContext().getSharedPreferences("cenir_prefs", Context.MODE_PRIVATE)
            .edit().putInt("theme_mode", mode).apply()

    private fun currentThemeMode(): Int =
        requireContext().getSharedPreferences("cenir_prefs", Context.MODE_PRIVATE)
            .getInt("theme_mode", MODE_NIGHT_FOLLOW_SYSTEM)

    private fun notificationsEnabled(): Boolean =
        requireContext().getSharedPreferences("cenir_prefs", Context.MODE_PRIVATE)
            .getBoolean("notifications_enabled", true)

    private fun saveNotificationsPref(enabled: Boolean) =
        requireContext().getSharedPreferences("cenir_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("notifications_enabled", enabled).apply()

    private fun applyTheme(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(mode)
        saveThemePref(mode)
    }

    private fun levelName(level: Int): String = when (level) {
        1, 2       -> "Aprendiz"
        3, 4       -> "Estudiante"
        in 5..7    -> "Dedicado"
        in 8..10   -> "Experto"
        else       -> "Maestro"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Vistas generales ──────────────────────────────────────────────
        val tvGreeting      = view.findViewById<TextView>(R.id.tvGreeting)
        val fab             = view.findViewById<FloatingActionButton>(R.id.fabAddTask)
        val btnTheme        = view.findViewById<ImageButton>(R.id.btnThemeOptions)

        val sectionOverdue  = view.findViewById<LinearLayout>(R.id.sectionOverdue)
        val dividerOverdue  = view.findViewById<View>(R.id.dividerOverdue)
        val rvOverdue       = view.findViewById<RecyclerView>(R.id.rvOverdue)
        val sectionDaily    = view.findViewById<LinearLayout>(R.id.sectionDaily)
        val dividerSections = view.findViewById<View>(R.id.dividerSections)
        val rvDaily         = view.findViewById<RecyclerView>(R.id.rvDaily)
        val rvTasks         = view.findViewById<RecyclerView>(R.id.rvTasks)
        val sectionUpcoming = view.findViewById<LinearLayout>(R.id.sectionUpcoming)
        val rvUpcoming      = view.findViewById<RecyclerView>(R.id.rvUpcoming)

        // ── Sección objetivos (dentro de la card de nivel) ────────────────
        val sectionObjectives  = view.findViewById<LinearLayout>(R.id.sectionObjectives)
        val dividerObjectives  = view.findViewById<View>(R.id.dividerObjectives)
        val rvObjectives       = view.findViewById<RecyclerView>(R.id.rvObjectives)

        // ── Card nivel ────────────────────────────────────────────────────
        val tvLevelLabel = view.findViewById<TextView>(R.id.tvLevelLabel)
        val tvTotalXp    = view.findViewById<TextView>(R.id.tvTotalXp)
        val tvLevelName  = view.findViewById<TextView>(R.id.tvLevelName)
        val pbXp         = view.findViewById<LinearProgressIndicator>(R.id.pbXp)
        val tvXpCurrent  = view.findViewById<TextView>(R.id.tvXpCurrent)
        val tvXpNeeded   = view.findViewById<TextView>(R.id.tvXpNeeded)

        // ── Sección objetivos (contadores y círculos) ─────────────────────
        val tvObjectivesCounter = view.findViewById<TextView>(R.id.tvObjectivesCounter)
        val tvObjectivesStatus  = view.findViewById<TextView>(R.id.tvObjectivesStatus)
        val btnManageObjectives = view.findViewById<TextView>(R.id.btnManageObjectives)
        val objCircles = listOf(
            view.findViewById<View>(R.id.circleObj1),
            view.findViewById<View>(R.id.circleObj2),
            view.findViewById<View>(R.id.circleObj3),
            view.findViewById<View>(R.id.circleObj4),
            view.findViewById<View>(R.id.circleObj5)
        )

        // ── Sección racha ─────────────────────────────────────────────────
        val tvStreakCount = view.findViewById<TextView>(R.id.tvStreakCount)
        val streakBars = listOf(
            view.findViewById<View>(R.id.barDay1),
            view.findViewById<View>(R.id.barDay2),
            view.findViewById<View>(R.id.barDay3),
            view.findViewById<View>(R.id.barDay4),
            view.findViewById<View>(R.id.barDay5),
            view.findViewById<View>(R.id.barDay6),
            view.findViewById<View>(R.id.barDay7)
        )

        // ── Card misiones ─────────────────────────────────────────────────
        val tvMission1Title    = view.findViewById<TextView>(R.id.tvMission1Title)
        val tvMission1Progress = view.findViewById<TextView>(R.id.tvMission1Progress)
        val pbMission1         = view.findViewById<LinearProgressIndicator>(R.id.pbMission1)
        val tvMission1Done     = view.findViewById<TextView>(R.id.tvMission1Done)
        val tvMission2Title    = view.findViewById<TextView>(R.id.tvMission2Title)
        val tvMission2Progress = view.findViewById<TextView>(R.id.tvMission2Progress)
        val pbMission2         = view.findViewById<LinearProgressIndicator>(R.id.pbMission2)
        val tvMission2Done     = view.findViewById<TextView>(R.id.tvMission2Done)

        // ── Adapters ──────────────────────────────────────────────────────
        adapterObjectives = TaskAdapter(
            onToggle = { viewModel.toggleTask(it) }, onDelete = { viewModel.deleteTask(it) },
            onUndo = { viewModel.restoreTask(it) }, showDailyBadge = false, allowDelete = false
        )
        adapterOverdue = TaskAdapter(
            onToggle = { viewModel.toggleTask(it) }, onDelete = { viewModel.deleteTask(it) },
            onUndo = { viewModel.restoreTask(it) }, showDailyBadge = false, allowDelete = true
        )
        adapterDaily = TaskAdapter(
            onToggle = { viewModel.toggleTask(it) }, onDelete = { viewModel.deleteTask(it) },
            onUndo = { viewModel.restoreTask(it) }, showDailyBadge = false, allowDelete = false
        )
        adapterToday = TaskAdapter(
            onToggle = { viewModel.toggleTask(it) }, onDelete = { viewModel.deleteTask(it) },
            onUndo = { viewModel.restoreTask(it) }, showDailyBadge = true, allowDelete = true
        )
        adapterUpcoming = TaskAdapter(
            onToggle = { viewModel.toggleTask(it) }, onDelete = { viewModel.deleteTask(it) },
            onUndo = { viewModel.restoreTask(it) }, showDailyBadge = false, allowDelete = true
        )

        // ── RecyclerViews ─────────────────────────────────────────────────
        rvObjectives.layoutManager = LinearLayoutManager(requireContext())
        rvObjectives.adapter = adapterObjectives
        rvObjectives.isNestedScrollingEnabled = false

        rvOverdue.layoutManager = LinearLayoutManager(requireContext())
        rvOverdue.adapter = adapterOverdue
        rvOverdue.isNestedScrollingEnabled = false
        adapterOverdue.attachSwipeToDelete(rvOverdue) { task, onConfirm, _ ->
            showDeleteDialog(task, onConfirm)
        }

        rvDaily.layoutManager = LinearLayoutManager(requireContext())
        rvDaily.adapter = adapterDaily
        rvDaily.isNestedScrollingEnabled = false

        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        rvTasks.adapter = adapterToday
        rvTasks.isNestedScrollingEnabled = false
        adapterToday.attachSwipeToDelete(rvTasks) { task, onConfirm, _ ->
            showDeleteDialog(task, onConfirm)
        }

        rvUpcoming.layoutManager = LinearLayoutManager(requireContext())
        rvUpcoming.adapter = adapterUpcoming
        rvUpcoming.isNestedScrollingEnabled = false
        adapterUpcoming.attachSwipeToDelete(rvUpcoming) { task, onConfirm, _ ->
            showDeleteDialog(task, onConfirm)
        }

        // ── Observers tareas ──────────────────────────────────────────────

        // Objetivos — sección separada dentro de la card de nivel
        viewModel.todayObjectiveTasks.observe(viewLifecycleOwner) { tasks ->
            adapterObjectives.submitList(TaskAdapter.buildItems(tasks))
            val visible = tasks.isNotEmpty()
            sectionObjectives.visibility = if (visible) View.VISIBLE else View.GONE
            dividerObjectives.visibility = if (visible) View.VISIBLE else View.GONE
            // Actualizar también los círculos y contador
            updateObjectivesUI(tasks, tvObjectivesCounter, tvObjectivesStatus, objCircles)
        }

        viewModel.overdueTasks.observe(viewLifecycleOwner) { tasks ->
            adapterOverdue.submitList(TaskAdapter.buildItems(tasks))
            sectionOverdue.visibility = if (tasks.isNotEmpty()) View.VISIBLE else View.GONE
            dividerOverdue.visibility = if (tasks.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.dailyTasks.observe(viewLifecycleOwner) { tasks ->
            adapterDaily.submitList(TaskAdapter.buildItems(tasks))
            sectionDaily.visibility    = if (tasks.isNotEmpty()) View.VISIBLE else View.GONE
            dividerSections.visibility = if (tasks.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.todayTasks.observe(viewLifecycleOwner) { tasks ->
            adapterToday.submitList(TaskAdapter.buildItems(tasks))
        }

        viewModel.upcomingTasks.observe(viewLifecycleOwner) { tasks ->
            adapterUpcoming.submitList(TaskAdapter.buildItems(tasks))
            sectionUpcoming.visibility = if (tasks.isNotEmpty()) View.VISIBLE else View.GONE
        }

        // ── Observer: gamificación (nivel + racha) ────────────────────────
        viewModel.userStats.observe(viewLifecycleOwner) { stats ->
            if (stats == null) return@observe
            val level    = stats.currentLevel
            val totalXp  = stats.totalXp
            val xpIn     = LevelSystem.xpInCurrentLevel(totalXp)
            val xpNeeded = LevelSystem.xpForNextLevel(level)
            val percent  = (LevelSystem.progressPercent(totalXp) * 100).toInt().coerceIn(0, 100)

            tvLevelLabel.text = "NIVEL $level"
            tvTotalXp.text    = "$totalXp XP"
            tvLevelName.text  = levelName(level)
            pbXp.setProgressCompat(percent, true)
            tvXpCurrent.text  = "$xpIn XP"
            tvXpNeeded.text   = "/ $xpNeeded XP para nivel ${level + 1}"

            tvStreakCount.text = stats.currentStreak.toString()
            val streak = stats.currentStreak.coerceAtMost(7)
            streakBars.forEachIndexed { i, bar ->
                bar.setBackgroundResource(
                    if (i >= 7 - streak) R.drawable.bg_streak_bar_active
                    else                  R.drawable.bg_streak_bar_inactive
                )
            }
        }

        // Cuando cambia el mínimo requerido, refresca el contador
        viewModel.streakSettings.observe(viewLifecycleOwner) {
            val objectives = viewModel.todayObjectiveTasks.value ?: emptyList()
            updateObjectivesUI(objectives, tvObjectivesCounter, tvObjectivesStatus, objCircles)
        }

        // ── Observer: resultado de racha ──────────────────────────────────
        viewModel.streakResult.observe(viewLifecycleOwner) { result ->
            if (result == null) return@observe
            if (result is StreakResult.STREAK_UPDATED) {
                showStreakSnackbar(result.newStreak)
            }
            viewModel.onStreakResultHandled()
        }

        // ── Observer: misiones ────────────────────────────────────────────
        viewModel.dailyMissions.observe(viewLifecycleOwner) { missions ->
            missions.firstOrNull { it.id.contains("mission_tasks") }?.let { m ->
                tvMission1Title.text = m.description
                if (m.isCompleted) {
                    tvMission1Progress.visibility = View.GONE
                    pbMission1.visibility         = View.GONE
                    tvMission1Done.text           = "✅ +${m.xpReward} XP"
                    tvMission1Done.visibility     = View.VISIBLE
                } else {
                    val pct = if (m.targetCount > 0)
                        (m.currentCount * 100 / m.targetCount).coerceIn(0, 100) else 0
                    tvMission1Progress.text       = "${m.currentCount}/${m.targetCount}"
                    tvMission1Progress.visibility = View.VISIBLE
                    pbMission1.setProgressCompat(pct, true)
                    pbMission1.visibility         = View.VISIBLE
                    tvMission1Done.visibility     = View.GONE
                }
            }
            missions.firstOrNull { it.id.contains("mission_focus") }?.let { m ->
                tvMission2Title.text = m.description
                if (m.isCompleted) {
                    tvMission2Progress.visibility = View.GONE
                    pbMission2.visibility         = View.GONE
                    tvMission2Done.text           = "✅ +${m.xpReward} XP"
                    tvMission2Done.visibility     = View.VISIBLE
                } else {
                    val pct = if (m.targetCount > 0)
                        (m.currentCount * 100 / m.targetCount).coerceIn(0, 100) else 0
                    tvMission2Progress.text       = "${m.currentCount}/${m.targetCount}"
                    tvMission2Progress.visibility = View.VISIBLE
                    pbMission2.setProgressCompat(pct, true)
                    pbMission2.visibility         = View.VISIBLE
                    tvMission2Done.visibility     = View.GONE
                }
            }
        }

        // ── Observer: subida de nivel ─────────────────────────────────────
        viewModel.levelUpEvent.observe(viewLifecycleOwner) { newLevel ->
            if (newLevel == null) return@observe
            showLevelUpSnackbar(newLevel)
            viewModel.onLevelUpEventHandled()
        }

        // ── Nombre ────────────────────────────────────────────────────────
        val name = viewModel.getUserName(requireContext())
        if (name.isEmpty()) showNameDialog(tvGreeting)
        else tvGreeting.text = "Hola, $name 👋"

        // ── Click listeners ───────────────────────────────────────────────
        btnTheme.setOnClickListener { showOptionsSheet() }
        fab.setOnClickListener { showAddTaskSheet() }
        btnManageObjectives.setOnClickListener { showObjectiveSelectorSheet() }
    }

    // ── Actualiza la UI de objetivos ──────────────────────────────────────
    private fun updateObjectivesUI(
        objectives: List<Task>,
        tvCounter:  TextView,
        tvStatus:   TextView,
        circles:    List<View>
    ) {
        val minRequired = viewModel.streakSettings.value?.streakMinObjectives ?: 3
        val total       = objectives.size
        val completed   = objectives.count { it.isCompleted }

        tvCounter.text = "$completed/$minRequired"

        val displayCount = minOf(total, 5)
        circles.forEachIndexed { index, circle ->
            when {
                index >= displayCount -> {
                    circle.setBackgroundResource(R.drawable.bg_obj_circle_empty)
                    circle.alpha = 0.25f
                }
                index < completed -> {
                    circle.setBackgroundResource(R.drawable.bg_obj_circle_done)
                    circle.alpha = 1f
                }
                else -> {
                    circle.setBackgroundResource(R.drawable.bg_obj_circle_empty)
                    circle.alpha = 1f
                }
            }
        }

        tvStatus.text = when {
            total == 0 -> {
                tvStatus.setTextColor(0x59FFFFFF.toInt())
                "Marca tareas como objetivo para activar la racha"
            }
            total < minRequired -> {
                tvStatus.setTextColor(0x59FFFFFF.toInt())
                "Necesitas ${minRequired - total} objetivo(s) más"
            }
            completed >= minRequired -> {
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.mint))
                "✅ Racha sumada hoy"
            }
            else -> {
                tvStatus.setTextColor(0x59FFFFFF.toInt())
                "Completa ${minRequired - completed} objetivo(s) más para la racha"
            }
        }
    }

    // ── Sheet selector de objetivos ───────────────────────────────────────
    private fun showObjectiveSelectorSheet() {
        val allTasks = (
                (viewModel.overdueTasks.value  ?: emptyList()) +
                        (viewModel.dailyTasks.value    ?: emptyList()) +
                        (viewModel.todayTasks.value    ?: emptyList()) +
                        (viewModel.upcomingTasks.value ?: emptyList())
                ).distinctBy { it.id }

        val sheet = BottomSheetDialog(requireContext(), R.style.RoundedBottomSheet)

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 56)
        }

        // Título
        TextView(requireContext()).apply {
            text = "🎯 Objetivos del día"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(requireContext().getColor(R.color.ink))
            setPadding(64, 52, 64, 4)
        }.also { container.addView(it) }

        // Subtítulo
        TextView(requireContext()).apply {
            text = "Las tareas marcadas cuentan para tu racha hasta que las desactives"
            textSize = 12f
            setTextColor(requireContext().getColor(R.color.ash))
            setPadding(64, 0, 64, 20)
        }.also { container.addView(it) }

        // Separador
        View(requireContext()).apply {
            setBackgroundColor(requireContext().getColor(R.color.chalk))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).also { it.bottomMargin = 8 }
        }.also { container.addView(it) }

        if (allTasks.isEmpty()) {
            TextView(requireContext()).apply {
                text = "No hay tareas creadas todavía"
                textSize = 14f
                setTextColor(requireContext().getColor(R.color.ash))
                gravity = Gravity.CENTER
                setPadding(64, 48, 64, 48)
            }.also { container.addView(it) }
        } else {
            val amberColor = ContextCompat.getColor(requireContext(), R.color.amber)
            val ashColor   = ContextCompat.getColor(requireContext(), R.color.ash)
            val inkColor   = ContextCompat.getColor(requireContext(), R.color.ink)

            allTasks.forEach { task ->
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity     = Gravity.CENTER_VERTICAL
                    setPadding(64, 14, 64, 14)
                }

                val cb = CheckBox(requireContext()).apply {
                    isChecked = task.isObjective
                    buttonTintList = ColorStateList(
                        arrayOf(
                            intArrayOf(android.R.attr.state_checked),
                            intArrayOf(-android.R.attr.state_checked)
                        ),
                        intArrayOf(amberColor, ashColor)
                    )
                    setOnCheckedChangeListener { _, checked ->
                        viewModel.setObjective(task, checked)
                    }
                }

                val tvTitle = TextView(requireContext()).apply {
                    text = task.title
                    textSize = 15f
                    setTextColor(inkColor)
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    ).also { it.marginStart = 16 }
                }

                val tvBadge = TextView(requireContext()).apply {
                    textSize = 12f
                    text = when {
                        task.isUrgent -> "⚡"
                        task.isDaily  -> "🔁"
                        else          -> ""
                    }
                    visibility = if (task.isUrgent || task.isDaily) View.VISIBLE else View.GONE
                }

                row.setOnClickListener { cb.toggle() }
                row.addView(cb)
                row.addView(tvTitle)
                row.addView(tvBadge)
                container.addView(row)
            }
        }

        // Botón cerrar
        Button(requireContext()).apply {
            text = "Listo"
            setTextColor(requireContext().getColor(R.color.ink))
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.amber))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(64, 24, 64, 0) }
            setOnClickListener { sheet.dismiss() }
        }.also { container.addView(it) }

        val scroll = ScrollView(requireContext()).apply { addView(container) }
        sheet.setContentView(scroll)
        sheet.show()
    }

    // ── BottomSheet de opciones ───────────────────────────────────────────
    private fun showOptionsSheet() {
        val sheet     = BottomSheetDialog(requireContext(), R.style.RoundedBottomSheet)
        val sheetView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_theme_picker, null)

        val currentMode = currentThemeMode()
        sheetView.findViewById<ImageView>(R.id.checkLight).visibility =
            if (currentMode == MODE_NIGHT_NO)            View.VISIBLE else View.GONE
        sheetView.findViewById<ImageView>(R.id.checkDark).visibility =
            if (currentMode == MODE_NIGHT_YES)           View.VISIBLE else View.GONE
        sheetView.findViewById<ImageView>(R.id.checkSystem).visibility =
            if (currentMode == MODE_NIGHT_FOLLOW_SYSTEM) View.VISIBLE else View.GONE

        sheetView.findViewById<LinearLayout>(R.id.optionLight).setOnClickListener {
            applyTheme(MODE_NIGHT_NO); sheet.dismiss()
        }
        sheetView.findViewById<LinearLayout>(R.id.optionDark).setOnClickListener {
            applyTheme(MODE_NIGHT_YES); sheet.dismiss()
        }
        sheetView.findViewById<LinearLayout>(R.id.optionSystem).setOnClickListener {
            applyTheme(MODE_NIGHT_FOLLOW_SYSTEM); sheet.dismiss()
        }

        val switchNotif = sheetView.findViewById<SwitchMaterial>(R.id.switchNotifications)
        switchNotif.isChecked = notificationsEnabled()
        switchNotif.setOnCheckedChangeListener { _, checked -> saveNotificationsPref(checked) }

        sheetView.findViewById<LinearLayout>(R.id.optionClearCompleted).setOnClickListener {
            sheet.dismiss()
            AlertDialog.Builder(requireContext())
                .setTitle("Borrar completadas")
                .setMessage("¿Eliminar todas las tareas completadas?")
                .setPositiveButton("Eliminar") { _, _ -> viewModel.deleteAllCompleted() }
                .setNegativeButton("Cancelar", null).show()
        }

        sheetView.findViewById<LinearLayout>(R.id.optionAbout).setOnClickListener {
            val version = try {
                requireContext().packageManager
                    .getPackageInfo(requireContext().packageName, 0).versionName
            } catch (e: PackageManager.NameNotFoundException) { "1.0" }
            AlertDialog.Builder(requireContext())
                .setTitle("Cenir v$version")
                .setMessage("App de productividad estudiantil.\nGestiona tareas, entrena concentración y sube de nivel estudiando.")
                .setPositiveButton("OK", null).show()
        }

        sheet.setContentView(sheetView)
        sheet.show()
    }

    // ── Snackbar racha ────────────────────────────────────────────────────
    private fun showStreakSnackbar(newStreak: Int) {
        val snackbar = Snackbar.make(
            requireView(),
            "🔥 ¡Racha actualizada! $newStreak días seguidos",
            Snackbar.LENGTH_LONG
        )
        val snackView = snackbar.view
        snackView.background = ContextCompat.getDrawable(
            requireContext(), R.drawable.bg_snackbar_amber
        )
        snackView.setPadding(32, 0, 32, 0)
        (snackView.layoutParams as?
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams)
            ?.also { it.setMargins(48, 0, 48, 160); snackView.layoutParams = it }
        snackbar.setTextColor(ContextCompat.getColor(requireContext(), R.color.ink))
        snackbar.show()
    }

    // ── Snackbar subida de nivel ──────────────────────────────────────────
    private fun showLevelUpSnackbar(newLevel: Int) {
        val snackbar = Snackbar.make(
            requireView(), "⭐ ¡Subiste al nivel $newLevel!", Snackbar.LENGTH_LONG
        )
        val snackView = snackbar.view
        snackView.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_snackbar)
        snackView.setPadding(32, 0, 32, 0)
        (snackView.layoutParams as?
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams)
            ?.also { it.setMargins(48, 0, 48, 160); snackView.layoutParams = it }
        snackbar.setTextColor(requireContext().getColor(R.color.ink))
        snackbar.show()
    }

    // ── Diálogo eliminar ──────────────────────────────────────────────────
    private fun showDeleteDialog(task: Task, onConfirm: () -> Unit) {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar tarea")
            .setMessage("¿Eliminar \"${task.title}\"?")
            .setPositiveButton("Eliminar") { _, _ -> onConfirm() }
            .setNegativeButton("Cancelar", null).show()
    }

    // ── Sheet añadir tarea ────────────────────────────────────────────────
    private fun showAddTaskSheet() {
        val sheet = AddTaskSheet()
        sheet.showDailyOption = false
        sheet.onTaskAdded = { title, subject, isUrgent, isDaily, dueDate, wantsReminder, isObjective ->
            viewModel.addTask(title, subject, isUrgent, isDaily, dueDate, wantsReminder, isObjective)
        }
        sheet.show(childFragmentManager, AddTaskSheet.TAG)
    }

    // ── Diálogo nombre ────────────────────────────────────────────────────
    private fun showNameDialog(tvGreeting: TextView) {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }
        val etName = EditText(context).apply {
            hint = "Tu nombre"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        container.addView(etName)

        val dialog = AlertDialog.Builder(context)
            .setTitle("¿Cómo te llamas?")
            .setView(container)
            .setCancelable(false)
            .setPositiveButton("Empezar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = etName.text.toString().trim()
                if (name.isEmpty()) etName.error = "Introduce tu nombre"
                else {
                    viewModel.saveUserName(context, name)
                    tvGreeting.text = "Hola, $name 👋"
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }
}
