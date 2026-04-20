package com.betnow.app.ui.geo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.betnow.app.R
import com.betnow.app.databinding.ActivityGeoBlockBinding
import com.betnow.app.ui.auth.LoginActivity
import com.betnow.app.util.GeoRestrictionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class GeoBlockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGeoBlockBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGeoBlockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val country = intent.getStringExtra(EXTRA_COUNTRY)
        val closeOnly = intent.getBooleanExtra(EXTRA_CLOSE_ONLY, false)
        renderCountry(country, closeOnly)

        binding.geoBlockRetry.setOnClickListener { recheck() }
    }

    private fun renderCountry(code: String?, closeOnly: Boolean) {
        val display = code?.let { countryDisplayName(it) }
        binding.geoBlockCountry.text = when {
            display != null && closeOnly -> getString(R.string.geo_block_country_close_only, display)
            display != null -> getString(R.string.geo_block_country_label, display)
            else -> ""
        }
    }

    private fun recheck() {
        binding.geoBlockRetry.isEnabled = false
        lifecycleScope.launch {
            val status = withContext(Dispatchers.IO) { GeoRestrictionManager.check(this@GeoBlockActivity) }
            binding.geoBlockRetry.isEnabled = true
            when (status) {
                is GeoRestrictionManager.Status.Allowed -> {
                    startActivity(Intent(this@GeoBlockActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
                is GeoRestrictionManager.Status.Blocked -> {
                    renderCountry(status.countryCode, status.closeOnly)
                }
                GeoRestrictionManager.Status.PermissionMissing,
                GeoRestrictionManager.Status.LocationUnavailable -> {
                    binding.geoBlockCountry.text = getString(R.string.geo_block_location_unavailable)
                }
            }
        }
    }

    private fun countryDisplayName(code: String): String {
        val name = Locale("", code).getDisplayCountry(Locale.US)
        return if (name.isNullOrBlank()) code else name
    }

    companion object {
        const val EXTRA_COUNTRY = "extra_country"
        const val EXTRA_CLOSE_ONLY = "extra_close_only"

        fun newIntent(
            context: android.content.Context,
            countryCode: String?,
            closeOnly: Boolean
        ): Intent = Intent(context, GeoBlockActivity::class.java).apply {
            putExtra(EXTRA_COUNTRY, countryCode)
            putExtra(EXTRA_CLOSE_ONLY, closeOnly)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }
}
