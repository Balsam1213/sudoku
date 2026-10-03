package com.balsam.sudoku.data

import androidx.room.ColumnInfo

/** 按难度聚合的战绩。 */
data class DifficultyStats(
    @ColumnInfo(name = "difficulty") val difficulty: String,
    @ColumnInfo(name = "played") val played: Int,
    @ColumnInfo(name = "wins") val wins: Int?,
    @ColumnInfo(name = "bestTimeSeconds") val bestTimeSeconds: Int?,
    @ColumnInfo(name = "averageTimeSeconds") val averageTimeSeconds: Double?,
    @ColumnInfo(name = "lastPlayedAt") val lastPlayedAt: Long?,
) {
    fun winCount(): Int = wins ?: 0
}
