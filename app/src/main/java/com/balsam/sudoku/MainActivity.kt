package com.balsam.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.balsam.sudoku.data.ThemeMode
import com.balsam.sudoku.game.Difficulty
import com.balsam.sudoku.ui.achievements.AchievementsScreen
import com.balsam.sudoku.ui.menu.DailyCalendarScreen
import com.balsam.sudoku.ui.menu.MenuScreen
import com.balsam.sudoku.ui.play.GameScreen
import com.balsam.sudoku.ui.settings.SettingsScreen
import com.balsam.sudoku.ui.stats.StatsScreen
import com.balsam.sudoku.ui.theme.SudokuTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SudokuApp
        setContent {
            val settings by app.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = null)
            val themeMode = settings?.themeMode ?: ThemeMode.SYSTEM
            SudokuTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "menu") {
                    composable("menu") {
                        MenuScreen(
                            app = app,
                            onPlay = { difficulty ->
                                navController.navigate("game/${difficulty.name}")
                            },
                            onContinue = {
                                navController.navigate("game/continue")
                            },
                            onOpenDailyCalendar = {
                                navController.navigate("daily_calendar")
                            },
                            onOpenStats = { navController.navigate("stats") },
                            onOpenAchievements = { navController.navigate("achievements") },
                            onOpenSettings = { navController.navigate("settings") },
                        )
                    }
                    composable(
                        route = "game/{difficulty}?date={date}",
                        arguments = listOf(
                            navArgument("difficulty") { type = NavType.StringType },
                            navArgument("date") {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                        ),
                    ) { entry ->
                        GameScreen(
                            app = app,
                            difficultyName = entry.arguments?.getString("difficulty").orEmpty(),
                            dailyDate = entry.arguments?.getString("date").orEmpty(),
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable("daily_calendar") {
                        DailyCalendarScreen(
                            app = app,
                            onBack = { navController.popBackStack() },
                            onPlayDate = { date ->
                                navController.navigate("game/daily?date=$date")
                            },
                        )
                    }
                    composable("stats") {
                        StatsScreen(app = app, onBack = { navController.popBackStack() })
                    }
                    composable("achievements") {
                        AchievementsScreen(app = app, onBack = { navController.popBackStack() })
                    }
                    composable("settings") {
                        SettingsScreen(app = app, onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
