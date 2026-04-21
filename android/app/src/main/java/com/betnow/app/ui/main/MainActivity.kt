package com.betnow.app.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.betnow.app.R
import com.betnow.app.databinding.ActivityMainBinding
import com.betnow.app.network.SocketManager
import com.betnow.app.ui.auth.LoginActivity
import com.betnow.app.ui.bets.MyBetsFragment
import com.betnow.app.ui.leaderboard.LeaderboardFragment
import com.betnow.app.ui.markets.MarketListFragment
import com.betnow.app.ui.profile.ProfileFragment
import com.betnow.app.ui.watchlist.WatchlistFragment
import com.betnow.app.util.TokenManager
import com.betnow.app.util.UiFormatters

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_logout) {
                logout(); true
            } else false
        }
        binding.toolbar.subtitle = UiFormatters.currency(TokenManager.getBalance(this))
        binding.toolbar.setSubtitleTextColor(resources.getColor(R.color.primary, theme))

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_markets -> MarketListFragment()
                R.id.nav_watchlist -> WatchlistFragment()
                R.id.nav_bets -> MyBetsFragment()
                R.id.nav_leaderboard -> LeaderboardFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> return@setOnItemSelectedListener false
            }
            loadFragment(fragment)
            true
        }

        if (savedInstanceState == null) loadFragment(MarketListFragment())
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun updateBalance(newBalance: Double) {
        TokenManager.updateBalance(this, newBalance)
        binding.toolbar.subtitle = UiFormatters.currency(newBalance)
    }

    private fun logout() {
        TokenManager.clear(this)
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    override fun onStart() {
        super.onStart()
        SocketManager.connect()
    }

    override fun onStop() {
        super.onStop()
        SocketManager.disconnect()
    }
}
