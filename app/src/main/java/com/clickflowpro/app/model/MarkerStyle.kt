package com.clickflowpro.app.model

import androidx.compose.runtime.Immutable

/**
 * Ekranda her tiklama noktasinda beliren kucuk isaretleyicinin (pulse)
 * gorunumu. Kullanici Ayarlar > Isaretleyici ekranindan sekli ve rengini
 * degistirebilir; deger ProfileStore icinde kalici olarak saklanir.
 */
enum class MarkerShape(val label: String) {
    RING("Halka"),
    DOT("Nokta"),
    CROSS("Artı"),
    SQUARE("Kare"),
    DIAMOND("Elmas"),
}

@Immutable
data class MarkerStyle(
    val shape: MarkerShape = MarkerShape.RING,
    val colorArgb: Int = DEFAULT_COLOR,
) {
    companion object {
        const val DEFAULT_COLOR = 0xFF4CD964.toInt() // eski sabit yesil renk

        /** Kullanicinin secebilecegi renk paleti (ayarlar ekraninda gosterilir). */
        val PRESET_COLORS = listOf(
            0xFF4CD964.toInt(), // yesil (varsayilan)
            0xFF5B5FEF.toInt(), // indigo
            0xFF19B6A5.toInt(), // teal
            0xFFFF9F0A.toInt(), // turuncu
            0xFFFF453A.toInt(), // kirmizi
            0xFFFFD60A.toInt(), // sari
            0xFFBF5AF2.toInt(), // mor
            0xFFFFFFFF.toInt(), // beyaz
        )
    }
}
