package com.balsam.sudoku

import com.balsam.sudoku.data.GameSave
import com.balsam.sudoku.ui.menu.DailyCalendarLogic
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DailyCalendarTest {

    @Test
    fun monthGrid_layoutWithMondayStart() {
        // 2026-10-01 是周四 → 前面 3 个空白（一/二/三），共 31 天，末尾补到 35 格
        val cells = DailyCalendarLogic.monthGridCells(YearMonth.of(2026, 10))
        assertEquals(35, cells.size)
        assertNull(cells[0]); assertNull(cells[1]); assertNull(cells[2])
        assertEquals(LocalDate.of(2026, 10, 1), cells[3])
        assertEquals(LocalDate.of(2026, 10, 31), cells[33])
        assertNull(cells[34])
    }

    @Test
    fun monthGrid_handlesAllMonthLengths() {
        // 平年 2 月 28 天：2026-02-01 是周日 → 6 个空白，共 34 格补到 35
        val feb2026 = DailyCalendarLogic.monthGridCells(YearMonth.of(2026, 2))
        assertEquals(35, feb2026.size)
        assertNull(feb2026[5])
        assertEquals(LocalDate.of(2026, 2, 1), feb2026[6])
        assertEquals(LocalDate.of(2026, 2, 28), feb2026[33])
        // 闰年 2 月 29 天
        val feb2028 = DailyCalendarLogic.monthGridCells(YearMonth.of(2028, 2))
        assertEquals(35, feb2028.size)
        // 30 天月
        assertEquals(35, DailyCalendarLogic.monthGridCells(YearMonth.of(2026, 4)).size)
        // 周一开始的月份没有前置空白：2026-06-01 是周一
        val jun2026 = DailyCalendarLogic.monthGridCells(YearMonth.of(2026, 6))
        assertEquals(LocalDate.of(2026, 6, 1), jun2026[0])
        assertEquals(30, jun2026.count { it != null })
    }

    @Test
    fun makeUpGuard_requiresTodayCompleted() {
        val today = LocalDate.of(2026, 10, 2)
        val yesterday = today.minusDays(1)
        // 今日未完成：不能补玩
        assertFalse(DailyCalendarLogic.canMakeUp(yesterday, today, todayCompleted = false))
        // 今日已完成：可以补玩过去日期
        assertTrue(DailyCalendarLogic.canMakeUp(yesterday, today, todayCompleted = true))
        // 今天/未来永远不算补玩
        assertFalse(DailyCalendarLogic.canMakeUp(today, today, todayCompleted = true))
        assertFalse(DailyCalendarLogic.canMakeUp(today.plusDays(1), today, todayCompleted = true))
    }

    @Test
    fun gameSave_oldJsonWithoutLightningFields_stillParses() {
        // 旧版存档没有 lightning 字段，必须能正常反序列化（使用默认值）
        val oldJson = """
            {"difficulty":"BEGINNER","puzzle":[1,0],"solution":[1,2],"values":[1,0],
             "notes":[[],[]],"elapsedSeconds":10,"mistakes":0,"hintsUsed":0,"notesUsed":false}
        """.trimIndent()
        val save = Json.decodeFromString<GameSave>(oldJson)
        assertEquals(true, save.lightningMode)
        assertNull(save.lightningDigit)

        // 新版存档往返一致
        val newSave = GameSave(
            difficulty = "BEGINNER", puzzle = listOf(1), solution = listOf(1),
            values = listOf(1), notes = emptyList(), elapsedSeconds = 5,
            mistakes = 0, hintsUsed = 0, notesUsed = false,
            lightningMode = false, lightningDigit = 7,
        )
        val json = Json.encodeToString(GameSave.serializer(), newSave)
        val parsed = Json.decodeFromString<GameSave>(json)
        assertEquals(false, parsed.lightningMode)
        assertEquals(7, parsed.lightningDigit)
    }
}
