package com.endiar.purrodoro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local store for account-owned domain data (tasks, categories, habits and
 * their logs, session records, daily stamps). Replaces the legacy DataStore
 * JSON blobs; rows carry sync bookkeeping for the offline-first sync engine.
 */
@Database(
    entities = [
        TaskEntity::class,
        CategoryEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        SessionRecordEntity::class,
        DailyStampEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class PurrodoroDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun sessionRecordDao(): SessionRecordDao
    abstract fun dailyStampDao(): DailyStampDao

    companion object {
        @Volatile private var INSTANCE: PurrodoroDatabase? = null

        fun get(context: Context): PurrodoroDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PurrodoroDatabase::class.java,
                    "purrodoro.db",
                ).build().also { INSTANCE = it }
            }
    }
}
