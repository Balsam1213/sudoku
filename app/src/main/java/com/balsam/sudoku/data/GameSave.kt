package com.balsam.sudoku.data

import kotlinx.serialization.Serializable

/** 进行中对局的完整快照，序列化为 JSON 存入 DataStore，随时可恢复。 */
@Serializable
data class GameSave(
    val difficulty: String,
    val puzzle: List<Int>,
    val solution: List<Int>,
    val values: List<Int>,
    val notes: List<Set<Int>>,
    val elapsedSeconds: Int,
    val mistakes: Int,
    val hintsUsed: Int,
    val notesUsed: Boolean,
)
