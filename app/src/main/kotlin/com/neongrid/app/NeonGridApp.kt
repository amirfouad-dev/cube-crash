package com.neongrid.app

import android.app.Application
import com.neongrid.app.di.AppContainer

class NeonGridApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
