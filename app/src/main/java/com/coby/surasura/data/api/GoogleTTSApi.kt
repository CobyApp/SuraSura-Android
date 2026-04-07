package com.coby.surasura.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

// ──────────────────────────────────────────────────────────────────────────────
// Google Text-to-Speech API
// POST https://texttospeech.googleapis.com/v1/text:synthesize?key={API_KEY}
// ──────────────────────────────────────────────────────────────────────────────

interface GoogleTTSApi {
    @POST("v1/text:synthesize")
    suspend fun synthesize(
        @Query("key") apiKey: String,
        @Body request: TTSSynthesizeRequest
    ): TTSSynthesizeResponse
}

data class TTSSynthesizeRequest(
    @SerializedName("input") val input: TTSInput,
    @SerializedName("voice") val voice: TTSVoice,
    @SerializedName("audioConfig") val audioConfig: TTSAudioConfig
)

data class TTSInput(
    @SerializedName("text") val text: String
)

data class TTSVoice(
    @SerializedName("languageCode") val languageCode: String,
    @SerializedName("ssmlGender") val ssmlGender: String
)

data class TTSAudioConfig(
    @SerializedName("audioEncoding") val audioEncoding: String = "MP3",
    @SerializedName("speakingRate") val speakingRate: Double = 1.0,
    @SerializedName("pitch") val pitch: Double = 0.0
)

data class TTSSynthesizeResponse(
    @SerializedName("audioContent") val audioContent: String  // base64 encoded MP3
)
