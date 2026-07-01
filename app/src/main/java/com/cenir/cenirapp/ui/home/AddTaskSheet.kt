package com.cenir.cenirapp.ui.home

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.cenir.cenirapp.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class AddTaskSheet : BottomSheetDialogFragment() {

    var onTaskAdded: ((
        title:        String,
        subject:      String,
        isUrgent:     Boolean,
        isDaily:      Boolean,
        dueDate:      Long?,
        wantsReminder:Boolean,
        isObjective:  Boolean
    ) -> Unit)? = null

    /** false → oculta el switch "🔁 Tarea diaria" (usado desde Home) */
    var showDailyOption: Boolean = true

    private var selectedDateMs: Long? = null
    private var selectedHour:   Int   = -1
    private var selectedMinute: Int   = -1

    private val selectedDateTime: Long?
        get() {
            val dateMs = selectedDateMs ?: return null
            if (selectedHour < 0) return null
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.timeInMillis = dateMs
            val localCal = Calendar.getInstance()
            localCal.set(
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH), selectedHour, selectedMinute, 0
            )
            localCal.set(Calendar.MILLISECOND, 0)
            return localCal.timeInMillis
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
            bottomSheet?.let {
                BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_add_task, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etTitle         = view.findViewById<TextInputEditText>(R.id.etTaskTitle)
        val etSubject       = view.findViewById<TextInputEditText>(R.id.etTaskSubject)
        val switchUrgent    = view.findViewById<SwitchCompat>(R.id.switchUrgent)
        val switchDaily     = view.findViewById<SwitchCompat>(R.id.switchDaily)
        val tvDailyHint     = view.findViewById<TextView>(R.id.tvDailyHint)
        val switchObjective = view.findViewById<SwitchCompat>(R.id.switchObjective)
        val tvObjectiveHint = view.findViewById<TextView>(R.id.tvObjectiveHint)
        val btnDate         = view.findViewById<Button>(R.id.btnDate)
        val btnTime         = view.findViewById<Button>(R.id.btnTime)
        val rowReminder     = view.findViewById<View>(R.id.rowReminder)
        val switchReminder  = view.findViewById<SwitchCompat>(R.id.switchReminder)
        val tvReminderInfo  = view.findViewById<TextView>(R.id.tvReminderInfo)
        val btnAdd          = view.findViewById<Button>(R.id.btnAddTask)

        // Ocultar opción diaria si se abre desde Home
        if (!showDailyOption) {
            switchDaily.visibility  = View.GONE
            tvDailyHint.visibility  = View.GONE
        }

        btnTime.isEnabled      = false
        rowReminder.visibility = View.GONE
        switchReminder.isChecked = true

        // ── Habilitar botón solo si hay título ────────────────────────────
        etTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                btnAdd.isEnabled = s?.toString()?.trim()?.isNotEmpty() == true
            }
        })

        // ── Switch diaria ─────────────────────────────────────────────────
        switchDaily.setOnCheckedChangeListener { _, checked ->
            if (showDailyOption) {
                tvDailyHint.visibility = if (checked) View.VISIBLE else View.GONE
            }
        }

        // ── Botón FECHA ───────────────────────────────────────────────────
        btnDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecciona fecha límite")
                .setSelection(selectedDateMs ?: MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                selectedDateMs = selection
                val fmt = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es")).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                btnDate.text    = "📅 ${fmt.format(selection)}"
                btnTime.isEnabled = true
                updateReminderRow(rowReminder, tvReminderInfo)
            }
            picker.show(parentFragmentManager, "date_picker")
        }

        // ── Botón HORA ────────────────────────────────────────────────────
        btnTime.setOnClickListener {
            val initHour   = if (selectedHour   >= 0) selectedHour   else 8
            val initMinute = if (selectedMinute >= 0) selectedMinute else 0

            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(initHour)
                .setMinute(initMinute)
                .setTitleText("Selecciona hora límite")
                .build()

            picker.addOnPositiveButtonClickListener {
                selectedHour   = picker.hour
                selectedMinute = picker.minute
                btnTime.text   = "🕐 %02d:%02d".format(selectedHour, selectedMinute)
                updateReminderRow(rowReminder, tvReminderInfo)
            }
            picker.show(parentFragmentManager, "time_picker")
        }

        // ── Botón AÑADIR ──────────────────────────────────────────────────
        btnAdd.setOnClickListener {
            val title         = etTitle.text.toString().trim()
            val subject       = etSubject.text.toString().trim()
            val isUrgent      = switchUrgent.isChecked
            val isDaily       = showDailyOption && switchDaily.isChecked
            val isObjective   = false
            val dueDate       = selectedDateTime
            val wantsReminder = switchReminder.isChecked && dueDate != null

            onTaskAdded?.invoke(title, subject, isUrgent, isDaily, dueDate, wantsReminder, isObjective)
            dismiss()
        }
    }

    private fun updateReminderRow(rowReminder: View, tvReminderInfo: TextView) {
        val dt = selectedDateTime
        if (dt == null) {
            rowReminder.visibility = View.GONE
            return
        }
        rowReminder.visibility = View.VISIBLE
        val reminderMs = dt - TWO_HOURS_MS
        val cal = Calendar.getInstance()
        cal.timeInMillis = reminderMs
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        tvReminderInfo.text = "Recibirás un aviso a las %02d:%02d".format(h, m)
    }

    companion object {
        const val TAG = "AddTaskSheet"
        private const val TWO_HOURS_MS = 2 * 60 * 60 * 1000L
    }
}
