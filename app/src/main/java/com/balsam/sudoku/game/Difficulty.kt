package com.balsam.sudoku.game

/**
 * 六档难度。targetHoles 为挖空目标数量：
 * 初学者 ~25、简单 ~35、中级 ~45、困难 ~54、专家 ~58、极端 60+。
 */
enum class Difficulty(val label: String, val targetHoles: Int) {
    BEGINNER("初学者", 25),
    EASY("简单", 35),
    MEDIUM("中级", 45),
    HARD("困难", 54),
    EXPERT("专家", 58),
    EXTREME("极端", 60);

    companion object {
        fun fromName(name: String): Difficulty = entries.first { it.name == name }
    }
}
