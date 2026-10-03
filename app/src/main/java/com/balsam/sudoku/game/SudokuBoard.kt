package com.balsam.sudoku.game

import kotlinx.serialization.Serializable

/** 单个格子的运行时状态。 */
@Serializable
data class SudokuCell(
    val value: Int = 0,
    val isGiven: Boolean = false,
    val notes: Set<Int> = emptySet(),
)

/** 一道完整题目：谜面与唯一解。 */
data class GeneratedPuzzle(
    val puzzle: IntArray,
    val solution: IntArray,
    val difficulty: Difficulty,
) {
    override fun equals(other: Any?): Boolean =
        other is GeneratedPuzzle && puzzle.contentEquals(other.puzzle) &&
            solution.contentEquals(other.solution) && difficulty == other.difficulty

    override fun hashCode(): Int = puzzle.contentHashCode() * 31 + solution.contentHashCode()
}
