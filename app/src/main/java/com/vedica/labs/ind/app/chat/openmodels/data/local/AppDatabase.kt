package com.vedica.labs.ind.app.chat.openmodels.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vedica.labs.ind.app.chat.openmodels.data.local.dao.*
import com.vedica.labs.ind.app.chat.openmodels.data.local.entity.*

@Database(
    entities = [
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        BenchmarkEntity::class,
        FileContextEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun benchmarkDao(): BenchmarkDao
    abstract fun fileContextDao(): FileContextDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "openmodels_chat.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
