package com.betnow.app.ui.bets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.betnow.app.R
import com.betnow.app.databinding.ItemBetBinding
import com.betnow.app.network.models.Bet
import com.betnow.app.util.UiFormatters

class MyBetsAdapter(
    private val onRedeem: (Bet) -> Unit
) : ListAdapter<Bet, MyBetsAdapter.ViewHolder>(Diff) {

    inner class ViewHolder(private val binding: ItemBetBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(bet: Bet) {
            val ctx = binding.root.context
            val isYes = bet.side.equals("YES", ignoreCase = true)

            binding.marketQuestion.text = bet.marketQuestion
            binding.sideBadge.text = bet.side.uppercase()
            binding.sideBadge.setBackgroundResource(if (isYes) R.drawable.bg_yes_badge else R.drawable.bg_no_badge)
            binding.sideBadge.setTextColor(ctx.getColor(if (isYes) R.color.yes_green else R.color.no_red))
            binding.sharesText.text = UiFormatters.shares(bet.shares)
            binding.costText.text = ctx.getString(R.string.bet_cost, UiFormatters.currency(bet.totalCost))
            binding.priceText.text = ctx.getString(R.string.bet_price, UiFormatters.cents(bet.pricePerShare))
            binding.dateText.text = UiFormatters.dateTime(bet.createdAt)

            binding.statusText.visibility = View.VISIBLE
            binding.redeemButton.visibility = View.GONE

            when {
                bet.redeemed -> {
                    val won = bet.payout > 0
                    binding.statusText.text = if (won) {
                        ctx.getString(R.string.bet_redeemed_win, UiFormatters.currency(bet.payout))
                    } else ctx.getString(R.string.bet_redeemed_lose)
                    binding.statusText.setTextColor(ctx.getColor(if (won) R.color.yes_green else R.color.no_red))
                }
                bet.marketResolved -> {
                    binding.statusText.text = ctx.getString(R.string.redeem)
                    binding.statusText.setTextColor(ctx.getColor(R.color.primary))
                    binding.redeemButton.visibility = View.VISIBLE
                    binding.redeemButton.setOnClickListener { onRedeem(bet) }
                }
                else -> {
                    binding.statusText.text = ctx.getString(R.string.bet_active)
                    binding.statusText.setTextColor(ctx.getColor(R.color.text_secondary))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemBetBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    private object Diff : DiffUtil.ItemCallback<Bet>() {
        override fun areItemsTheSame(old: Bet, new: Bet) = old.id == new.id
        override fun areContentsTheSame(old: Bet, new: Bet) = old == new
    }
}
