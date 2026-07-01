package com.cenir.cenirapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cenir.cenirapp.R
import com.cenir.cenirapp.ui.home.AddTaskSheet
import com.cenir.cenirapp.ui.home.TaskAdapter
import com.cenir.cenirapp.ui.tasks.TaskFilter
import com.cenir.cenirapp.ui.tasks.TasksViewModel
import com.google.android.material.chip.ChipGroup

class TasksFragment : Fragment() {

    private val viewModel: TasksViewModel by viewModels()
    private lateinit var adapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_tasks, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Adaptador ────────────────────────────────────────────────────
        adapter = TaskAdapter(
            onToggle = { viewModel.toggleTask(it) },
            onDelete = { viewModel.deleteTask(it) },
            onUndo   = { viewModel.restoreTask(it) },
            showDailyBadge = true,
            allowDelete    = true
        )

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerTasks)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        // ── Swipe para borrar con confirmación ───────────────────────────
        adapter.attachSwipeToDelete(recycler) { task, onConfirm, _ ->
            AlertDialog.Builder(requireContext())
                .setTitle("Eliminar tarea")
                .setMessage("¿Seguro que quieres eliminar \"${task.title}\"?")
                .setPositiveButton("Eliminar") { _, _ -> onConfirm() }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // ── Chips de filtro ───────────────────────────────────────────────
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupFilter)
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chipPending   -> TaskFilter.PENDING
                R.id.chipDaily     -> TaskFilter.DAILY
                R.id.chipCompleted -> TaskFilter.COMPLETED
                else               -> TaskFilter.ALL
            }
            viewModel.activeFilter.value = filter
        }

        view.findViewById<View>(R.id.btnAddTaskTop).setOnClickListener {
            val sheet = AddTaskSheet()
            sheet.showDailyOption = true   // o false, según quieras
            sheet.onTaskAdded = { title, subject, isUrgent, isDaily, dueDate, wantsReminder, _ ->
                // isObjective ignorado aquí (ver problema 3)
                viewModel.addTask(title, subject, isUrgent, isDaily, dueDate, wantsReminder)
            }
            sheet.show(parentFragmentManager, "add_task")
        }

        // ── Observar tareas ───────────────────────────────────────────────
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)
        viewModel.filteredTasks.observe(viewLifecycleOwner) { tasks ->
            val items = TaskAdapter.buildItems(tasks)
            adapter.submitList(items)
            tvEmpty.isVisible = tasks.isEmpty()
        }
    }
}
