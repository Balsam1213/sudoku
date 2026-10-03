package com.balsam.sudoku.game

/** 运行时规则判定：冲突检测与完成判定。 */
object GameLogic {

    /**
     * 计算每个格子是否与同行/同列/同宫中的相同数字冲突。
     * 返回长度 81 的数组，true 表示该格当前显示的数字与其他格重复。
     */
    fun findConflicts(values: List<Int>): BooleanArray {
        val conflict = BooleanArray(81)
        markDuplicates(values, conflict) { cell -> cell / 9 }          // 行
        markDuplicates(values, conflict) { cell -> cell % 9 }          // 列
        markDuplicates(values, conflict) { cell -> (cell / 27) * 3 + (cell % 9) / 3 } // 宫
        return conflict
    }

    private inline fun markDuplicates(
        values: List<Int>,
        conflict: BooleanArray,
        groupOf: (Int) -> Int,
    ) {
        val seen = IntArray(81) // 记录该组第一次出现某数字的格子，-1 表示未出现
        seen.fill(-1)
        for (cell in 0..80) {
            val v = values[cell]
            if (v == 0) continue
            val g = groupOf(cell)
            val first = seen[g * 9 + v - 1]
            if (first >= 0) {
                conflict[first] = true
                conflict[cell] = true
            } else {
                seen[g * 9 + v - 1] = cell
            }
        }
    }

    /** 棋盘是否填满（填满且与唯一解一致才由 ViewModel 判定胜利）。 */
    fun isFilled(values: List<Int>): Boolean = values.all { it in 1..9 }

    /** cell 的行/列/宫中是否已存在 digit（不含 cell 自身）。 */
    fun peersContainDigit(values: List<Int>, cell: Int, digit: Int): Boolean {
        val row = cell / 9
        val col = cell % 9
        val box = (row / 3) * 3 + col / 3
        for (i in 0..80) {
            if (i == cell) continue
            if (values[i] != digit) continue
            val r = i / 9
            val c = i % 9
            val b = (r / 3) * 3 + c / 3
            if (r == row || c == col || b == box) return true
        }
        return false
    }

    /** 该数字在棋盘上最靠上（行优先）的那个格子；尚未出现返回 null。 */
    fun topmostCellOfDigit(values: List<Int>, digit: Int): Int? =
        (0..80).firstOrNull { values[it] == digit }

    /** 一组格子（行/列/宫）是否全部填入且与解一致（"填对一行/列/宫"成就判定）。 */
    fun isRegionCorrect(values: List<Int>, solution: List<Int>, cells: List<Int>): Boolean =
        cells.all { values[it] == solution[it] }

    /** cell 所在行的 81 格下标列表。 */
    fun rowCells(cell: Int): List<Int> {
        val row = cell / 9
        return (0..8).map { row * 9 + it }
    }

    /** cell 所在列的 81 格下标列表。 */
    fun colCells(cell: Int): List<Int> {
        val col = cell % 9
        return (0..8).map { it * 9 + col }
    }

    /** cell 所在宫的 9 格下标列表。 */
    fun boxCells(cell: Int): List<Int> {
        val boxRow = (cell / 9) / 3 * 3
        val boxCol = (cell % 9) / 3 * 3
        return (0..8).map { (boxRow + it / 3) * 9 + boxCol + it % 3 }
    }
}
