package com.cenir.cenirapp.ui.achievements

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cenir.cenirapp.R
import com.cenir.cenirapp.data.model.Achievement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AchievementsAdapter :
    ListAdapter<Achievement, AchievementsAdapter.ViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val viewIconBg = itemView.findViewById<View>(R.id.viewIconBg)
        private val tvIcon     = itemView.findViewById<TextView>(R.id.tvIcon)
        private val tvTitle    = itemView.findViewById<TextView>(R.id.tvAchievementTitle)
        private val tvDesc     = itemView.findViewById<TextView>(R.id.tvAchievementDesc)
        private val tvXp       = itemView.findViewById<TextView>(R.id.tvAchievementXp)
        private val tvDate     = itemView.findViewById<TextView>(R.id.tvUnlockedDate)

        fun bind(achievement: Achievement) {
            tvTitle.text = achievement.title
            tvDesc.text  = achievement.description

            if (achievement.isUnlocked) {
                // ── Desbloqueado: colores fijos modo claro ────────────────
                viewIconBg.setBackgroundResource(R.drawable.bg_circle_filled)
                tvIcon.text = "🏆"
                tvTitle.setTextColor(0xFF1A1A2E.toInt())   // ink fijo
                tvDesc.setTextColor(0xFF9E9E9E.toInt())    // ash fijo
                tvXp.text = "+${achievement.xpReward} XP"
                tvXp.setTextColor(0xFFF39C12.toInt())      // amber fijo

                val ts = achievement.unlockedAt
                if (ts != null) {
                    tvDate.text       = "Desbloqueado el ${dateFormat.format(Date(ts))}"
                    tvDate.visibility = View.VISIBLE
                } else {
                    tvDate.visibility = View.GONE
                }
            } else {
                // ── Bloqueado: todo en gris ───────────────────────────────
                viewIconBg.setBackgroundResource(R.drawable.bg_circle_locked)
                tvIcon.text = "🔒"
                tvTitle.setTextColor(0xFF9E9E9E.toInt())   // ash fijo
                tvDesc.setTextColor(0xFF9E9E9E.toInt())    // ash fijo
                tvXp.text = "???"
                tvXp.setTextColor(0xFF9E9E9E.toInt())      // ash fijo
                tvDate.visibility = View.GONE
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Achievement>() {
        override fun areItemsTheSame(a: Achievement, b: Achievement) = a.id == b.id
        override fun areContentsTheSame(a: Achievement, b: Achievement) = a == b
    }
}
