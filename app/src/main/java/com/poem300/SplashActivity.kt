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
 * 开屏页 - 最小广告展示测试
 * 初始化 → 加载 → 展示 → 用户关闭后跳转
 */
class SplashActivity : Activity() {

    companion object {
        private const val TAG = "SplashAd"
        private const val ADMOB_SLOT_ID = "ca-app-pub-1212786513185567/1371799713"
    }

    private var hasNavigated = false
    private val handler = Handler(Looper.getMainLooper())

    private val timeoutRunnable = Runnable {
        Log.w(TAG, "timeout")
        goToMain()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        handler.postDelayed(timeoutRunnable, 8000)

        MobileAds.initialize(this) { status ->
            Log.d(TAG, "init: $status")
            loadAd()
        }
    }

    private fun loadAd() {
        Log.d(TAG, "loadAd")
        try {
            AppOpenAd.load(
                this,
                ADMOB_SLOT_ID,
                AdRequest.Builder().build(),
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdFailedToLoad(e: LoadAdError) {
                        Log.w(TAG, "load failed: ${e.message}")
                        goToMain()
                    }
                    override fun onAdLoaded(ad: AppOpenAd) {
                        Log.d(TAG, "loaded, showing...")
                        handler.removeCallbacks(timeoutRunnable)
                        showAd(ad)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "load crash: ${e.message}")
            goToMain()
        }
    }

    private fun showAd(ad: AppOpenAd) {
        Log.d(TAG, "showAd enter")
        try {
            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "dismissed")
                    goToMain()
                }
                override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) {
                    Log.w(TAG, "show failed: ${e.message}")
                    goToMain()
                }
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "showed ok")
                }
            }
            Log.d(TAG, "calling ad.show()...")
            ad.show(this)
            Log.d(TAG, "ad.show() returned")
        } catch (e: Exception) {
            Log.e(TAG, "show crash: ${e.message}")
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
            Log.e(TAG, "nav fail: ${e.message}")
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeoutRunnable)
        super.onDestroy()
    }
}
