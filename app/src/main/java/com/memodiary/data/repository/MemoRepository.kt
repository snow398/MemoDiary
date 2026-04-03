package com.memodiary.data.repository

import com.memodiary.data.local.dao.MemoDao
import com.memodiary.data.local.entity.MemoEntity
import com.memodiary.domain.model.Memo
import com.memodiary.domain.model.MoodType
import com.memodiary.domain.model.NoteColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single source of truth for memo data.
 * Maps between MemoEntity (Room / database layer) and Memo (domain layer).
 */
class MemoRepository(private val memoDao: MemoDao) {

    fun getAllMemos(): Flow<List<Memo>> =
        memoDao.getAllMemos().map { list -> list.map { it.toDomain() } }

    fun searchMemos(query: String): Flow<List<Memo>> =
        memoDao.searchMemos(query).map { list -> list.map { it.toDomain() } }

    suspend fun getMemoById(id: Long): Memo? =
        memoDao.getMemoById(id)?.toDomain()

    suspend fun insertMemo(memo: Memo): Long =
        memoDao.insertMemo(memo.toEntity())

    suspend fun updateMemo(memo: Memo) =
        memoDao.updateMemo(memo.toEntity())

    suspend fun deleteMemo(id: Long) =
        memoDao.deleteMemoById(id)

    fun getMemosWithLocation(): Flow<List<Memo>> =
        memoDao.getMemosWithLocation().map { list -> list.map { it.toDomain() } }

    fun getMemosByCity(city: String): Flow<List<Memo>> =
        memoDao.getMemosByCity(city).map { list -> list.map { it.toDomain() } }

    fun searchMemosWithLocation(query: String): Flow<List<Memo>> =
        memoDao.searchMemosWithLocation(query).map { list -> list.map { it.toDomain() } }

    fun getMemosByDateRange(startMs: Long, endMs: Long): Flow<List<Memo>> =
        memoDao.getMemosByDateRange(startMs, endMs).map { list -> list.map { it.toDomain() } }

    // --------------- Mapping helpers ---------------

    private fun MemoEntity.toDomain() = Memo(
        id, title, content, createdAt, updatedAt,
        latitude, longitude, country, province, city, address,
        mood = try { MoodType.valueOf(mood ?: "NONE") } catch (_: Exception) { MoodType.NONE },
        imagePaths = imagePaths?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        noteColor = try { NoteColor.valueOf(noteColor ?: "NONE") } catch (_: Exception) { NoteColor.NONE }
    )
    private fun Memo.toEntity() = MemoEntity(
        id, title, content, createdAt, updatedAt,
        latitude, longitude, country, province, city, address,
        mood = if (mood == MoodType.NONE) null else mood.name,
        imagePaths = imagePaths.joinToString(",").ifBlank { null },
        noteColor = if (noteColor == NoteColor.NONE) null else noteColor.name
    )
}