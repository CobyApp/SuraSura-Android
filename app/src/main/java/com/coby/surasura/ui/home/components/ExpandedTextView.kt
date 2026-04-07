package com.coby.surasura.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
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

/**
 * 전체화면 텍스트 확장 뷰
 * iOS ExpandedTextView와 동일:
 *  - 상단: TTS 버튼(왼쪽) + 닫기 버튼(오른쪽)  (iOS: 스피커 + 원형 닫기)
 *  - 본문: 세로 스크롤 (가로·세로 회전 모두)
 *  - 대면 모드: 본문 영역만 미러 (툴바는 그대로 — 가로 Dialog에서 조작 가능)
 */
@Composable
fun ExpandedTextView(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    isFaceToFace: Boolean,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
    onStopSpeak: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = if (isSpeaking) onStopSpeak else onSpeak,
                enabled = text.isNotEmpty()
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Filled.Stop else Icons.Filled.RecordVoiceOver,
                    contentDescription = if (isSpeaking) "読み上げを停止" else "読み上げ",
                    tint = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = "閉じる",
                    tint = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer(
                    scaleX = if (isFaceToFace) -1f else 1f,
                    scaleY = if (isFaceToFace) -1f else 1f
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 28.dp, end = 28.dp, top = 8.dp, bottom = 24.dp)
            ) {
                Text(
                    text = text.ifEmpty { "\u3000" },
                    style = TextStyle(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 46.sp,
                        color = textColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
