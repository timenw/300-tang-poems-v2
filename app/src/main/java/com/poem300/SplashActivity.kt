package com.poem300

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity

/**
 * 开屏页 - 无广告版本（待后续添加广告）
 * 直接跳转到 MainActivity
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    companion object {
        private const val TAG = "SplashAd"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "SplashActivity created, navigating to MainActivity")

        // 直接跳转到主页
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
