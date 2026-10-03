package com.balsam.sudoku.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.balsam.sudoku.SudokuApp
import com.balsam.sudoku.data.DifficultyStats
import com.balsam.sudoku.game.Difficulty
import com.balsam.sudoku.ui.common.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    app: SudokuApp,
    onPlay: (Difficulty) -> Unit,
    onContinue: () -> Unit,
    onPlayDaily: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: MenuViewModel = viewModel(
        factory = viewModelFactory { initializer { MenuViewModel(app) } },
    )
    val hasCurrentGame by vm.hasCurrentGame.collectAsStateWithLifecycle()
    val currentDifficulty by vm.currentGameDifficulty.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val dailyInfo by vm.dailyInfo.collectAsStateWithLifecycle()

    var pendingDifficulty by remember { mutableStateOf<Difficulty?>(null) }
    var showDailyReplayDialog by remember { mutableStateOf(false) }

    // 从对局返回菜单时刷新每日挑战状态
    LifecycleResumeEffect(Unit) {
        vm.refreshDaily()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("数独", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "离线也能玩，随时挑战纪录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.navigationBarsPadding(),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    BottomNavItem(Icons.Rounded.BarChart, "战绩统计", onOpenStats)
                    BottomNavItem(Icons.Rounded.EmojiEvents, "成就", onOpenAchievements)
                    BottomNavItem(Icons.Rounded.Settings, "设置", onOpenSettings)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (hasCurrentGame) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onContinue() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.PlayCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "继续上局",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "进行中 · ${currentDifficulty?.label ?: "数独"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            dailyInfo?.let { daily ->
                DailyChallengeCard(
                    info = daily,
                    onClick = {
                        if (daily.completedToday) showDailyReplayDialog = true else onPlayDaily()
                    },
                )
                Spacer(Modifier.height(12.dp))
            }

            Difficulty.entries.forEach { difficulty ->
                DifficultyButton(
                    difficulty = difficulty,
                    stats = stats[difficulty],
                    onClick = {
                        if (hasCurrentGame) pendingDifficulty = difficulty else onPlay(difficulty)
                    },
                )
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    pendingDifficulty?.let { difficulty ->
        AlertDialog(
            onDismissRequest = { pendingDifficulty = null },
            title = { Text("开始新对局？") },
            text = { Text("当前有一局进行中，开新局将放弃旧局（旧局会记为未完成）。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.abandonCurrentGame()
                    onPlay(difficulty)
                    pendingDifficulty = null
                }) { Text("开新局") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDifficulty = null }) { Text("取消") }
            },
        )
    }

    if (showDailyReplayDialog) {
        AlertDialog(
            onDismissRequest = { showDailyReplayDialog = false },
            title = { Text("今日挑战已完成") },
            text = { Text("可以重玩同一道题，但成绩不再计入每日挑战奖励。") },
            confirmButton = {
                TextButton(onClick = {
                    showDailyReplayDialog = false
                    onPlayDaily()
                }) { Text("重玩") }
            },
            dismissButton = {
                TextButton(onClick = { showDailyReplayDialog = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun DailyChallengeCard(info: DailyInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (info.completedToday) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.secondaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (info.completedToday) Icons.Rounded.Check else Icons.Rounded.WbSunny,
                contentDescription = null,
                tint = if (info.completedToday) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "每日挑战 · ${info.today.monthValue} 月 ${info.today.dayOfMonth} 日",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    buildString {
                        append("今日难度：${info.suggestedDifficulty.label}")
                        append(" · 本月 ${info.monthCompleted}/${info.monthTotal} 天")
                        if (info.trophies.isNotEmpty()) append(" · 🏆${info.trophies.size}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                if (info.completedToday) "已完成" else "去挑战",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DifficultyButton(
    difficulty: Difficulty,
    stats: DifficultyStats?,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = difficulty.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                val best = stats?.bestTimeSeconds
                Text(
                    text = if (best != null) "最佳 ${formatDuration(best)}" else "尚未挑战",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
                val played = stats?.played ?: 0
                val wins = stats?.winCount() ?: 0
                Text(
                    text = if (played > 0) "玩过 $played 局 · 完成 $wins 局" else "点击开始",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 4.dp),
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
