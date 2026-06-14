package com.poem300

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

/**
 * 开屏广告页
 * 展示 AdMob 开屏广告，加载失败或展示完毕后进入主页
 *
 * TODO: 替换以下广告位 ID 为真实值
 * - ADMOB_SLOT_ID: AdMob 开屏广告单元 ID
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    companion object {
        private const val TAG = "SplashAd"

        // ===== AdMob 广告单元 ID =====
        // TODO: 在 AdMob 后台创建开屏广告单元后填入
        private const val ADMOB_SLOT_ID = "YOUR_ADMOB_SPLASH_AD_UNIT_ID"

        // 最大等待广告时间（毫秒），超时直接进主页
        private const val MAX_WAIT_MS = 5000L
    }

    private var admobAd: AppOpenAd? = null
    private var hasNavigated = false
    private val handler = Handler(Looper.getMainLooper())

    // 超时兜底：无论广告是否加载，5秒后一定进主页
    private val timeoutRunnable = Runnable {
        Log.w(TAG, "Ad load timeout, going to main")
        goToMain()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置超时兜底
        handler.postDelayed(timeoutRunnable, MAX_WAIT_MS)

        // 加载 AdMob 开屏广告
        loadAdMobAd()
    }

    /**
     * 加载 AdMob 开屏广告
     */
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
                        // 加载失败，直接进入主页
                        goToMain()
                    }

                    override fun onAdLoaded(ad: AppOpenAd) {
                        Log.d(TAG, "AdMob ad loaded successfully")
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

    /**
     * 展示 AdMob 开屏广告
     */
    private fun showAdMobAd() {
        val ad = admobAd ?: run {
            goToMain()
            return
        }

        try {
            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "AdMob ad dismissed")
                    goToMain()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    Log.w(TAG, "AdMob ad show failed: ${error.message}")
                    goToMain()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "AdMob ad showed")
                }
            }

            ad.show(this)
        } catch (e: Exception) {
            Log.e(TAG, "AdMob show exception: ${e.message}")
            goToMain()
        }
    }

    /**
     * 跳转到主页
     */
    private fun goToMain() {
        if (hasNavigated) return
        hasNavigated = true

        // 取消超时任务
        handler.removeCallbacks(timeoutRunnable)

        Log.d(TAG, "Navigating to MainActivity")
        try {
            startActivity(Intent(this, MainActivity::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Navigation failed: ${e.message}")
        }
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeoutRunnable)
        admobAd = null
        super.onDestroy()
    }
}
