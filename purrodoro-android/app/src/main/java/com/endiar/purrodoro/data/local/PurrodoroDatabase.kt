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
        SoundscapeTrackEntity::class,
        SoundscapeBundledEntity::class,
    ],
    version = 5,
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
    abstract fun soundscapeTrackDao(): SoundscapeTrackDao
    abstract fun soundscapeBundledDao(): SoundscapeBundledDao

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

        /** v3 adds the soundscape_track cloud-catalog cache. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `soundscape_track` (" +
                        "`id` INTEGER NOT NULL, " +
                        "`kind` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`artist` TEXT NOT NULL, " +
                        "`symbol` TEXT NOT NULL, " +
                        "`filePath` TEXT NOT NULL, " +
                        "`fileSizeBytes` INTEGER NOT NULL, " +
                        "`isPremium` INTEGER NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL, " +
                        "`updatedAtMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
            }
        }

        /** v4 adds focusTypeName to session_records for local focus-type tracking. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `session_records` ADD COLUMN `focusTypeName` TEXT")
            }
        }

        /** v5 adds the soundscape_bundled overrides cache for APK tracks. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `soundscape_bundled` (" +
                        "`kind` TEXT NOT NULL, " +
                        "`localId` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`artist` TEXT NOT NULL, " +
                        "`symbol` TEXT NOT NULL, " +
                        "`rawResource` TEXT NOT NULL, " +
                        "`isPremium` INTEGER NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL, " +
                        "`isActive` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`kind`, `localId`))"
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build().also { INSTANCE = it }
            }
    }
}
