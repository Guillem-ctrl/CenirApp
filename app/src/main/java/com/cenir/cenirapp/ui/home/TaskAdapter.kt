package com.cenir.cenirapp.ui.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cenir.cenirapp.R
import com.cenir.cenirapp.data.model.Task
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Locale

sealed class TaskItem {
    data class Entry(val task: Task) : TaskItem()
    object Header : TaskItem()
}

class TaskAdapter(
    private val onToggle:       (Task) -> Unit,
    private val onDelete:       (Task) -> Unit,
    private val onUndo:         (Task) -> Unit,
    private val showDailyBadge: Boolean = true,
    private val allowDelete:    Boolean = true
) : ListAdapter<TaskItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private const val TYPE_TASK   = 0
        private const val TYPE_HEADER = 1

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TaskItem>() {
            override fun areItemsTheSame(oldItem: TaskItem, newItem: TaskItem) = when {
                oldItem is TaskItem.Entry && newItem is TaskItem.Entry ->
                    oldItem.task.id == newItem.task.id
                oldItem is TaskItem.Header && newItem is TaskItem.Header -> true
                else -> false
            }
            override fun areContentsTheSame(oldItem: TaskItem, newItem: TaskItem) =
                oldItem == newItem
        }

        fun buildItems(tasks: List<Task>): List<TaskItem> {
            val items = mutableListOf<TaskItem>()
            var headerInserted = false
            for (task in tasks) {
                if (task.isCompleted && !headerInserted) {
                    items.add(TaskItem.Header)
                    headerInserted = true
                }
                items.add(TaskItem.Entry(task))
            }
            return items
        }
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card:      MaterialCardView = itemView as MaterialCardView
        private val checkbox:  CheckBox         = itemView.findViewById(R.id.checkboxTask)
        private val tvTitle:   TextView         = itemView.findViewById(R.id.tvTaskTitle)
        private val tvUrgent:  TextView         = itemView.findViewById(R.id.tvUrgent)
        private val tvMeta:    TextView         = itemView.findViewById(R.id.tvTaskMeta)
        private val tvDaily:   TextView         = itemView.findViewById(R.id.tvDaily)
        private val tvDueDate: TextView         = itemView.findViewById(R.id.tvDueDate)

        fun bind(task: Task) {
            tvTitle.text = task.title

            tvMeta.isVisible = task.subject.isNotBlank()
            if (task.subject.isNotBlank()) tvMeta.text = task.subject

            tvDaily.isVisible = task.isDaily && !task.isCompleted && showDailyBadge

            val showUrgent = task.isUrgent && !task.isCompleted
            tvUrgent.visibility = if (showUrgent) View.VISIBLE else View.GONE

            checkbox.isChecked = task.isCompleted
            card.setOnClickListener { onToggle(task) }

            // ── Estilo según estado ───────────────────────────────────────
            if (task.isCompleted) {
                tvTitle.paintFlags = tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tvTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.ash))
                val completedTint = if (task.isObjective) R.color.objective else R.color.mint
                checkbox.buttonTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.context, completedTint)
                )
                card.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.mint_light)
                )
                card.strokeColor = ContextCompat.getColor(itemView.context, R.color.mint_light)
            } else {
                tvTitle.paintFlags = tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                tvTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.ink))

                when {
                    task.isObjective -> {
                        card.setCardBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.objective_light)
                        )
                        card.strokeColor = ContextCompat.getColor(itemView.context, R.color.objective)
                        checkbox.buttonTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.context, R.color.objective)
                        )
                    }
                    task.isUrgent -> {
                        card.setCardBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.amber_light)
                        )
                        card.strokeColor = ContextCompat.getColor(itemView.context, R.color.amber)
                        checkbox.buttonTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.context, R.color.amber)
                        )
                    }
                    else -> {
                        card.setCardBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.card_bg)
                        )
                        card.strokeColor = ContextCompat.getColor(itemView.context, R.color.card_border)
                        checkbox.buttonTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.context, R.color.mint)
                        )
                    }
                }
            }

            // ── Fecha límite ──────────────────────────────────────────────
            val due = task.dueDate
            if (due == null) {
                tvDueDate.visibility = View.GONE
            } else {
                tvDueDate.visibility = View.VISIBLE
                val now   = System.currentTimeMillis()
                val in24h = now + 24 * 60 * 60 * 1000L
                val dateFmt = SimpleDateFormat("d MMM", Locale.forLanguageTag("es"))
                val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                val dateStr = dateFmt.format(due)
                val timeStr = timeFmt.format(due)
                when {
                    due < now -> {
                        tvDueDate.text = "📅 $dateStr · $timeStr — vencida"
                        tvDueDate.setTextColor(Color.parseColor("#E05252"))
                    }
                    due <= in24h -> {
                        val minutesLeft = ((due - now) / 60_000L).toInt()
                        val hoursLeft   = minutesLeft / 60
                        val timeLeftStr = when {
                            minutesLeft < 60 -> "en menos de 1 hora"
                            hoursLeft == 1   -> "en 1 hora"
                            else             -> "dentro de $hoursLeft horas"
                        }
                        tvDueDate.text = "📅 $dateStr · $timeStr — $timeLeftStr"
                        tvDueDate.setTextColor(
                            ContextCompat.getColor(itemView.context, R.color.amber)
                        )
                    }
                    else -> {
                        tvDueDate.text = "📅 $dateStr · $timeStr"
                        tvDueDate.setTextColor(
                            ContextCompat.getColor(itemView.context, R.color.ash)
                        )
                    }
                }
            }
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is TaskItem.Entry  -> TYPE_TASK
            is TaskItem.Header -> TYPE_HEADER
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                inflater.inflate(R.layout.item_section_header, parent, false)
            )
            else -> TaskViewHolder(
                inflater.inflate(R.layout.item_task, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TaskViewHolder) {
            val item = getItem(position)
            if (item is TaskItem.Entry) holder.bind(item.task)
        }
    }

    fun attachSwipeToDelete(
        recyclerView: RecyclerView,
        onSwipeConfirm: (task: Task, onConfirm: () -> Unit, onCancel: () -> Unit) -> Unit
    ) {
        val swipe = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                if (viewHolder is HeaderViewHolder) return 0
                if (!allowDelete) return 0
                return super.getSwipeDirs(recyclerView, viewHolder)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return
                val item = getItem(pos)
                if (item !is TaskItem.Entry) return
                val task = item.task
                notifyItemChanged(pos)
                onSwipeConfirm(task, { onDelete(task) }, { })
            }
        }
        ItemTouchHelper(swipe).attachToRecyclerView(recyclerView)
    }
}
