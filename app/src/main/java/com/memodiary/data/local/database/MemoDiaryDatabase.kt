package com.memodiary.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.memodiary.data.local.dao.MemoDao
import com.memodiary.data.local.entity.MemoEntity
import android.content.Context

@Database(entities = [MemoEntity::class], version = 2, exportSchema = false)
abstract class MemoDiaryDatabase : RoomDatabase() {

    abstract fun memoDao(): MemoDao

    companion object {
        @Volatile
        private var INSTANCE: MemoDiaryDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE notes ADD COLUMN longitude REAL")
                db.execSQL("ALTER TABLE notes ADD COLUMN country TEXT")
                db.execSQL("ALTER TABLE notes ADD COLUMN province TEXT")
                db.execSQL("ALTER TABLE notes ADD COLUMN city TEXT")
                db.execSQL("ALTER TABLE notes ADD COLUMN address TEXT")
            }
        }

        fun getDatabase(context: Context): MemoDiaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemoDiaryDatabase::class.java,
                    "memo_diary_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}