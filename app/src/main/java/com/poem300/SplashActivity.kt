package com.poem300

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.pangle.global.ads.PAGAppDownloadListener
import com.pangle.global.ads.PAGLInitConfig
import com.pangle.global.ads.PangleGlobal
import com.pangle.global.ads.PAGRequest
import com.pangle.global.ads.open.PAGAppOpenAd
import com.pangle.global.ads.open.PAGAppOpenAdLoadListener
import com.pangle.global.ads.open.PAGAppOpenRequest

/**
 * 开屏广告页
 * 优先展示穿山甲开屏广告，失败时降级到 AdMob，再失败则直接进入主页
 *
 * TODO: 替换以下广告位 ID 为真实值
 * - PANGLE_SLOT_ID: 穿山甲开屏广告 Slot ID
 * - ADMOB_SLOT_ID: AdMob 开屏广告单元 ID
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    companion object {
        private const val TAG = "SplashAd"

        // ===== 穿山甲广告位 ID =====
        // TODO: 在穿山甲后台创建开屏广告位后填入
        private const val PANGLE_SLOT_ID = "YOUR_PANGLE_SPLASH_SLOT_ID"

        // ===== AdMob 广告单元 ID =====
        // TODO: 在 AdMob 后台创建开屏广告单元后填入
        private const val ADMOB_SLOT_ID = "YOUR_ADMOB_SPLASH_AD_UNIT_ID"

        // 广告加载超时时间（毫秒）
        private const val AD_TIMEOUT_MS = 5000L
    }

    private var pangleAd: PAGAppOpenAd? = null
    private var admobAd: AppOpenAd? = null
    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 初始化穿山甲 SDK
        initPangle()

        // 开始加载广告
        loadPangleAd()
    }

    /**
     * 初始化穿山甲 SDK
     */
    private fun initPangle() {
        try {
            val config = PAGLInitConfig().apply {
                // TODO: 替换为穿山甲后台的真实 App ID
                appId = "YOUR_PANGLE_APP_ID"
                // 测试阶段开启日志，上线后关闭
                debugLog = BuildConfig.DEBUG
                // 允许直接下载类广告（需要 READ_PHONE_STATE 权限）
                directDownloadNetworkType = PAGLInitConfig.NETWORK_STATE_MOBILE or
                        PAGLInitConfig.NETWORK_STATE_WIFI
            }
            PangleGlobal.init(this, config)
            Log.d(TAG, "Pangle SDK initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Pangle init failed: ${e.message}")
        }
    }

    /**
     * 第一步：尝试加载穿山甲开屏广告
     */
    private fun loadPangleAd() {
        Log.d(TAG, "Loading Pangle splash ad...")

        val request = PAGRequest().apply {
            setAdId(PANGLE_SLOT_ID)
        }
        val adRequest = PAGAppOpenRequest(request)

        PAGAppOpenAd.loadAd(PANGLE_SLOT_ID, adRequest, object : PAGAppOpenAdLoadListener() {
            override fun onError(p0: Int, p1: String?) {
                Log.w(TAG, "Pangle ad load failed: code=$p0, msg=$p1")
                // 穿山甲失败，降级到 AdMob
                loadAdMobAd()
            }

            override fun onAdLoad(p0: PAGAppOpenAd?) {
                Log.d(TAG, "Pangle ad loaded successfully")
                pangleAd = p0
                showPangleAd()
            }
        })
    }

    /**
     * 展示穿山甲开屏广告
     */
    private fun showPangleAd() {
        val ad = pangleAd ?: run {
            loadAdMobAd()
            return
        }

        ad.setAdInteractionListener(object : PAGAppOpenAd.PAGAppOpenAdInteractionListener() {
            override fun onAdShowed() {
                Log.d(TAG, "Pangle ad showed")
            }

            override fun onAdClicked() {
                Log.d(TAG, "Pangle ad clicked")
            }

            override fun onAdDismissed() {
                Log.d(TAG, "Pangle ad dismissed")
                goToMain()
            }
        })

        ad.showAd(this)
    }

    /**
     * 第二步：降级加载 AdMob 开屏广告
     */
    private fun loadAdMobAd() {
        Log.d(TAG, "Loading AdMob splash ad...")

        val request = AdRequest.Builder().build()

        AppOpenAd.load(
            this,
            ADMOB_SLOT_ID,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "AdMob ad load failed: ${error.message}")
                    // 两个平台都失败，直接进入主页
                    goToMain()
                }

                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "AdMob ad loaded successfully")
                    admobAd = ad
                    showAdMobAd()
                }
            }
        )
    }

    /**
     * 展示 AdMob 开屏广告
     */
    private fun showAdMobAd() {
        val ad = admobAd ?: run {
            goToMain()
            return
        }

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
    }

    /**
     * 跳转到主页
     */
    private fun goToMain() {
        if (hasNavigated) return
        hasNavigated = true

        Log.d(TAG, "Navigating to MainActivity")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        pangleAd?.destroy()
    }
}
