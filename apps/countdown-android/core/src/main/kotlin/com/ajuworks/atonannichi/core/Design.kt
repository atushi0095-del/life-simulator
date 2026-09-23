package com.ajuworks.atonannichi.core

/**
 * 表示デザイン。ウィジェットとアプリ内カードで共通。
 * 色は ARGB。[bgTop]/[bgBottom] は縦グラデーション。PHOTO は写真が無ければ SOFT で描く。
 */
enum class Design(
    val bgTop: Int,
    val bgBottom: Int,
    val text: Int,
    val subText: Int,
    val accent: Int,
    /** 数字をどれだけ大きくするか（1 = 標準） */
    val numberScale: Float,
) {
    MINIMAL(0xFFFFFFFF.toInt(), 0xFFF1F3F6.toInt(), 0xFF111827.toInt(), 0xFF6B7280.toInt(), 0xFF111827.toInt(), 1.0f),
    PHOTO(0xFF334155.toInt(), 0xFF0F172A.toInt(), 0xFFFFFFFF.toInt(), 0xE6FFFFFF.toInt(), 0xFFFFFFFF.toInt(), 1.0f),
    DARK(0xFF1F2937.toInt(), 0xFF030712.toInt(), 0xFFF9FAFB.toInt(), 0xFF9CA3AF.toInt(), 0xFFFBBF24.toInt(), 1.0f),
    SOFT(0xFFFDE2E4.toInt(), 0xFFDDEBE7.toInt(), 0xFF3D2C3E.toInt(), 0xFF7A6A7B.toInt(), 0xFFD6336C.toInt(), 1.0f),
    NUMBER(0xFF2563EB.toInt(), 0xFF7C3AED.toInt(), 0xFFFFFFFF.toInt(), 0xCCFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 1.45f),
    ;

    companion object {
        fun of(name: String?): Design = entries.firstOrNull { it.name == name } ?: SOFT
    }
}

/** 選べるアイコン（絵文字）。端末の絵文字フォントで描けるので画像素材が要らない。 */
val ICONS = listOf("✈️", "🏝️", "🎤", "🎂", "🚗", "💍", "🎓", "👶", "🎉", "⛰️", "🎄", "🏠", "⚽", "🎮", "📚", "❤️")
