package com.memodiary.domain.model

import androidx.compose.ui.graphics.Color

enum class NoteColor(val label: String, val color: Color, val onColor: Color) {
    NONE("默认", Color.Unspecified, Color.Unspecified),
    PINK("浅粉", Color(0xFFFCE4EC), Color(0xFF880E4F)),
    BLUE("浅蓝", Color(0xFFE3F2FD), Color(0xFF0D47A1)),
    YELLOW("浅黄", Color(0xFFFFFDE7), Color(0xFF827717))
}
