package com.example.features.lyrics

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

enum class LyricsStylePreset(
    val presetName: String,
    val fontSize: TextUnit,
    val fontWeight: FontWeight,
    val fontFamily: FontFamily,
    val textColor: Color,
    val textAlign: TextAlign,
    val isLowerThird: Boolean
) {
    WORSHIP(
        presetName = "Worship",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
        textColor = Color.White,
        textAlign = TextAlign.Center,
        isLowerThird = false
    ),
    MODERN(
        presetName = "Modern",
        fontSize = 32.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.SansSerif,
        textColor = Color(0xFFD0BCFF),
        textAlign = TextAlign.Center,
        isLowerThird = false
    ),
    CLASSIC(
        presetName = "Classic",
        fontSize = 26.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Serif,
        textColor = Color(0xFFFFFBEB),
        textAlign = TextAlign.Center,
        isLowerThird = false
    ),
    MINIMAL(
        presetName = "Lower Third",
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.SansSerif,
        textColor = Color.White,
        textAlign = TextAlign.Start,
        isLowerThird = true
    )
}
