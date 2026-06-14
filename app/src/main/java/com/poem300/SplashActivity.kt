package com.poem300

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * 开屏页 - 纯跳转，无广告
 */
class SplashActivity : Activity() {

    companion object {
        private const val TAG = "Splash"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate - navigating to MainActivity")

        // 延迟 1 秒后跳转，让系统完成初始化
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
