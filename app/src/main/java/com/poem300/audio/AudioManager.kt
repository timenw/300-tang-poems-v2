package com.poem300.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Manages AI-generated poem audio: download, cache, and playback.
 * Audio files are stored in: /data/data/com.poem300/files/audio/
 * File naming: poem_001.mp3, poem_002.mp3, ...
 */
class AudioManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioManager"
        private const val AUDIO_DIR = "audio"
        private const val ZIP_FILE = "poem_audio_v2.zip"
        // GitHub Release download URL
        private const val AUDIO_ZIP_URL =
            "https://github.com/timenw/300-tang-poems-v2/releases/download/v2.0.0/poem_audio_v2.zip"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var currentPoemId: Int? = null

    private val audioDir: File
        get() = File(context.filesDir, AUDIO_DIR).also { it.mkdirs() }

    /** Check if audio file exists locally for a given poem ID. */
    fun hasAudio(poemId: Int): Boolean {
        return File(audioDir, "poem_${poemId.toString().padStart(3, '0')}.mp3").exists()
    }

    /** Get total count of locally cached audio files. */
    fun cachedCount(): Int = audioDir.listFiles()?.count { it.name.endsWith(".mp3") } ?: 0

    /** Download and extract the audio ZIP package. */
    suspend fun downloadAudioPack(
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Downloading audio pack from $AUDIO_ZIP_URL")
            val url = URL(AUDIO_ZIP_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.requestMethod = "GET"
            connection.connect()

            val totalSize = connection.contentLength.toLong()
            Log.d(TAG, "Audio pack size: ${totalSize / 1024}KB")

            val zipFile = File(context.cacheDir, ZIP_FILE)
            connection.inputStream.use { input ->
                FileOutputStream(zipFile).use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        onProgress(downloaded, totalSize)
                    }
                }
            }
            connection.disconnect()

            Log.d(TAG, "Download complete, extracting...")
            extractZip(zipFile)
            zipFile.delete()

            val count = cachedCount()
            Log.d(TAG, "Extraction complete, $count audio files cached")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            Result.failure(e)
        }
    }

    private fun extractZip(zipFile: File) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".mp3")) {
                    val outFile = File(audioDir, entry.name.substringAfterLast("/"))
                    outFile.outputStream().use { fos ->
                        zis.copyTo(fos)
                    }
                    Log.d(TAG, "Extracted: ${outFile.name}")
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    /** Play audio for a poem. Returns true if playback started. */
    fun play(poemId: Int): Boolean {
        val file = File(audioDir, "poem_${poemId.toString().padStart(3, '0')}.mp3")
        if (!file.exists()) {
            Log.w(TAG, "Audio file not found: ${file.absolutePath}")
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
