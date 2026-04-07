package com.coby.surasura.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

// ──────────────────────────────────────────────────────────────────────────────
// Google Speech-to-Text REST API
// POST https://speech.googleapis.com/v1/speech:recognize?key={API_KEY}
// ──────────────────────────────────────────────────────────────────────────────

interface GoogleSpeechApi {
    @POST("v1/speech:recognize")
    suspend fun recognize(
        @Query("key") apiKey: String,
        @Body request: SpeechRecognizeRequest
    ): SpeechRecognizeResponse
}

data class SpeechRecognizeRequest(
    @SerializedName("config") val config: SpeechConfig,
    @SerializedName("audio") val audio: SpeechAudio
)

data class SpeechConfig(
    @SerializedName("encoding") val encoding: String = "LINEAR16",
    @SerializedName("sampleRateHertz") val sampleRateHertz: Int = 16000,
    @SerializedName("languageCode") val languageCode: String,
    @SerializedName("enableAutomaticPunctuation") val enableAutomaticPunctuation: Boolean = true
)

data class SpeechAudio(
    @SerializedName("content") val content: String  // base64 encoded
)

data class SpeechRecognizeResponse(
    @SerializedName("results") val results: List<SpeechResult>?
)

data class SpeechResult(
    @SerializedName("alternatives") val alternatives: List<SpeechAlternative>?
)

data class SpeechAlternative(
    @SerializedName("transcript") val transcript: String?,
    @SerializedName("confidence") val confidence: Float?
)
