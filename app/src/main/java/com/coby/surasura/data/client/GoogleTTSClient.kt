package com.coby.surasura.data.client

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import com.coby.surasura.BuildConfig
import com.coby.surasura.data.model.SupportedLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Google Cloud Text-to-Speech **v1 REST** (`text:synthesize` + API key).
 * Returns MP3 bytes from base64 [audioContent] and plays via [MediaPlayer].
 */
@Singleton
class GoogleTTSClient @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private var mediaPlayer: MediaPlayer? = null

    suspend fun speak(text: String, language: SupportedLanguage) {
        if (text.isBlank()) return
        val apiKey = BuildConfig.GOOGLE_CLOUD_API_KEY.trim()
        if (apiKey.isBlank()) {
            throw IllegalStateException("GOOGLE_CLOUD_API_KEY is not configured")
        }

        stop()

        val mp3Bytes = withContext(Dispatchers.IO) {
            val encodedKey = URLEncoder.encode(apiKey, Charsets.UTF_8.name())
            val url = URL("https://texttospeech.googleapis.com/v1/text:synthesize?key=$encodedKey")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput = true
                connectTimeout = 30_000
                readTimeout = 60_000
            }

            val ssmlGender = when (language.googleTTSGender.uppercase()) {
                "FEMALE" -> "FEMALE"
                "MALE" -> "MALE"
                else -> "NEUTRAL"
            }

            val body = JSONObject()
                .put("input", JSONObject().put("text", text))
                .put(
                    "voice",
                    JSONObject()
                        .put("languageCode", language.googleTTSCode)
                        .put("ssmlGender", ssmlGender)
                )
                .put(
                    "audioConfig",
                    JSONObject()
                        .put("audioEncoding", "MP3")
                        .put("speakingRate", 1.0)
                        .put("pitch", 0.0)
                )
                .toString()

            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val responseText = (if (conn.responseCode in 200..299) {
                conn.inputStream
            } else {
                conn.errorStream
            }).bufferedReader().use { it.readText() }

            val json = JSONObject(responseText)
            if (json.has("error")) {
                val err = json.getJSONObject("error")
                throw IllegalStateException(err.optString("message", err.toString()))
            }
            val b64 = json.optString("audioContent", "")
            if (b64.isEmpty()) {
                throw IllegalStateException("TTS audioContent empty")
            }
            Base64.decode(b64, Base64.DEFAULT)
        }

        val tempFile = File(context.cacheDir, "tts_audio_${System.currentTimeMillis()}.mp3")
        FileOutputStream(tempFile).use { it.write(mp3Bytes) }

        suspendCancellableCoroutine { continuation ->
            val player = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    tempFile.delete()
                    continuation.resume(Unit)
                }
                setOnErrorListener { _, _, _ ->
                    tempFile.delete()
                    continuation.resumeWithException(Exception("TTS playback error"))
                    true
                }
            }
            mediaPlayer = player
            player.start()

            continuation.invokeOnCancellation {
                try {
                    player.stop()
                } catch (_: Exception) {
                }
                player.release()
                mediaPlayer = null
                tempFile.delete()
            }
        }

        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun stop() {
        try {
            mediaPlayer?.let { player ->
                try {
                    if (player.isPlaying) player.stop()
                } catch (_: IllegalStateException) {
                }
                try {
                    player.release()
                } catch (_: IllegalStateException) {
                }
            }
        } finally {
            mediaPlayer = null
        }
    }
}
