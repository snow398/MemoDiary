package com.memodiary.di

import android.content.Context
import com.memodiary.data.local.database.MemoDiaryDatabase
import com.memodiary.data.location.LocationRepository
import com.memodiary.data.repository.MemoRepository

object AppModule {
    private lateinit var _database: MemoDiaryDatabase

    lateinit var repository: MemoRepository
        private set

    lateinit var locationRepository: LocationRepository
        private set

    fun initialize(context: Context) {
        _database = MemoDiaryDatabase.getDatabase(context)
        repository = MemoRepository(_database.memoDao())
        locationRepository = LocationRepository(context)
    }
}