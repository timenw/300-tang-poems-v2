package com.poem300.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
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

    private val audioDir: File
        get() = File(context.filesDir, AUDIO_DIR).also { it.mkdirs() }

    /** Get the audio file for a poem. Copies from assets if not cached locally. */
    private fun getAudioFile(poemId: Int): File? {
        val fileName = "poem_${poemId.toString().padStart(3, '0')}.mp3"
        val localFile = File(audioDir, fileName)

        if (localFile.exists() && localFile.length() > 0) {
            return localFile
        }

        // Copy from assets to internal storage
        return try {
            context.assets.open("$AUDIO_ASSETS_DIR/$fileName").use { input ->
                FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Copied from assets: $fileName (${localFile.length()} bytes)")
            localFile
        } catch (e: Exception) {
            Log.e(TAG, "Audio not in assets: $fileName", e)
            null
        }
    }

    /** Check if audio exists for a poem (in assets or cached locally). */
    fun hasAudio(poemId: Int): Boolean {
        val fileName = "poem_${poemId.toString().padStart(3, '0')}.mp3"
        val localFile = File(audioDir, fileName)
        if (localFile.exists() && localFile.length() > 0) return true
        // Check assets
        return try {
            context.assets.open("$AUDIO_ASSETS_DIR/$fileName").use { it.available() > 0 }
        } catch (e: Exception) {
            false
        }
    }

    /** Play audio for a poem. Returns true if playback started. */
    fun play(poemId: Int): Boolean {
        val file = getAudioFile(poemId) ?: run {
            Log.w(TAG, "Audio not available for poem $poemId")
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
            Log.e(TAG, "Play failed", e)
            return false
        }
    }

    /** Stop current playback. */
    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        currentPoemId = null
    }

    /** Check if currently playing. */
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    /** Get currently playing poem ID. */
    fun getCurrentPoemId(): Int? = currentPoemId

    /** Release resources. */
    fun release() {
        stop()
    }
}
