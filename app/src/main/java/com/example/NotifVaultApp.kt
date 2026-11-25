package com.example

import android.app.Application
import com.example.data.local.NotifVaultDatabase
import com.example.data.local.PreferencesManager
import com.example.data.repository.InstalledAppsProvider
import com.example.data.repository.NotificationRepository

class AppContainer(application: Application) {
    val database: NotifVaultDatabase by lazy {
        NotifVaultDatabase.getInstance(application)
    }
    val notificationRepository: NotificationRepository by lazy {
        NotificationRepository(database.notificationDao())
    }
    val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(application)
    }
    val installedAppsProvider: InstalledAppsProvider by lazy {
        InstalledAppsProvider(application)
    }
}

class NotifVaultApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
