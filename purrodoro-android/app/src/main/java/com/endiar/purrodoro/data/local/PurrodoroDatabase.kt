package com.endiar.purrodoro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Local store for account-owned domain data (tasks, categories, habits and
 * their logs, session records, daily stamps, the singleton profile row).
 * Replaces the legacy DataStore JSON blobs; rows carry sync bookkeeping for
 * the offline-first sync engine.
 */
@Database(
    entities = [
        TaskEntity::class,
        CategoryEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        SessionRecordEntity::class,
        DailyStampEntity::class,
        ProfileEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class PurrodoroDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun sessionRecordDao(): SessionRecordDao
    abstract fun dailyStampDao(): DailyStampDao
    abstract fun profileDao(): ProfileDao

    companion object {
        /**
         * v2 adds the singleton [ProfileEntity] row for profile sync.
         *
         * Accepted limitation: [exportSchema] is off, so there is no
         * exported-schema-based MigrationTestHelper coverage — migrations are
         * verified by inspection against the entity definitions pre-prod.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `profile` (" +
                        "`rowId` INTEGER NOT NULL, " +
                        "`displayName` TEXT NOT NULL, " +
                        "`timerSettingsJson` TEXT NOT NULL, " +
                        "`dirty` INTEGER NOT NULL, " +
                        "`locallyModifiedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`rowId`))"
                )
            }
        }

        @Volatile private var INSTANCE: PurrodoroDatabase? = null

        fun get(context: Context): PurrodoroDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PurrodoroDatabase::class.java,
                    "purrodoro.db",
                ).addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
    }
}
