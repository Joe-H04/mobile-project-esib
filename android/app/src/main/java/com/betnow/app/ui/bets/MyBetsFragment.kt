package com.betnow.app.ui.bets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.betnow.app.R
import com.betnow.app.databinding.FragmentMyBetsBinding
import com.betnow.app.ui.main.MainActivity
import com.betnow.app.util.Resource
import com.betnow.app.util.UiFormatters
import com.google.android.material.snackbar.Snackbar

class MyBetsFragment : Fragment() {

    private var _binding: FragmentMyBetsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyBetsViewModel by viewModels()
    private lateinit var adapter: MyBetsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyBetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MyBetsAdapter { bet ->
            viewModel.redeem(bet.id)
        }
        binding.betsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.betsRecycler.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadBets()
        }

        binding.retryButton.setOnClickListener {
            viewModel.loadBets()
        }

        viewModel.bets.observe(viewLifecycleOwner) { result ->
            binding.swipeRefresh.isRefreshing = false
            when (result) {
                is Resource.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.betsRecycler.isVisible = false
                    binding.emptyState.isVisible = false
                }
                is Resource.Success -> {
                    binding.progressBar.isVisible = false
                    if (result.data.isEmpty()) {
                        binding.betsRecycler.isVisible = false
                        binding.emptyState.isVisible = true
                        binding.emptyText.text = getString(R.string.no_bets)
                        binding.retryButton.isVisible = false
                    } else {
                        binding.emptyState.isVisible = false
                        binding.betsRecycler.isVisible = true
                        adapter.submitList(result.data)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.isVisible = false
                    binding.betsRecycler.isVisible = false
                    binding.emptyState.isVisible = true
                    binding.emptyText.text = result.message
                    binding.retryButton.isVisible = true
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.redeemResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                null -> Unit
                is Resource.Loading -> Unit
                is Resource.Success -> {
                    (activity as? MainActivity)?.updateBalance(result.data.newBalance)
                    val msg = if (result.data.won) {
                        getString(
                            R.string.redeem_success_win,
                            UiFormatters.currency(result.data.payout)
                        )
                    } else {
                        getString(R.string.redeem_success_lose)
                    }
                    Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                    viewModel.clearRedeemResult()
                }
                is Resource.Error -> {
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                    viewModel.clearRedeemResult()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadBets()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
