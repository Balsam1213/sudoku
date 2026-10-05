package com.balsam.sudoku.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balsam.sudoku.SudokuApp
import com.balsam.sudoku.data.DailyChallengeEntity
import com.balsam.sudoku.data.MonthTrophyEntity
import com.balsam.sudoku.game.Difficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** 日历上单个日期的展示状态（null 单元格为月首/月末的空白占位）。 */
data class CalendarDay(
    val date: LocalDate,
    val completed: DailyChallengeEntity?,
    val planDifficulty: Difficulty?,
    val isToday: Boolean,
    val isPast: Boolean,
)

data class CalendarUiState(
    val displayedMonth: YearMonth = YearMonth.now(),
    /** 月网格，含 null 空白占位，长度为 7 的倍数。 */
    val days: List<CalendarDay?> = emptyList(),
    val selected: LocalDate? = null,
    val today: LocalDate = LocalDate.now(),
    val todayCompleted: Boolean = false,
    val monthTrophy: MonthTrophyEntity? = null,
    val monthCompletedCount: Int = 0,
    val monthTotal: Int = YearMonth.now().lengthOfMonth(),
) {
    /** 是否允许补玩（已过日期）：必须先完成当日的每日挑战。 */
    fun canMakeUp(date: LocalDate): Boolean =
        DailyCalendarLogic.canMakeUp(date, today, todayCompleted)
}

class DailyCalendarViewModel(private val app: SudokuApp) : ViewModel() {

    private val dailyDao = app.database.dailyChallengeDao()
    private val planDao = app.database.dailyPlanDao()

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state

    init {
        refresh()
    }

    fun refresh() {
        load(_state.value.displayedMonth, _state.value.selected)
    }

    /** 切换显示的月份；默认选中今天（当月）或该月 1 号（历史月份）。 */
    fun selectMonth(month: YearMonth) {
        val selected = if (month == YearMonth.now()) LocalDate.now() else month.atDay(1)
        load(month, selected)
    }

    fun selectDate(date: LocalDate) {
        load(_state.value.displayedMonth, date)
    }

    private fun load(month: YearMonth, selected: LocalDate?) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val rows = dailyDao.monthRows(month.toString()).associateBy { it.date }
            val plans = planDao.monthRows(month.toString()).associateBy { it.date }
            val days = DailyCalendarLogic.monthGridCells(month).map { date ->
                date?.let {
                    CalendarDay(
                        date = it,
                        completed = rows[it.toString()],
                        planDifficulty = plans[it.toString()]?.let { p ->
                            runCatching { Difficulty.valueOf(p.difficulty) }.getOrNull()
                        },
                        isToday = it == today,
                        isPast = it.isBefore(today),
                    )
                }
            }
            _state.value = CalendarUiState(
                displayedMonth = month,
                days = days,
                selected = selected,
                today = today,
                todayCompleted = dailyDao.getByDate(today.toString()) != null,
                monthTrophy = app.database.monthTrophyDao().all()
                    .firstOrNull { it.month == month.toString() },
                monthCompletedCount = rows.size,
                monthTotal = month.lengthOfMonth(),
            )
        }
    }

}

/** 日历纯逻辑（独立对象便于单元测试）。 */
object DailyCalendarLogic {

    /**
     * 生成月网格：周一起始，月首之前的空白用 null 占位，
     * 末尾补 null 凑满整周。
     */
    fun monthGridCells(month: YearMonth): List<LocalDate?> {
        val first = month.atDay(1)
        // DayOfWeek: MONDAY=1 .. SUNDAY=7
        val leadingBlanks = first.dayOfWeek.value - 1
        val cells = mutableListOf<LocalDate?>()
        repeat(leadingBlanks) { cells.add(null) }
        for (d in 1..month.lengthOfMonth()) cells.add(month.atDay(d))
        while (cells.size % 7 != 0) cells.add(null)
        return cells
    }

    /** 补玩守卫：必须先完成当日的每日挑战，且日期不能是今天/未来。 */
    fun canMakeUp(date: LocalDate, today: LocalDate, todayCompleted: Boolean): Boolean =
        date.isBefore(today) && todayCompleted
}
