package com.clickflowpro.app.util

import android.content.Context

/** Uygulamanın ilk açılış izin ekranının daha önce gösterilip gösterilmediğini tutar. */
object OnboardingPrefs {
    private const val PREFS_NAME = "tikclick_onboarding_prefs"
    private const val KEY_DONE = "onboarding_done"

    fun isOnboardingDone(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DONE, false)

    fun setOnboardingDone(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DONE, true)
            .apply()
    }
}
