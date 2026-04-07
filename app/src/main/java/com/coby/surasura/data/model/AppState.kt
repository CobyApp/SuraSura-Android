package com.coby.surasura.data.model

import android.content.Context

/**
 * 활성 마이크 패널 (iOS ActiveMic과 동일)
 * - BOTTOM: 하단 마이크 활성 → bottomLanguage로 인식, topLanguage로 번역
 * - TOP:    상단 마이크 활성 → topLanguage로 인식, bottomLanguage로 번역
 */
enum class ActiveMic { BOTTOM, TOP }

/**
 * 앱 색상 스킴 (iOS AppColorScheme과 동일)
 */
enum class AppColorScheme {
    SYSTEM, LIGHT, DARK;

    fun isDark(context: Context): Boolean {
        return when (this) {
            LIGHT -> false
            DARK -> true
            SYSTEM -> {
                val nightModeFlags = context.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }
}

/**
 * 음성 인식 상태 (iOS SpeechRecognitionReducer.State와 동일)
 */
data class SpeechRecognitionState(
    val recognizedText: String = "",
    val isListening: Boolean = false,
    val sourceLanguage: SupportedLanguage = SupportedLanguage.JAPANESE,
    val errorMessage: String? = null
)

/**
 * 번역 상태 (iOS TranslationReducer.State와 동일)
 */
data class TranslationState(
    val translatedText: String = "",
    val isTranslating: Boolean = false,
    val isSpeaking: Boolean = false,
    val targetLanguage: SupportedLanguage = SupportedLanguage.ENGLISH,
    val errorMessage: String? = null
)

/**
 * 홈 화면 전체 상태 (iOS HomeReducer.State와 동일)
 *
 * topLanguage / bottomLanguage: 패널에 고정된 언어
 *   - activeMic == BOTTOM: bottom=인식, top=번역
 *   - activeMic == TOP:    top=인식, bottom=번역
 */
data class HomeState(
    val speechRecognition: SpeechRecognitionState = SpeechRecognitionState(),
    val translation: TranslationState = TranslationState(),
    val isSessionActive: Boolean = false,
    val activeMic: ActiveMic = ActiveMic.BOTTOM,
    // 패널 언어 (iOS topLanguage / bottomLanguage와 동일)
    val topLanguage: SupportedLanguage = SupportedLanguage.ENGLISH,
    val bottomLanguage: SupportedLanguage = SupportedLanguage.JAPANESE,
    val isFaceToFaceMode: Boolean = false,
    val appColorScheme: AppColorScheme = AppColorScheme.SYSTEM,
    val isAutoSpeakEnabled: Boolean = true,
    val isTopPickerVisible: Boolean = false,
    val isBottomPickerVisible: Boolean = false,
    val isTopExpanded: Boolean = false,
    val isBottomExpanded: Boolean = false
)
