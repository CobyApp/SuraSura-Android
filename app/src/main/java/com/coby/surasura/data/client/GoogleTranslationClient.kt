package com.coby.surasura.data.client

import com.coby.surasura.BuildConfig
import com.coby.surasura.data.api.GoogleTranslationApi
import com.coby.surasura.data.api.TranslationRequest
import com.coby.surasura.data.model.SupportedLanguage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Translation API 클라이언트
 * iOS GoogleTranslationClientLive.swift와 동일한 역할
 */
@Singleton
class GoogleTranslationClient @Inject constructor(
    private val api: GoogleTranslationApi
) {
    suspend fun translate(text: String, targetLanguage: SupportedLanguage): String {
        if (text.isBlank()) return ""

        val response = api.translate(
            apiKey = BuildConfig.GOOGLE_CLOUD_API_KEY,
            request = TranslationRequest(
                q = text,
                target = targetLanguage.googleTranslationCode,
                format = "text"
            )
        )

        return response.data.translations.firstOrNull()?.translatedText
            ?: throw Exception("번역 결과가 비어있습니다")
    }
}
