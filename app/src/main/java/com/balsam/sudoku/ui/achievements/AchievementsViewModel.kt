package com.balsam.sudoku.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balsam.sudoku.SudokuApp
import com.balsam.sudoku.achievements.AchievementDef
import com.balsam.sudoku.achievements.Achievements
import com.balsam.sudoku.game.Difficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AchievementUiItem(
    val def: AchievementDef,
    val unlockedAt: Long?,
    /** 累计式成就的当前进度（未解锁时用于进度条）；非累计式为 null。 */
    val progress: Int? = null,
    /** 累计式成就的目标值。 */
    val target: Int? = null,
)

class AchievementsViewModel(private val app: SudokuApp) : ViewModel() {

    private val recordDao = app.database.gameRecordDao()
    private val dailyDao = app.database.dailyChallengeDao()

    private val _items = MutableStateFlow<List<AchievementUiItem>>(emptyList())
    val items: StateFlow<List<AchievementUiItem>> = _items

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val unlocked = app.database.achievementDao().all().associateBy { it.id }
            val totalWins = recordDao.totalWins()
            val winsByDifficulty = Difficulty.entries.associateWith { d ->
                recordDao.statsFor(d.name)?.winCount() ?: 0
            }
            val distinctDays = recordDao.distinctWinDates(500).size
            val pencilGames = recordDao.pencilGamesCount()
            val totalHints = recordDao.totalHintsUsed() ?: 0
            val dailyCount = dailyDao.countAll()

            _items.value = Achievements.ALL.map { def ->
                val unlockedAt = unlocked[def.id]?.unlockedAt
                val progressPair = progressFor(
                    def,
                    totalWins,
                    winsByDifficulty,
                    distinctDays,
                    pencilGames,
                    totalHints,
                    dailyCount,
                )
                AchievementUiItem(
                    def = def,
                    unlockedAt = unlockedAt,
                    progress = progressPair?.first,
                    target = progressPair?.second,
                )
            }
        }
    }

    companion object {
        /** 累计式成就的当前进度与目标；非累计式返回 null。 */
        fun progressFor(
            def: AchievementDef,
            totalWins: Int,
            winsByDifficulty: Map<Difficulty, Int>,
            distinctDays: Int,
            pencilGames: Int,
            totalHints: Int,
            dailyCount: Int,
        ): Pair<Int, Int>? {
            fun threshold(prefix: String): Int? =
                def.id.removePrefix(prefix).toIntOrNull()

            return when {
                def.id.startsWith("total_") -> totalWins to threshold("total_")!!
                def.id.startsWith("count_") -> {
                    val parts = def.id.split('_')
                    val d = Difficulty.entries.firstOrNull { it.name == parts.getOrNull(1) }
                        ?: return null
                    (winsByDifficulty[d] ?: 0) to parts[2].toInt()
                }
                def.id.startsWith("days_") -> distinctDays to threshold("days_")!!
                def.id.startsWith("pencil_") -> pencilGames to threshold("pencil_")!!
                def.id.startsWith("hints_") -> totalHints to threshold("hints_")!!
                def.id.startsWith("daily_") -> dailyCount to threshold("daily_")!!
                else -> null
            }
        }
    }
}
