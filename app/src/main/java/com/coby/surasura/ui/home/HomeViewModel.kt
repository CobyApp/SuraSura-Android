package com.coby.surasura.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coby.surasura.data.client.GoogleSpeechClient
import com.coby.surasura.data.client.GoogleTranslationClient
import com.coby.surasura.data.client.GoogleTTSClient
import com.coby.surasura.data.model.ActiveMic
import com.coby.surasura.data.prefs.LanguagePreferenceStore
import com.coby.surasura.data.model.HomeState
import com.coby.surasura.data.model.SupportedLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 홈 화면 ViewModel
 * iOS HomeReducer + SpeechRecognitionReducer + TranslationReducer 통합
 *
 * activeMic + topLanguage/bottomLanguage 구조:
 *   - BOTTOM 마이크: bottomLanguage로 인식 → topLanguage로 번역
 *   - TOP    마이크: topLanguage로 인식   → bottomLanguage로 번역
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val speechClient: GoogleSpeechClient,
    private val translationClient: GoogleTranslationClient,
    private val ttsClient: GoogleTTSClient,
    private val languagePreferenceStore: LanguagePreferenceStore
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        val saved = languagePreferenceStore.load()
        _state.update { current ->
            current.copy(
                topLanguage = saved.top,
                bottomLanguage = saved.bottom,
                speechRecognition = current.speechRecognition.copy(sourceLanguage = saved.bottom),
                translation = current.translation.copy(targetLanguage = saved.top)
            )
        }
    }

    private var speechJob: Job? = null
    private var translationJob: Job? = null
    private var translationDebounceJob: Job? = null
    private var ttsJob: Job? = null

    private companion object {
        /** Coalesce rapid STT emissions so we translate the latest text once. */
        private const val TRANSLATION_DEBOUNCE_MS = 90L
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 세션 제어
    // iOS HomeReducer.startSessionTapped / startTopSessionTapped
    // ──────────────────────────────────────────────────────────────────────────

    /** 하단 마이크: bottomLanguage로 인식 → topLanguage로 번역 */
    fun startSession() {
        if (_state.value.isSessionActive) return
        val src = _state.value.bottomLanguage
        val tgt = _state.value.topLanguage
        _state.update {
            it.copy(
                activeMic = ActiveMic.BOTTOM,
                isSessionActive = true,
                speechRecognition = it.speechRecognition.copy(sourceLanguage = src),
                translation = it.translation.copy(targetLanguage = tgt, translatedText = "")
            )
        }
        startListening(sourceLanguage = src, targetLanguage = tgt)
    }

    /** 상단 마이크: topLanguage로 인식 → bottomLanguage로 번역 */
    fun startTopSession() {
        if (_state.value.isSessionActive) return
        val src = _state.value.topLanguage
        val tgt = _state.value.bottomLanguage
        _state.update {
            it.copy(
                activeMic = ActiveMic.TOP,
                isSessionActive = true,
                speechRecognition = it.speechRecognition.copy(sourceLanguage = src),
                translation = it.translation.copy(targetLanguage = tgt, translatedText = "")
            )
        }
        startListening(sourceLanguage = src, targetLanguage = tgt)
    }

    fun stopSession() {
        speechJob?.cancel()
        speechJob = null
        translationDebounceJob?.cancel()
        translationDebounceJob = null
        ttsClient.stop()
        _state.update {
            it.copy(
                isSessionActive = false,
                speechRecognition = it.speechRecognition.copy(isListening = false),
                translation = it.translation.copy(isSpeaking = false)
            )
        }
    }

    private fun startListening(sourceLanguage: SupportedLanguage, targetLanguage: SupportedLanguage) {
        speechJob?.cancel()
        _state.update {
            it.copy(
                speechRecognition = it.speechRecognition.copy(
                    isListening = true,
                    recognizedText = "",
                    errorMessage = null
                ),
                translation = it.translation.copy(
                    translatedText = "",
                    errorMessage = null
                )
            )
        }

        speechJob = viewModelScope.launch {
            try {
                speechClient.startStreaming(sourceLanguage).collect { recognizedText ->
                    _state.update {
                        it.copy(
                            speechRecognition = it.speechRecognition.copy(
                                recognizedText = recognizedText
                            )
                        )
                    }
                    scheduleDebouncedTranslation(recognizedText, targetLanguage)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSessionActive = false,
                        speechRecognition = it.speechRecognition.copy(
                            isListening = false,
                            errorMessage = e.localizedMessage
                        )
                    )
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 언어 선택
    // iOS topLanguageChanged / bottomLanguageChanged / swapLanguagesTapped
    // ──────────────────────────────────────────────────────────────────────────

    /** 상단 패널 언어 변경 */
    fun changeTopLanguage(language: SupportedLanguage) {
        _state.update { state ->
            val newSpeech = state.speechRecognition.copy(
                recognizedText = "",
                sourceLanguage = if (state.activeMic == ActiveMic.TOP) language
                else state.speechRecognition.sourceLanguage
            )
            val newTranslation = state.translation.copy(
                translatedText = "",
                targetLanguage = if (state.activeMic == ActiveMic.BOTTOM) language
                else state.translation.targetLanguage
            )
            state.copy(
                topLanguage = language,
                speechRecognition = newSpeech,
                translation = newTranslation,
                isTopPickerVisible = false
            )
        }
        persistPanelLanguages()
    }

    /** 하단 패널 언어 변경 */
    fun changeBottomLanguage(language: SupportedLanguage) {
        _state.update { state ->
            val newSpeech = state.speechRecognition.copy(
                recognizedText = "",
                sourceLanguage = if (state.activeMic == ActiveMic.BOTTOM) language
                else state.speechRecognition.sourceLanguage
            )
            val newTranslation = state.translation.copy(
                translatedText = "",
                targetLanguage = if (state.activeMic == ActiveMic.TOP) language
                else state.translation.targetLanguage
            )
            state.copy(
                bottomLanguage = language,
                speechRecognition = newSpeech,
                translation = newTranslation,
                isBottomPickerVisible = false
            )
        }
        persistPanelLanguages()
    }

    /** 언어 교환 (iOS swapLanguagesTapped) */
    fun swapLanguages() {
        _state.update { state ->
            val newTop = state.bottomLanguage
            val newBottom = state.topLanguage
            val newSpeech = state.speechRecognition.copy(
                recognizedText = "",
                sourceLanguage = if (state.activeMic == ActiveMic.BOTTOM) newBottom else newTop
            )
            val newTranslation = state.translation.copy(
                translatedText = "",
                targetLanguage = if (state.activeMic == ActiveMic.BOTTOM) newTop else newBottom
            )
            state.copy(
                topLanguage = newTop,
                bottomLanguage = newBottom,
                speechRecognition = newSpeech,
                translation = newTranslation
            )
        }
        persistPanelLanguages()
    }

    private fun persistPanelLanguages() {
        val s = _state.value
        languagePreferenceStore.save(s.topLanguage, s.bottomLanguage)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 번역
    // ──────────────────────────────────────────────────────────────────────────

    private fun scheduleDebouncedTranslation(text: String, targetLanguage: SupportedLanguage) {
        if (text.isBlank()) return
        translationDebounceJob?.cancel()
        translationDebounceJob = viewModelScope.launch {
            delay(TRANSLATION_DEBOUNCE_MS)
            requestTranslation(text, targetLanguage)
        }
    }

    private fun requestTranslation(text: String, targetLanguage: SupportedLanguage) {
        if (text.isBlank()) return

        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            _state.update {
                it.copy(translation = it.translation.copy(isTranslating = true))
            }
            try {
                val translated = translationClient.translate(text, targetLanguage)
                _state.update {
                    it.copy(
                        translation = it.translation.copy(
                            translatedText = translated,
                            isTranslating = false
                        )
                    )
                }
                if (_state.value.isAutoSpeakEnabled) {
                    requestSpeak(translated, targetLanguage)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        translation = it.translation.copy(
                            isTranslating = false,
                            errorMessage = e.localizedMessage
                        )
                    )
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TTS
    // ──────────────────────────────────────────────────────────────────────────

    /** 전체화면 확장 뷰에서 TTS 재생 (iOS speakExpanded) */
    fun speakExpanded(text: String, language: SupportedLanguage) {
        if (text.isBlank()) return
        requestSpeak(text, language)
    }

    fun stopSpeaking() {
        ttsJob?.cancel()
        ttsClient.stop()
        _state.update {
            it.copy(translation = it.translation.copy(isSpeaking = false))
        }
    }

    private fun requestSpeak(text: String, language: SupportedLanguage) {
        ttsJob?.cancel()
        ttsJob = viewModelScope.launch {
            _state.update {
                it.copy(translation = it.translation.copy(isSpeaking = true))
            }
            try {
                ttsClient.speak(text, language)
            } catch (_: Exception) {
                // TTS 실패해도 앱은 계속 동작
            } finally {
                _state.update {
                    it.copy(translation = it.translation.copy(isSpeaking = false))
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 피커 제어
    // ──────────────────────────────────────────────────────────────────────────

    fun showTopPicker() = _state.update { it.copy(isTopPickerVisible = true) }
    fun hideTopPicker() = _state.update { it.copy(isTopPickerVisible = false) }
    fun showBottomPicker() = _state.update { it.copy(isBottomPickerVisible = true) }
    fun hideBottomPicker() = _state.update { it.copy(isBottomPickerVisible = false) }

    // ──────────────────────────────────────────────────────────────────────────
    // UI 상태 제어
    // ──────────────────────────────────────────────────────────────────────────

    fun toggleFaceToFaceMode() {
        _state.update { it.copy(isFaceToFaceMode = !it.isFaceToFaceMode) }
    }

    fun expandTopPanel() = _state.update { it.copy(isTopExpanded = true) }
    fun collapseTopPanel() = _state.update { it.copy(isTopExpanded = false) }
    fun expandBottomPanel() = _state.update { it.copy(isBottomExpanded = true) }
    fun collapseBottomPanel() = _state.update { it.copy(isBottomExpanded = false) }

    override fun onCleared() {
        super.onCleared()
        speechJob?.cancel()
        translationDebounceJob?.cancel()
        translationJob?.cancel()
        ttsJob?.cancel()
        ttsClient.stop()
    }
}
