package com.betnow.app.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
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
            when (item.itemId) {
                R.id.action_logout -> {
                    TokenManager.clear(this)
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                    true
                }
                else -> false
            }
        }

        binding.toolbar.subtitle = UiFormatters.currency(TokenManager.getBalance(this))
        binding.toolbar.setSubtitleTextColor(resources.getColor(R.color.on_primary, theme))

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_markets -> {
                    loadFragment(MarketListFragment())
                    true
                }
                R.id.nav_watchlist -> {
                    loadFragment(WatchlistFragment())
                    true
                }
                R.id.nav_bets -> {
                    loadFragment(MyBetsFragment())
                    true
                }
                R.id.nav_leaderboard -> {
                    loadFragment(LeaderboardFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            loadFragment(MarketListFragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun updateBalance(newBalance: Double) {
        TokenManager.updateBalance(this, newBalance)
        binding.toolbar.subtitle = UiFormatters.currency(newBalance)
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
