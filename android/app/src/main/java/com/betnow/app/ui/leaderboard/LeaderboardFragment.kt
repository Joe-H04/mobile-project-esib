package com.betnow.app.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.betnow.app.databinding.FragmentLeaderboardBinding
import com.betnow.app.util.Resource
import com.google.android.material.snackbar.Snackbar

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LeaderboardViewModel by viewModels()
    private val adapter = LeaderboardAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.leaderboardRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.leaderboardRecycler.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }

        viewModel.entries.observe(viewLifecycleOwner) { result ->
            binding.swipeRefresh.isRefreshing = false
            when (result) {
                is Resource.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.emptyText.isVisible = false
                    binding.leaderboardRecycler.isVisible = false
                }
                is Resource.Success -> {
                    binding.progressBar.isVisible = false
                    val empty = result.data.isEmpty()
                    binding.emptyText.isVisible = empty
                    binding.leaderboardRecycler.isVisible = !empty
                    if (!empty) adapter.submitList(result.data)
                }
                is Resource.Error -> {
                    binding.progressBar.isVisible = false
                    binding.emptyText.isVisible = true
                    binding.emptyText.text = result.message
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
