package com.coby.surasura.data.client

import com.coby.surasura.BuildConfig
import com.coby.surasura.data.model.SupportedLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Cloud Translation API **v2 (REST)** with API key query parameter.
 *
 * Translation API **v3 gRPC** rejects API keys (`UNAUTHENTICATED: API keys are not supported by this API`);
 * v2 Basic is the practical option for mobile apps that only ship an API key.
 *
 * REST reference: https://cloud.google.com/translate/docs/reference/rest/v2/translate
 */
@Singleton
class GoogleTranslationClient @Inject constructor() {

    suspend fun translate(text: String, targetLanguage: SupportedLanguage): String {
        if (text.isBlank()) return ""

        val apiKey = BuildConfig.GOOGLE_CLOUD_API_KEY.trim()
        if (apiKey.isBlank()) {
            throw IllegalStateException("GOOGLE_CLOUD_API_KEY is not configured")
        }

        return withContext(Dispatchers.IO) {
            val encodedKey = URLEncoder.encode(apiKey, Charsets.UTF_8.name())
            val url = URL("https://translation.googleapis.com/language/translate/v2?key=$encodedKey")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput = true
                connectTimeout = 30_000
                readTimeout = 60_000
            }

            val body = JSONObject()
                .put("q", text)
                .put("target", targetLanguage.googleTranslationCode)
                .put("format", "text")
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
                val msg = err.optString("message", err.toString())
                throw IllegalStateException(msg)
            }

            val translations = json.getJSONObject("data").getJSONArray("translations")
            if (translations.length() == 0) {
                throw IllegalStateException("Empty translations array")
            }
            translations.getJSONObject(0).getString("translatedText").trim()
                .takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException("Translated text is empty")
        }
    }
}
