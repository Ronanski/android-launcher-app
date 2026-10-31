package com.ronanski.launcher

import android.app.Application
import android.content.Context

class LauncherApp : Application() {
    lateinit var settings: LauncherSettings
        private set
    lateinit var repo: AppRepository
        private set
    lateinit var updater: Updater
        private set

    override fun onCreate() {
        super.onCreate()
        settings = LauncherSettings(this)
        repo = AppRepository(this, settings)
        updater = Updater(this, settings)
    }
}

val Context.launcherApp: LauncherApp
    get() = applicationContext as LauncherApp
