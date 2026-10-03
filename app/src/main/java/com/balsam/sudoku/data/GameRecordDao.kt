package com.balsam.sudoku.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GameRecordDao {

    @Insert
    suspend fun insert(record: GameRecord): Long

    @Query("SELECT * FROM game_records ORDER BY completedAt DESC LIMIT :limit")
    suspend fun recentRecords(limit: Int): List<GameRecord>

    @Query(
        """
        SELECT * FROM (
            SELECT difficulty,
                   COUNT(*) AS played,
                   SUM(CASE WHEN isWin THEN 1 ELSE 0 END) AS wins,
                   MIN(CASE WHEN isWin THEN durationSeconds END) AS bestTimeSeconds,
                   AVG(CASE WHEN isWin THEN durationSeconds END) AS averageTimeSeconds,
                   MAX(completedAt) AS lastPlayedAt
            FROM game_records
            WHERE difficulty = :difficulty
        ) WHERE played > 0
        """
    )
    suspend fun statsFor(difficulty: String): DifficultyStats?

    @Query(
        """
        SELECT difficulty,
               COUNT(*) AS played,
               SUM(CASE WHEN isWin THEN 1 ELSE 0 END) AS wins,
               MIN(CASE WHEN isWin THEN durationSeconds END) AS bestTimeSeconds,
               AVG(CASE WHEN isWin THEN durationSeconds END) AS averageTimeSeconds,
               MAX(completedAt) AS lastPlayedAt
        FROM game_records
        GROUP BY difficulty
        """
    )
    fun statsFlow(): kotlinx.coroutines.flow.Flow<List<DifficultyStats>>

    @Query("SELECT COUNT(*) FROM game_records WHERE isWin = 1")
    suspend fun totalWins(): Int

    @Query("SELECT completedAt FROM game_records WHERE isWin = 1 ORDER BY completedAt DESC LIMIT 60")
    suspend fun recentWinTimes(): List<Long>

    @Query("SELECT completedAt FROM game_records WHERE isWin = 1 AND completedAt >= :since")
    suspend fun winTimesSince(since: Long): List<Long>

    @Query("SELECT MIN(completedAt) FROM game_records WHERE isWin = 1 AND difficulty = :difficulty")
    suspend fun firstWinTime(difficulty: String): Long?

    @Query("SELECT * FROM game_records WHERE isWin = 1 AND difficulty = :difficulty ORDER BY durationSeconds ASC LIMIT 1")
    suspend fun bestRecord(difficulty: String): GameRecord?

    @Query("SELECT COUNT(*) FROM game_records")
    suspend fun totalCount(): Int

    /** 最近的胜利记录（每日挑战难度自适应用）。 */
    @Query("SELECT * FROM game_records WHERE isWin = 1 ORDER BY completedAt DESC LIMIT :limit")
    suspend fun recentWins(limit: Int): List<GameRecord>

    /** 有胜利记录的日期串（yyyy-MM-dd），去重后按日期倒序。 */
    @Query(
        """
        SELECT DISTINCT date(completedAt / 1000, 'unixepoch', 'localtime') AS d
        FROM game_records WHERE isWin = 1
        ORDER BY d DESC LIMIT :limit
        """
    )
    suspend fun distinctWinDates(limit: Int): List<String>

    /** 累计使用提示次数（跨所有对局，含未完成的对局）。 */
    @Query("SELECT SUM(hintsUsed) FROM game_records")
    suspend fun totalHintsUsed(): Int?

    /** 使用过铅笔笔记的对局数。 */
    @Query("SELECT COUNT(*) FROM game_records WHERE notesUsed = 1")
    suspend fun pencilGamesCount(): Int
}
