package com.coby.surasura.data.client

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.coby.surasura.BuildConfig
import com.coby.surasura.data.model.SttStreamSegment
import com.coby.surasura.data.model.SupportedLanguage
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechGrpc
import com.google.cloud.speech.v1.StreamingRecognitionConfig
import com.google.cloud.speech.v1.StreamingRecognizeRequest
import com.google.cloud.speech.v1.StreamingRecognizeResponse
import com.google.protobuf.ByteString
import io.grpc.StatusRuntimeException
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Cloud Speech-to-Text **v1 StreamingRecognize** over gRPC (OkHttp transport).
 *
 * Sends LINEAR16 mono 16 kHz chunks as they are captured; emits batches of interim/final segments.
 * Uses the same API key as the former REST client ([x-goog-api-key] metadata).
 */
@Singleton
class GoogleSpeechClient @Inject constructor() {

    companion object {
        private const val TAG = "GoogleSTT"
        private const val SAMPLE_RATE = 16000
        private const val HOST = "speech.googleapis.com"
        private const val PORT = 443
    }

    /** Try VOICE_RECOGNITION first, then MIC — some devices fail the former. */
    private fun buildAudioRecord(bufferSize: Int): AudioRecord? {
        val sources = intArrayOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC
        )
        for (source in sources) {
            val rec = AudioRecord(
                source,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            if (rec.state == AudioRecord.STATE_INITIALIZED) return rec
            rec.release()
        }
        return null
    }

    fun startStreaming(language: SupportedLanguage): Flow<List<SttStreamSegment>> = callbackFlow {
        if (BuildConfig.GOOGLE_CLOUD_API_KEY.isBlank()) {
            close(IllegalStateException("GOOGLE_CLOUD_API_KEY is not configured"))
            return@callbackFlow
        }

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(8192)

        // VOICE_RECOGNITION first (better AGC on many devices); MIC fallback if OEM rejects it.
        val audioRecord = buildAudioRecord(bufferSize)
        if (audioRecord == null) {
            close(Exception("AudioRecord initialization failed: check RECORD_AUDIO permission."))
            return@callbackFlow
        }

        val channel = OkHttpChannelBuilder.forAddress(HOST, PORT)
            .intercept(GoogleCloudGrpcApiKeyInterceptor(BuildConfig.GOOGLE_CLOUD_API_KEY))
            .build()

        val closed = AtomicBoolean(false)
        fun closeFlowOnce(t: Throwable?) {
            if (closed.compareAndSet(false, true)) {
                if (t != null) close(t) else close()
            }
        }

        val responseObserver = object : StreamObserver<StreamingRecognizeResponse> {
            override fun onNext(value: StreamingRecognizeResponse) {
                try {
                    val segments = value.resultsList.mapNotNull { result ->
                        if (result.alternativesCount == 0) return@mapNotNull null
                        val text = result.getAlternatives(0).transcript.trim()
                        if (text.isEmpty()) return@mapNotNull null
                        SttStreamSegment(text = text, isFinal = result.isFinal)
                    }
                    if (segments.isNotEmpty()) {
                        val r = trySend(segments)
                        if (!r.isSuccess) {
                            Log.w(TAG, "trySend segments failed: ${r.exceptionOrNull()?.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onNext parse failed", e)
                    closeFlowOnce(e)
                }
            }

            override fun onError(t: Throwable) {
                val detail = if (t is StatusRuntimeException) {
                    "${t.status.code}: ${t.status.description ?: t.message}"
                } else {
                    t.message ?: t.javaClass.simpleName
                }
                Log.e(TAG, "Streaming STT error: $detail", t)
                closeFlowOnce(t)
            }

            override fun onCompleted() {
                Log.d(TAG, "Streaming STT server completed response stream")
            }
        }

        val requestObserver: StreamObserver<StreamingRecognizeRequest> = try {
            SpeechGrpc.newStub(channel).streamingRecognize(responseObserver)
        } catch (e: Exception) {
            channel.shutdownNow()
            audioRecord.release()
            close(e)
            return@callbackFlow
        }

        val recognitionConfig = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
            .setSampleRateHertz(SAMPLE_RATE)
            .setLanguageCode(language.googleSpeechCode)
            .setEnableAutomaticPunctuation(true)
            .build()

        val streamingConfig = StreamingRecognitionConfig.newBuilder()
            .setConfig(recognitionConfig)
            .setInterimResults(true)
            .build()

        try {
            requestObserver.onNext(
                StreamingRecognizeRequest.newBuilder()
                    .setStreamingConfig(streamingConfig)
                    .build()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send streaming config", e)
            try {
                channel.shutdownNow()
            } catch (_: Exception) {
            }
            audioRecord.release()
            close(e)
            return@callbackFlow
        }

        val readBuffer = ShortArray(bufferSize / 2)
        try {
            audioRecord.startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "startRecording failed", e)
            try {
                requestObserver.onCompleted()
            } catch (_: Exception) {
            }
            channel.shutdownNow()
            audioRecord.release()
            close(e)
            return@callbackFlow
        }
        Log.d(TAG, "gRPC streaming started (language=${language.googleSpeechCode})")

        val readJob = launch(Dispatchers.IO) {
            while (isActive && !closed.get()) {
                val read = audioRecord.read(readBuffer, 0, readBuffer.size)
                when {
                    read > 0 -> {
                        val byteBuffer = ByteBuffer.allocate(read * 2).order(ByteOrder.LITTLE_ENDIAN)
                        for (i in 0 until read) {
                            byteBuffer.putShort(readBuffer[i])
                        }
                        try {
                            requestObserver.onNext(
                                StreamingRecognizeRequest.newBuilder()
                                    .setAudioContent(ByteString.copyFrom(byteBuffer.array()))
                                    .build()
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "send audio chunk failed", e)
                            break
                        }
                    }
                    read == AudioRecord.ERROR_DEAD_OBJECT -> {
                        closeFlowOnce(Exception("Microphone disconnected"))
                        break
                    }
                    read < 0 -> {
                        Log.e(TAG, "AudioRecord.read error code=$read")
                        closeFlowOnce(Exception("Microphone read failed (code $read)"))
                        break
                    }
                }
            }
        }

        awaitClose {
            // Never runBlocking(Dispatchers.IO) here: this runs on an IO worker from flowOn;
            // cancelAndJoin inside would starve the pool and deadlock with HomeViewModel.cancelAndJoin.
            try {
                if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.stop()
                }
            } catch (e: Exception) {
                Log.e(TAG, "AudioRecord.stop failed", e)
            }
            readJob.cancel()
            try {
                requestObserver.onCompleted()
            } catch (e: Exception) {
                Log.d(TAG, "onCompleted: ${e.message}")
            }
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
            } catch (e: Exception) {
                Log.e(TAG, "channel.awaitTermination", e)
            }
            if (!channel.isShutdown) {
                channel.shutdownNow()
            }
            try {
                audioRecord.release()
            } catch (e: Exception) {
                Log.e(TAG, "AudioRecord.release failed", e)
            }
        }
    }.flowOn(Dispatchers.IO)
}
