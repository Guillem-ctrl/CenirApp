package com.cenir.cenirapp.ui.achievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cenir.cenirapp.R
import com.google.android.material.progressindicator.LinearProgressIndicator

class AchievementsFragment : Fragment() {

    private val viewModel: AchievementsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_achievements, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Vistas ────────────────────────────────────────────────────────
        val tvSubtitle       = view.findViewById<TextView>(R.id.tvAchievementSubtitle)
        val tvUnlockedCount  = view.findViewById<TextView>(R.id.tvUnlockedCount)
        val tvTotalXpEarned  = view.findViewById<TextView>(R.id.tvTotalXpEarned)
        val pbAchievements   = view.findViewById<LinearProgressIndicator>(R.id.pbAchievements)
        val tvProgress       = view.findViewById<TextView>(R.id.tvAchievementProgress)
        val rvAchievements   = view.findViewById<RecyclerView>(R.id.rvAchievements)

        // ── Adapter + RecyclerView ────────────────────────────────────────
        val adapter = AchievementsAdapter()
        rvAchievements.layoutManager = LinearLayoutManager(requireContext())
        rvAchievements.adapter = adapter
        rvAchievements.isNestedScrollingEnabled = false

        // ── Observers ─────────────────────────────────────────────────────
        viewModel.achievements.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }

        viewModel.unlockedCount.observe(viewLifecycleOwner) { unlocked ->
            val total = viewModel.totalCount.value ?: 0
            tvSubtitle.text      = "$unlocked / $total desbloqueados"
            tvUnlockedCount.text = "$unlocked logros desbloqueados"
            tvProgress.text      = "$unlocked de $total logros"
        }

        viewModel.totalCount.observe(viewLifecycleOwner) { total ->
            val unlocked = viewModel.unlockedCount.value ?: 0
            tvSubtitle.text = "$unlocked / $total desbloqueados"
            tvProgress.text = "$unlocked de $total logros"
        }

        viewModel.totalXpEarned.observe(viewLifecycleOwner) { xp ->
            tvTotalXpEarned.text = "$xp XP"
        }

        viewModel.progressPercent.observe(viewLifecycleOwner) { pct ->
            pbAchievements.setProgressCompat(pct, true)
        }
    }
}
