package com.balsam.sudoku.ui.menu

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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** 每日挑战的菜单展示信息。 */
data class DailyInfo(
    val today: LocalDate,
    val suggestedDifficulty: Difficulty,
    val completedToday: Boolean,
    val monthCompleted: Int,
    val monthTotal: Int,
    val trophies: List<MonthTrophyEntity>,
)

class MenuViewModel(private val app: SudokuApp) : ViewModel() {

    private val recordDao = app.database.gameRecordDao()
    private val dailyDao = app.database.dailyChallengeDao()

    val hasCurrentGame: StateFlow<Boolean> = app.settingsRepository.currentGame
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentGameDifficulty: StateFlow<Difficulty?> = app.settingsRepository.currentGame
        .map { it?.difficulty?.let { name -> runCatching { Difficulty.valueOf(name) }.getOrNull() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val stats: StateFlow<Map<Difficulty, DifficultyStats?>> =
        recordDao.statsFlow()
            .map { list -> list.associateBy { Difficulty.valueOf(it.difficulty) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _dailyInfo = MutableStateFlow<DailyInfo?>(null)
    val dailyInfo: StateFlow<DailyInfo?> = _dailyInfo

    init {
        refreshDaily()
    }

    fun refreshDaily() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val month = YearMonth.from(today)
            val recent = recordDao.recentWins(DAILY_DIFFICULTY_WINDOW)
                .mapNotNull { runCatching { Difficulty.valueOf(it.difficulty) }.getOrNull() }
            _dailyInfo.value = DailyInfo(
                today = today,
                suggestedDifficulty = AchievementEngine.suggestDailyDifficulty(recent),
                completedToday = dailyDao.getByDate(today.toString()) != null,
                monthCompleted = dailyDao.monthDates(month.toString()).size,
                monthTotal = month.lengthOfMonth(),
                trophies = app.database.monthTrophyDao().all(),
            )
        }
    }

    /** 放弃当前进行中的对局：记一条未完成记录并清除存档。 */
    fun abandonCurrentGame() {
        viewModelScope.launch {
            val save = app.settingsRepository.currentGame.firstOrNull()
            if (save != null) {
                recordDao.insert(
                    GameRecord(
                        difficulty = save.difficulty,
                        durationSeconds = save.elapsedSeconds,
                        mistakes = save.mistakes,
                        hintsUsed = save.hintsUsed,
                        completedAt = System.currentTimeMillis(),
                        isWin = false,
                        notesUsed = save.notesUsed,
                    ),
                )
            }
            app.settingsRepository.saveCurrentGame(null)
        }
    }

    companion object {
        private const val DAILY_DIFFICULTY_WINDOW = 20
    }
}
