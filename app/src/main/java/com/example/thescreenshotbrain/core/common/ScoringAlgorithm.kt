package com.example.thescreenshotbrain.core.common

import com.example.thescreenshotbrain.data.local.entity.ScreenshotEntity
import java.util.regex.Pattern

object ScoringAlgorithm {
    
    private val keywordWeights = mapOf(
        ScreenshotEntity.TYPE_BANK to mapOf(
            "ngân hàng" to 10f,
            "bank" to 10f,
            "tài khoản" to 8f,
            "account" to 8f,
            "stk" to 9f,
            "số tài khoản" to 9f,
            "chủ tài khoản" to 8f,
            "vietqr" to 10f,
            "napas" to 9f,
            "chuyển tiền" to 7f,
            "transfer" to 7f,
            "số dư" to 6f,
            "balance" to 6f,
            "vietcombank" to 10f,
            "bidv" to 10f,
            "agribank" to 10f,
            "techcombank" to 10f,
            "vietinbank" to 10f,
            "acb" to 10f,
            "tpb" to 10f,
            "mbbank" to 10f,
            "vpbank" to 10f,
        ),
        ScreenshotEntity.TYPE_URL to mapOf(
            "http" to 10f,
            "https" to 10f,
            "www" to 8f,
            ".com" to 8f,
            ".vn" to 8f,
            ".net" to 7f,
            ".org" to 7f,
            "://" to 10f,
            "facebook" to 5f,
            "youtube" to 5f,
            "instagram" to 5f,
            "twitter" to 5f,
        ),
        ScreenshotEntity.TYPE_PHONE to mapOf(
            "điện thoại" to 8f,
            "phone" to 8f,
            "số điện thoại" to 9f,
            "mobile" to 7f,
            "call" to 7f,
            "gọi" to 8f,
            "liên hệ" to 7f,
            "contact" to 7f,
            "hotline" to 9f,
            "tel" to 8f,
        ),
        ScreenshotEntity.TYPE_EVENT to mapOf(
            "ngày" to 7f,
            "date" to 7f,
            "thời gian" to 8f,
            "time" to 7f,
            "lịch" to 9f,
            "calendar" to 9f,
            "event" to 9f,
            "sự kiện" to 9f,
            "hẹn" to 8f,
            "appointment" to 8f,
            "meeting" to 8f,
            "cuộc họp" to 8f,
            "deadline" to 7f,
            "hạn chót" to 7f,
            "ngày giờ" to 8f,
            "datetime" to 8f,
        ),
        ScreenshotEntity.TYPE_MAP to mapOf(
            "địa chỉ" to 9f,
            "address" to 9f,
            "đường" to 7f,
            "street" to 7f,
            "phường" to 8f,
            "quận" to 8f,
            "huyện" to 8f,
            "tỉnh" to 8f,
            "thành phố" to 8f,
            "city" to 8f,
            "province" to 7f,
            "district" to 7f,
            "ward" to 7f,
            "số nhà" to 8f,
            "location" to 9f,
            "vị trí" to 9f,
            "map" to 9f,
            "bản đồ" to 9f,
            "google map" to 10f,
            "maps" to 9f,
            "gps" to 8f,
            "coordinates" to 7f,
            "tọa độ" to 7f,
        ),
        ScreenshotEntity.TYPE_NOTE to mapOf(
            "ghi chú" to 9f,
            "note" to 9f,
            "memo" to 8f,
            "nhớ" to 7f,
            "lưu ý" to 8f,
            "reminder" to 8f,
            "todo" to 8f,
            "cần làm" to 7f,
            "checklist" to 8f,
            "danh sách" to 7f,
            "list" to 7f,
            "idea" to 6f,
            "ý tưởng" to 6f,
        )
    )
    
    private val PHONE_REGEX = Pattern.compile("(03|05|07|08|09)[0-9]{8}\\b")
    private val URL_PATTERN = android.util.Patterns.WEB_URL
    
    fun scoreText(text: String, type: String): Float {
        val textLower = text.lowercase()
        val weights = keywordWeights[type] ?: return 0f
        
        var totalScore = 0f
        var matchCount = 0
        
        weights.forEach { (keyword, weight) ->
            if (textLower.contains(keyword, ignoreCase = true)) {
                totalScore += weight
                matchCount++
            }
        }
        
        val baseScore = totalScore / weights.size.coerceAtLeast(1)
        val bonus = if (matchCount > 1) matchCount * 0.1f else 0f
        
        return (baseScore + bonus).coerceAtMost(1f)
    }
    
    fun determineType(text: String, qrContent: String? = null): String {
        if (!qrContent.isNullOrBlank()) {
            return ScreenshotEntity.TYPE_BANK
        }
        
        if (URL_PATTERN.matcher(text).find()) {
            return ScreenshotEntity.TYPE_URL
        }
        
        if (PHONE_REGEX.matcher(text).find()) {
            return ScreenshotEntity.TYPE_PHONE
        }
        
        val scores = keywordWeights.keys.associateWith { type ->
            scoreText(text, type)
        }
        
        val bestType = scores.maxByOrNull { it.value }?.key
        
        return when {
            bestType != null && scores[bestType]!! >= 0.1f -> bestType
            text.length > 50 -> ScreenshotEntity.TYPE_NOTE
            else -> ScreenshotEntity.TYPE_OTHER
        }
    }
}

