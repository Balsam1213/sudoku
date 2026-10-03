package com.balsam.sudoku

import android.app.Application
import com.balsam.sudoku.data.AppDatabase
import com.balsam.sudoku.data.SettingsRepository

/** 轻量依赖容器：整个应用离线运行，无任何网络访问。 */
class SudokuApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.get(this)
        settingsRepository = SettingsRepository(this)
    }
}
