package com.betnow.app.ui.markets

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.betnow.app.R
import com.betnow.app.databinding.FragmentMarketListBinding
import com.betnow.app.ui.detail.MarketDetailFragment
import com.betnow.app.util.Resource
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar

class MarketListFragment : Fragment() {

    private var _binding: FragmentMarketListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MarketListViewModel by viewModels()
    private lateinit var adapter: MarketListAdapter
    private var shouldScrollToTopOnNextResults = false
    private var hasPositionedInitialCategoryScroll = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarketListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MarketListAdapter(
            onClick = { market ->
                val fragment = MarketDetailFragment.newInstance(market.id)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onWatchlistToggle = { market ->
                viewModel.toggleWatchlist(market)
            }
        )

        binding.marketsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.marketsRecycler.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            requestResultsScrollToTop()
            viewModel.loadWatchlistIds()
            viewModel.loadMarkets()
        }

        binding.retryButton.setOnClickListener {
            requestResultsScrollToTop()
            viewModel.loadMarkets()
        }

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                requestResultsScrollToTop()
                viewModel.onSearchChanged(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.sortChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val id = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val sort = when (id) {
                R.id.chip_sort_volume -> "volume"
                R.id.chip_sort_liquidity -> "liquidity"
                R.id.chip_sort_ending -> "ending"
                else -> ""
            }
            requestResultsScrollToTop()
            viewModel.onSortChanged(sort)
        }

        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            renderCategoryChips(categories.map { it.name })
            if (categories.isNotEmpty() && !hasPositionedInitialCategoryScroll) {
                scrollCategoryChipsToStart()
                hasPositionedInitialCategoryScroll = true
            }
        }

        viewModel.watchlistIds.observe(viewLifecycleOwner) { ids ->
            adapter.setWatchlistIds(ids)
        }

        viewModel.markets.observe(viewLifecycleOwner) { result ->
            binding.swipeRefresh.isRefreshing = false
            when (result) {
                is Resource.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.emptyState.isVisible = false
                }
                is Resource.Success -> {
                    binding.progressBar.isVisible = false
                    if (result.data.isEmpty()) {
                        consumeResultsScrollToTop()
                        binding.marketsRecycler.isVisible = false
                        binding.emptyState.isVisible = true
                        binding.emptyText.text = getString(R.string.no_markets)
                        binding.retryButton.isVisible = false
                    } else {
                        binding.emptyState.isVisible = false
                        binding.marketsRecycler.isVisible = true
                        val shouldScroll = consumeResultsScrollToTop()
                        adapter.submitList(result.data) {
                            if (shouldScroll) {
                                binding.marketsRecycler.scrollToPosition(0)
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    consumeResultsScrollToTop()
                    binding.progressBar.isVisible = false
                    binding.marketsRecycler.isVisible = false
                    binding.emptyState.isVisible = true
                    binding.emptyText.text = result.message
                    binding.retryButton.isVisible = true
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun renderCategoryChips(categories: List<String>) {
        val group = binding.categoryChipGroup
        group.removeAllViews()

        val names = buildList {
            add(getString(R.string.category_all))
            add(getString(R.string.category_sports))
            addAll(
                categories.filterNot {
                    it.equals(getString(R.string.category_sports), ignoreCase = true)
                }
            )
        }
        names.forEachIndexed { index, name ->
            val chip = Chip(requireContext()).apply {
                text = name
                isCheckable = true
                isChecked = (name == viewModel.category) || (index == 0 && viewModel.category == "All")
                setOnClickListener {
                    if (isChecked) {
                        requestResultsScrollToTop()
                        viewModel.onCategoryChanged(name)
                    }
                }
            }
            group.addView(chip)
        }
    }

    private fun scrollCategoryChipsToStart() {
        binding.categoryScroll.post {
            val direction = if (binding.root.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                View.FOCUS_RIGHT
            } else {
                View.FOCUS_LEFT
            }
            binding.categoryScroll.fullScroll(direction)
        }
    }

    private fun requestResultsScrollToTop() {
        shouldScrollToTopOnNextResults = true
    }

    private fun consumeResultsScrollToTop(): Boolean {
        val shouldScroll = shouldScrollToTopOnNextResults
        shouldScrollToTopOnNextResults = false
        return shouldScroll
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
        hasPositionedInitialCategoryScroll = false
        _binding = null
    }
}
