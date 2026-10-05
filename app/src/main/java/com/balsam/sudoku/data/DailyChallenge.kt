package com.balsam.sudoku.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/** 每日挑战完成记录：date 为 "yyyy-MM-dd"，每天只保留首次完成。 */
@Entity(tableName = "daily_challenges")
data class DailyChallengeEntity(
    @PrimaryKey val date: String,
    val difficulty: String,
    val durationSeconds: Int,
    val mistakes: Int,
    val hintsUsed: Int,
    val completedAt: Long,
)

/** 每日挑战出题计划：date 为 "yyyy-MM-dd"，首次进入该日期的对局时写入，保证同一天永远同一道题。 */
@Entity(tableName = "daily_plans")
data class DailyPlanEntity(
    @PrimaryKey val date: String,
    val difficulty: String,
)

@Dao
interface DailyPlanDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: DailyPlanEntity): Long

    @Query("SELECT * FROM daily_plans WHERE date = :date")
    suspend fun getByDate(date: String): DailyPlanEntity?

    @Query("SELECT * FROM daily_plans WHERE date LIKE :monthPrefix || '%'")
    suspend fun monthRows(monthPrefix: String): List<DailyPlanEntity>
}

/** 月度全勤奖杯：month 为 "yyyy-MM"。 */
@Entity(tableName = "month_trophies")
data class MonthTrophyEntity(
    @PrimaryKey val month: String,
    val unlockedAt: Long,
)

@Dao
interface DailyChallengeDao {

    /** 只在当天还没有完成记录时写入（重玩不覆盖）。返回 true 表示本次为首次完成。 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: DailyChallengeEntity): Long

    @Query("SELECT * FROM daily_challenges WHERE date = :date")
    suspend fun getByDate(date: String): DailyChallengeEntity?

    @Query("SELECT date FROM daily_challenges WHERE date LIKE :monthPrefix || '%'")
    suspend fun monthDates(monthPrefix: String): List<String>

    /** 当月完成明细（日历展示用：日期、用时、难度）。 */
    @Query("SELECT * FROM daily_challenges WHERE date LIKE :monthPrefix || '%' ORDER BY date")
    suspend fun monthRows(monthPrefix: String): List<DailyChallengeEntity>

    @Query("SELECT COUNT(*) FROM daily_challenges")
    suspend fun countAll(): Int

    @Query("SELECT * FROM daily_challenges ORDER BY date DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<DailyChallengeEntity>
}

@Dao
interface MonthTrophyDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlock(entity: MonthTrophyEntity): Long

    @Query("SELECT * FROM month_trophies ORDER BY month DESC")
    suspend fun all(): List<MonthTrophyEntity>
}
