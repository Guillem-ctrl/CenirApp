package com.cenir.cenirapp.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.cenir.cenirapp.R
import com.cenir.cenirapp.ui.stats.StatsViewModel

class StatsFragment : Fragment() {

    private val viewModel: StatsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_stats, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvGreeting      = view.findViewById<TextView>(R.id.tvStatsGreeting)
        val tvToday         = view.findViewById<TextView>(R.id.tvCompletedToday)
        val tvStreak        = view.findViewById<TextView>(R.id.tvStreak)
        val tvTotal         = view.findViewById<TextView>(R.id.tvTotalCompleted)
        val layoutDots      = view.findViewById<LinearLayout>(R.id.layoutDots)

        // Nombre de usuario desde SharedPreferences
        val name = requireContext()
            .getSharedPreferences("cenir_prefs", Context.MODE_PRIVATE)
            .getString("user_name", "") ?: ""
        if (name.isNotEmpty()) tvGreeting.text = "Buen trabajo, $name 💪"

        // Observar LiveData
        viewModel.completedToday.observe(viewLifecycleOwner) { count ->
            tvToday.text = count.toString()
        }

        viewModel.currentStreak.observe(viewLifecycleOwner) { streak ->
            tvStreak.text = "$streak días"
        }

        viewModel.totalCompleted.observe(viewLifecycleOwner) { total ->
            tvTotal.text = total.toString()
        }

        viewModel.lastSevenDays.observe(viewLifecycleOwner) { days ->
            updateDots(layoutDots, days)
        }
    }

    private fun updateDots(container: LinearLayout, days: List<Boolean>) {
        container.removeAllViews()
        val size   = resources.getDimensionPixelSize(R.dimen.dot_size)   // 12dp
        val margin = resources.getDimensionPixelSize(R.dimen.dot_margin) // 4dp

        days.forEach { active ->
            val dot = View(requireContext())
            val params = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = margin
            }
            dot.layoutParams = params
            dot.background = ContextCompat.getDrawable(
                requireContext(),
                if (active) R.drawable.bg_circle_filled
                else R.drawable.bg_circle_empty
            )
            container.addView(dot)
        }
    }
}
