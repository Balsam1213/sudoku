package com.balsam.sudoku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 每一局结束（完成 / 中途放弃 / 失败）都会写入一条记录。
 * isWin = false 的记录用于统计"玩过次数"与胜率。
 */
@Entity(tableName = "game_records")
data class GameRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val difficulty: String,
    val durationSeconds: Int,
    val mistakes: Int,
    val hintsUsed: Int,
    val completedAt: Long,
    val isWin: Boolean,
    // 本局是否使用过铅笔笔记（铅笔成就按"使用铅笔的对局数"统计）
    val notesUsed: Boolean = false,
)
