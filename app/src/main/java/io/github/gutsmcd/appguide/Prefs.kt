package io.github.gutsmcd.appguide

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** Small settings plus the in-app language switch (English default). */
object Prefs {
    const val LANG_EN = "en"
    const val LANG_JA = "ja"
    private fun sp(c: Context) = c.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun lang(c: Context): String = sp(c).getString("lang", LANG_EN) ?: LANG_EN
    fun setLang(c: Context, v: String) = sp(c).edit().putString("lang", v).apply()
    fun sortRecent(c: Context) = sp(c).getBoolean("sort_recent", false)
    fun setSortRecent(c: Context, v: Boolean) = sp(c).edit().putBoolean("sort_recent", v).apply()

    fun localized(c: Context): Context {
        val conf = Configuration(c.resources.configuration)
        conf.setLocale(Locale.forLanguageTag(lang(c)))
        return c.createConfigurationContext(conf)
    }

    fun catLabel(loc: Context, key: String): String = when (key) {
        "health" -> loc.getString(R.string.cat_health)
        "work" -> loc.getString(R.string.cat_work)
        "photo" -> loc.getString(R.string.cat_photo)
        "tools" -> loc.getString(R.string.cat_tools)
        "learning" -> loc.getString(R.string.cat_learning)
        "games" -> loc.getString(R.string.cat_games)
        "travel" -> loc.getString(R.string.cat_travel)
        "money" -> loc.getString(R.string.cat_money)
        "social" -> loc.getString(R.string.cat_social)
        "other" -> loc.getString(R.string.cat_other)
        else -> key
    }
}
