package com.poem300

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * 开屏页 - 纯跳转，无广告初始化
 * 用于测试：保留 AdMob SDK 依赖和 meta-data，但不调用任何广告 API
 */
class SplashActivity : Activity() {

    companion object {
        private const val TAG = "Splash"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate - navigating to MainActivity (no ad init)")

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
