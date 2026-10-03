package com.balsam.sudoku.achievements

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EditOff
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.TableRows
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.ui.graphics.vector.ImageVector
import com.balsam.sudoku.game.Difficulty

/** 成就分组：成就页按此分组展示，每组 4~6 个。 */
enum class AchievementGroup(val title: String) {
    MILESTONE("里程碑"),
    BEGINNER("数独菜鸟"),
    EASY("进阶"),
    MEDIUM("中阶"),
    HARD("高阶"),
    EXPERT("学士"),
    EXTREME("大师"),
    BOARD_SKILL("棋盘技巧"),
    PEAK("巅峰表现"),
    PERSISTENT("持之以恒"),
    DAILY_CHALLENGE("挑战先锋"),
    PENCIL("铅笔狂魔"),
    HINTS("巧夺天工"),
}

/** 成就定义。 */
data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val group: AchievementGroup,
)

object Achievements {

    private val difficultyIcons = mapOf(
        Difficulty.BEGINNER to Icons.Rounded.WbSunny,
        Difficulty.EASY to Icons.Rounded.SentimentSatisfied,
        Difficulty.MEDIUM to Icons.Rounded.FitnessCenter,
        Difficulty.HARD to Icons.Rounded.LocalFireDepartment,
        Difficulty.EXPERT to Icons.Rounded.WorkspacePremium,
        Difficulty.EXTREME to Icons.Rounded.Whatshot,
    )

    private fun difficultyGroup(d: Difficulty): AchievementGroup = when (d) {
        Difficulty.BEGINNER -> AchievementGroup.BEGINNER
        Difficulty.EASY -> AchievementGroup.EASY
        Difficulty.MEDIUM -> AchievementGroup.MEDIUM
        Difficulty.HARD -> AchievementGroup.HARD
        Difficulty.EXPERT -> AchievementGroup.EXPERT
        Difficulty.EXTREME -> AchievementGroup.EXTREME
    }

    /** 手写的固定成就。 */
    private val FIXED: List<AchievementDef> = listOf(
        // 里程碑
        AchievementDef("first_win", "初试身手", "完成你的第一局数独", Icons.Rounded.EmojiEvents, AchievementGroup.MILESTONE),
        AchievementDef("total_10", "十局老手", "累计完成 10 局", Icons.Rounded.MilitaryTech, AchievementGroup.MILESTONE),
        AchievementDef("total_50", "五十胜场", "累计完成 50 局", Icons.Rounded.Stars, AchievementGroup.MILESTONE),
        AchievementDef("total_100", "百局大师", "累计完成 100 局", Icons.Rounded.Leaderboard, AchievementGroup.MILESTONE),
        AchievementDef("new_record", "突破自我", "刷新任意难度的最佳时间", Icons.Rounded.Bolt, AchievementGroup.MILESTONE),
        AchievementDef("daily_5", "日行五局", "单日完成 5 局", Icons.Rounded.Celebration, AchievementGroup.MILESTONE),
        // 棋盘技巧
        AchievementDef("first_correct", "妙手初成", "第一次填入正确的数字", Icons.Rounded.Verified, AchievementGroup.BOARD_SKILL),
        AchievementDef("first_row", "行云流水", "第一次填对一整行", Icons.Rounded.TableRows, AchievementGroup.BOARD_SKILL),
        AchievementDef("first_col", "一以贯之", "第一次填对一整列", Icons.Rounded.ViewColumn, AchievementGroup.BOARD_SKILL),
        AchievementDef("first_box", "九宫归位", "第一次填对一整个宫", Icons.Rounded.GridView, AchievementGroup.BOARD_SKILL),
        // 巅峰表现
        AchievementDef("zero_mistake", "零失误", "全程无错误地完成一局", Icons.Rounded.Verified, AchievementGroup.PEAK),
        AchievementDef("no_hint", "自力更生", "不使用提示完成一局", Icons.Rounded.Lightbulb, AchievementGroup.PEAK),
        AchievementDef("no_notes_hard", "心中有序", "不使用笔记完成困难及以上难度", Icons.Rounded.EditOff, AchievementGroup.PEAK),
        AchievementDef("speed_10min", "十分钟挑战", "在任意难度下 10 分钟内完成", Icons.Rounded.Timer, AchievementGroup.PEAK),
        AchievementDef("speed_expert_30", "专家疾速", "在专家难度下 30 分钟内完成", Icons.Rounded.Speed, AchievementGroup.PEAK),
    )

    private val GENERATED: List<AchievementDef> = buildList {
        // 每难度完成局数：5/10/20/50/100（与该难度首胜同组）
        val countMilestones = listOf(5, 10, 20, 50, 100)
        for (difficulty in Difficulty.entries) {
            val icon = difficultyIcons[difficulty]!!
            add(
                AchievementDef(
                    id = "win_${difficulty.name.lowercase()}",
                    title = "${difficulty.label}首胜",
                    description = "完成一局${difficulty.label}难度",
                    icon = icon,
                    group = difficultyGroup(difficulty),
                ),
            )
            for (n in countMilestones) {
                add(
                    AchievementDef(
                        id = "count_${difficulty.name}_$n",
                        title = "$n 局达成",
                        description = "完成 $n 局${difficulty.label}难度",
                        icon = icon,
                        group = difficultyGroup(difficulty),
                    ),
                )
            }
        }
        // 累计完成天数（不同的完成日）
        val dayIcons = mapOf(
            7 to Icons.Rounded.Today,
            30 to Icons.Rounded.CalendarMonth,
            100 to Icons.Rounded.EventNote,
            180 to Icons.Rounded.EventAvailable,
            365 to Icons.Rounded.EmojiEvents,
        )
        for (n in listOf(7, 30, 100, 180, 365)) {
            add(
                AchievementDef(
                    id = "days_$n",
                    title = "坚持 $n 天",
                    description = "累计 $n 个不同的日子完成数独",
                    icon = dayIcons[n]!!,
                    group = AchievementGroup.PERSISTENT,
                ),
            )
        }
        // 铅笔：使用铅笔的对局数
        for (n in listOf(1, 5, 10, 20, 50, 100)) {
            add(
                AchievementDef(
                    id = "pencil_$n",
                    title = "铅笔 $n 局",
                    description = "在 $n 局对局中使用铅笔笔记",
                    icon = Icons.Rounded.EditNote,
                    group = AchievementGroup.PENCIL,
                ),
            )
        }
        // 提示：累计使用次数
        for (n in listOf(1, 5, 10, 20, 50, 100)) {
            add(
                AchievementDef(
                    id = "hints_$n",
                    title = "提示 $n 次",
                    description = "累计使用 $n 次提示",
                    icon = Icons.Rounded.TipsAndUpdates,
                    group = AchievementGroup.HINTS,
                ),
            )
        }
        // 每日挑战累计天数
        val dailyIcons = mapOf(
            3 to Icons.Rounded.WbSunny,
            7 to Icons.Rounded.Today,
            30 to Icons.Rounded.CalendarMonth,
            100 to Icons.Rounded.EmojiEvents,
        )
        for (n in listOf(3, 7, 30, 100)) {
            add(
                AchievementDef(
                    id = "daily_$n",
                    title = "挑战 $n 天",
                    description = "累计完成 $n 天每日挑战",
                    icon = dailyIcons[n]!!,
                    group = AchievementGroup.DAILY_CHALLENGE,
                ),
            )
        }
    }

    val ALL: List<AchievementDef> = FIXED + GENERATED

    private val byId: Map<String, AchievementDef> = ALL.associateBy { it.id }

    fun byId(id: String): AchievementDef? = byId[id]

    /** 成就页分组展示顺序。 */
    val GROUP_ORDER: List<AchievementGroup> = AchievementGroup.entries.toList()
}
