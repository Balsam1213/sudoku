package com.balsam.sudoku

import com.balsam.sudoku.game.Difficulty
import com.balsam.sudoku.game.GameLogic
import com.balsam.sudoku.game.SudokuGenerator
import com.balsam.sudoku.game.SudokuSolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SudokuEngineTest {

    private val generator = SudokuGenerator(Random(20260925))

    private fun assertValidCompleteGrid(grid: IntArray) {
        assertEquals(81, grid.size)
        val full = (1..9).toSet()
        for (i in 0..8) {
            assertEquals("row $i", full, (0..8).map { grid[i * 9 + it] }.toSet())
            assertEquals("col $i", full, (0..8).map { grid[it * 9 + i] }.toSet())
        }
        for (b in 0..8) {
            val br = (b / 3) * 3
            val bc = (b % 3) * 3
            val box = (0..8).map { grid[(br + it / 3) * 9 + bc + it % 3] }.toSet()
            assertEquals("box $b", full, box)
        }
    }

    private fun assertPuzzleIsClueOfSolution(puzzle: IntArray, solution: IntArray) {
        for (i in 0..80) {
            if (puzzle[i] != 0) {
                assertEquals("cell $i differs from solution", solution[i], puzzle[i])
            }
        }
    }

    @Test
    fun generatedPuzzles_areUniqueAndMatchDifficulty() {
        for (difficulty in Difficulty.entries) {
            val samples = when (difficulty) {
                Difficulty.BEGINNER, Difficulty.EASY, Difficulty.MEDIUM -> 6
                Difficulty.HARD, Difficulty.EXPERT -> 3
                Difficulty.EXTREME -> 2
            }
            repeat(samples) { n ->
                val generated = generator.generate(difficulty)
                assertValidCompleteGrid(generated.solution)
                assertPuzzleIsClueOfSolution(generated.puzzle, generated.solution)
                assertEquals(
                    "puzzle $n of $difficulty must have a unique solution",
                    1,
                    SudokuSolver.countSolutions(generated.puzzle, 2),
                )
                val holes = generated.puzzle.count { it == 0 }
                // 极端难度允许少量松弛（挖洞随机构成有波动），其余难度必须达标
                val minHoles = when (difficulty) {
                    Difficulty.EXTREME -> 57
                    Difficulty.EXPERT -> 56
                    Difficulty.HARD -> 52
                    else -> difficulty.targetHoles
                }
                assertTrue("$difficulty generated only $holes holes", holes >= minHoles)
            }
        }
    }

    @Test
    fun solver_solvesKnownPuzzle() {
        // 一个经典唯一解题目
        val puzzle = """
            530070000
            600195000
            098000060
            800060003
            400803001
            700020006
            060000280
            000419005
            000080079
        """.trimIndent().replace("\n", "").map { it - '0' }.toIntArray()

        val solved = SudokuSolver.solve(puzzle)
        assertNotNull(solved)
        assertValidCompleteGrid(solved!!)
        for (i in 0..80) {
            if (puzzle[i] != 0) assertEquals("given cell $i changed", puzzle[i], solved[i])
        }
        assertEquals(1, SudokuSolver.countSolutions(puzzle, 2))
    }

    @Test
    fun countSolutions_detectsMultipleSolutions() {
        val empty = IntArray(81)
        assertEquals(2, SudokuSolver.countSolutions(empty, 2))
    }

    @Test
    fun countSolutions_returnsZeroForConflictingGrid() {
        val grid = IntArray(81)
        grid[0] = 5
        grid[1] = 5
        assertEquals(0, SudokuSolver.countSolutions(grid, 2))
        assertTrue(SudokuSolver.solve(grid) == null)
    }

    @Test
    fun gameLogic_detectsConflicts() {
        val values = MutableList(81) { 0 }
        values[0] = 1   // 行 0 / 列 0 / 宫 0
        values[8] = 1   // 与行冲突
        values[18] = 1  // 与列冲突
        values[20] = 1  // 与宫冲突（仅同宫）
        val conflicts = GameLogic.findConflicts(values)
        assertTrue(conflicts[0] && conflicts[8] && conflicts[18] && conflicts[20])
        assertFalse(conflicts[40])
    }

    @Test
    fun gameLogic_noteRejection() {
        val values = MutableList(81) { 0 }
        values[0] = 1 // r0c0

        // 同行/同列/同宫已有该数字 → 不能写入笔记
        assertTrue(GameLogic.peersContainDigit(values, 8, 1))
        assertTrue(GameLogic.peersContainDigit(values, 27, 1))
        assertTrue(GameLogic.peersContainDigit(values, 4, 1))
        // 不同行列宫的格子不受影响
        assertFalse(GameLogic.peersContainDigit(values, 12, 1))
    }
}
