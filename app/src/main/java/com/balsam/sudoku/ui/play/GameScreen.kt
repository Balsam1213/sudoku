package com.balsam.sudoku.ui.play

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.balsam.sudoku.SudokuApp
import com.balsam.sudoku.achievements.AchievementDef
import com.balsam.sudoku.data.AppSettings
import com.balsam.sudoku.ui.common.EraserIcon
import com.balsam.sudoku.ui.common.ShareBoard
import com.balsam.sudoku.ui.common.SoundPlayer
import com.balsam.sudoku.ui.common.SudokuShare
import com.balsam.sudoku.ui.common.formatDuration
import com.balsam.sudoku.ui.theme.AccentAmberLight
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    app: SudokuApp,
    difficultyName: String,
    onBack: () -> Unit,
) {
    val vm: GameViewModel = viewModel(
        key = "game_$difficultyName",
        factory = viewModelFactory { initializer { GameViewModel(app, difficultyName) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings by app.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = AppSettings())
    val sound = remember { SoundPlayer() }
    DisposableEffect(Unit) {
        onDispose { sound.release() }
    }

    // 操作反馈：填对=音效、填错=震动、选择=音效；胜利/失败另有提示
    LaunchedEffect(state.feedbackId) {
        if (state.feedbackId == 0) return@LaunchedEffect
        when (state.feedbackKind) {
            GameFeedback.CORRECT -> if (settings.soundEnabled) sound.correct()
            GameFeedback.WRONG -> if (settings.vibrationEnabled) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            GameFeedback.SELECT_CELL, GameFeedback.SELECT_DIGIT ->
                if (settings.soundEnabled) sound.select()
            null -> {}
        }
    }
    LaunchedEffect(state.winInfo) {
        if (state.winInfo != null) sound.success()
    }
    LaunchedEffect(state.lost) {
        if (state.lost) sound.error()
    }

    // 退到后台自动暂停
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) vm.autoPause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = !state.finished && !state.loading) {
        vm.onExit()
        onBack()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (state.isDaily) "每日挑战" else state.difficulty.label,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        if (state.isDaily) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = state.difficulty.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        if (state.limitMistakes) {
                            Text(
                                text = "错误 ${state.mistakes}/3",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (state.mistakes >= 2) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = "错误 ${state.mistakes}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.finished || state.loading) onBack() else showExitDialog = true
                    }) {
                        Icon(Icons.Rounded.Close, contentDescription = "退出")
                    }
                },
                actions = {
                    if (!state.loading) {
                        IconButton(onClick = {
                            vm.inGameShareData()?.let { SudokuShare.share(context, it) }
                        }) {
                            Icon(Icons.Rounded.Share, contentDescription = "分享")
                        }
                    }
                    if (!state.finished && !state.loading) {
                        IconButton(onClick = { vm.pause() }) {
                            Icon(Icons.Rounded.Pause, contentDescription = "暂停")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("正在生成题目…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 顶部信息行：计时
            Text(
                text = when {
                    state.paused -> "已暂停"
                    !settings.showTimer -> "计时已隐藏"
                    else -> formatDuration(state.elapsedSeconds)
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (state.paused) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))

            SudokuBoard(
                state = state,
                highlightRegion = settings.highlightRegion,
                highlightSame = settings.highlightSame,
                onCellClick = { vm.selectCell(it) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            NumberPad(
                values = state.values,
                enabled = !state.paused && !state.finished,
                highlight = if (state.lightningMode) state.lightningDigit else state.lastDigit,
                onNumber = { number ->
                    vm.inputNumber(number)
                },
            )

            Spacer(Modifier.height(12.dp))

            ActionBar(
                noteMode = state.noteMode,
                lightningMode = state.lightningMode,
                enabled = !state.paused && !state.finished,
                canUndo = true,
                canRedo = true,
                onUndo = { vm.undo() },
                onRedo = { vm.redo() },
                onToggleNotes = { vm.toggleNoteMode() },
                onToggleLightning = { vm.toggleLightning() },
                onErase = { vm.erase() },
                onHint = { vm.hint() },
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    // 暂停遮罩
    if (state.paused && !state.finished) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.97f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.Pause,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text("游戏已暂停", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(20.dp))
                Button(onClick = { vm.resume() }) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("继续游戏")
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("退出本局？") },
            text = { Text("当前进度会自动保存，之后可以从主菜单继续本局。") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    vm.onExit()
                    onBack()
                }) { Text("退出") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("继续玩") }
            },
        )
    }

    state.winInfo?.let { win ->
        WinDialog(
            winInfo = win,
            onPlayAgain = { vm.playNext() },
            onBackToMenu = { onBack() },
            onShare = { vm.winShareData()?.let { SudokuShare.share(context, it) } },
        )
    }

    if (state.lost) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("本局失败") },
            text = { Text("错误次数已达上限（3 次）。可以重新开始本局，或返回主菜单。") },
            confirmButton = {
                Button(onClick = { vm.playNext() }) { Text("再来一局") }
            },
            dismissButton = {
                TextButton(onClick = { onBack() }) { Text("返回菜单") }
            },
        )
    }

    // 局中成就解锁提示：非阻塞顶部卡片，不打断游玩（对局结束后由结算页列出）
    state.achievementPopup?.let { def ->
        key(def.id) {
            AchievementPopup(
                def = def,
                onSound = { if (settings.soundEnabled) sound.achievement() },
                onDismiss = { vm.dismissAchievementPopup() },
            )
        }
    }
}

/** 成就解锁的顶部提示卡片：自动消失，可点击跳过，不阻塞对局操作。 */
@Composable
private fun AchievementPopup(
    def: AchievementDef,
    onSound: () -> Unit,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(def.id) {
        onSound()
        delay(2500)
        onDismiss()
    }
    val visibleState = remember { MutableTransitionState(false) }.apply { targetState = true }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = fadeOut(),
        ) {
            Card(
                modifier = Modifier
                    .padding(top = 56.dp, start = 16.dp, end = 16.dp)
                    .clickable { onDismiss() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        def.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "成就解锁 · ${def.title}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            def.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SudokuBoard(
    state: GameUiState,
    highlightRegion: Boolean,
    highlightSame: Boolean,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val conflicts = state.conflicts
    // 高亮跟随的数字：闪电模式下是锁定的数字，否则是选中格的数字
    val activeDigit = if (state.lightningMode) {
        state.lightningDigit
    } else {
        state.selected?.let { state.values[it] }?.takeIf { it != 0 }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .drawWithContent {
                drawContent()
                drawGridLines(
                    lineColor = colors.onSurfaceVariant.copy(alpha = 0.35f),
                    boxLineColor = colors.onSurfaceVariant.copy(alpha = 0.9f),
                )
            },
    ) {
        val cellSize = maxWidth / 9
        val numberFontSize = (cellSize.value * 0.52f).sp
        val noteFontSize = (cellSize.value * 0.30f).sp

        Column(Modifier.fillMaxSize()) {
            for (row in 0..8) {
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    for (col in 0..8) {
                        val index = row * 9 + col
                        val value = state.values[index]
                        val given = state.givenMask[index]
                        val isSelected = state.selected == index
                        // 选中格的行、列、宫（宫按选中格真实行列所在的三宫计算）
                        val selRow = state.selected?.div(9) ?: -1
                        val selCol = state.selected?.rem(9) ?: -1
                        val inRegion = highlightRegion && state.selected != null &&
                            (row == selRow || col == selCol ||
                                (row / 3 == selRow / 3 && col / 3 == selCol / 3))
                        val sameValue = highlightSame && activeDigit != null &&
                            value == activeDigit && !isSelected
                        val conflict = conflicts[index]
                        val isError = !given && value != 0 && value != state.solution[index]
                        // 实心绿只用于有数字的格子（选中空格不用实心，避免盖住笔记）
                        val isGreen = !conflict &&
                            (sameValue || (isSelected && value != 0))
                        val isTint = !conflict && !isGreen &&
                            (inRegion || isSelected)

                        val bg = when {
                            conflict -> colors.errorContainer
                            isGreen -> colors.primary
                            isTint -> colors.primary.copy(alpha = 0.12f)
                            else -> colors.surface
                        }
                        val textColor = when {
                            conflict || isError -> colors.error
                            isGreen -> colors.onPrimary
                            given -> colors.onSurface
                            else -> colors.primary
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(bg)
                                .clickable { onCellClick(index) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (value != 0) {
                                Text(
                                    text = value.toString(),
                                    fontSize = numberFontSize,
                                    fontWeight = if (given) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor,
                                )
                            } else if (state.notes[index].isNotEmpty()) {
                                NoteGrid(
                                    notes = state.notes[index],
                                    fontSize = noteFontSize,
                                    color = colors.onSurfaceVariant,
                                    activeDigit = activeDigit,
                                    highlightBg = colors.primary,
                                    highlightText = colors.onPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 统一绘制网格线：细线为普通边界，粗线为 3x3 宫边界，最外圈加粗描边。 */
private fun DrawScope.drawGridLines(lineColor: Color, boxLineColor: Color) {
    val w = size.width
    val h = size.height
    val thin = 1.dp.toPx()
    val thick = 2.dp.toPx()
    for (i in 1..8) {
        val x = w * i / 9f
        drawLine(
            color = if (i % 3 == 0) boxLineColor else lineColor,
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = if (i % 3 == 0) thick else thin,
        )
        val y = h * i / 9f
        drawLine(
            color = if (i % 3 == 0) boxLineColor else lineColor,
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = if (i % 3 == 0) thick else thin,
        )
    }
    drawRect(
        color = boxLineColor,
        topLeft = Offset.Zero,
        size = Size(w, h),
        style = Stroke(width = 2.dp.toPx()),
    )
}

@Composable
private fun NoteGrid(
    notes: Set<Int>,
    fontSize: TextUnit,
    color: Color,
    activeDigit: Int?,
    highlightBg: Color,
    highlightText: Color,
) {
    // 用 Canvas 绘制候选数字：按字形墨迹包围盒（而非字体行框）居中，
    // 保证 3x3 均匀分布且不受字体度量差异影响。
    // 与当前选中数字相同的候选：整个槽位绘制填充圆角方块，数字反白（参考主流数独应用样式）。
    val textMeasurer = rememberTextMeasurer()
    val normalStyle = TextStyle(
        fontSize = fontSize,
        color = color,
        fontFamily = FontFamily.SansSerif,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
    val highlightStyle = normalStyle.copy(color = highlightText, fontWeight = FontWeight.Bold)
    val cornerRadius = 3.dp
    Canvas(Modifier.fillMaxSize()) {
        val inset = 1.dp.toPx()
        notes.forEach { digit ->
            val row = (digit - 1) / 3
            val col = (digit - 1) % 3
            val slotW = size.width / 3f
            val slotH = size.height / 3f
            val isActive = digit == activeDigit
            if (isActive) {
                drawRoundRect(
                    color = highlightBg,
                    topLeft = Offset(slotW * col + inset, slotH * row + inset),
                    size = Size(slotW - inset * 2, slotH - inset * 2),
                    cornerRadius = CornerRadius(cornerRadius.toPx()),
                )
            }
            val layout = textMeasurer.measure(
                digit.toString(),
                if (isActive) highlightStyle else normalStyle,
            )
            val bb = layout.getBoundingBox(0)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = slotW * col + (slotW - bb.width) / 2f - bb.left,
                    y = slotH * row + (slotH - bb.height) / 2f - bb.top,
                ),
            )
        }
    }
}

@Composable
private fun NumberPad(
    values: List<Int>,
    enabled: Boolean,
    highlight: Int?,
    onNumber: (Int) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (digit in 1..9) {
            val placed = values.count { it == digit }
            val complete = placed >= 9
            val active = enabled && !complete
            val isHighlighted = highlight == digit
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .alpha(if (active || isHighlighted) 1f else 0.35f)
                    .background(
                        if (isHighlighted) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(10.dp),
                    )
                    .border(
                        width = if (isHighlighted) 1.5.dp else 0.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .clickable(enabled = active) { onNumber(digit) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = digit.toString(),
                    fontSize = 22.sp,
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${maxOf(0, 9 - placed)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActionBar(
    noteMode: Boolean,
    lightningMode: Boolean,
    enabled: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleNotes: () -> Unit,
    onToggleLightning: () -> Unit,
    onErase: () -> Unit,
    onHint: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionButton(
            icon = { Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "撤销") },
            enabled = enabled && canUndo,
            onClick = onUndo,
        )
        ActionButton(
            icon = { Icon(Icons.Rounded.Redo, contentDescription = "重做") },
            enabled = enabled && canRedo,
            onClick = onRedo,
        )
        ActionButton(
            icon = {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = "笔记",
                    tint = if (noteMode) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            enabled = enabled,
            highlighted = noteMode,
            onClick = onToggleNotes,
        )
        ActionButton(
            icon = {
                Icon(
                    Icons.Rounded.Bolt,
                    contentDescription = "闪电模式",
                    tint = if (lightningMode) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            enabled = enabled,
            highlighted = lightningMode,
            onClick = onToggleLightning,
        )
        ActionButton(
            icon = { Icon(EraserIcon, contentDescription = "擦除") },
            enabled = enabled,
            onClick = onErase,
        )
        ActionButton(
            icon = { Icon(Icons.Rounded.Lightbulb, contentDescription = "提示", tint = AccentAmberLight) },
            enabled = enabled,
            onClick = onHint,
        )
    }
}

@Composable
private fun ActionButton(
    icon: @Composable () -> Unit,
    enabled: Boolean,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        icon()
    }
}

@Composable
private fun WinDialog(
    winInfo: WinInfo,
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit,
    onShare: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onShare) {
                    Icon(
                        Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("分享")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onPlayAgain) { Text("再来一局") }
            }
        },
        dismissButton = {
            TextButton(onClick = onBackToMenu) { Text("返回菜单") }
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🎉", fontSize = 40.sp)
                Text(
                    "完成！",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (winInfo.isNewRecord) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            "🏆 新纪录！",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (winInfo.isDaily) "每日挑战 · ${formatDuration(winInfo.durationSeconds)}"
                    else "${winInfo.difficulty.label} · ${formatDuration(winInfo.durationSeconds)}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "最佳纪录 ${formatDuration(winInfo.bestTimeSeconds)} · 错误 ${winInfo.mistakes} 次 · 提示 ${winInfo.hintsUsed} 次",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (winInfo.unlockedAchievements.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "解锁成就",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    for (achievement in winInfo.unlockedAchievements) {
                        Text(
                            "🏅 ${achievement.title} — ${achievement.description}",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        },
    )
}
