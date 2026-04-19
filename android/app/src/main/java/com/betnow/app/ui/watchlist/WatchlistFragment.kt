package com.betnow.app.ui.watchlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.betnow.app.R
import com.betnow.app.databinding.FragmentWatchlistBinding
import com.betnow.app.ui.detail.MarketDetailFragment
import com.betnow.app.ui.markets.MarketListAdapter
import com.betnow.app.util.Resource
import com.google.android.material.snackbar.Snackbar

class WatchlistFragment : Fragment() {

    private var _binding: FragmentWatchlistBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WatchlistViewModel by viewModels()
    private lateinit var adapter: MarketListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWatchlistBinding.inflate(inflater, container, false)
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
                viewModel.remove(market)
            }
        )

        binding.watchlistRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.watchlistRecycler.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadWatchlist()
        }

        viewModel.markets.observe(viewLifecycleOwner) { result ->
            binding.swipeRefresh.isRefreshing = false
            when (result) {
                is Resource.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.emptyState.isVisible = false
                    binding.watchlistRecycler.isVisible = false
                }
                is Resource.Success -> {
                    binding.progressBar.isVisible = false
                    val ids = result.data.map { it.id }.toSet()
                    adapter.setWatchlistIds(ids)
                    if (result.data.isEmpty()) {
                        binding.watchlistRecycler.isVisible = false
                        binding.emptyState.isVisible = true
                    } else {
                        binding.emptyState.isVisible = false
                        binding.watchlistRecycler.isVisible = true
                        adapter.submitList(result.data)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.isVisible = false
                    binding.watchlistRecycler.isVisible = false
                    binding.emptyState.isVisible = true
                    binding.emptyText.text = result.message
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadWatchlist()
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
