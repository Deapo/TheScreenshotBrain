package com.example.thescreenshotbrain.core.common

import com.example.thescreenshotbrain.data.local.entity.ScreenshotEntity

object DataAnalyzer {
    // Regex url
    private val URL_REGEX = "https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)".toRegex()

    // Regex số điện thoại Việt Nam
    private val PHONE_REGEX = "(03|05|07|08|09)+([0-9]{8})\\b".toRegex()

    // Từ khóa ngân hàng
    private val BANK_KEYWORDS = listOf("stk", "số tài khoản", "vietcombank", "mbbank", "techcombank", "chuyen khoan", "so du")

    fun analyzeType (text: String): String{
        val normalized = text.lowercase()

        return when{
            URL_REGEX.containsMatchIn(normalized) -> ScreenshotEntity.TYPE_URL
            PHONE_REGEX.containsMatchIn(normalized) -> ScreenshotEntity.TYPE_PHONE
            BANK_KEYWORDS.any { it in normalized } -> ScreenshotEntity.TYPE_BANK
            else -> ScreenshotEntity.TYPE_OTHER
        }
    }
}