package com.memodiary

import android.app.Application
import com.memodiary.di.AppModule

/** Initialises app-level singletons before any Activity or ViewModel is created. */
class MemoDiaryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppModule.initialize(this)
    }
}
