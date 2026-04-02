package com.memodiary

import com.memodiary.data.local.dao.MemoDao
import com.memodiary.data.local.entity.MemoEntity
import com.memodiary.data.repository.MemoRepository
import com.memodiary.domain.model.Memo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit test for [MemoRepository] using a hand-rolled fake DAO.
 * No Android components are required — runs on the JVM.
 */
class MemoRepositoryTest {

    private lateinit var fakeDao: FakeMemoDao
    private lateinit var repository: MemoRepository

    @Before
    fun setup() {
        fakeDao = FakeMemoDao()
        repository = MemoRepository(fakeDao)
    }

    @Test
    fun insertMemo_retrievesMemo() = runBlocking {
        val memo = Memo(id = 0, title = "Hello", content = "World",
            createdAt = 1_000L, updatedAt = 1_000L)
        val newId = repository.insertMemo(memo)

        val retrieved = repository.getMemoById(newId)
        assertEquals(memo.title, retrieved?.title)
        assertEquals(memo.content, retrieved?.content)
    }

    @Test
    fun deleteMemo_retrievesNull() = runBlocking {
        val memo = Memo(id = 0, title = "To Delete", content = "gone",
            createdAt = 2_000L, updatedAt = 2_000L)
        val newId = repository.insertMemo(memo)
        repository.deleteMemo(newId)

        val retrieved = repository.getMemoById(newId)
        assertNull(retrieved)
    }

    @Test
    fun updateMemo_retrievesUpdatedMemo() = runBlocking {
        val memo = Memo(id = 0, title = "Original", content = "initial",
            createdAt = 3_000L, updatedAt = 3_000L)
        val newId = repository.insertMemo(memo)

        val updated = memo.copy(id = newId, title = "Updated", content = "changed")
        repository.updateMemo(updated)

        val retrieved = repository.getMemoById(newId)
        assertEquals("Updated", retrieved?.title)
        assertEquals("changed", retrieved?.content)
    }
}

// --------------- Fake DAO --------------- //

private class FakeMemoDao : MemoDao {
    private val store = mutableMapOf<Long, MemoEntity>()
    private var nextId = 1L

    override suspend fun insertMemo(memo: MemoEntity): Long {
        val id = if (memo.id == 0L) nextId++ else memo.id
        store[id] = memo.copy(id = id)
        return id
    }

    override suspend fun updateMemo(memo: MemoEntity) {
        if (store.containsKey(memo.id)) store[memo.id] = memo
    }

    override suspend fun deleteMemoById(id: Long) { store.remove(id) }

    override fun getAllMemos(): Flow<List<MemoEntity>> = flowOf(store.values.toList())

    override fun searchMemos(query: String): Flow<List<MemoEntity>> =
        flowOf(store.values.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.content.contains(query, ignoreCase = true)
        })

    override suspend fun getMemoById(id: Long): MemoEntity? = store[id]
}