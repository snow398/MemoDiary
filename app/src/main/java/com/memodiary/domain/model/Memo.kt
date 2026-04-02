package com.memodiary.domain.model

/**
 * Domain model for a single memo.
 * Timestamps are UTC epoch milliseconds so they map directly to/from the Room entity.
 */
data class Memo(
    val id: Long,
    val title: String,
    val content: String,
    val createdAt: Long,  // UTC epoch millis
    val updatedAt: Long   // UTC epoch millis
)