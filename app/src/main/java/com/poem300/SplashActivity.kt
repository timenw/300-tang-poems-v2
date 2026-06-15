package com.poem300

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.MobileAds

/**
 * 开屏页 - 初始化 AdMob SDK，但不加载广告
 * 用于测试：排查 MobileAds.initialize() 是否导致闪退
 */
class SplashActivity : Activity() {

    companion object {
        private const val TAG = "Splash"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate - initializing AdMob SDK only")

        // 初始化 AdMob SDK（不加载广告）
        try {
            MobileAds.initialize(this) { status ->
                Log.d(TAG, "AdMob initialized: $status")
            }
        } catch (e: Exception) {
            Log.e(TAG, "AdMob init failed: ${e.message}")
        }

        // 延迟 1 秒后跳转
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start MainActivity: ${e.message}")
            }
        }, 1000)
    }
}
