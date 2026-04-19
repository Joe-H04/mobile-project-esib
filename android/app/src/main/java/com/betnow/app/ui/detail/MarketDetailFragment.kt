package com.betnow.app.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.betnow.app.R
import com.betnow.app.databinding.FragmentMarketDetailBinding
import com.betnow.app.network.models.Market
import com.betnow.app.ui.main.MainActivity
import com.betnow.app.util.Resource
import com.betnow.app.util.UiFormatters
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar

class MarketDetailFragment : Fragment() {

    private var _binding: FragmentMarketDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MarketDetailViewModel by viewModels()

    private val marketId: String by lazy {
        requireArguments().getString(ARG_MARKET_ID).orEmpty()
    }

    private var currentMarket: Market? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarketDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.chipYes.isChecked = true
        binding.retryButton.setOnClickListener { viewModel.loadMarket(marketId) }
        binding.sideChipGroup.setOnCheckedChangeListener { _, _ -> updateSharesPreview() }
        binding.amountInput.doAfterTextChanged {
            binding.amountInputLayout.error = null
            updateSharesPreview()
        }
        binding.placeBetButton.setOnClickListener { submitBet() }
        binding.detailWatchlistStar.setOnClickListener {
            viewModel.toggleWatchlist(marketId)
        }

        viewModel.isWatching.observe(viewLifecycleOwner) { watching ->
            binding.detailWatchlistStar.setImageResource(
                if (watching) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            )
            binding.detailWatchlistStar.contentDescription = getString(
                if (watching) R.string.remove_from_watchlist else R.string.add_to_watchlist
            )
        }

        viewModel.market.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> renderLoading()
                is Resource.Success -> renderMarket(result.data)
                is Resource.Error -> renderError(result.message)
            }
        }

        viewModel.betResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                null -> setBetLoading(false)
                is Resource.Loading -> setBetLoading(true)
                is Resource.Success -> {
                    setBetLoading(false)
                    binding.amountInput.text?.clear()
                    (activity as? MainActivity)?.updateBalance(result.data.newBalance)
                    Snackbar.make(
                        binding.root,
                        getString(
                            R.string.bet_success,
                            UiFormatters.currency(result.data.bet.totalCost),
                            result.data.bet.side
                        ),
                        Snackbar.LENGTH_LONG
                    ).show()
                    viewModel.clearBetResult()
                }
                is Resource.Error -> {
                    setBetLoading(false)
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                    viewModel.clearBetResult()
                }
            }
        }

        if (viewModel.market.value == null) {
            viewModel.loadMarket(marketId)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.observePriceUpdates(marketId)
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopPriceUpdates()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun renderLoading() {
        binding.detailProgress.isVisible = true
        binding.errorState.isVisible = false
        binding.contentScroll.isVisible = false
    }

    private fun renderMarket(market: Market) {
        currentMarket = market
        binding.detailProgress.isVisible = false
        binding.errorState.isVisible = false
        binding.contentScroll.isVisible = true

        binding.detailQuestion.text = market.question
        binding.detailDescription.text = market.description
        binding.yesPrice.text = UiFormatters.cents(market.outcomePrices.getOrNull(0) ?: 0.0)
        binding.noPrice.text = UiFormatters.cents(market.outcomePrices.getOrNull(1) ?: 0.0)
        binding.volumeText.text = getString(
            R.string.market_volume_full,
            UiFormatters.currencyFromString(market.volume)
        )
        binding.endDateText.text = getString(
            R.string.market_end_date,
            UiFormatters.dateTime(market.endDate)
        )

        val betEnabled = market.active && !market.closed && !market.resolved
        binding.sideChipGroup.isEnabled = betEnabled
        binding.chipYes.isEnabled = betEnabled
        binding.chipNo.isEnabled = betEnabled
        binding.amountInputLayout.isEnabled = betEnabled
        binding.placeBetButton.isEnabled = betEnabled
        if (!betEnabled) {
            binding.sharesPreview.text = getString(R.string.market_closed)
        } else {
            updateSharesPreview()
        }

        if (market.resolved) {
            binding.detailResolvedBanner.visibility = View.VISIBLE
            binding.detailResolvedBanner.text = if (market.winningOutcome == "YES") {
                getString(R.string.market_resolved_yes)
            } else {
                getString(R.string.market_resolved_no)
            }
        } else {
            binding.detailResolvedBanner.visibility = View.GONE
        }

        if (market.image.isNotBlank()) {
            Glide.with(this)
                .load(market.image)
                .placeholder(R.drawable.ic_markets)
                .centerCrop()
                .into(binding.detailImage)
        } else {
            binding.detailImage.setImageResource(R.drawable.ic_markets)
        }
    }

    private fun renderError(message: String) {
        currentMarket = null
        binding.detailProgress.isVisible = false
        binding.contentScroll.isVisible = false
        binding.errorState.isVisible = true
        binding.errorText.text = message
    }

    private fun submitBet() {
        val market = currentMarket ?: return
        val amount = binding.amountInput.text?.toString()?.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            binding.amountInputLayout.error = getString(R.string.amount_required)
            return
        }

        val side = selectedSide()
        if (side == null) {
            return
        }

        viewModel.placeBet(market.id, side, amount)
    }

    private fun updateSharesPreview() {
        val market = currentMarket
        val side = selectedSide()
        val amount = binding.amountInput.text?.toString()?.toDoubleOrNull()

        when {
            market == null -> binding.sharesPreview.text = getString(R.string.shares_preview_placeholder)
            side == null || amount == null || amount <= 0.0 ->
                binding.sharesPreview.text = getString(R.string.shares_preview_placeholder)
            else -> {
                val price = priceForSide(market, side)
                if (price == null || price <= 0.0) {
                    binding.sharesPreview.text = getString(R.string.shares_preview_unavailable, side)
                } else {
                    val shares = amount / price
                    binding.sharesPreview.text = getString(
                        R.string.shares_preview_value,
                        UiFormatters.shares(shares),
                        UiFormatters.sidePrice(side, price)
                    )
                }
            }
        }
    }

    private fun selectedSide(): String? = when (binding.sideChipGroup.checkedChipId) {
        R.id.chip_yes -> "YES"
        R.id.chip_no -> "NO"
        else -> null
    }

    private fun priceForSide(market: Market, side: String): Double? = when (side) {
        "YES" -> market.outcomePrices.getOrNull(0)
        "NO" -> market.outcomePrices.getOrNull(1)
        else -> null
    }

    private fun setBetLoading(isLoading: Boolean) {
        binding.betProgress.isVisible = isLoading
        val market = currentMarket
        val betEnabled = market != null && market.active && !market.closed && !market.resolved
        if (betEnabled) {
            binding.placeBetButton.isEnabled = !isLoading
            binding.sideChipGroup.isEnabled = !isLoading
            binding.chipYes.isEnabled = !isLoading
            binding.chipNo.isEnabled = !isLoading
            binding.amountInputLayout.isEnabled = !isLoading
        }
    }

    companion object {
        private const val ARG_MARKET_ID = "market_id"

        fun newInstance(marketId: String): MarketDetailFragment =
            MarketDetailFragment().apply {
                arguments = bundleOf(ARG_MARKET_ID to marketId)
            }
    }
}
