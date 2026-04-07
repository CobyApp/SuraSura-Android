package com.coby.surasura.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

// ──────────────────────────────────────────────────────────────────────────────
// Google Translation API
// POST https://translation.googleapis.com/language/translate/v2?key={API_KEY}
// ──────────────────────────────────────────────────────────────────────────────

interface GoogleTranslationApi {
    @POST("language/translate/v2")
    suspend fun translate(
        @Query("key") apiKey: String,
        @Body request: TranslationRequest
    ): TranslationResponse
}

data class TranslationRequest(
    @SerializedName("q") val q: String,
    @SerializedName("target") val target: String,
    @SerializedName("format") val format: String = "text"
)

data class TranslationResponse(
    @SerializedName("data") val data: TranslationData
)

data class TranslationData(
    @SerializedName("translations") val translations: List<TranslationResult>
)

data class TranslationResult(
    @SerializedName("translatedText") val translatedText: String
)
