package com.balsam.sudoku.achievements

import com.balsam.sudoku.data.AchievementDao
import com.balsam.sudoku.data.AchievementEntity
import com.balsam.sudoku.data.DailyChallengeDao
import com.balsam.sudoku.data.GameRecordDao
import com.balsam.sudoku.game.Difficulty
import com.balsam.sudoku.game.GameLogic
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** 一局结束后用于成就判定的上下文（记录已写入数据库之后构建）。 */
data class GameEndContext(
    val difficulty: Difficulty,
    val durationSeconds: Int,
    val mistakes: Int,
    val hintsUsed: Int,
    val notesUsed: Boolean,
    val isWin: Boolean,
    val isNewRecord: Boolean,
    val bestTimeBefore: Int?,
    /** 本局是否为每日挑战（且为当日首次完成）。 */
    val isDailyWin: Boolean = false,
    val zone: ZoneId = ZoneId.systemDefault(),
)

/** 成就判定：三类评估入口，返回每次评估中新解锁的成就。 */
class AchievementEngine(
    private val achievementDao: AchievementDao,
    private val recordDao: GameRecordDao,
    private val dailyDao: DailyChallengeDao? = null,
) {

    private var unlockedCache: MutableSet<String>? = null

    private suspend fun unlocked(): MutableSet<String> {
        unlockedCache?.let { return it }
        val set = achievementDao.all().mapTo(mutableSetOf()) { it.id }
        unlockedCache = set
        return set
    }

    private suspend fun unlockIfNew(id: String, newly: MutableList<AchievementDef>) {
        if (id in unlocked()) return
        achievementDao.unlock(AchievementEntity(id, System.currentTimeMillis()))
        unlocked().add(id)
        Achievements.byId(id)?.let { newly.add(it) }
    }

    /** 一局结束（胜负记录已写入）后的整体评估。 */
    suspend fun evaluateGameEnd(ctx: GameEndContext): List<AchievementDef> {
        val newly = mutableListOf<AchievementDef>()
        val totalWins = recordDao.totalWins()
        val winsByDifficulty = Difficulty.entries.associateWith { d ->
            recordDao.statsFor(d.name)?.winCount() ?: 0
        }
        val distinctDays = if (ctx.isWin) recordDao.distinctWinDates(MAX_DAY_QUERY).size else 0
        val pencilGames = recordDao.pencilGamesCount()
        val totalHints = recordDao.totalHintsUsed() ?: 0
        val today = LocalDate.now(ctx.zone)
        val todayWins = recordDao.winTimesSince(today.atStartOfDay(ctx.zone).toInstant().toEpochMilli()).size
        val dailyCount = dailyDao?.countAll() ?: 0

        for (def in Achievements.ALL) {
            val satisfied = when {
                def.id == "first_win" -> ctx.isWin && totalWins >= 1
                def.id == "new_record" -> ctx.isWin && ctx.isNewRecord
                def.id == "daily_5" -> ctx.isWin && todayWins >= 5
                def.id.startsWith("win_") -> {
                    val d = Difficulty.entries.firstOrNull { def.id == "win_${it.name.lowercase()}" }
                    d != null && ctx.isWin && (winsByDifficulty[d] ?: 0) >= 1
                }
                def.id.startsWith("total_") -> ctx.isWin && totalWins >= def.id.removePrefix("total_").toIntOrNull().orZero()
                def.id.startsWith("count_") -> {
                    // count_<DIFF>_<n>
                    val parts = def.id.split('_')
                    if (parts.size != 3) false
                    else {
                        val d = Difficulty.entries.firstOrNull { it.name == parts[1] }
                        d != null && ctx.isWin && (winsByDifficulty[d] ?: 0) >= parts[2].toIntOrNull().orZero()
                    }
                }
                def.id.startsWith("days_") -> ctx.isWin && distinctDays >= def.id.removePrefix("days_").toIntOrNull().orZero()
                def.id.startsWith("pencil_") -> pencilGames >= def.id.removePrefix("pencil_").toIntOrNull().orZero()
                def.id.startsWith("hints_") -> totalHints >= def.id.removePrefix("hints_").toIntOrNull().orZero()
                def.id.startsWith("daily_") -> ctx.isDailyWin && dailyCount >= def.id.removePrefix("daily_").toIntOrNull().orZero()
                def.id == "speed_10min" -> ctx.isWin && ctx.durationSeconds < 600
                def.id == "speed_expert_30" -> ctx.isWin && ctx.difficulty == Difficulty.EXPERT && ctx.durationSeconds < 1800
                def.id == "zero_mistake" -> ctx.isWin && ctx.mistakes == 0
                def.id == "no_hint" -> ctx.isWin && ctx.hintsUsed == 0
                def.id == "no_notes_hard" -> ctx.isWin && !ctx.notesUsed && ctx.difficulty in HARD_OR_ABOVE
                else -> false
            }
            if (satisfied) unlockIfNew(def.id, newly)
        }
        return newly
    }

    /** 玩家填对一个数字后的局中评估（含行/列/宫完整判定）。 */
    suspend fun evaluatePlacement(values: List<Int>, solution: List<Int>, cell: Int): List<AchievementDef> {
        val newly = mutableListOf<AchievementDef>()
        unlockIfNew("first_correct", newly)
        if (GameLogic.isRegionCorrect(values, solution, GameLogic.rowCells(cell))) unlockIfNew("first_row", newly)
        if (GameLogic.isRegionCorrect(values, solution, GameLogic.colCells(cell))) unlockIfNew("first_col", newly)
        if (GameLogic.isRegionCorrect(values, solution, GameLogic.boxCells(cell))) unlockIfNew("first_box", newly)
        return newly
    }

    /** 使用提示后的累计里程碑评估（totalHints = 历史记录合计 + 本局已用）。 */
    suspend fun evaluateHintMilestones(totalHints: Int): List<AchievementDef> {
        val newly = mutableListOf<AchievementDef>()
        for (n in listOf(1, 5, 10, 20, 50, 100)) {
            if (totalHints >= n) unlockIfNew("hints_$n", newly)
        }
        return newly
    }

    private val HARD_OR_ABOVE =
        setOf(Difficulty.HARD, Difficulty.EXPERT, Difficulty.EXTREME)

    companion object {
        private const val MAX_DAY_QUERY = 500

        private fun Int?.orZero(): Int = this ?: 0

        /** 每日挑战难度自适应：最近完成的对局难度众数（并列取更难），无历史 → 简单。 */
        fun suggestDailyDifficulty(recentWinDifficulties: List<Difficulty>): Difficulty {
            if (recentWinDifficulties.isEmpty()) return Difficulty.EASY
            val counts = recentWinDifficulties.groupingBy { it }.eachCount()
            val maxCount = counts.values.max()
            return Difficulty.entries
                .filter { counts[it] == maxCount }
                .max() // 并列取更难的难度
        }

        /** 月度全勤：completedDates 为 "yyyy-MM-dd" 集合，该月每一天都完成才算全勤。 */
        fun isMonthComplete(completedDates: Set<String>, month: YearMonth): Boolean {
            val days = month.lengthOfMonth()
            return (1..days).all { d -> "%s-%02d".format(month, d) in completedDates }
        }

        /** 由去重后的完成日期计算（当前连续天数, 最长连续天数）。 */
        fun computeStreaks(winDates: List<LocalDate>, today: LocalDate): Pair<Int, Int> {
            if (winDates.isEmpty()) return 0 to 0
            val sorted = winDates.distinct().sorted()
            // 最长连续
            var longest = 1
            var run = 1
            for (i in 1 until sorted.size) {
                run = if (sorted[i] == sorted[i - 1].plusDays(1)) run + 1 else 1
                if (run > longest) longest = run
            }
            // 当前连续：允许今天或昨天作为起点
            var current = 0
            var expected = if (today in sorted) today else today.minusDays(1)
            for (date in sorted.asReversed()) {
                if (date == expected) {
                    current++
                    expected = date.minusDays(1)
                } else if (date < expected) {
                    break
                }
            }
            return current to longest
        }
    }
}
