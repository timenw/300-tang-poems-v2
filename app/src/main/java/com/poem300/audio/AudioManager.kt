package com.poem300.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

/**
 * Manages AI-generated poem audio from res/raw.
 * Audio files: res/raw/poem_001.mp3, poem_002.mp3, ...
 */
class AudioManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioManager"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var currentPoemId: Int? = null

    /** Get resource ID for a poem's audio file */
    private fun getRawResId(poemId: Int): Int {
        val name = "poem_${poemId.toString().padStart(3, '0')}"
        return context.resources.getIdentifier(name, "raw", context.packageName)
    }

    /** Play audio for a poem. Returns true if playback started. */
    fun play(poemId: Int): Boolean {
        val resId = getRawResId(poemId)
        Log.d(TAG, "play poemId=$poemId resId=$resId")

        if (resId == 0) {
            val msg = "No audio for poem $poemId (resId=0)"
            Log.e(TAG, msg)
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            return false
        }

        try {
            stop()
            // Copy raw resource to a temp file for MediaPlayer
            val tmpFile = File(context.cacheDir, "play_tmp.mp3")
            context.resources.openRawResource(resId).use { input ->
                FileOutputStream(tmpFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Copied ${tmpFile.length()} bytes for poem $poemId")

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tmpFile.absolutePath)
                setOnCompletionListener {
                    currentPoemId = null
                }
                prepare()
                start()
            }
            currentPoemId = poemId
            Log.d(TAG, "Playing poem $poemId")
            return true
        } catch (e: Exception) {
            val msg = "Play failed: ${e.message}"
            Log.e(TAG, msg, e)
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            return false
        }
    }

    /** Stop current playback. */
    fun stop() {
        try {
            mediaPlayer?.let {
                try { if (it.isPlaying) it.stop() } catch (_: Exception) {}
                it.release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        currentPoemId = null
    }

    /** Release resources. */
    fun release() {
        stop()
    }
}
