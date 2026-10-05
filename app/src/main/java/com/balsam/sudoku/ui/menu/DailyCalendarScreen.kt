package com.balsam.sudoku.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.balsam.sudoku.SudokuApp
import com.balsam.sudoku.ui.common.formatDuration
import java.time.LocalDate
import java.time.YearMonth

private val WEEKDAY_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyCalendarScreen(
    app: SudokuApp,
    onBack: () -> Unit,
    onPlayDate: (LocalDate) -> Unit,
) {
    val vm: DailyCalendarViewModel = viewModel(
        factory = viewModelFactory { initializer { DailyCalendarViewModel(app) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    // 从对局返回时刷新日历状态
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("每日挑战") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
        ) {
            // 月份切换 + 奖杯
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { vm.selectMonth(state.displayedMonth.minusMonths(1)) },
                    enabled = state.displayedMonth > YearMonth.of(2000, 1),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "上一月")
                }
                Text(
                    text = "${state.displayedMonth.year} 年 ${state.displayedMonth.monthValue} 月",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                if (state.monthTrophy != null) {
                    Text("🏆", fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                }
                IconButton(
                    onClick = { vm.selectMonth(state.displayedMonth.plusMonths(1)) },
                    enabled = state.displayedMonth < YearMonth.now(),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "下一月")
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "本月完成 ${state.monthCompletedCount} / ${state.monthTotal} 天",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))

            // 星期表头
            Row(Modifier.fillMaxWidth()) {
                WEEKDAY_LABELS.forEach { label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            // 日历网格
            state.days.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.9f)
                                .padding(2.dp),
                        ) {
                            if (day != null) {
                                CalendarCell(
                                    day = day,
                                    selected = state.selected == day.date,
                                    canMakeUp = state.canMakeUp(day.date),
                                    selectable = day.completed != null || day.isToday || day.isPast,
                                    onClick = { vm.selectDate(day.date) },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 选中日期详情
            val selected = state.selected
            val selectedDay = state.days.firstOrNull { it?.date == selected }
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    if (selected == null || selectedDay == null) {
                        Text(
                            "点击日期查看详情",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    } else {
                        val completed = selectedDay.completed
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${selected.monthValue} 月 ${selected.dayOfMonth} 日",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            if (selectedDay.isToday) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "今天",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            if (completed != null) {
                                Text(
                                    "✓ ${formatDuration(completed.durationSeconds)}",
                                    color = colors.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        val difficultyLabel = completed?.difficulty
                            ?: selectedDay.planDifficulty?.let { DifficultyLabel(it) }
                            ?: ""
                        if (difficultyLabel.isNotEmpty()) {
                            Text(
                                "难度：$difficultyLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        val canMakeUp = state.canMakeUp(selected)
                        when {
                            completed != null -> {
                                Text(
                                    "已完成${if (selectedDay.isToday) "" else "（补玩）"}，重玩同一道题不再计入奖励",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { onPlayDate(selected) }) { Text("重玩") }
                            }
                            selected.isAfter(state.today) -> {
                                Text(
                                    "未来日期，敬请期待",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            selected == state.today -> {
                                Button(
                                    onClick = { onPlayDate(selected) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("开始今日挑战") }
                            }
                            canMakeUp -> {
                                Button(
                                    onClick = { onPlayDate(selected) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("补玩该日") }
                            }
                            else -> {
                                Text(
                                    "先完成今日挑战，才能补玩过去的日期",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.error,
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { },
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("补玩该日") }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "完成当日挑战后即可补玩过去的日期；补玩成绩计入月度全勤奖杯与每日挑战累计成就",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/** 难度枚举 → 中文名（日历页使用，避免引入组合层依赖）。 */
private fun DifficultyLabel(difficulty: com.balsam.sudoku.game.Difficulty): String =
    when (difficulty) {
        com.balsam.sudoku.game.Difficulty.BEGINNER -> "初学者"
        com.balsam.sudoku.game.Difficulty.EASY -> "简单"
        com.balsam.sudoku.game.Difficulty.MEDIUM -> "中级"
        com.balsam.sudoku.game.Difficulty.HARD -> "困难"
        com.balsam.sudoku.game.Difficulty.EXPERT -> "专家"
        com.balsam.sudoku.game.Difficulty.EXTREME -> "极端"
    }

@Composable
private fun CalendarCell(
    day: CalendarDay,
    selected: Boolean,
    canMakeUp: Boolean,
    selectable: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val completed = day.completed != null
    val isFuture = !day.isPast && !day.isToday
    val isActionable = selectable

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (isFuture) 0.35f else 1f)
            .background(
                when {
                    completed -> colors.primary
                    selected -> colors.primaryContainer
                    else -> colors.surface
                },
                RoundedCornerShape(10.dp),
            )
            .border(
                width = when {
                    selected -> 2.dp
                    day.isToday && !completed -> 1.5.dp
                    else -> 0.dp
                },
                color = if (selected) colors.tertiary else colors.primary,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(enabled = isActionable, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (completed || day.isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    completed -> colors.onPrimary
                    selected -> colors.onPrimaryContainer
                    else -> colors.onSurface
                },
            )
            if (completed) {
                Text(
                    text = formatDuration(day.completed!!.durationSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = colors.onPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}
