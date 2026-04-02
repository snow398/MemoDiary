package com.memodiary.di

import android.content.Context
import com.memodiary.data.local.database.MemoDiaryDatabase
import com.memodiary.data.repository.MemoRepository

/**
 * Lightweight dependency container — replaces a full DI framework for this app.
 * Call [initialize] exactly once in [com.memodiary.MemoDiaryApplication.onCreate]
 * before any ViewModel accesses [repository].
 */
object AppModule {
    private lateinit var _database: MemoDiaryDatabase

    lateinit var repository: MemoRepository
        private set

    fun initialize(context: Context) {
        _database = MemoDiaryDatabase.getDatabase(context)
        repository = MemoRepository(_database.memoDao())
    }
}