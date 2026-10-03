package com.balsam.sudoku.ui.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balsam.sudoku.SudokuApp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import com.balsam.sudoku.achievements.AchievementDef
import com.balsam.sudoku.achievements.AchievementEngine
import com.balsam.sudoku.achievements.AchievementGroup
import com.balsam.sudoku.achievements.GameEndContext
import com.balsam.sudoku.data.GameRecord
import com.balsam.sudoku.data.GameSave
import com.balsam.sudoku.game.Difficulty
import com.balsam.sudoku.game.GameLogic
import com.balsam.sudoku.game.SudokuGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 一次操作产生的用户反馈（音效/震动由 UI 层播放）。 */
enum class GameFeedback { CORRECT, WRONG, SELECT_CELL, SELECT_DIGIT }

/** 本局结算信息（胜利时展示）。 */
data class WinInfo(
    val difficulty: Difficulty,
    val durationSeconds: Int,
    val mistakes: Int,
    val hintsUsed: Int,
    val isNewRecord: Boolean,
    val bestTimeSeconds: Int,
    val unlockedAchievements: List<AchievementDef>,
    val isDaily: Boolean = false,
    val values: List<Int> = emptyList(),
    val givenMask: List<Boolean> = emptyList(),
)

data class GameUiState(
    val loading: Boolean = true,
    val difficulty: Difficulty = Difficulty.EASY,
    val values: List<Int> = List(81) { 0 },
    val givenMask: List<Boolean> = List(81) { false },
    val solution: List<Int> = List(81) { 0 },
    val notes: List<Set<Int>> = List(81) { emptySet() },
    val selected: Int? = null,
    val noteMode: Boolean = false,
    val elapsedSeconds: Int = 0,
    val paused: Boolean = false,
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val notesUsed: Boolean = false,
    val limitMistakes: Boolean = false,
    val finished: Boolean = false,
    val winInfo: WinInfo? = null,
    val lost: Boolean = false,
    // 闪电模式：锁定一个数字，依次填入所有适用的格子
    val lightningMode: Boolean = false,
    val lightningDigit: Int? = null,
    // 最近一次填入的数字（普通模式下键盘高亮用）
    val lastDigit: Int? = null,
    // 操作反馈：feedbackId 单调递增，UI 据此播放音效/震动
    val feedbackId: Int = 0,
    val feedbackKind: GameFeedback? = null,
    // 本局是否为每日挑战
    val isDaily: Boolean = false,
    // 局中成就解锁弹窗队列（队首为当前显示项）
    val popupQueue: List<AchievementDef> = emptyList(),
) {
    val conflicts: BooleanArray
        get() = GameLogic.findConflicts(values)

    val achievementPopup: AchievementDef?
        get() = popupQueue.firstOrNull()
}

/** 撤销快照：只回退盘面内容；错误数与提示数一经发生不回退。 */
private data class Snapshot(
    val values: List<Int>,
    val notes: List<Set<Int>>,
)

class GameViewModel(private val app: SudokuApp, private val difficultyName: String) : ViewModel() {

    private val settingsRepository = app.settingsRepository
    private val recordDao = app.database.gameRecordDao()
    private val dailyChallengeDao = app.database.dailyChallengeDao()
    private val monthTrophyDao = app.database.monthTrophyDao()
    private val achievementEngine = AchievementEngine(
        app.database.achievementDao(),
        recordDao,
        app.database.dailyChallengeDao(),
    )
    private val generator = SudokuGenerator()

    /** 本局是否为每日挑战。 */
    private val isDaily = difficultyName == "daily"

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state

    private var puzzle: List<Int> = emptyList()
    private var solution: List<Int> = emptyList()

    private val undoStack = ArrayDeque<Snapshot>()
    private val redoStack = ArrayDeque<Snapshot>()

    private var ticker: Job? = null
    private var advanceJob: Job? = null
    private var started = false

    init {
        viewModelScope.launch {
            when (difficultyName) {
                "continue" -> restoreSavedGame()
                "daily" -> startNewGame(suggestDailyDifficulty())
                else -> startNewGame(Difficulty.fromName(difficultyName))
            }
            val settings = settingsRepository.settings.first()
            _state.update { it.copy(limitMistakes = settings.limitMistakes) }
        }
    }

    /** 每日挑战难度：最近 20 局完成的难度众数（并列取更难），无历史 → 简单。 */
    private suspend fun suggestDailyDifficulty(): Difficulty {
        val recent = recordDao.recentWins(DAILY_DIFFICULTY_WINDOW)
            .mapNotNull { runCatching { Difficulty.valueOf(it.difficulty) }.getOrNull() }
        return AchievementEngine.suggestDailyDifficulty(recent)
    }

    private suspend fun startNewGame(difficulty: Difficulty, seed: Long? = null) {
        _state.update { it.copy(loading = true) }
        val generated = withContext(Dispatchers.Default) { generator.generate(difficulty, seed) }
        puzzle = generated.puzzle.toList()
        solution = generated.solution.toList()
        undoStack.clear()
        redoStack.clear()
        started = true
        _state.update {
            it.copy(
                loading = false,
                difficulty = difficulty,
                values = puzzle.toList(),
                givenMask = puzzle.map { v -> v != 0 },
                solution = solution,
                notes = List(81) { emptySet() },
                selected = null,
                noteMode = false,
                elapsedSeconds = 0,
                paused = false,
                mistakes = 0,
                hintsUsed = 0,
                notesUsed = false,
                finished = false,
                winInfo = null,
                lost = false,
                // 新局默认进入闪电模式，等玩家锁定第一个数字
                lightningMode = true,
                lightningDigit = null,
                isDaily = isDaily,
            )
        }
        startTicker()
        persistSave()
    }

    private suspend fun restoreSavedGame() {
        val save = settingsRepository.currentGame.first()
        if (save == null) {
            startNewGame(Difficulty.EASY)
            return
        }
        puzzle = save.puzzle
        solution = save.solution
        undoStack.clear()
        redoStack.clear()
        started = true
        _state.update {
            it.copy(
                loading = false,
                difficulty = Difficulty.fromName(save.difficulty),
                values = save.values,
                givenMask = save.puzzle.map { v -> v != 0 },
                solution = save.solution,
                notes = save.notes,
                elapsedSeconds = save.elapsedSeconds,
                mistakes = save.mistakes,
                hintsUsed = save.hintsUsed,
                notesUsed = save.notesUsed,
                finished = false,
            )
        }
        startTicker()
        // 恢复的棋局可能已是完成状态（如进程在结算前被杀），补一次胜利判定
        checkWin()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive && !_state.value.finished) {
                delay(1000)
                val s = _state.value
                if (!s.paused && !s.finished && !s.loading) {
                    _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }
    }

    private fun emit(kind: GameFeedback) = _state.update {
        it.copy(feedbackId = it.feedbackId + 1, feedbackKind = kind)
    }

    fun selectCell(index: Int) {
        val s = _state.value
        if (s.finished || s.paused || s.loading) return
        if (s.lightningMode) {
            val digit = s.lightningDigit
            if (digit == null) {
                // 未锁定数字时，点击已填数字的格子即锁定该数字，并选中其最上方的格子
                val v = s.values[index]
                if (v in 1..9) {
                    _state.update {
                        it.copy(
                            selected = GameLogic.topmostCellOfDigit(it.values, v),
                            lightningDigit = v,
                        )
                    }
                    emit(GameFeedback.SELECT_DIGIT)
                } else {
                    _state.update { it.copy(selected = index) }
                    emit(GameFeedback.SELECT_CELL)
                }
                return
            }
            if (s.noteMode) {
                // 闪电 + 笔记：点击棋盘写入当前数字的笔记
                if (s.givenMask[index] || GameLogic.peersContainDigit(s.values, index, digit)) return
                pushUndo()
                val current = s.notes[index]
                val updated = if (digit in current) current - digit else current + digit
                _state.update {
                    it.copy(
                        notes = it.notes.toMutableList().also { l -> l[index] = updated },
                        notesUsed = true,
                    )
                }
                emit(GameFeedback.SELECT_CELL)
                persistSave()
                return
            }
            // 闪电填数：任何空格都可以填入（填错会计入错误），填满 9 个后自动切换
            if (s.values[index] == 0 && !s.givenMask[index]) {
                placeValue(index, digit)
                scheduleLightningAdvance(digit)
                persistSave()
            }
            return
        }
        _state.update { it.copy(selected = index) }
        emit(GameFeedback.SELECT_CELL)
    }

    fun inputNumber(number: Int) {
        val s = _state.value
        if (s.finished || s.paused || s.loading || number !in 1..9) return
        // 闪电模式下，数字键盘用于切换锁定的数字；
        // 相当于选中棋盘上该数字最上方的那一格，高亮随之变化
        if (s.lightningMode) {
            _state.update {
                it.copy(
                    lightningDigit = number,
                    selected = GameLogic.topmostCellOfDigit(it.values, number),
                )
            }
            emit(GameFeedback.SELECT_DIGIT)
            return
        }
        val cell = s.selected ?: return
        if (s.givenMask[cell]) return

        if (s.noteMode) {
            // 同行列宫已有该数字时禁止写入笔记
            if (GameLogic.peersContainDigit(s.values, cell, number)) return
            pushUndo()
            val current = s.notes[cell]
            val updated = if (number in current) current - number else current + number
            _state.update {
                it.copy(
                    notes = it.notes.toMutableList().also { l -> l[cell] = updated },
                    notesUsed = true,
                )
            }
        } else {
            placeValue(cell, number)
        }
        persistSave()
    }

    /** 写入正式数字（含错误计数、清笔记、胜负判定与反馈事件）。 */
    private fun placeValue(cell: Int, number: Int) {
        pushUndo()
        val s = _state.value
        val correct = number == solution[cell]
        val newMistakes = if (correct) s.mistakes else s.mistakes + 1
        _state.update {
            it.copy(
                values = it.values.toMutableList().also { l -> l[cell] = number },
                notes = it.notes.toMutableList().also { l -> l[cell] = emptySet() },
                mistakes = newMistakes,
                lastDigit = number,
            )
        }
        emit(if (correct) GameFeedback.CORRECT else GameFeedback.WRONG)
        clearPeerNotes(cell, number)
        if (correct) {
            // 局中成就：首对数字、填对行/列/宫
            val s0 = _state.value
            viewModelScope.launch {
                val newly = achievementEngine.evaluatePlacement(s0.values, s0.solution, cell)
                enqueuePopups(newly)
            }
        }
        checkMistakeLimit(newMistakes)
        checkWin()
    }

    /** 把新解锁成就加入局中弹窗队列（对局已结束时不弹，由结算页列出）。 */
    private fun enqueuePopups(newly: List<AchievementDef>) {
        if (newly.isEmpty() || _state.value.finished) return
        _state.update { it.copy(popupQueue = it.popupQueue + newly) }
    }

    /** 当前成就弹窗展示完毕（自动超时或用户点击）后调用。 */
    fun dismissAchievementPopup() {
        _state.update { it.copy(popupQueue = it.popupQueue.drop(1)) }
    }

    /** 填入数字后，自动清掉同行/列/宫中该数字的笔记。 */
    private fun clearPeerNotes(cell: Int, number: Int) {
        val row = cell / 9
        val col = cell % 9
        val box = (row / 3) * 3 + col / 3
        _state.update { s ->
            val notes = s.notes.toMutableList()
            for (i in 0..80) {
                if (notes[i].contains(number)) {
                    val r = i / 9
                    val c = i % 9
                    val b = (r / 3) * 3 + c / 3
                    if (r == row || c == col || b == box) {
                        notes[i] = notes[i] - number
                    }
                }
            }
            s.copy(notes = notes)
        }
    }

    fun toggleNoteMode() {
        _state.update { it.copy(noteMode = !it.noteMode) }
    }

    fun erase() {
        val s = _state.value
        val cell = s.selected ?: return
        if (s.finished || s.paused || s.loading || s.givenMask[cell]) return
        if (s.values[cell] == 0 && s.notes[cell].isEmpty()) return
        pushUndo()
        _state.update {
            it.copy(
                values = it.values.toMutableList().also { l -> l[cell] = 0 },
                notes = it.notes.toMutableList().also { l -> l[cell] = emptySet() },
            )
        }
        persistSave()
    }

    fun undo() {
        val snapshot = undoStack.removeLastOrNull() ?: return
        val s = _state.value
        redoStack.addLast(Snapshot(s.values, s.notes))
        _state.update {
            // 错误数与提示数不随撤销回退
            it.copy(values = snapshot.values, notes = snapshot.notes)
        }
        persistSave()
    }

    fun redo() {
        val snapshot = redoStack.removeLastOrNull() ?: return
        val s = _state.value
        undoStack.addLast(Snapshot(s.values, s.notes))
        _state.update {
            it.copy(values = snapshot.values, notes = snapshot.notes)
        }
        persistSave()
    }

    private fun pushUndo() {
        val s = _state.value
        undoStack.addLast(Snapshot(s.values, s.notes))
        if (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        redoStack.clear()
    }

    /** 切换闪电模式。激活时若已选中一个数字格，则锁定该格的数字。 */
    fun toggleLightning() {
        advanceJob?.cancel()
        _state.update { s ->
            if (s.lightningMode) {
                s.copy(lightningMode = false, lightningDigit = null)
            } else {
                val digit = s.selected?.let { s.values[it] }?.takeIf { it in 1..9 }
                s.copy(
                    lightningMode = true,
                    lightningDigit = digit,
                    selected = digit?.let { GameLogic.topmostCellOfDigit(s.values, it) },
                )
            }
        }
    }

    /** 当前数字填满 9 个后稍作停留（展示填入结果）再自动切换到下一个数字。 */
    private fun scheduleLightningAdvance(completedDigit: Int) {
        if (!_state.value.lightningMode) return
        if (placedCount(completedDigit) < 9) return
        advanceJob?.cancel()
        advanceJob = viewModelScope.launch {
            delay(600)
            val cur = _state.value
            if (cur.lightningMode && cur.lightningDigit == completedDigit) {
                advanceLightning()
            }
        }
    }

    /** 切换到下一个还没填满 9 个的数字；全部填完则退出闪电模式。
     *  调用前提：当前数字已填满 9 个（由 scheduleLightningAdvance 保证）。 */
    private fun advanceLightning() {
        val s = _state.value
        if (!s.lightningMode) return
        val digit = s.lightningDigit ?: return
        val next = ((digit + 1)..9).firstOrNull { placedCount(it) < 9 }
            ?: (1 until digit).firstOrNull { placedCount(it) < 9 }
        _state.update {
            if (next == null) {
                it.copy(lightningMode = false, lightningDigit = null)
            } else {
                it.copy(
                    lightningDigit = next,
                    selected = GameLogic.topmostCellOfDigit(it.values, next),
                )
            }
        }
    }

    private fun placedCount(digit: Int): Int = _state.value.values.count { it == digit }

    fun hint() {
        val s = _state.value
        if (s.finished || s.paused || s.loading) return
        val target = s.selected?.takeIf { !s.givenMask[it] && s.values[it] != solution[it] }
            ?: (0..80).firstOrNull { !s.givenMask[it] && s.values[it] != solution[it] }
            ?: return
        val value = solution[target]
        placeValue(target, value)
        _state.update {
            it.copy(selected = target, hintsUsed = it.hintsUsed + 1)
        }
        // 提示累计里程碑（历史合计 + 本局已用）
        viewModelScope.launch {
            val pastTotal = recordDao.totalHintsUsed() ?: 0
            val newly = achievementEngine.evaluateHintMilestones(pastTotal + _state.value.hintsUsed)
            enqueuePopups(newly)
        }
        if (_state.value.lightningMode) {
            scheduleLightningAdvance(_state.value.lightningDigit ?: return)
        }
        persistSave()
    }

    fun pause() {
        if (_state.value.finished) return
        _state.update { it.copy(paused = true) }
        persistSave()
    }

    fun resume() {
        _state.update { it.copy(paused = false) }
    }

    /** 应用退到后台时自动暂停（防作弊 + 计时准确）。 */
    fun autoPause() {
        val s = _state.value
        if (!s.finished && !s.loading && !s.paused) {
            _state.update { it.copy(paused = true) }
            persistSave()
        }
    }

    /** 从系统返回键 / 顶栏返回退出本页。 */
    fun onExit() {
        persistSave()
    }

    private fun checkMistakeLimit(mistakes: Int) {
        val s = _state.value
        if (s.limitMistakes && mistakes >= MAX_MISTAKES) {
            viewModelScope.launch { onLost() }
        }
    }

    private fun checkWin() {
        val s = _state.value
        if (s.finished) return
        val filled = GameLogic.isFilled(s.values)
        if (!filled) return
        val correct = (0..80).all { s.values[it] == solution[it] }
        if (correct) {
            viewModelScope.launch { onWin() }
        }
    }

    private suspend fun onWin() {
        val s = _state.value
        val difficulty = s.difficulty
        val bestBefore = recordDao.bestRecord(difficulty.name)
        // 首胜只是"建立"该难度的纪录，不算"打破"；只有超越已有纪录才算
        val isNewRecord = bestBefore != null && s.elapsedSeconds < bestBefore.durationSeconds
        val bestTimeSeconds = if (bestBefore != null && bestBefore.durationSeconds <= s.elapsedSeconds) {
            bestBefore.durationSeconds
        } else {
            s.elapsedSeconds
        }
        recordDao.insert(
            GameRecord(
                difficulty = difficulty.name,
                durationSeconds = s.elapsedSeconds,
                mistakes = s.mistakes,
                hintsUsed = s.hintsUsed,
                completedAt = System.currentTimeMillis(),
                isWin = true,
                notesUsed = s.notesUsed,
            ),
        )

        // 每日挑战：当天首次完成才记录，随后检查月度全勤奖杯
        var dailyFirstWin = false
        val trophyDefs = mutableListOf<AchievementDef>()
        if (isDaily) {
            val today = java.time.LocalDate.now()
            dailyFirstWin = dailyChallengeDao.insertIfAbsent(
                com.balsam.sudoku.data.DailyChallengeEntity(
                    date = today.toString(),
                    difficulty = difficulty.name,
                    durationSeconds = s.elapsedSeconds,
                    mistakes = s.mistakes,
                    hintsUsed = s.hintsUsed,
                    completedAt = System.currentTimeMillis(),
                ),
            ) != -1L
            if (dailyFirstWin) {
                val month = java.time.YearMonth.from(today)
                val monthDates = dailyChallengeDao.monthDates(month.toString()).toSet()
                if (AchievementEngine.isMonthComplete(monthDates, month)) {
                    val inserted = monthTrophyDao.unlock(
                        com.balsam.sudoku.data.MonthTrophyEntity(
                            month = month.toString(),
                            unlockedAt = System.currentTimeMillis(),
                        ),
                    ) != -1L
                    if (inserted) {
                        trophyDefs += AchievementDef(
                            id = "trophy_$month",
                            title = "${month.monthValue} 月全勤奖杯",
                            description = "完成 ${month.year} 年 ${month.monthValue} 月全部每日挑战",
                            icon = Icons.Rounded.EmojiEvents,
                            group = AchievementGroup.DAILY_CHALLENGE,
                        )
                    }
                }
            }
        }

        val unlocked = achievementEngine.evaluateGameEnd(
            GameEndContext(
                difficulty = difficulty,
                durationSeconds = s.elapsedSeconds,
                mistakes = s.mistakes,
                hintsUsed = s.hintsUsed,
                notesUsed = s.notesUsed,
                isWin = true,
                isNewRecord = isNewRecord,
                bestTimeBefore = bestBefore?.durationSeconds,
                isDailyWin = isDaily && dailyFirstWin,
            ),
        )
        settingsRepository.saveCurrentGame(null)
        _state.update {
            it.copy(
                finished = true,
                winInfo = WinInfo(
                    difficulty = difficulty,
                    durationSeconds = it.elapsedSeconds,
                    mistakes = it.mistakes,
                    hintsUsed = it.hintsUsed,
                    isNewRecord = isNewRecord,
                bestTimeSeconds = bestTimeSeconds,
                unlockedAchievements = trophyDefs + unlocked,
                isDaily = isDaily,
                values = s.values,
                givenMask = s.givenMask,
            ),
        )
    }
    }

    private suspend fun onLost() {
        val s = _state.value
        recordDao.insert(
            GameRecord(
                difficulty = s.difficulty.name,
                durationSeconds = s.elapsedSeconds,
                mistakes = s.mistakes,
                hintsUsed = s.hintsUsed,
                completedAt = System.currentTimeMillis(),
                isWin = false,
                notesUsed = s.notesUsed,
            ),
        )
        // 铅笔/提示里程碑在对局中止后同样可能达成
        achievementEngine.evaluateGameEnd(
            GameEndContext(
                difficulty = s.difficulty,
                durationSeconds = s.elapsedSeconds,
                mistakes = s.mistakes,
                hintsUsed = s.hintsUsed,
                notesUsed = s.notesUsed,
                isWin = false,
                isNewRecord = false,
                bestTimeBefore = null,
            ),
        )
        settingsRepository.saveCurrentGame(null)
        _state.update { it.copy(finished = true, lost = true) }
    }

    /** 用户主动放弃（在确认弹窗中确认）。 */
    fun abandonGame() {
        val s = _state.value
        if (s.finished || !started) return
        viewModelScope.launch {
            recordDao.insert(
                GameRecord(
                    difficulty = s.difficulty.name,
                    durationSeconds = s.elapsedSeconds,
                    mistakes = s.mistakes,
                    hintsUsed = s.hintsUsed,
                    completedAt = System.currentTimeMillis(),
                    isWin = false,
                    notesUsed = s.notesUsed,
                ),
            )
            // 铅笔/提示里程碑在放弃的对局中同样可能达成
            achievementEngine.evaluateGameEnd(
                GameEndContext(
                    difficulty = s.difficulty,
                    durationSeconds = s.elapsedSeconds,
                    mistakes = s.mistakes,
                    hintsUsed = s.hintsUsed,
                    notesUsed = s.notesUsed,
                    isWin = false,
                    isNewRecord = false,
                    bestTimeBefore = null,
                ),
            )
            settingsRepository.saveCurrentGame(null)
        }
    }

    private fun persistSave() {
        val s = _state.value ?: return
        if (s.finished || s.loading) return
        val save = GameSave(
            difficulty = s.difficulty.name,
            puzzle = puzzle,
            solution = solution,
            values = s.values,
            notes = s.notes,
            elapsedSeconds = s.elapsedSeconds,
            mistakes = s.mistakes,
            hintsUsed = s.hintsUsed,
            notesUsed = s.notesUsed,
        )
        viewModelScope.launch { settingsRepository.saveCurrentGame(save) }
    }

    fun playNext() {
        viewModelScope.launch {
            if (isDaily) {
                // 每日挑战再来一局：同题重玩（不计奖励由完成时的首次判定保证）
                startNewGame(_state.value.difficulty, seed = java.time.LocalDate.now().toEpochDay())
            } else {
                startNewGame(_state.value.difficulty)
            }
        }
    }

    /** 分享数据：对局中分享题目本身（仅提示数，不含玩家进度）。 */
    fun inGameShareData(): com.balsam.sudoku.ui.common.ShareBoard? {
        val s = _state.value
        if (s.loading || puzzle.isEmpty()) return null
        val title = if (s.isDaily) "每日挑战 · ${s.difficulty.label}" else "数独 · ${s.difficulty.label}"
        return com.balsam.sudoku.ui.common.ShareBoard(
            title = title,
            subtitle = "来试试这道数独吧",
            values = puzzle,
            givenMask = puzzle.map { it != 0 },
        )
    }

    /** 分享数据：结算页分享完成的盘面与成绩。 */
    fun winShareData(): com.balsam.sudoku.ui.common.ShareBoard? {
        val s = _state.value
        val win = s.winInfo ?: return null
        val title = if (win.isDaily) "每日挑战 · ${win.difficulty.label}" else "数独 · ${win.difficulty.label}"
        return com.balsam.sudoku.ui.common.ShareBoard(
            title = title,
            subtitle = "用时 ${com.balsam.sudoku.ui.common.formatDuration(win.durationSeconds)} · 错误 ${win.mistakes} · 提示 ${win.hintsUsed}",
            values = win.values,
            givenMask = win.givenMask,
        )
    }

    companion object {
        private const val MAX_UNDO = 200
        const val MAX_MISTAKES = 3
        private const val DAILY_DIFFICULTY_WINDOW = 20
    }
}
