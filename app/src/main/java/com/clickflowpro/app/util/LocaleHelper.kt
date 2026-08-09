package com.clickflowpro.app.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Uygulama içi dil seçimini (sistem dilinden bağımsız olarak) kalıcı hale
 * getirir ve verilen Context'i seçilen dile göre sarmalar (wrap).
 *
 * Hem MainActivity hem de AutoClickAccessibilityService (ve varsayılan
 * Application) attachBaseContext() içinde bunu çağırarak uygulamanın her
 * tarafında (ekranlar + servis üzerindeki kayan panel) aynı dilin
 * kullanılmasını sağlar.
 */
object LocaleHelper {
    private const val PREFS_NAME = "tikclick_locale_prefs"
    private const val KEY_LANGUAGE = "app_language"

    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_TURKISH = "tr"

    /** Kaydedilmiş dil kodu; hiç seçim yapılmadıysa cihazın sistem diline göre en/tr, aksi halde en. */
    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_LANGUAGE, null)
        if (saved != null) return saved
        val systemLang = Locale.getDefault().language
        return if (systemLang == LANGUAGE_TURKISH) LANGUAGE_TURKISH else LANGUAGE_ENGLISH
    }

    fun setLanguage(context: Context, language: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }

    /** Verilen Context'i kayıtlı dile göre yapılandırılmış yeni bir Context ile sarmalar. */
    fun wrap(context: Context): Context {
        val language = getLanguage(context)
        val locale = Locale(language)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }
}
