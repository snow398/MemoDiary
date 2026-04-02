package com.memodiary.data.repository

import com.memodiary.data.local.dao.MemoDao
import com.memodiary.data.local.entity.MemoEntity
import com.memodiary.domain.model.Memo
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

    // --------------- Mapping helpers ---------------

    private fun MemoEntity.toDomain() = Memo(id, title, content, createdAt, updatedAt)
    private fun Memo.toEntity() = MemoEntity(id, title, content, createdAt, updatedAt)
}