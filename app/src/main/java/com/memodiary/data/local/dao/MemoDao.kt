package com.memodiary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.memodiary.data.local.entity.MemoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemo(memo: MemoEntity): Long

    @Update
    suspend fun updateMemo(memo: MemoEntity)

    /** Delete by primary key — avoids loading the entity just to delete it. */
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteMemoById(id: Long)

    /** Reactive stream of all memos, newest first. Room emits a new list on every change. */
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllMemos(): Flow<List<MemoEntity>>

    /** Full-text search across title and content, newest first. */
    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchMemos(query: String): Flow<List<MemoEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getMemoById(id: Long): MemoEntity?

    /** Memos whose createdAt falls in [startMs, endMs), newest first. Used by CalendarViewModel. */
    @Query("SELECT * FROM notes WHERE createdAt >= :startMs AND createdAt < :endMs ORDER BY createdAt DESC")
    fun getMemosByDateRange(startMs: Long, endMs: Long): Flow<List<MemoEntity>>

    /** All memos that have any address/location data, newest first. */
    @Query("SELECT * FROM notes WHERE city IS NOT NULL OR province IS NOT NULL OR country IS NOT NULL OR address IS NOT NULL ORDER BY createdAt DESC")
    fun getMemosWithLocation(): Flow<List<MemoEntity>>

    /** Memos for a specific city, newest first. */
    @Query("SELECT * FROM notes WHERE city = :city ORDER BY createdAt DESC")
    fun getMemosByCity(city: String): Flow<List<MemoEntity>>

    /** Search memos that have address/location fields, by city/province/country/address. */
    @Query("SELECT * FROM notes WHERE (city IS NOT NULL OR province IS NOT NULL OR country IS NOT NULL OR address IS NOT NULL) AND (city LIKE '%' || :query || '%' OR province LIKE '%' || :query || '%' OR country LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun searchMemosWithLocation(query: String): Flow<List<MemoEntity>>
}