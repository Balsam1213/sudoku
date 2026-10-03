package com.balsam.sudoku

import com.balsam.sudoku.achievements.AchievementEngine
import com.balsam.sudoku.achievements.Achievements
import com.balsam.sudoku.game.Difficulty
import com.balsam.sudoku.game.GameLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class AchievementSystemTest {

    @Test
    fun achievements_idsAreUniqueAndComplete() {
        val ids = Achievements.ALL.map { it.id }
        assertEquals("成就 id 有重复", ids.size, ids.toSet().size)
        // 手写固定成就（15）+ 程序化生成（6 难度×6 + 天数5 + 铅笔6 + 提示6 + 每日4）
        assertEquals(15 + 36 + 5 + 6 + 6 + 4, Achievements.ALL.size)
        // 每个分组都有成就，且规模在 4~6 个之间（避免臃肿）
        for (group in com.balsam.sudoku.achievements.AchievementGroup.entries) {
            val size = Achievements.ALL.count { it.group == group }
            assertTrue("分组 ${group.title} 只有 $size 个成就", size in 4..6)
        }
        assertEquals(13, com.balsam.sudoku.achievements.AchievementGroup.entries.size)
        // 已移除的连续天数成就不再存在
        assertFalse(ids.contains("streak_3"))
        // 每难度首胜与累计档位在同一分组
        assertEquals(
            Achievements.byId("win_beginner")!!.group,
            Achievements.byId("count_BEGINNER_5")!!.group,
        )
    }

    @Test
    fun monthCompletion_detectsAllMonthLengths() {
        fun datesOf(year: Int, month: Int): Set<String> {
            val ym = YearMonth.of(year, month)
            return (1..ym.lengthOfMonth()).map { "%s-%02d".format(ym, it) }.toSet()
        }
        // 31 天月
        val jan = YearMonth.of(2026, 1)
        assertTrue(AchievementEngine.isMonthComplete(datesOf(2026, 1), jan))
        assertFalse(AchievementEngine.isMonthComplete(datesOf(2026, 1) - "2026-01-31", jan))
        // 2 月平年 28 天
        val feb2026 = YearMonth.of(2026, 2)
        assertTrue(AchievementEngine.isMonthComplete(datesOf(2026, 2), feb2026))
        // 闰年 2 月 29 天：只完成 28 天不算全勤
        val feb2028 = YearMonth.of(2028, 2)
        assertFalse(AchievementEngine.isMonthComplete(datesOf(2026, 2), feb2028))
        assertTrue(AchievementEngine.isMonthComplete(datesOf(2028, 2), feb2028))
        // 30 天月
        val apr = YearMonth.of(2026, 4)
        assertTrue(AchievementEngine.isMonthComplete(datesOf(2026, 4), apr))
    }

    @Test
    fun dailyDifficulty_suggestsModePreferringHarder() {
        assertEquals(
            Difficulty.EASY,
            AchievementEngine.suggestDailyDifficulty(emptyList()),
        )
        assertEquals(
            Difficulty.HARD,
            AchievementEngine.suggestDailyDifficulty(
                listOf(Difficulty.EASY, Difficulty.HARD, Difficulty.HARD),
            ),
        )
        // 众数并列时取更难的
        assertEquals(
            Difficulty.EXPERT,
            AchievementEngine.suggestDailyDifficulty(
                listOf(Difficulty.MEDIUM, Difficulty.EXPERT),
            ),
        )
        assertEquals(
            Difficulty.BEGINNER,
            AchievementEngine.suggestDailyDifficulty(
                listOf(Difficulty.BEGINNER, Difficulty.BEGINNER, Difficulty.EASY),
            ),
        )
    }

    @Test
    fun streaks_currentAndLongest() {
        val today = LocalDate.of(2026, 10, 2)
        // 今天完成：当前连续 3 天
        val (c1, l1) = AchievementEngine.computeStreaks(
            listOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(10)),
            today,
        )
        assertEquals(3, c1)
        assertEquals(3, l1)
        // 今天没完成但昨天完成：当前连续以昨天为起点
        val (c2, _) = AchievementEngine.computeStreaks(
            listOf(today.minusDays(1), today.minusDays(2)),
            today,
        )
        assertEquals(2, c2)
        // 断档后最长连续大于当前
        val (c3, l3) = AchievementEngine.computeStreaks(
            listOf(
                today, today.minusDays(1),
                today.minusDays(5), today.minusDays(6), today.minusDays(7), today.minusDays(8),
            ),
            today,
        )
        assertEquals(2, c3)
        assertEquals(4, l3)
        // 空记录
        assertEquals(0 to 0, AchievementEngine.computeStreaks(emptyList(), today))
    }

    @Test
    fun gameLogic_regionCompletion() {
        // 用一个完整终盘验证行/列/宫判定
        val generator = com.balsam.sudoku.game.SudokuGenerator(kotlin.random.Random(42))
        val puzzle = generator.generate(Difficulty.EASY)
        val solution = puzzle.solution.toList()
        // 全空盘面：任何区域都不完整
        val empty = List(81) { 0 }
        assertFalse(GameLogic.isRegionCorrect(empty, solution, GameLogic.rowCells(0)))
        // 直接把 r4 所在行填成解 → 该行完成，其它行未完成
        val values = empty.toMutableList()
        for (c in GameLogic.rowCells(40)) values[c] = solution[c]
        assertTrue(GameLogic.isRegionCorrect(values, solution, GameLogic.rowCells(40)))
        assertFalse(GameLogic.isRegionCorrect(values, solution, GameLogic.rowCells(0)))
        // 填错一个格（与解不一致）→ 不算填对
        values[40] = if (solution[40] == 1) 2 else 1
        assertFalse(GameLogic.isRegionCorrect(values, solution, GameLogic.rowCells(40)))
        // 列/宫的格子索引形状
        assertEquals(9, GameLogic.colCells(0).size)
        assertEquals(9, GameLogic.boxCells(0).size)
        assertEquals(9, GameLogic.rowCells(80).size)
        // boxCells(0) 应为左上宫：行 0-2，列 0-2
        assertTrue(GameLogic.boxCells(0).all { it / 9 < 3 && it % 9 < 3 })
        // colCells(8) 应为最后一列
        assertTrue(GameLogic.colCells(8).all { it % 9 == 8 })
    }
}
