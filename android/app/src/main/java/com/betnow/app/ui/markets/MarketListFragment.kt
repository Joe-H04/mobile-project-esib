package com.betnow.app.ui.markets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.betnow.app.R
import com.betnow.app.databinding.FragmentMarketListBinding
import com.betnow.app.network.models.Category
import com.betnow.app.network.models.Market
import com.betnow.app.ui.detail.MarketDetailFragment
import com.betnow.app.util.Resource
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar

class MarketListFragment : Fragment() {

    private var _binding: FragmentMarketListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MarketListViewModel by viewModels()
    private lateinit var adapter: MarketListAdapter
    private var scrollToTopOnNext = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarketListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MarketListAdapter(
            onClick = { openDetail(it.id) },
            onWatchlistToggle = { viewModel.toggleWatchlist(it) }
        )

        binding.marketsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.marketsRecycler.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            scrollToTopOnNext = true
            viewModel.loadWatchlistIds()
            viewModel.loadMarkets()
        }

        binding.retryButton.setOnClickListener {
            scrollToTopOnNext = true
            viewModel.loadMarkets()
        }

        binding.searchInput.doAfterTextChanged {
            scrollToTopOnNext = true
            viewModel.onSearchChanged(it?.toString().orEmpty())
        }

        binding.sortChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val sort = when (checkedIds.firstOrNull()) {
                R.id.chip_sort_volume -> "volume"
                R.id.chip_sort_liquidity -> "liquidity"
                R.id.chip_sort_ending -> "ending"
                else -> return@setOnCheckedStateChangeListener
            }
            scrollToTopOnNext = true
            viewModel.onSortChanged(sort)
        }

        viewModel.categories.observe(viewLifecycleOwner, ::renderCategoryChips)
        viewModel.watchlistIds.observe(viewLifecycleOwner, adapter::setWatchlistIds)
        viewModel.markets.observe(viewLifecycleOwner, ::renderMarkets)
    }

    private fun renderMarkets(result: Resource<List<Market>>) {
        binding.swipeRefresh.isRefreshing = false
        when (result) {
            is Resource.Loading -> {
                binding.progressBar.isVisible = true
                binding.emptyState.isVisible = false
            }
            is Resource.Success -> {
                binding.progressBar.isVisible = false
                val empty = result.data.isEmpty()
                binding.marketsRecycler.isVisible = !empty
                binding.emptyState.isVisible = empty
                binding.retryButton.isVisible = false
                binding.emptyText.text = getString(R.string.no_markets)
                val shouldScroll = consumeScrollRequest()
                adapter.submitList(result.data) {
                    if (shouldScroll) _binding?.marketsRecycler?.scrollToPosition(0)
                }
            }
            is Resource.Error -> {
                consumeScrollRequest()
                binding.progressBar.isVisible = false
                binding.marketsRecycler.isVisible = false
                binding.emptyState.isVisible = true
                binding.emptyText.text = result.message
                binding.retryButton.isVisible = true
                Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun renderCategoryChips(categories: List<Category>) {
        val group = binding.categoryChipGroup
        group.removeAllViews()

        val sportsLabel = getString(R.string.category_sports)
        val names = buildList {
            add(getString(R.string.category_all))
            add(sportsLabel)
            addAll(categories.map { it.name }.filterNot { it.equals(sportsLabel, ignoreCase = true) })
        }
        names.forEachIndexed { index, name ->
            group.addView(Chip(requireContext()).apply {
                text = name
                isCheckable = true
                isChecked = name == viewModel.category || (index == 0 && viewModel.category == "All")
                setOnClickListener {
                    if (isChecked) {
                        scrollToTopOnNext = true
                        viewModel.onCategoryChanged(name)
                    }
                }
            })
        }
    }

    private fun consumeScrollRequest(): Boolean {
        val scroll = scrollToTopOnNext
        scrollToTopOnNext = false
        return scroll
    }

    private fun openDetail(marketId: String) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, MarketDetailFragment.newInstance(marketId))
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        viewModel.startOddsUpdates()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopOddsUpdates()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
