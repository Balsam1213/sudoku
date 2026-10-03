package com.balsam.sudoku.game

import kotlin.random.Random

/**
 * 数独生成器：随机生成完整终盘，再按难度挖洞。
 * 每移除一个数字都用求解器验证解仍然唯一，不唯一则回填，保证题目必定有且仅有一个解。
 */
class SudokuGenerator(private val random: Random = Random.Default) {

    /** 用指定种子生成（每日挑战用：同一天同一难度同一题）。null 表示用构造时的随机源。 */
    fun generate(difficulty: Difficulty, seed: Long? = null): GeneratedPuzzle {
        val rng = seed?.let { Random(it) } ?: random
        var bestPuzzle: IntArray? = null
        var bestSolution: IntArray? = null
        var bestHoles = -1

        repeat(attemptsFor(difficulty)) {
            val solution = fullGrid(rng)
            val puzzle = digHoles(solution, difficulty.targetHoles, rng)
            val holes = puzzle.count { it == 0 }
            if (holes > bestHoles) {
                bestHoles = holes
                bestPuzzle = puzzle
                bestSolution = solution
            }
            if (bestHoles >= difficulty.targetHoles) return@repeat
        }

        return GeneratedPuzzle(
            puzzle = bestPuzzle!!,
            solution = bestSolution!!,
            difficulty = difficulty,
        )
    }

    /** 极端/专家难度更难达标，多试几轮。 */
    private fun attemptsFor(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EXTREME -> 8
        Difficulty.EXPERT -> 5
        Difficulty.HARD -> 3
        else -> 2
    }

    /** 生成一个随机完整终盘：对角三个宫随机填数，其余用随机化求解器补齐。 */
    private fun fullGrid(rng: Random): IntArray {
        while (true) {
            val grid = IntArray(81)
            for (box in intArrayOf(0, 4, 8)) {
                val digits = (1..9).shuffled(rng)
                val boxRow = (box / 3) * 3
                val boxCol = (box % 3) * 3
                var k = 0
                for (r in boxRow until boxRow + 3) {
                    for (c in boxCol until boxCol + 3) {
                        grid[r * 9 + c] = digits[k++]
                    }
                }
            }
            SudokuSolver.solve(grid, rng)?.let { return it }
        }
    }

    /**
     * 多轮挖洞：每轮按随机顺序尝试移除，每次都验证唯一解。
     * 一轮不再有进展且未达标则结束。
     */
    private fun digHoles(full: IntArray, target: Int, rng: Random): IntArray {
        val puzzle = full.copyOf()
        var holes = 0
        var passes = 0
        while (holes < target && passes < MAX_PASSES) {
            var progressed = false
            val order = (0..80).filter { puzzle[it] != 0 }.shuffled(rng)
            for (idx in order) {
                if (holes >= target) break
                val backup = puzzle[idx]
                puzzle[idx] = 0
                if (SudokuSolver.countSolutions(puzzle, 2) == 1) {
                    holes++
                    progressed = true
                } else {
                    puzzle[idx] = backup
                }
            }
            if (!progressed) break
            passes++
        }
        return puzzle
    }

    companion object {
        private const val MAX_PASSES = 4
    }
}
