package com.memodiary.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.memodiary.data.local.dao.MemoDao
import com.memodiary.data.local.entity.MemoEntity
import android.content.Context

@Database(entities = [MemoEntity::class], version = 1, exportSchema = false)
abstract class MemoDiaryDatabase : RoomDatabase() {

    abstract fun memoDao(): MemoDao

    companion object {
        @Volatile
        private var INSTANCE: MemoDiaryDatabase? = null

        fun getDatabase(context: Context): MemoDiaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemoDiaryDatabase::class.java,
                    "memo_diary_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}