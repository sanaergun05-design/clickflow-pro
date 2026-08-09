package com.clickflowpro.app.model

import androidx.compose.runtime.Immutable

@Immutable
data class ClickProfile(
    val id: String,
    val name: String,
    val cps: Float = 8f,
    val intervalMs: Long = 125L,
    val points: List<ClickPoint> = listOf(ClickPoint()),
)

val defaultProfiles = listOf(
    ClickProfile(
        id = "quick-tap",
        name = "Quick Tap",
        cps = 8f,
        intervalMs = 125L,
        points = listOf(ClickPoint(x = 540, y = 960, label = "Center")),
    ),
)