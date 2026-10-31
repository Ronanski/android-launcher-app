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
    lateinit var widgets: WidgetManager
        private set

    override fun onCreate() {
        super.onCreate()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            try {
                org.lsposed.hiddenapibypass.HiddenApiBypass.addHiddenApiExemptions("L")
            } catch (e: Throwable) {
            }
        }
        settings = LauncherSettings(this)
        repo = AppRepository(this, settings)
        updater = Updater(this, settings)
        widgets = WidgetManager(this)
    }
}

val Context.launcherApp: LauncherApp
    get() = applicationContext as LauncherApp
