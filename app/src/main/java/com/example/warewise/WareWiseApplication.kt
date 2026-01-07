package com.example.warewise

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WareWiseApplication : Application() {
    companion object {
        const val PREFS_NAME = "WareWisePrefs" // The ONE file name everyone must use
        const val KEY_THEME = "app_theme_mode" // The ONE key
        const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
}

