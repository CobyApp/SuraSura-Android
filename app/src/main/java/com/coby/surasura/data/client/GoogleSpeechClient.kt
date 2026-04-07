package com.coby.surasura.data.client

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import com.coby.surasura.BuildConfig
import com.coby.surasura.data.api.GoogleSpeechApi
import com.coby.surasura.data.api.SpeechAudio
import com.coby.surasura.data.api.SpeechConfig
import com.coby.surasura.data.api.SpeechRecognizeRequest
import com.coby.surasura.data.model.SupportedLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Speech-to-Text REST API 전용 음성 인식 클라이언트
 *
 * iOS의 Google REST 폴백 방식과 완전히 동일한 구조:
 *   - AudioRecord로 마이크 PCM 녹음 (16kHz, Mono, LINEAR16)
 *   - 짧은 간격으로 버퍼를 base64 인코딩 → Google STT REST API 배치 호출
 *   - 인식된 텍스트를 Flow<String>으로 방출
 *   - 모든 언어 동일하게 처리 (언어 분기 없음)
 */
@Singleton
class GoogleSpeechClient @Inject constructor(
    private val speechApi: GoogleSpeechApi
) {
    companion object {
        private const val TAG = "GoogleSTT"
        private const val SAMPLE_RATE = 16000
        /** Steady batch cadence after the first chunk. */
        private const val BATCH_INTERVAL_MS = 550L
        /** First batch fires sooner so the first words appear quickly. */
        private const val FIRST_BATCH_DELAY_MS = 320L
    }

    fun startStreaming(language: SupportedLanguage): Flow<String> = callbackFlow {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(8192)

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        // AudioRecord 초기화 실패 시 (권한 없음 또는 하드웨어 오류) 조기 종료
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            close(Exception("AudioRecord 초기화 실패: 마이크 권한을 확인하거나 다시 시도해주세요."))
            return@callbackFlow
        }

        val pcmBuffer = mutableListOf<Short>()
        val readBuffer = ShortArray(bufferSize / 2)

        audioRecord.startRecording()
        Log.d(TAG, "AudioRecord 시작 (언어=${language.googleSpeechCode}, bufferSize=$bufferSize)")

        // ── Periodic Google STT REST batch (shorter interval for snappier UI) ──
        val batchJob = launch(Dispatchers.IO) {
            var batchIndex = 0
            while (isActive) {
                delay(if (batchIndex == 0) FIRST_BATCH_DELAY_MS else BATCH_INTERVAL_MS)
                batchIndex++

                val snapshot: ShortArray = synchronized(pcmBuffer) {
                    if (pcmBuffer.isEmpty()) return@synchronized ShortArray(0)
                    pcmBuffer.toShortArray().also { pcmBuffer.clear() }
                }
                if (snapshot.isEmpty()) {
                    Log.d(TAG, "배치: 버퍼 비어있음 (무음 또는 아직 데이터 없음)")
                    continue
                }

                Log.d(TAG, "배치 전송: ${snapshot.size} shorts (${snapshot.size * 2 / 1000}KB)")

                try {
                    // Short[] → ByteArray (Little-Endian LINEAR16)
                    val byteBuffer = ByteBuffer
                        .allocate(snapshot.size * 2)
                        .order(ByteOrder.LITTLE_ENDIAN)
                    snapshot.forEach { byteBuffer.putShort(it) }
                    val base64Audio = Base64.encodeToString(
                        byteBuffer.array(), Base64.NO_WRAP
                    )

                    val response = speechApi.recognize(
                        apiKey = BuildConfig.GOOGLE_CLOUD_API_KEY,
                        request = SpeechRecognizeRequest(
                            config = SpeechConfig(
                                encoding = "LINEAR16",
                                sampleRateHertz = SAMPLE_RATE,
                                languageCode = language.googleSpeechCode,
                                enableAutomaticPunctuation = true
                            ),
                            audio = SpeechAudio(content = base64Audio)
                        )
                    )

                    val transcript = response.results
                        ?.firstOrNull()
                        ?.alternatives
                        ?.firstOrNull()
                        ?.transcript

                    Log.d(TAG, "STT 응답: transcript=$transcript, results=${response.results?.size ?: 0}개")

                    if (!transcript.isNullOrBlank()) {
                        trySend(transcript)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "STT API 오류: ${e.message}", e)
                    // 배치 오류는 무시하고 계속 녹음 (네트워크 일시 오류 등에 대비)
                }
            }
        }

        // ── 마이크 읽기 루프 ──
        val readJob = launch(Dispatchers.IO) {
            while (isActive) {
                val read = audioRecord.read(readBuffer, 0, readBuffer.size)
                when {
                    read > 0 -> synchronized(pcmBuffer) {
                        for (i in 0 until read) pcmBuffer.add(readBuffer[i])
                    }
                    read == AudioRecord.ERROR_INVALID_OPERATION ->
                        Log.e(TAG, "AudioRecord.read: ERROR_INVALID_OPERATION")
                    read == AudioRecord.ERROR_BAD_VALUE ->
                        Log.e(TAG, "AudioRecord.read: ERROR_BAD_VALUE")
                    read == AudioRecord.ERROR_DEAD_OBJECT -> {
                        Log.e(TAG, "AudioRecord.read: ERROR_DEAD_OBJECT — 녹음 중단")
                        close(Exception("마이크 연결이 끊겼습니다. 다시 시도해주세요."))
                        break
                    }
                }
            }
        }

        awaitClose {
            batchJob.cancel()
            readJob.cancel()
            if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord.stop()
            }
            audioRecord.release()
        }
    }
}
