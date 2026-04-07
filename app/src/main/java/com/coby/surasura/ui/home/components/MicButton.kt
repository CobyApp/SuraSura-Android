package com.coby.surasura.ui.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.coby.surasura.ui.UiCopy

/**
 * 마이크 버튼 — iOS MicButton과 완전히 동일
 *
 * 비활성: [bgColor] 배경, mic 아이콘, 펄스 없음
 * 활성:   빨간색 배경, stop 아이콘
 *         + 1.0→1.22x 0.85s 반복 펄스 링 (iOS: Circle().stroke(red.opacity(0.35), lineWidth:2.5))
 *
 * @param bgColor 비활성 상태 배경색 (상단=white.30%, 하단=accentBlue)
 */
@Composable
fun MicButton(
    isListening: Boolean,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 펄스 스케일 애니메이션 (iOS: 1.0 → 1.22, 0.85s easeInOut 반복)
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(76.dp)
    ) {
        // 펄스 링 (활성 시에만 표시)
        // iOS: Circle().stroke(Color.red.opacity(0.35), lineWidth: 2.5) — 테두리만
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .scale(pulseScale)
                    .drawBehind {
                        drawCircle(
                            color = Color.Red.copy(alpha = 0.35f),
                            radius = size.minDimension / 2f,
                            style = Stroke(width = 5f)   // 2.5pt → 5px 근사
                        )
                    }
            )
        }

        // Inner circle: no Compose shadow — it polygon-approximates and shows an octagonal halo
        // inside the circle (especially on the top panel’s translucent white fill).
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(
                    color = if (isListening) Color.Red else bgColor,
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = if (isListening) UiCopy.A11Y_STOP else UiCopy.A11Y_RECORD,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
