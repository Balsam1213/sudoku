package com.balsam.sudoku.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        GameRecord::class,
        AchievementEntity::class,
        DailyChallengeEntity::class,
        MonthTrophyEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameRecordDao(): GameRecordDao
    abstract fun achievementDao(): AchievementDao
    abstract fun dailyChallengeDao(): DailyChallengeDao
    abstract fun monthTrophyDao(): MonthTrophyDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE game_records ADD COLUMN notesUsed INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS daily_challenges (" +
                        "date TEXT NOT NULL PRIMARY KEY, " +
                        "difficulty TEXT NOT NULL, " +
                        "durationSeconds INTEGER NOT NULL, " +
                        "mistakes INTEGER NOT NULL, " +
                        "hintsUsed INTEGER NOT NULL, " +
                        "completedAt INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS month_trophies (" +
                        "month TEXT NOT NULL PRIMARY KEY, " +
                        "unlockedAt INTEGER NOT NULL)",
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sudoku.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
