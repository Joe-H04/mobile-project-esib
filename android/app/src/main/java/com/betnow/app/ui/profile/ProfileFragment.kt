package com.betnow.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.betnow.app.R
import com.betnow.app.databinding.FragmentProfileBinding
import com.betnow.app.network.models.UserStats
import com.betnow.app.ui.auth.LoginActivity
import com.betnow.app.ui.main.MainActivity
import com.betnow.app.ui.support.SupportActivity
import com.betnow.app.util.Resource
import com.betnow.app.util.TokenManager
import com.betnow.app.util.UiFormatters
import com.google.android.material.snackbar.Snackbar

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }

        binding.supportButton.setOnClickListener {
            startActivity(Intent(requireContext(), SupportActivity::class.java))
        }

        binding.logoutButton.setOnClickListener {
            TokenManager.clear(requireContext())
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            requireActivity().finish()
        }

        viewModel.me.observe(viewLifecycleOwner) { result ->
            binding.swipeRefresh.isRefreshing = false
            when (result) {
                is Resource.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.profileScroll.isVisible = false
                    binding.errorText.isVisible = false
                }
                is Resource.Success -> {
                    binding.progressBar.isVisible = false
                    binding.errorText.isVisible = false
                    binding.profileScroll.isVisible = true
                    binding.emailText.text = result.data.user.email
                    binding.balanceText.text = UiFormatters.currency(result.data.user.balance)
                    (activity as? MainActivity)?.updateBalance(result.data.user.balance)
                    renderStats(result.data.stats)
                }
                is Resource.Error -> {
                    binding.progressBar.isVisible = false
                    binding.profileScroll.isVisible = false
                    binding.errorText.isVisible = true
                    binding.errorText.text = result.message
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun renderStats(stats: UserStats) {
        val ctx = requireContext()
        binding.betsCountText.text = stats.betsCount.toString()
        binding.openBetsText.text = stats.openBets.toString()
        binding.wageredText.text = UiFormatters.currency(stats.wagered)
        binding.redeemedText.text = UiFormatters.currency(stats.redeemed)
        binding.winsText.text = stats.wins.toString()
        binding.profitText.text = UiFormatters.currency(stats.netProfit)
        binding.profitText.setTextColor(
            ctx.getColor(if (stats.netProfit >= 0) R.color.yes_green else R.color.no_red)
        )
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
