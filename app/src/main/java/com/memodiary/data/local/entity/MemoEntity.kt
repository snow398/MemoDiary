package com.memodiary.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the "notes" table.
 * Timestamps are stored as UTC epoch milliseconds (Long) — no TypeConverter needed.
 */
@Entity(tableName = "notes")
data class MemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    val mood: String? = null,          // MoodType.name
    val imagePaths: String? = null,    // comma-separated absolute file paths
    val noteColor: String? = null      // NoteColor.name
)