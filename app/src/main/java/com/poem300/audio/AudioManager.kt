package com.poem300.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

/**
 * Manages AI-generated poem audio from bundled assets.
 * Audio files are bundled at assets/audio/poem_001.mp3 etc.
 * On first play, they are copied to internal storage for MediaPlayer compatibility.
 * File naming: poem_001.mp3, poem_002.mp3, ...
 */
class AudioManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioManager"
        private const val AUDIO_ASSETS_DIR = "audio"
        private const val AUDIO_DIR = "audio"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var currentPoemId: Int? = null
    private var initError: String? = null

    private val audioDir: File
        get() = File(context.filesDir, AUDIO_DIR).also { it.mkdirs() }

    init {
        // Verify audio files exist in assets
        try {
            val assetFiles = context.assets.list(AUDIO_ASSETS_DIR) ?: emptyArray()
            Log.d(TAG, "Assets/$AUDIO_ASSETS_DIR contains ${assetFiles.size} files")
            if (assetFiles.isEmpty()) {
                initError = "No audio files found in assets/$AUDIO_ASSETS_DIR"
                Log.e(TAG, initError!!)
            } else {
                Log.d(TAG, "First files: ${assetFiles.take(5).joinToString()}")
            }
        } catch (e: Exception) {
            initError = "Cannot read assets/$AUDIO_ASSETS_DIR: ${e.message}"
            Log.e(TAG, initError!!, e)
        }
    }

    /** Show error toast to user */
    private fun showError(msg: String) {
        Log.e(TAG, msg)
        try {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}
    }

    /** Get the audio file for a poem. Copies from assets if not cached locally. */
    private fun getAudioFile(poemId: Int): File? {
        val fileName = "poem_${poemId.toString().padStart(3, '0')}.mp3"
        val localFile = File(audioDir, fileName)

        if (localFile.exists() && localFile.length() > 0) {
            return localFile
        }

        // Copy from assets to internal storage
        try {
            val assetPath = "$AUDIO_ASSETS_DIR/$fileName"
            context.assets.open(assetPath).use { input ->
                FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Copied $fileName: ${localFile.length()} bytes")
            if (localFile.length() > 0) {
                return localFile
            } else {
                showError("Audio file empty: $fileName")
                return null
            }
        } catch (e: Exception) {
            showError("No audio for poem $poemId: ${e.message}")
            return null
        }
    }

    /** Play audio for a poem. Returns true if playback started. */
    fun play(poemId: Int): Boolean {
        if (initError != null) {
            showError("Audio not available: $initError")
            return false
        }

        val file = getAudioFile(poemId)
        if (file == null) {
            showError("Cannot load audio for poem $poemId")
            return false
        }

        try {
            stop()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    currentPoemId = null
                }
                prepare()
                start()
            }
            currentPoemId = poemId
            Log.d(TAG, "Playing: ${file.name}")
            return true
        } catch (e: Exception) {
            showError("Play failed: ${e.message}")
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
