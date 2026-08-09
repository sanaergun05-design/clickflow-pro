package com.clickflowpro.app

import android.app.Application
import android.content.Context
import com.clickflowpro.app.util.LocaleHelper

/**
 * Uygulama genelinde seçilen dilin (LocaleHelper) tüm bileşenlere -
 * Activity'ler ve AccessibilityService dahil - uygulanmasını sağlar.
 */
class ClickFlowApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }
}
