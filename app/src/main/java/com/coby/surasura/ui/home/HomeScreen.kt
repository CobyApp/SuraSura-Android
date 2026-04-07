package com.coby.surasura.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.ContextCompat
import com.coby.surasura.data.model.ActiveMic
import com.coby.surasura.data.model.SupportedLanguage
import com.coby.surasura.ui.home.components.ExpandedTextView
import com.coby.surasura.ui.home.components.LanguagePickerOverlay
import com.coby.surasura.ui.home.components.MicButton
import com.coby.surasura.ui.UiCopy
import com.coby.surasura.ui.theme.AccentBlue

/** Same metrics for top & bottom panel body/hint (avoids theme merge + font padding skew). */
private fun panelContentTextStyle(color: Color): TextStyle = TextStyle(
    color = color,
    fontSize = 22.sp,
    lineHeight = 30.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

/**
 * Compose [Dialog] with [DialogProperties.usePlatformDefaultWidth] still uses wrap-content on some
 * devices in landscape; pin the window to [MATCH_PARENT] so expanded text truly fills the display.
 */
@Composable
private fun FullscreenExpandedDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false
        )
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            window.setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

/**
 * 홈 화면 — iOS HomeView.swift와 동일한 역할
 * Portrait: 상·하 반분 | Landscape: 좌(파랑)·우(밝은) 반분
 *
 * 권한 처리:
 *  - 이미 RECORD_AUDIO 권한 있음 → 바로 시작
 *  - 권한 없음 → 시스템 다이얼로그 표시 후 허용 시 시작
 *
 * activeMic 기반 텍스트 표시:
 *  - activeMic == BOTTOM: 상단=번역결과, 하단=인식텍스트
 *  - activeMic == TOP:    상단=인식텍스트, 하단=번역결과
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // ── 권한 요청 런처 (권한 없을 때만 실제로 다이얼로그 표시) ──────────────
    val bottomMicPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startSession()
    }
    val topMicPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startTopSession()
    }

    // 권한 확인 후: 이미 허용이면 바로 시작, 아니면 시스템 다이얼로그 표시
    fun checkPermissionAndStartBottom() {
        if (state.isSessionActive) {
            viewModel.stopSession()
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) viewModel.startSession()
            else bottomMicPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun checkPermissionAndStartTop() {
        if (state.isSessionActive) {
            viewModel.stopSession()
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) viewModel.startTopSession()
            else topMicPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // 대면 모드 회전 애니메이션 (iOS 0.35s easeInOut)
    val faceToFaceRotation by animateFloatAsState(
        targetValue = if (state.isFaceToFaceMode) 180f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "face_to_face"
    )

    // activeMic 기반 각 패널에 표시할 텍스트
    val topDisplayText = if (state.activeMic == ActiveMic.TOP)
        state.speechRecognition.recognizedText
    else
        state.translation.translatedText

    val bottomDisplayText = if (state.activeMic == ActiveMic.TOP)
        state.translation.translatedText
    else
        state.speechRecognition.recognizedText

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize()) {

        if (isLandscape) {
            // Landscape: left = blue (translation) panel, right = recognition panel
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(AccentBlue)
                        .graphicsLayer(rotationZ = faceToFaceRotation)
                ) {
                    TranslationPanel(
                        displayText = topDisplayText,
                        isTranslating = state.translation.isTranslating && state.activeMic == ActiveMic.BOTTOM,
                        language = state.topLanguage,
                        isSessionActive = state.activeMic == ActiveMic.TOP && state.isSessionActive,
                        isSttListening = state.speechRecognition.isListening && state.activeMic == ActiveMic.TOP,
                        sttError = if (state.activeMic == ActiveMic.TOP) state.speechRecognition.errorMessage else null,
                        translationError = if (state.activeMic == ActiveMic.BOTTOM) state.translation.errorMessage else null,
                        onLanguageTap = { viewModel.showTopPicker() },
                        onMicTap = { checkPermissionAndStartTop() },
                        onTap = { if (topDisplayText.isNotBlank()) viewModel.expandTopPanel() },
                        navigationBarsInsetOnBottomChrome = true,
                        bottomBarMicOnStart = true
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    RecognitionPanel(
                        displayText = bottomDisplayText,
                        isTranslating = state.translation.isTranslating && state.activeMic == ActiveMic.TOP,
                        isListening = state.speechRecognition.isListening && state.activeMic == ActiveMic.BOTTOM,
                        language = state.bottomLanguage,
                        isSessionActive = state.activeMic == ActiveMic.BOTTOM && state.isSessionActive,
                        sttError = if (state.activeMic == ActiveMic.BOTTOM) state.speechRecognition.errorMessage else null,
                        translationError = if (state.activeMic == ActiveMic.TOP) state.translation.errorMessage else null,
                        isFaceToFace = state.isFaceToFaceMode,
                        onFaceToFaceTap = { viewModel.toggleFaceToFaceMode() },
                        onSwapTap = { viewModel.swapLanguages() },
                        onLanguageTap = { viewModel.showBottomPicker() },
                        onMicTap = { checkPermissionAndStartBottom() },
                        onTap = { if (bottomDisplayText.isNotBlank()) viewModel.expandBottomPanel() },
                        statusBarsInsetOnScrollContent = true,
                        showFaceToFaceButton = false
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(AccentBlue)
                        .graphicsLayer(rotationZ = faceToFaceRotation)
                ) {
                    TranslationPanel(
                        displayText = topDisplayText,
                        isTranslating = state.translation.isTranslating && state.activeMic == ActiveMic.BOTTOM,
                        language = state.topLanguage,
                        isSessionActive = state.activeMic == ActiveMic.TOP && state.isSessionActive,
                        isSttListening = state.speechRecognition.isListening && state.activeMic == ActiveMic.TOP,
                        sttError = if (state.activeMic == ActiveMic.TOP) state.speechRecognition.errorMessage else null,
                        translationError = if (state.activeMic == ActiveMic.BOTTOM) state.translation.errorMessage else null,
                        onLanguageTap = { viewModel.showTopPicker() },
                        onMicTap = { checkPermissionAndStartTop() },
                        onTap = { if (topDisplayText.isNotBlank()) viewModel.expandTopPanel() }
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    RecognitionPanel(
                        displayText = bottomDisplayText,
                        isTranslating = state.translation.isTranslating && state.activeMic == ActiveMic.TOP,
                        isListening = state.speechRecognition.isListening && state.activeMic == ActiveMic.BOTTOM,
                        language = state.bottomLanguage,
                        isSessionActive = state.activeMic == ActiveMic.BOTTOM && state.isSessionActive,
                        sttError = if (state.activeMic == ActiveMic.BOTTOM) state.speechRecognition.errorMessage else null,
                        translationError = if (state.activeMic == ActiveMic.TOP) state.translation.errorMessage else null,
                        isFaceToFace = state.isFaceToFaceMode,
                        onFaceToFaceTap = { viewModel.toggleFaceToFaceMode() },
                        onSwapTap = { viewModel.swapLanguages() },
                        onLanguageTap = { viewModel.showBottomPicker() },
                        onMicTap = { checkPermissionAndStartBottom() },
                        onTap = { if (bottomDisplayText.isNotBlank()) viewModel.expandBottomPanel() }
                    )
                }
            }
        }

        // ── 언어 피커 (상단: topLanguage) ────────────────────────────────
        LanguagePickerOverlay(
            visible = state.isTopPickerVisible,
            selectedLanguage = state.topLanguage,
            backgroundColor = AccentBlue,
            textColor = Color.White,
            accentColor = Color.White,
            isFaceToFace = state.isFaceToFaceMode,
            onSelect = { viewModel.changeTopLanguage(it) },
            onDismiss = { viewModel.hideTopPicker() }
        )

        // ── 언어 피커 (하단: bottomLanguage) ─────────────────────────────
        LanguagePickerOverlay(
            visible = state.isBottomPickerVisible,
            selectedLanguage = state.bottomLanguage,
            backgroundColor = MaterialTheme.colorScheme.background,
            textColor = MaterialTheme.colorScheme.onBackground,
            accentColor = AccentBlue,
            isFaceToFace = false,
            onSelect = { viewModel.changeBottomLanguage(it) },
            onDismiss = { viewModel.hideBottomPicker() }
        )

        // ── 전체화면 확장 (상단) ─────────────────────────────────────────
        if (state.isTopExpanded) {
            FullscreenExpandedDialog(onDismissRequest = { viewModel.collapseTopPanel() }) {
                val topLang = state.topLanguage
                ExpandedTextView(
                    text = topDisplayText,
                    backgroundColor = AccentBlue,
                    textColor = Color.White,
                    isFaceToFace = state.isFaceToFaceMode,
                    isSpeaking = state.translation.isSpeaking,
                    onSpeak = { viewModel.speakExpanded(topDisplayText, topLang) },
                    onStopSpeak = { viewModel.stopSpeaking() },
                    onDismiss = { viewModel.collapseTopPanel() }
                )
            }
        }

        // ── 전체화면 확장 (하단) ─────────────────────────────────────────
        if (state.isBottomExpanded) {
            FullscreenExpandedDialog(onDismissRequest = { viewModel.collapseBottomPanel() }) {
                val bottomLang = state.bottomLanguage
                ExpandedTextView(
                    text = bottomDisplayText,
                    backgroundColor = MaterialTheme.colorScheme.background,
                    textColor = MaterialTheme.colorScheme.onBackground,
                    isFaceToFace = false,
                    isSpeaking = state.translation.isSpeaking,
                    onSpeak = { viewModel.speakExpanded(bottomDisplayText, bottomLang) },
                    onStopSpeak = { viewModel.stopSpeaking() },
                    onDismiss = { viewModel.collapseBottomPanel() }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// 상단 번역 패널
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun TranslationPanel(
    displayText: String,
    isTranslating: Boolean,
    language: SupportedLanguage,
    isSessionActive: Boolean,
    /** True when top mic session is actively capturing audio (STT pipeline running). */
    isSttListening: Boolean = false,
    /** Shown when Streaming STT failed for the top mic session. */
    sttError: String? = null,
    /** Shown when this panel displays translation and the Translate API failed. */
    translationError: String? = null,
    onLanguageTap: () -> Unit,
    onMicTap: () -> Unit,
    onTap: () -> Unit,
    /** Landscape: bottom chrome sits on screen edge — match right panel nav insets. */
    navigationBarsInsetOnBottomChrome: Boolean = false,
    /** Landscape left panel: mic on the left, language chip directly to its right. */
    bottomBarMicOnStart: Boolean = false
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTap
                )
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 130.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!sttError.isNullOrBlank()) {
                    Text(
                        text = sttError,
                        style = panelContentTextStyle(Color(0xFFFFB4AB)).copy(
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }
                if (!translationError.isNullOrBlank()) {
                    Text(
                        text = translationError,
                        style = panelContentTextStyle(Color(0xFFFFCC80)).copy(
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }
                Box(
                    modifier = Modifier.animateContentSize(
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    )
                ) {
                    if (displayText.isBlank()) {
                        Text(
                            text = if (isTranslating) "…" else UiCopy.PANEL_MIC_HINT,
                            style = panelContentTextStyle(Color.White.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        val bodyAlpha = if (isTranslating) 0.72f else 1f
                        Text(
                            text = displayText,
                            style = panelContentTextStyle(Color.White.copy(alpha = bodyAlpha)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AccentBlue.copy(alpha = 0f), AccentBlue, AccentBlue)
                    )
                )
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 20.dp,
                    bottom = if (navigationBarsInsetOnBottomChrome) 16.dp else 24.dp
                )
                .then(
                    if (navigationBarsInsetOnBottomChrome) {
                        Modifier.navigationBarsPadding()
                    } else {
                        Modifier
                    }
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (bottomBarMicOnStart) {
                    // Mic on the left, language chip immediately to its right (landscape left panel)
                    MicButton(
                        isListening = isSessionActive && isSttListening,
                        bgColor = Color.White.copy(alpha = 0.3f),
                        onClick = onMicTap
                    )
                    LanguageButton(language = language, textColor = Color.White, onClick = onLanguageTap)
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                    LanguageButton(language = language, textColor = Color.White, onClick = onLanguageTap)
                    MicButton(
                        isListening = isSessionActive && isSttListening,
                        bgColor = Color.White.copy(alpha = 0.3f),
                        onClick = onMicTap
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// 하단 음성 인식 패널
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun RecognitionPanel(
    displayText: String,
    /** True when this panel shows translation (top mic) and a new translation is in flight. */
    isTranslating: Boolean,
    isListening: Boolean,
    language: SupportedLanguage,
    isSessionActive: Boolean,
    /** Shown when Streaming STT failed for the bottom mic session. */
    sttError: String? = null,
    /** Shown when this panel displays translation and the Translate API failed. */
    translationError: String? = null,
    isFaceToFace: Boolean,
    onFaceToFaceTap: () -> Unit,
    onSwapTap: () -> Unit,
    onLanguageTap: () -> Unit,
    onMicTap: () -> Unit,
    onTap: () -> Unit,
    /** Landscape: panel spans full window height — match left panel status-bar inset on body text. */
    statusBarsInsetOnScrollContent: Boolean = false,
    /** Landscape: hide face-to-face (flip) control — two panels already face each user. */
    showFaceToFaceButton: Boolean = true
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (statusBarsInsetOnScrollContent) {
                        Modifier.statusBarsPadding()
                    } else {
                        Modifier
                    }
                )
                .verticalScroll(rememberScrollState())
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTap
                )
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 130.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!sttError.isNullOrBlank()) {
                    Text(
                        text = sttError,
                        style = panelContentTextStyle(MaterialTheme.colorScheme.error).copy(
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }
                if (!translationError.isNullOrBlank()) {
                    Text(
                        text = translationError,
                        style = panelContentTextStyle(MaterialTheme.colorScheme.error).copy(
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }
                Box(
                    modifier = Modifier.animateContentSize(
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    )
                ) {
                    if (displayText.isBlank()) {
                        val hint = when {
                            isTranslating -> "…"
                            isListening -> UiCopy.PANEL_LISTENING
                            else -> UiCopy.PANEL_MIC_HINT
                        }
                        Text(
                            text = hint,
                            style = panelContentTextStyle(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        val base = MaterialTheme.colorScheme.onBackground
                        val bodyColor = if (isTranslating) base.copy(alpha = 0.72f) else base
                        Text(
                            text = displayText,
                            style = panelContentTextStyle(bodyColor),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        val bgColor = MaterialTheme.colorScheme.background
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(colors = listOf(bgColor.copy(alpha = 0f), bgColor, bgColor)))
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showFaceToFaceButton) {
                    CircleIconButton(Icons.Filled.Group, UiCopy.A11Y_FACE_TO_FACE,
                        if (isFaceToFace) AccentBlue else MaterialTheme.colorScheme.secondary,
                        if (isFaceToFace) AccentBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                        onFaceToFaceTap)
                }
                CircleIconButton(Icons.Filled.SwapHoriz, UiCopy.A11Y_SWAP, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.surfaceVariant, onSwapTap)
                Spacer(modifier = Modifier.weight(1f))
                LanguageButton(language = language, textColor = MaterialTheme.colorScheme.onBackground, onClick = onLanguageTap)
                MicButton(
                    isListening = isSessionActive && isListening,
                    bgColor = AccentBlue,
                    onClick = onMicTap
                )
            }
        }
    }
}

@Composable
private fun LanguageButton(language: SupportedLanguage, textColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.width(110.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = language.flag, fontSize = 22.sp)
            Text(text = language.shortName, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textColor), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(imageVector = Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun CircleIconButton(icon: ImageVector, contentDescription: String, tint: Color, bgColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(40.dp).background(bgColor, CircleShape).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(18.dp))
    }
}
