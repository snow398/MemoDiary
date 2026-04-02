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
}