package com.cenir.cenirapp.ui

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.cenir.cenirapp.R
import com.cenir.cenirapp.ui.focus.FocusViewModel
import com.cenir.cenirapp.ui.focus.TimerRingView
import com.google.android.material.snackbar.Snackbar

class FocusFragment : Fragment() {

    private val viewModel: FocusViewModel by viewModels()
    private var ringAnimator: ValueAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_focus, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTimer      = view.findViewById<TextView>(R.id.tvTimer)
        val tvLabel      = view.findViewById<TextView>(R.id.tvSessionLabel)
        val timerRing    = view.findViewById<TimerRingView>(R.id.timerRing)
        val btnPlayPause = view.findViewById<ImageButton>(R.id.btnPlayPause)
        val btnReset     = view.findViewById<Button>(R.id.btnReset)
        val btn25        = view.findViewById<Button>(R.id.btn25)
        val btn45        = view.findViewById<Button>(R.id.btn45)
        val btn60        = view.findViewById<Button>(R.id.btn60)
        val etCustomMin  = view.findViewById<EditText>(R.id.etCustomMinutes)
        val btnCustom    = view.findViewById<Button>(R.id.btnCustomMinutes)

        // ── Adapta el color del timer al tema actual (claro = oscuro, oscuro = blanco) ──
        val isNightMode = (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val timerTextColor = if (isNightMode) 0xFFE8E8F0.toInt() else 0xFF1A1A2E.toInt()
        tvTimer.setTextColor(timerTextColor)

        // ── Botones de duración ───────────────────────────────────────────────
        fun updateDurationButtons(selected: Int) {
            listOf(btn25 to 25, btn45 to 45, btn60 to 60).forEach { (btn, min) ->
                val isActive = min == selected
                btn.background = ContextCompat.getDrawable(
                    requireContext(),
                    if (isActive) R.drawable.bg_duration_active
                    else R.drawable.bg_duration_inactive
                )
                btn.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (isActive) R.color.amber else R.color.ash
                    )
                )
            }
        }

        // ── Estado inicial inmediato (evita texto invisible y botón blanco) ───
        tvTimer.text = String.format("%02d:00", viewModel.selectedMinutes)
        timerRing.progress = 1f
        updateDurationButtons(viewModel.selectedMinutes) // ← corrige el color amber del btn25

        // ── Tiempo restante → texto y anillo ─────────────────────────────────
        viewModel.remainingSeconds.observe(viewLifecycleOwner) { secs ->
            val m = secs / 60
            val s = secs % 60
            tvTimer.text = String.format("%02d:%02d", m, s)

            val total = viewModel.totalSeconds.toFloat()
            val target = if (total > 0f) secs.toFloat() / total else 0f
            animateRingTo(timerRing, target)
        }

        // ── Estado running → icono + etiqueta ────────────────────────────────
        viewModel.isRunning.observe(viewLifecycleOwner) { running ->
            btnPlayPause.setImageResource(
                if (running) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            )
            tvLabel.text = when {
                running -> "EN FOCO"
                viewModel.remainingSeconds.value == viewModel.totalSeconds -> "LISTO"
                else -> "PAUSADO"
            }
        }

        // ── Sesión completada ─────────────────────────────────────────────────
        viewModel.sessionDone.observe(viewLifecycleOwner) { done ->
            if (!done) return@observe
            tvLabel.text = "¡COMPLETADO!"
            animateRingTo(timerRing, 0f)
            val snack = Snackbar.make(
                view,
                "Sesión completada 🎉 Tómate un descanso",
                Snackbar.LENGTH_LONG
            )
            snack.view.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.mint)
            )
            snack.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            snack.show()
            viewModel.onSessionDoneHandled()
        }

        btn25.setOnClickListener {
            viewModel.setMinutes(25)
            updateDurationButtons(25)
            timerRing.progress = 1f
            tvTimer.text = "25:00"
            tvLabel.text = "LISTO"
        }
        btn45.setOnClickListener {
            viewModel.setMinutes(45)
            updateDurationButtons(45)
            timerRing.progress = 1f
            tvTimer.text = "45:00"
            tvLabel.text = "LISTO"
        }
        btn60.setOnClickListener {
            viewModel.setMinutes(60)
            updateDurationButtons(60)
            timerRing.progress = 1f
            tvTimer.text = "60:00"
            tvLabel.text = "LISTO"
        }

        btnCustom.setOnClickListener {
            val input = etCustomMin.text.toString().trim()
            val min = input.toIntOrNull()
            if (min != null && min in 1..180) {
                viewModel.setMinutes(min)
                updateDurationButtons(-1)
                timerRing.progress = 1f
                tvTimer.text = String.format("%02d:00", min)
                tvLabel.text = "LISTO"
                etCustomMin.setText("")
                etCustomMin.clearFocus()
            } else {
                etCustomMin.error = "1–180 min"
            }
        }

        // ── Play / Pause ──────────────────────────────────────────────────────
        btnPlayPause.setOnClickListener {
            if (viewModel.isRunning.value == true) viewModel.pause()
            else viewModel.start()
        }

        // ── Reset ─────────────────────────────────────────────────────────────
        btnReset.setOnClickListener {
            viewModel.reset()
            updateDurationButtons(viewModel.selectedMinutes)
            animateRingTo(timerRing, 1f)
            tvTimer.text = String.format("%02d:00", viewModel.selectedMinutes)
            tvLabel.text = "LISTO"
        }
    }

    /** Anima el anillo desde su valor actual hasta [target] en ~800ms */
    private fun animateRingTo(ring: TimerRingView, target: Float) {
        ringAnimator?.cancel()
        ringAnimator = ValueAnimator.ofFloat(ring.progress, target).apply {
            duration = 800L
            interpolator = LinearInterpolator()
            addUpdateListener { ring.progress = it.animatedValue as Float }
            start()
        }
    }

    override fun onDestroyView() {
        ringAnimator?.cancel()
        super.onDestroyView()
    }
}
