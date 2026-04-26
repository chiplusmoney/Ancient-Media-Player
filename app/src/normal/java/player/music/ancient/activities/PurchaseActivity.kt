package player.music.ancient.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import player.music.ancient.util.MaterialUtil
import player.music.ancient.App
import player.music.ancient.R
import player.music.ancient.activities.base.AbsThemeActivity
import player.music.ancient.billing.BillingManager
import player.music.ancient.databinding.ActivityProVersionBinding
import player.music.ancient.extensions.accentColor
import player.music.ancient.extensions.setLightStatusBar
import player.music.ancient.extensions.setStatusBarColor
import player.music.ancient.extensions.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PurchaseActivity : AbsThemeActivity() {

    private lateinit var binding: ActivityProVersionBinding
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProVersionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setStatusBarColor(Color.TRANSPARENT)
        setLightStatusBar(false)
        binding.toolbar.navigationIcon?.setTint(Color.WHITE)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        billingManager = App.getContext().billingManager

        MaterialUtil.setTint(binding.purchaseButton, true)

        binding.restoreButton.setOnClickListener {
            restorePurchase()
        }
        binding.purchaseButton.setOnClickListener {
            billingManager.launchBillingFlow(this@PurchaseActivity)
        }
        binding.bannerContainer.backgroundTintList =
            ColorStateList.valueOf(accentColor())
    }

    private fun restorePurchase() {
        billingManager.restorePurchases {
            if (App.isProVersion()) {
                showToast(R.string.restored_previous_purchase_please_restart)
                setResult(RESULT_OK)
            } else {
                showToast(R.string.no_purchase_found)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        billingManager.release()
        super.onDestroy()
    }
}