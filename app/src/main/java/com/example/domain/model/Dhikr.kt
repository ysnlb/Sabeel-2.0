package com.example.domain.model

data class DhikrItem(
    val id: String,
    val category: DhikrCategory,
    val arabicText: String,
    val translation: String,
    val targetCount: Int,
    val currentCount: Int = 0
)

enum class DhikrCategory {
    MORNING, EVENING, AFTER_PRAYER
}
