package com.memodiary

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.memodiary.data.local.database.MemoDiaryDatabase
import com.memodiary.data.local.dao.MemoDao
import com.memodiary.data.local.entity.MemoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var database: MemoDiaryDatabase
    private lateinit var memoDao: MemoDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Use in-memory DB so tests do not affect real data
        database = Room.inMemoryDatabaseBuilder(context, MemoDiaryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        memoDao = database.memoDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetMemo() = runBlocking {
        val memo = MemoEntity(
            id = 0,
            title = "Test Title",
            content = "Test Content",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val newId = memoDao.insertMemo(memo)

        val retrieved = memoDao.getMemoById(newId)
        assertEquals(memo.title, retrieved?.title)
        assertEquals(memo.content, retrieved?.content)
    }

    @Test
    fun deleteMemo() = runBlocking {
        val memo = MemoEntity(
            id = 0,
            title = "Delete Me",
            content = "Will be deleted",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val newId = memoDao.insertMemo(memo)
        memoDao.deleteMemoById(newId)

        val retrieved = memoDao.getMemoById(newId)
        assertNull(retrieved)
    }

    @Test
    fun updateMemo() = runBlocking {
        val memo = MemoEntity(
            id = 0,
            title = "Original",
            content = "Original content",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val newId = memoDao.insertMemo(memo)

        val updated = memo.copy(id = newId, title = "Updated", content = "Updated content")
        memoDao.updateMemo(updated)

        val retrieved = memoDao.getMemoById(newId)
        assertEquals("Updated", retrieved?.title)
        assertEquals("Updated content", retrieved?.content)
    }

    @Test
    fun getAllMemosReturnsAll() = runBlocking {
        memoDao.insertMemo(MemoEntity(0, "A", "a", 1000L, 1000L))
        memoDao.insertMemo(MemoEntity(0, "B", "b", 2000L, 2000L))

        val all = memoDao.getAllMemos().first()
        assertEquals(2, all.size)
    }
}