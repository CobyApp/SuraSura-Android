package com.coby.surasura.data.model

import java.util.Locale

/**
 * 앱에서 지원하는 언어 목록 (iOS SupportedLanguage.swift와 동일)
 * - 21개 언어 지원
 * - googleTTSGender: iOS와 동일 (Korean, Japanese, French, Italian, Vietnamese, Thai, Indonesian, Nepali = FEMALE, 나머지 = NEUTRAL)
 */
enum class SupportedLanguage(
    val displayName: String,    // 현지어 이름 (한국어, English, 日本語 ...)
    val shortName: String,      // 짧은 이름 (UI 버튼용)
    val flag: String,
    val googleSpeechCode: String,
    val googleTranslationCode: String,
    val googleTTSCode: String,
    val googleTTSGender: String,
    val localeTag: String       // BCP-47 (ko, en, zh-Hans ...)
) {
    KOREAN(
        displayName = "한국어",
        shortName = "한국어",
        flag = "🇰🇷",
        googleSpeechCode = "ko-KR",
        googleTranslationCode = "ko",
        googleTTSCode = "ko-KR",
        googleTTSGender = "FEMALE",
        localeTag = "ko"
    ),
    ENGLISH(
        displayName = "English",
        shortName = "English",
        flag = "🇺🇸",
        googleSpeechCode = "en-US",
        googleTranslationCode = "en",
        googleTTSCode = "en-US",
        googleTTSGender = "NEUTRAL",
        localeTag = "en"
    ),
    JAPANESE(
        displayName = "日本語",
        shortName = "日本語",
        flag = "🇯🇵",
        googleSpeechCode = "ja-JP",
        googleTranslationCode = "ja",
        googleTTSCode = "ja-JP",
        googleTTSGender = "FEMALE",
        localeTag = "ja"
    ),
    CHINESE_SIMPLIFIED(
        displayName = "中文 (简体)",
        shortName = "中文(简)",
        flag = "🇨🇳",
        googleSpeechCode = "zh-CN",
        googleTranslationCode = "zh-CN",
        googleTTSCode = "cmn-CN",
        googleTTSGender = "NEUTRAL",
        localeTag = "zh-Hans"
    ),
    CHINESE_TRADITIONAL(
        displayName = "中文 (繁體)",
        shortName = "中文(繁)",
        flag = "🇹🇼",
        googleSpeechCode = "zh-TW",
        googleTranslationCode = "zh-TW",
        googleTTSCode = "cmn-TW",
        googleTTSGender = "NEUTRAL",
        localeTag = "zh-Hant"
    ),
    SPANISH(
        displayName = "Español",
        shortName = "Español",
        flag = "🇪🇸",
        googleSpeechCode = "es-ES",
        googleTranslationCode = "es",
        googleTTSCode = "es-ES",
        googleTTSGender = "NEUTRAL",
        localeTag = "es"
    ),
    FRENCH(
        displayName = "Français",
        shortName = "Français",
        flag = "🇫🇷",
        googleSpeechCode = "fr-FR",
        googleTranslationCode = "fr",
        googleTTSCode = "fr-FR",
        googleTTSGender = "FEMALE",
        localeTag = "fr"
    ),
    GERMAN(
        displayName = "Deutsch",
        shortName = "Deutsch",
        flag = "🇩🇪",
        googleSpeechCode = "de-DE",
        googleTranslationCode = "de",
        googleTTSCode = "de-DE",
        googleTTSGender = "NEUTRAL",
        localeTag = "de"
    ),
    ITALIAN(
        displayName = "Italiano",
        shortName = "Italiano",
        flag = "🇮🇹",
        googleSpeechCode = "it-IT",
        googleTranslationCode = "it",
        googleTTSCode = "it-IT",
        googleTTSGender = "FEMALE",
        localeTag = "it"
    ),
    PORTUGUESE(
        displayName = "Português",
        shortName = "Português",
        flag = "🇧🇷",
        googleSpeechCode = "pt-BR",
        googleTranslationCode = "pt",
        googleTTSCode = "pt-BR",
        googleTTSGender = "NEUTRAL",
        localeTag = "pt"
    ),
    RUSSIAN(
        displayName = "Русский",
        shortName = "Русский",
        flag = "🇷🇺",
        googleSpeechCode = "ru-RU",
        googleTranslationCode = "ru",
        googleTTSCode = "ru-RU",
        googleTTSGender = "NEUTRAL",
        localeTag = "ru"
    ),
    ARABIC(
        displayName = "العربية",
        shortName = "العربية",
        flag = "🇸🇦",
        googleSpeechCode = "ar-SA",
        googleTranslationCode = "ar",
        googleTTSCode = "ar-XA",
        googleTTSGender = "NEUTRAL",
        localeTag = "ar"
    ),
    DUTCH(
        displayName = "Nederlands",
        shortName = "Nederlands",
        flag = "🇳🇱",
        googleSpeechCode = "nl-NL",
        googleTranslationCode = "nl",
        googleTTSCode = "nl-NL",
        googleTTSGender = "NEUTRAL",
        localeTag = "nl"
    ),
    TURKISH(
        displayName = "Türkçe",
        shortName = "Türkçe",
        flag = "🇹🇷",
        googleSpeechCode = "tr-TR",
        googleTranslationCode = "tr",
        googleTTSCode = "tr-TR",
        googleTTSGender = "NEUTRAL",
        localeTag = "tr"
    ),
    VIETNAMESE(
        displayName = "Tiếng Việt",
        shortName = "Việt",
        flag = "🇻🇳",
        googleSpeechCode = "vi-VN",
        googleTranslationCode = "vi",
        googleTTSCode = "vi-VN",
        googleTTSGender = "FEMALE",
        localeTag = "vi"
    ),
    INDONESIAN(
        displayName = "Bahasa Indonesia",
        shortName = "Indonesia",
        flag = "🇮🇩",
        googleSpeechCode = "id-ID",
        googleTranslationCode = "id",
        googleTTSCode = "id-ID",
        googleTTSGender = "FEMALE",
        localeTag = "id"
    ),
    THAI(
        displayName = "ภาษาไทย",
        shortName = "ไทย",
        flag = "🇹🇭",
        googleSpeechCode = "th-TH",
        googleTranslationCode = "th",
        googleTTSCode = "th-TH",
        googleTTSGender = "FEMALE",
        localeTag = "th"
    ),
    POLISH(
        displayName = "Polski",
        shortName = "Polski",
        flag = "🇵🇱",
        googleSpeechCode = "pl-PL",
        googleTranslationCode = "pl",
        googleTTSCode = "pl-PL",
        googleTTSGender = "NEUTRAL",
        localeTag = "pl"
    ),
    HINDI(
        displayName = "हिन्दी",
        shortName = "हिन्दी",
        flag = "🇮🇳",
        googleSpeechCode = "hi-IN",
        googleTranslationCode = "hi",
        googleTTSCode = "hi-IN",
        googleTTSGender = "NEUTRAL",
        localeTag = "hi"
    ),
    SWEDISH(
        displayName = "Svenska",
        shortName = "Svenska",
        flag = "🇸🇪",
        googleSpeechCode = "sv-SE",
        googleTranslationCode = "sv",
        googleTTSCode = "sv-SE",
        googleTTSGender = "NEUTRAL",
        localeTag = "sv"
    ),
    NEPALI(
        displayName = "नेपाली",
        shortName = "नेपाली",
        flag = "🇳🇵",
        googleSpeechCode = "ne-NP",
        googleTranslationCode = "ne",
        googleTTSCode = "ne-NP",
        googleTTSGender = "FEMALE",
        localeTag = "ne"
    );

    companion object {
        fun fromLocaleTag(tag: String): SupportedLanguage {
            return entries.find { it.localeTag == tag } ?: ENGLISH
        }
    }
}
