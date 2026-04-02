package com.memodiary.domain.model

import com.memodiary.domain.model.MoodType

/**
 * Domain model for a single memo.
 * Timestamps are UTC epoch milliseconds so they map directly to/from the Room entity.
 */
data class Memo(
    val id: Long,
    val title: String,
    val content: String,
    val createdAt: Long,  // UTC epoch millis
    val updatedAt: Long,  // UTC epoch millis
    val latitude: Double? = null,
    val longitude: Double? = null,
    val country: String? = null,
    val province: String? = null,
    val city: String? = null,
    val address: String? = null,
    val mood: MoodType = MoodType.NONE,
    val imagePaths: List<String> = emptyList()
)