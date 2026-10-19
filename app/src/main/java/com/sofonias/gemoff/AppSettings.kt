package com.sofonias.gemoff

import android.content.Context
import android.content.SharedPreferences

enum class AppTheme { SYSTEM, LIGHT, DARK }

object AppSettings {
    private const val PREFS_NAME = "gemoff_settings"
    private const val KEY_THEME = "app_theme"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getTheme(context: Context): AppTheme {
        val name = prefs(context).getString(KEY_THEME, AppTheme.SYSTEM.name)
        return try {
            AppTheme.valueOf(name ?: AppTheme.SYSTEM.name)
        } catch (_: Exception) {
            AppTheme.SYSTEM
        }
    }

    fun setTheme(context: Context, theme: AppTheme) {
        prefs(context).edit().putString(KEY_THEME, theme.name).apply()
    }
}
