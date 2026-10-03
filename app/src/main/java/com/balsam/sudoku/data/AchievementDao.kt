package com.balsam.sudoku.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Upsert
    suspend fun unlock(entity: AchievementEntity)

    @Query("SELECT * FROM achievements")
    fun allFlow(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun get(id: String): AchievementEntity?

    @Query("SELECT * FROM achievements")
    suspend fun all(): List<AchievementEntity>
}
