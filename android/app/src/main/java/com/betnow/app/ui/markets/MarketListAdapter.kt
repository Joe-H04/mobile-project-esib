package com.betnow.app.ui.markets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.betnow.app.R
import com.betnow.app.databinding.ItemMarketBinding
import com.betnow.app.network.models.Market
import com.betnow.app.util.UiFormatters
import com.bumptech.glide.Glide

class MarketListAdapter(
    private val onClick: (Market) -> Unit,
    private val onWatchlistToggle: (Market) -> Unit
) : ListAdapter<Market, MarketListAdapter.ViewHolder>(Diff) {

    private var watchlistIds: Set<String> = emptySet()

    fun setWatchlistIds(ids: Set<String>) {
        if (ids == watchlistIds) return
        watchlistIds = ids
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemMarketBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(market: Market) {
            val ctx = binding.root.context
            binding.marketQuestion.text = market.question

            val yes = market.outcomePrices.getOrNull(0) ?: 0.5
            val no = market.outcomePrices.getOrNull(1) ?: 0.5
            binding.yesPrice.text = UiFormatters.sidePrice("YES", yes)
            binding.noPrice.text = UiFormatters.sidePrice("NO", no)
            binding.marketVolume.text = ctx.getString(
                R.string.market_volume_short, UiFormatters.currencyFromString(market.volume)
            )

            binding.categoryChip.visibility = if (market.category.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.categoryChip.text = market.category.orEmpty()

            binding.resolvedBadge.visibility = if (market.resolved) View.VISIBLE else View.GONE
            if (market.resolved) {
                binding.resolvedBadge.text = ctx.getString(
                    if (market.winningOutcome == "YES") R.string.market_resolved_yes
                    else R.string.market_resolved_no
                )
            }

            val watched = market.id in watchlistIds
            binding.watchlistStar.setImageResource(
                if (watched) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            )
            binding.watchlistStar.contentDescription = ctx.getString(
                if (watched) R.string.remove_from_watchlist else R.string.add_to_watchlist
            )
            binding.watchlistStar.setOnClickListener { onWatchlistToggle(market) }

            if (market.image.isNotEmpty()) {
                Glide.with(binding.marketImage).load(market.image)
                    .placeholder(R.drawable.ic_markets).centerCrop()
                    .into(binding.marketImage)
            } else {
                binding.marketImage.setImageResource(R.drawable.ic_markets)
            }

            binding.root.setOnClickListener { onClick(market) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemMarketBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    private object Diff : DiffUtil.ItemCallback<Market>() {
        override fun areItemsTheSame(old: Market, new: Market) = old.id == new.id
        override fun areContentsTheSame(old: Market, new: Market) = old == new
    }
}
