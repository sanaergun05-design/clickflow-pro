package com.clickflowpro.app.model

import androidx.compose.runtime.Immutable
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Immutable
@Parcelize
data class ClickPoint(
    val id: String = UUID.randomUUID().toString(),
    val x: Int = 540,
    val y: Int = 960,
    val label: String = "Point",
) : Parcelable