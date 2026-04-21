package com.betnow.app.ui.leaderboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.betnow.app.R
import com.betnow.app.databinding.ItemLeaderboardBinding
import com.betnow.app.network.models.LeaderboardEntry
import com.betnow.app.util.UiFormatters

class LeaderboardAdapter : ListAdapter<LeaderboardEntry, LeaderboardAdapter.ViewHolder>(Diff) {

    inner class ViewHolder(private val binding: ItemLeaderboardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: LeaderboardEntry) {
            val ctx = binding.root.context
            binding.rankText.text = ctx.getString(R.string.leaderboard_rank, entry.rank)
            binding.emailText.text = entry.email
            binding.statsText.text = "${entry.betsCount} bets · wagered ${UiFormatters.currency(entry.wagered)}"
            binding.profitText.text = UiFormatters.currency(entry.netProfit)
            binding.profitText.setTextColor(
                ctx.getColor(if (entry.netProfit >= 0) R.color.yes_green else R.color.no_red)
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    private object Diff : DiffUtil.ItemCallback<LeaderboardEntry>() {
        override fun areItemsTheSame(old: LeaderboardEntry, new: LeaderboardEntry) = old.userId == new.userId
        override fun areContentsTheSame(old: LeaderboardEntry, new: LeaderboardEntry) = old == new
    }
}
