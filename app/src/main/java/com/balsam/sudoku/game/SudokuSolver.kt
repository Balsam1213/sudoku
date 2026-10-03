package com.balsam.sudoku.game

import kotlin.random.Random

/**
 * 基于位掩码 + MRV 启发式的回溯求解器。
 * countSolutions 用于生成时的唯一解验证：找到 limit 个解即提前剪枝返回。
 */
object SudokuSolver {

    /** 求出一个解；无解返回 null。randomize 非空时随机化候选顺序（用于生成完整终盘）。 */
    fun solve(grid: IntArray, randomize: Random? = null): IntArray? {
        val masks = Masks.from(grid) ?: return null
        val work = grid.copyOf()
        val count = intArrayOf(0)
        val first = IntArray(81)
        search(masks, work, randomize, 1, count, first)
        return if (count[0] > 0) first else null
    }

    /** 统计解的数量，最多数到 limit 个即返回（limit=2 即可判断唯一性）。 */
    fun countSolutions(grid: IntArray, limit: Int = 2): Int {
        val masks = Masks.from(grid) ?: return 0
        val count = intArrayOf(0)
        search(masks, grid.copyOf(), null, limit, count, null)
        return count[0]
    }

    private class Masks(
        val rows: IntArray,
        val cols: IntArray,
        val boxes: IntArray,
    ) {
        companion object {
            /** 从给定盘面初始化位掩码；盘面本身冲突或含非法数字时返回 null。 */
            fun from(grid: IntArray): Masks? {
                val rows = IntArray(9)
                val cols = IntArray(9)
                val boxes = IntArray(9)
                for (i in 0..80) {
                    val v = grid[i]
                    if (v == 0) continue
                    if (v !in 1..9) return null
                    val r = i / 9
                    val c = i % 9
                    val b = (r / 3) * 3 + c / 3
                    val bit = 1 shl v
                    if (rows[r] and bit != 0 || cols[c] and bit != 0 || boxes[b] and bit != 0) return null
                    rows[r] = rows[r] or bit
                    cols[c] = cols[c] or bit
                    boxes[b] = boxes[b] or bit
                }
                return Masks(rows, cols, boxes)
            }
        }
    }

    private fun search(
        masks: Masks,
        grid: IntArray,
        randomize: Random?,
        limit: Int,
        count: IntArray,
        first: IntArray?,
    ) {
        // 找候选数最少的空格（MRV）；候选数为 0 的分支直接剪掉
        var best = -1
        var bestMask = 0
        var bestCount = 10
        for (i in 0..80) {
            if (grid[i] != 0) continue
            val r = i / 9
            val c = i % 9
            val b = (r / 3) * 3 + c / 3
            val used = masks.rows[r] or masks.cols[c] or masks.boxes[b]
            var mask = 0
            var cnt = 0
            for (d in 1..9) {
                if (used and (1 shl d) == 0) {
                    mask = mask or (1 shl d)
                    cnt++
                }
            }
            if (cnt == 0) return
            if (cnt < bestCount) {
                bestCount = cnt
                best = i
                bestMask = mask
                if (cnt == 1) break
            }
        }

        if (best == -1) {
            // 无空格，找到一个完整解
            count[0]++
            if (first != null && count[0] == 1) System.arraycopy(grid, 0, first, 0, 81)
            return
        }

        val digits = ArrayList<Int>(bestCount)
        for (d in 1..9) if (bestMask and (1 shl d) != 0) digits.add(d)
        if (randomize != null) digits.shuffle(randomize)

        val r = best / 9
        val c = best % 9
        val b = (r / 3) * 3 + c / 3
        for (d in digits) {
            val bit = 1 shl d
            grid[best] = d
            masks.rows[r] = masks.rows[r] or bit
            masks.cols[c] = masks.cols[c] or bit
            masks.boxes[b] = masks.boxes[b] or bit

            search(masks, grid, randomize, limit, count, first)

            grid[best] = 0
            masks.rows[r] = masks.rows[r] and bit.inv()
            masks.cols[c] = masks.cols[c] and bit.inv()
            masks.boxes[b] = masks.boxes[b] and bit.inv()

            if (count[0] >= limit) return
        }
    }
}
