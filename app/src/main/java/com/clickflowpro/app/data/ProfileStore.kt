package com.clickflowpro.app.data

import android.content.Context
import com.clickflowpro.app.model.ClickPoint
import com.clickflowpro.app.model.ClickProfile
import com.clickflowpro.app.model.MarkerShape
import com.clickflowpro.app.model.MarkerStyle
import com.clickflowpro.app.model.defaultProfiles
import org.json.JSONArray
import org.json.JSONObject

class ProfileStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadProfiles(): List<ClickProfile> {
        val raw = preferences.getString(PROFILES_KEY, null) ?: return defaultProfiles
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val points = buildList {
                        val pointsArray = item.optJSONArray("points") ?: JSONArray()
                        for (j in 0 until pointsArray.length()) {
                            val point = pointsArray.getJSONObject(j)
                            add(
                                ClickPoint(
                                    id = point.optString("id"),
                                    x = point.optInt("x"),
                                    y = point.optInt("y"),
                                    label = point.optString("label", "Point"),
                                ),
                            )
                        }
                    }
                    add(
                        ClickProfile(
                            id = item.optString("id"),
                            name = item.optString("name", "Profile"),
                            cps = item.optDouble("cps", 8.0).toFloat(),
                            intervalMs = item.optLong("intervalMs", 125L),
                            points = points.ifEmpty { listOf(ClickPoint()) },
                        ),
                    )
                }
            }.ifEmpty { defaultProfiles }
        }.getOrElse { defaultProfiles }
    }

    fun saveProfiles(profiles: List<ClickProfile>) {
        val array = JSONArray()
        profiles.forEach { profile ->
            val points = JSONArray()
            profile.points.forEach { point ->
                points.put(
                    JSONObject()
                        .put("id", point.id)
                        .put("x", point.x)
                        .put("y", point.y)
                        .put("label", point.label),
                )
            }
            array.put(
                JSONObject()
                    .put("id", profile.id)
                    .put("name", profile.name)
                    .put("cps", profile.cps)
                    .put("intervalMs", profile.intervalMs)
                    .put("points", points),
            )
        }
        preferences.edit().putString(PROFILES_KEY, array.toString()).apply()
    }

    fun isDarkTheme(): Boolean = preferences.getBoolean(DARK_THEME_KEY, false)

    fun setDarkTheme(dark: Boolean) {
        preferences.edit().putBoolean(DARK_THEME_KEY, dark).apply()
    }

    /** Ekrandaki tiklama isaretleyicisinin (pulse) sekli ve rengi. */
    fun loadMarkerStyle(): MarkerStyle {
        val shapeName = preferences.getString(MARKER_SHAPE_KEY, null)
        val shape = shapeName?.let { name ->
            runCatching { MarkerShape.valueOf(name) }.getOrNull()
        } ?: MarkerShape.RING
        val color = preferences.getInt(MARKER_COLOR_KEY, MarkerStyle.DEFAULT_COLOR)
        return MarkerStyle(shape = shape, colorArgb = color)
    }

    fun saveMarkerStyle(style: MarkerStyle) {
        preferences.edit()
            .putString(MARKER_SHAPE_KEY, style.shape.name)
            .putInt(MARKER_COLOR_KEY, style.colorArgb)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "tapflow_preferences"
        private const val PROFILES_KEY = "profiles"
        private const val DARK_THEME_KEY = "dark_theme"
        private const val MARKER_SHAPE_KEY = "marker_shape"
        private const val MARKER_COLOR_KEY = "marker_color"
    }
}