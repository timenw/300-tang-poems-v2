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

    init {
        // Log available audio files in assets for debugging
        try {
            val assetFiles = context.assets.list(AUDIO_ASSETS_DIR) ?: emptyArray()
            Log.d(TAG, "Assets audio dir contains ${assetFiles.size} files")
            assetFiles.take(5).forEach { Log.d(TAG, "  asset: $it") }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list assets/$AUDIO_ASSETS_DIR", e)
        }
    }

    /** Get the audio file for a poem. Copies from assets if not cached locally. */
    private fun getAudioFile(poemId: Int): File? {
        val fileName = "poem_${poemId.toString().padStart(3, '0')}.mp3"
        val localFile = File(audioDir, fileName)

        if (localFile.exists() && localFile.length() > 0) {
            Log.d(TAG, "Found cached: $fileName (${localFile.length()} bytes)")
            return localFile
        }

        // Copy from assets to internal storage
        try {
            val assetPath = "$AUDIO_ASSETS_DIR/$fileName"
            Log.d(TAG, "Copying from assets: $assetPath")
            val input = context.assets.open(assetPath)
            val output = FileOutputStream(localFile)
            val buffer = ByteArray(8192)
            var total = 0
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
                total += read
            }
            output.flush()
            output.close()
            input.close()
            Log.d(TAG, "Copied: $fileName (${localFile.length()} bytes, wrote $total)")
            if (localFile.length() > 0) {
                return localFile
            } else {
                Log.e(TAG, "Copied file is empty!")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy from assets: $fileName", e)
            return null
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
            val mp = MediaPlayer()
            mp.setDataSource(file.absolutePath)
            mp.prepare()
            mp.start()
            mp.setOnCompletionListener {
                currentPoemId = null
            }
            mediaPlayer = mp
            currentPoemId = poemId
            Log.d(TAG, "Playing: ${file.name} (${file.length()} bytes) path=${file.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Play failed for poem $poemId", e)
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

    /** Check if currently playing. */
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    /** Get currently playing poem ID. */
    fun getCurrentPoemId(): Int? = currentPoemId

    /** Release resources. */
    fun release() {
        stop()
    }
}
