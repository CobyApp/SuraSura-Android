package com.coby.surasura.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coby.surasura.data.model.SupportedLanguage

/**
 * 전체화면 텍스트 확장 뷰
 * iOS ExpandedTextView와 동일:
 *  - 상단: TTS 버튼(왼쪽) + 닫기 버튼(오른쪽)  (iOS: speaker.wave.2 + xmark.circle.fill)
 *  - 중앙 스크롤: 34sp 텍스트, lineHeight 10pt 간격
 *  - 대면 모드 지원 (iOS scaleEffect x:-1, y:-1)
 */
@Composable
fun ExpandedTextView(
    text: String,
    language: SupportedLanguage,
    backgroundColor: Color,
    textColor: Color,
    isFaceToFace: Boolean,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
    onStopSpeak: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            // 대면 모드: 180도 회전 (iOS scaleEffect x:-1, y:-1)
            .graphicsLayer(
                scaleX = if (isFaceToFace) -1f else 1f,
                scaleY = if (isFaceToFace) -1f else 1f
            )
    ) {
        // 텍스트 스크롤 영역 (iOS: horizontal 28, vertical 28, fontSize 34, lineSpacing 10)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 72.dp, start = 28.dp, end = 28.dp, bottom = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = text.ifEmpty { "\u3000" },
                style = TextStyle(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 46.sp,   // iOS: lineSpacing 10 → 34+10=44
                    color = textColor
                )
            )
        }

        // 상단 버튼 행: [TTS][Spacer][닫기]
        // iOS: HStack { speakerBtn, Spacer, closeBtn } padding(horizontal:20, top:12)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 8.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TTS 버튼 (iOS: speaker.wave.2 / speaker.wave.3.fill)
            IconButton(
                onClick = if (isSpeaking) onStopSpeak else onSpeak,
                enabled = text.isNotEmpty()
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isSpeaking) "음성 중지" else "음성 재생",
                    tint = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 닫기 버튼 (iOS: xmark.circle.fill, 30pt)
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = "닫기",
                    tint = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}
