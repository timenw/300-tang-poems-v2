package com.poem300.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

/**
 * Manages AI-generated poem audio from bundled assets.
 * Audio files: assets/audio/poem_001.mp3, poem_002.mp3, ...
 * Files are copied to cache on first play for MediaPlayer compatibility.
 */
class AudioManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioManager"
        private const val AUDIO_ASSETS_PATH = "audio"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var currentPoemId: Int? = null
    var onPlaybackComplete: (() -> Unit)? = null

    /** Get the audio file for a poem. Copies from assets to cache if needed. */
    private fun getAudioFile(poemId: Int): File? {
        val fileName = "poem_${poemId.toString().padStart(3, '0')}.mp3"
        val cacheFile = File(context.cacheDir, fileName)

        if (cacheFile.exists() && cacheFile.length() > 100) {
            return cacheFile
        }

        return try {
            val assetPath = "$AUDIO_ASSETS_PATH/$fileName"
            context.assets.open(assetPath).use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Copied from assets: $fileName (${cacheFile.length()} bytes)")
            cacheFile
        } catch (e: Exception) {
            Log.e(TAG, "Cannot load audio: $fileName from $AUDIO_ASSETS_PATH", e)
            null
        }
    }

    /** Play audio for a poem. Returns true if playback started. */
    fun play(poemId: Int): Boolean {
        val file = getAudioFile(poemId)
        if (file == null) {
            val msg = "No audio for poem $poemId"
            Log.e(TAG, msg)
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            return false
        }

        try {
            stop()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    currentPoemId = null
                    onPlaybackComplete?.invoke()
                }
                prepare()
                start()
            }
            currentPoemId = poemId
            Log.d(TAG, "Playing: ${file.name} (${file.length()} bytes)")
            Toast.makeText(context, "Playing: ${file.name}", Toast.LENGTH_SHORT).show()
            return true
        } catch (e: Exception) {
            val msg = "Play failed: ${e.message}"
            Log.e(TAG, msg, e)
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
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
