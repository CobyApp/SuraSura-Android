package com.coby.surasura.data.client

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import com.coby.surasura.BuildConfig
import com.coby.surasura.data.api.GoogleTTSApi
import com.coby.surasura.data.api.TTSAudioConfig
import com.coby.surasura.data.api.TTSInput
import com.coby.surasura.data.api.TTSSynthesizeRequest
import com.coby.surasura.data.api.TTSVoice
import com.coby.surasura.data.model.SupportedLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Google Text-to-Speech API 클라이언트
 * iOS GoogleTTSClientLive.swift와 동일한 역할
 * - base64 MP3 수신 → 파일로 저장 → MediaPlayer로 재생
 */
@Singleton
class GoogleTTSClient @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val api: GoogleTTSApi
) {
    private var mediaPlayer: MediaPlayer? = null

    suspend fun speak(text: String, language: SupportedLanguage) {
        if (text.isBlank()) return

        // 기존 재생 중이면 중지
        stop()

        // TTS API 호출
        val response = api.synthesize(
            apiKey = BuildConfig.GOOGLE_CLOUD_API_KEY,
            request = TTSSynthesizeRequest(
                input = TTSInput(text = text),
                voice = TTSVoice(
                    languageCode = language.googleTTSCode,
                    ssmlGender = language.googleTTSGender
                ),
                audioConfig = TTSAudioConfig(
                    audioEncoding = "MP3",
                    speakingRate = 1.0,
                    pitch = 0.0
                )
            )
        )

        // base64 디코딩 → 임시 파일로 저장
        val audioBytes = Base64.decode(response.audioContent, Base64.DEFAULT)
        val tempFile = File(context.cacheDir, "tts_audio_${System.currentTimeMillis()}.mp3")
        FileOutputStream(tempFile).use { it.write(audioBytes) }

        // MediaPlayer로 재생 (완료까지 대기)
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
                    continuation.resumeWithException(Exception("TTS 재생 오류"))
                    true
                }
            }
            mediaPlayer = player
            player.start()

            continuation.invokeOnCancellation {
                player.stop()
                player.release()
                mediaPlayer = null
                tempFile.delete()
            }
        }

        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }
}
