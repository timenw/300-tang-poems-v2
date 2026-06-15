package com.poem300

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd

/**
 * 开屏广告页
 * 初始化 AdMob SDK，加载并展示开屏广告，然后跳转到 MainActivity
 */
class SplashActivity : Activity() {

    companion object {
        private const val TAG = "SplashAd"

        // AdMob 开屏广告单元 ID
        private const val ADMOB_SLOT_ID = "ca-app-pub-1212786513185567/1371799713"
    }

    private var admobAd: AppOpenAd? = null
    private var hasNavigated = false
    private val handler = Handler(Looper.getMainLooper())

    // 超时兜底：5秒后一定进主页
    private val timeoutRunnable = Runnable {
        Log.w(TAG, "Ad load timeout, going to main")
        goToMain()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        // 设置超时兜底
        handler.postDelayed(timeoutRunnable, 5000)

        // 初始化 AdMob SDK
        MobileAds.initialize(this) { status ->
            Log.d(TAG, "AdMob initialized: $status")
            loadAdMobAd()
        }
    }

    private fun loadAdMobAd() {
        Log.d(TAG, "Loading AdMob splash ad...")
        try {
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                this,
                ADMOB_SLOT_ID,
                request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "AdMob ad load failed: ${error.message}")
                        goToMain()
                    }

                    override fun onAdLoaded(ad: AppOpenAd) {
                        Log.d(TAG, "AdMob ad loaded successfully")
                        handler.removeCallbacks(timeoutRunnable)
                        admobAd = ad
                        showAdMobAd()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "AdMob load exception: ${e.message}")
            goToMain()
        }
    }

    private fun showAdMobAd() {
        val ad = admobAd ?: run { goToMain(); return }
        try {
            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { goToMain() }
                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) { goToMain() }
                override fun onAdShowedFullScreenContent() { Log.d(TAG, "Ad showed") }
            }
            ad.show(this)
        } catch (e: Exception) {
            Log.e(TAG, "Ad show exception: ${e.message}")
            goToMain()
        }
    }

    private fun goToMain() {
        if (hasNavigated) return
        hasNavigated = true
        handler.removeCallbacks(timeoutRunnable)
        try {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Navigation failed: ${e.message}")
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeoutRunnable)
        admobAd = null
        super.onDestroy()
    }
}
