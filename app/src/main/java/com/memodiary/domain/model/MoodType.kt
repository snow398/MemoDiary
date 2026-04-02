package com.memodiary.domain.model

enum class MoodType(val label: String, val emoji: String) {
    NONE("不记录", ""),
    HAPPY("开心", "😊"),
    NEUTRAL("普通", "😐"),
    SAD("难过", "😢")
}
