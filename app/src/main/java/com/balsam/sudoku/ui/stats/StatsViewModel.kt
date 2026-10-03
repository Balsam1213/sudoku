package com.balsam.sudoku.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balsam.sudoku.SudokuApp
import com.balsam.sudoku.achievements.AchievementEngine
import com.balsam.sudoku.data.DifficultyStats
import com.balsam.sudoku.data.GameRecord
import com.balsam.sudoku.data.MonthTrophyEntity
import com.balsam.sudoku.game.Difficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** 全局汇总：连续天数与累计完成天数。 */
data class DaySummary(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalDays: Int,
)

class StatsViewModel(private val app: SudokuApp) : ViewModel() {

    private val recordDao = app.database.gameRecordDao()
    private val dailyDao = app.database.dailyChallengeDao()

    val stats: StateFlow<Map<Difficulty, DifficultyStats?>> =
        recordDao.statsFlow()
            .map { list -> list.associateBy { Difficulty.valueOf(it.difficulty) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _history = MutableStateFlow<List<GameRecord>>(emptyList())
    val history: StateFlow<List<GameRecord>> = _history

    private val _daySummary = MutableStateFlow(DaySummary(0, 0, 0))
    val daySummary: StateFlow<DaySummary> = _daySummary

    private val _dailyMonth = MutableStateFlow<Pair<Int, Int>>(0 to 0)
    val dailyMonth: StateFlow<Pair<Int, Int>> = _dailyMonth

    private val _trophies = MutableStateFlow<List<MonthTrophyEntity>>(emptyList())
    val trophies: StateFlow<List<MonthTrophyEntity>> = _trophies

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _history.value = recordDao.recentRecords(HISTORY_LIMIT)
            val zone = java.time.ZoneId.systemDefault()
            val dates = recordDao.distinctWinDates(MAX_DAY_QUERY).mapNotNull {
                runCatching { LocalDate.parse(it) }.getOrNull()
            }
            val (current, longest) = AchievementEngine.computeStreaks(dates, LocalDate.now(zone))
            _daySummary.value = DaySummary(
                currentStreak = current,
                longestStreak = longest,
                totalDays = dates.size,
            )
            val month = YearMonth.now(zone)
            _dailyMonth.value = dailyDao.monthDates(month.toString()).size to month.lengthOfMonth()
            _trophies.value = app.database.monthTrophyDao().all()
        }
    }

    companion object {
        const val HISTORY_LIMIT = 50
        private const val MAX_DAY_QUERY = 500
    }
}
