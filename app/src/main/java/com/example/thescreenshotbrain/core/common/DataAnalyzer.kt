package com.example.thescreenshotbrain.core.common

import android.util.Log
import android.util.Patterns
import com.example.thescreenshotbrain.data.local.entity.ScreenshotEntity
import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityAnnotation
import com.google.mlkit.vision.text.Text

data class AnalysisResult(
    val type: String,
    val extractedContent: String,
    val extraData: Long? = null
)

object DataAnalyzer {
    private val PHONE_REGEX = Regex("(03|05|07|08|09)[0-9]{8}\\b")
    private val BANK_KEYWORDS = listOf("stk", "số tài khoản", "vietcombank", "mbbank", "techcombank", "chuyen khoan")

    fun analyze(visionText: Text, qrContent: String?, entities: List<Entity>): AnalysisResult {

        // 1. XỬ LÝ BANK / QR
        if (!qrContent.isNullOrBlank()) {
            // Bước A: Lấy thông tin từ QR
            var finalInfo = VietQrParser.extractBankInfo(qrContent)

            // Bước B: Nếu QR thiếu tên (chứa chữ mặc định), dùng OCR để tìm tên bù vào
            if (finalInfo != null && finalInfo.contains("CHỦ TÀI KHOẢN")) {
                val nameFromOcr = findNameFromOcr(visionText)
                if (nameFromOcr.isNotEmpty()) {
                    // Thay thế chữ "CHỦ TÀI KHOẢN" bằng tên tìm được từ OCR
                    finalInfo = finalInfo.replace("CHỦ TÀI KHOẢN", nameFromOcr)
                }
            }

            if (finalInfo != null) {
                return AnalysisResult(ScreenshotEntity.TYPE_BANK, finalInfo)
            }
        }

        // ... (Giữ nguyên logic URL, Phone cũ ở đây) ...
        val rawText = visionText.text

        // 2. URL
        val urlMatcher = Patterns.WEB_URL.matcher(rawText)
        if (urlMatcher.find()) {
            return AnalysisResult(ScreenshotEntity.TYPE_URL, urlMatcher.group())
        }

        // 3. Phone
        val phoneMatch = PHONE_REGEX.find(rawText)
        if (phoneMatch != null) {
            return AnalysisResult(ScreenshotEntity.TYPE_PHONE, phoneMatch.value)
        }

        //calendar
        for (entity in entities) {
            when (entity.type) {
                // A. Phát hiện Ngày/Giờ -> TYPE_EVENT
                Entity.TYPE_DATE_TIME -> {
                    val dateTimeEntity = entity.asDateTimeEntity()
                    // Lấy mốc thời gian (Millis) nếu có
                    val timestamp = dateTimeEntity?.timestampMillis

                    return AnalysisResult(
                        type = ScreenshotEntity.TYPE_EVENT,
                        extractedContent = rawText, // Lưu nội dung gốc để user đọc
                        extraData = timestamp
                    )
                }

                // B. Phát hiện Địa chỉ -> TYPE_MAP
                Entity.TYPE_ADDRESS -> {
                    // Trích xuất đúng cái đoạn địa chỉ (VD: "123 Ly Thuong Kiet")
                    val addressText = rawText.substring(entity.start, entity.end)

                    return AnalysisResult(
                        type = ScreenshotEntity.TYPE_MAP,
                        extractedContent = addressText // Lưu địa chỉ để search Map
                    )
                }
            }
        }

        // 4. Note (Logic Google Keep cũ)
        if (rawText.length > 50) {
            return AnalysisResult(ScreenshotEntity.TYPE_NOTE, rawText)
        }

        return AnalysisResult(ScreenshotEntity.TYPE_OTHER, rawText.trim())
    }

    /**
     * HÀM HEURISTIC: Tìm tên người trong mớ văn bản OCR
     * Logic: Tìm dòng nào VIẾT HOA HẾT, không có số, và không phải từ khóa rác.
     */
    private fun findNameFromOcr(visionText: Text): String {
        val ignoredWords = listOf("VIETQR", "NAPAS", "BANK", "CHUYEN", "TIEN", "QR", "SCAN", "QUET", "MA")

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val text = line.text.trim()

                // Điều kiện lọc tên người:
                // 1. Dài hơn 3 ký tự
                // 2. Viết hoa toàn bộ (NGUYEN VAN A) -> Kiểm tra bằng cách so sánh với chính nó khi uppercase
                // 3. Không chứa số (Tên người không có số)
                // 4. Không chứa từ khóa rác (VietQR, Napas...)

                if (text.length > 4 &&
                    text == text.uppercase() &&
                    !text.any { it.isDigit() } &&
                    ignoredWords.none { text.contains(it, ignoreCase = true) }
                ) {
                    return text // Tìm thấy ứng viên sáng giá nhất -> Trả về luôn
                }
            }
        }
        return "" // Không tìm thấy
    }
}