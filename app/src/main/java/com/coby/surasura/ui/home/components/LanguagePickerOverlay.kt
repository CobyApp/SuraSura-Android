package com.coby.surasura.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coby.surasura.data.model.SupportedLanguage
import com.coby.surasura.ui.UiCopy

/** Japanese first, then remaining languages in enum declaration order. */
private val languagesInPickerOrder: List<SupportedLanguage> =
    listOf(SupportedLanguage.JAPANESE) +
        SupportedLanguage.entries.filter { it != SupportedLanguage.JAPANESE }

/**
 * 언어 선택 오버레이
 * iOS LanguagePickerOverlay.swift와 동일:
 *  - 헤더: 뒤로가기 버튼
 *  - 언어 목록: displayName + 보조 라벨
 *  - 선택된 언어: 배경 강조 + 체크마크
 *  - 대면 모드 지원 (graphicsLayer scaleX/Y -1)
 */
@Composable
fun LanguagePickerOverlay(
    visible: Boolean,
    selectedLanguage: SupportedLanguage,
    backgroundColor: Color,
    textColor: Color,
    accentColor: Color,
    isFaceToFace: Boolean,
    onSelect: (SupportedLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    // iOS: .transition(.opacity.combined(with: .scale(scale: 0.98, anchor: .center)))
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.98f),
        exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.98f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .graphicsLayer(
                    scaleX = if (isFaceToFace) -1f else 1f,
                    scaleY = if (isFaceToFace) -1f else 1f
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // 헤더 (뒤로 버튼) — extra top inset so back button clears status bar / notch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = UiCopy.PICKER_BACK,
                            tint = textColor.copy(alpha = 0.9f)
                        )
                    }
                }

                // 구분선 (iOS: Divider.opacity(0.2))
                HorizontalDivider(
                    color = textColor.copy(alpha = 0.2f),
                    thickness = 0.5.dp
                )

                // 언어 목록
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(languagesInPickerOrder) { language ->
                        LanguageRow(
                            language = language,
                            isSelected = language == selectedLanguage,
                            textColor = textColor,
                            accentColor = accentColor,
                            onSelect = {
                                onSelect(language)
                                onDismiss()
                            }
                        )
                        if (language != languagesInPickerOrder.last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 68.dp),
                                color = textColor.copy(alpha = 0.12f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

/**
 * 언어 행
 * iOS: flag(28) + VStack(names) + checkmark
 */
@Composable
private fun LanguageRow(
    language: SupportedLanguage,
    isSelected: Boolean,
    textColor: Color,
    accentColor: Color,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) accentColor.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onSelect)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 국기 이모지 (iOS: font size 28)
        Text(text = language.flag, fontSize = 28.sp)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = language.displayName,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = textColor
            )
            if (language.shortName != language.displayName) {
                Text(
                    text = language.shortName,
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.45f)
                )
            }
        }

        // 체크마크 (선택된 언어)
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
