package com.example.thescreenshotbrain.core.common

import android.util.Patterns
import com.example.thescreenshotbrain.data.local.entity.ScreenshotEntity
import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityAnnotation // Nhớ import cái này
import com.google.mlkit.vision.text.Text

data class AnalysisResult(
    val type: String,
    val extractedContent: String,
    val extraData: Long? = null
)

object DataAnalyzer {
    private val PHONE_REGEX = Regex("(03|05|07|08|09)[0-9]{8}\\b")

    // SỬA 1: Đổi tham số đầu vào từ List<Entity> thành List<EntityAnnotation>
    fun analyze(visionText: Text, qrContent: String?, annotations: List<EntityAnnotation>): AnalysisResult {

        // 1. XỬ LÝ BANK / QR (Giữ nguyên)
        if (!qrContent.isNullOrBlank()) {
            var finalInfo = VietQrParser.extractBankInfo(qrContent)
            if (finalInfo != null && finalInfo.contains("CHỦ TÀI KHOẢN")) {
                val nameFromOcr = findNameFromOcr(visionText)
                if (nameFromOcr.isNotEmpty()) {
                    finalInfo = finalInfo.replace("CHỦ TÀI KHOẢN", nameFromOcr)
                }
            }
            if (finalInfo != null) {
                return AnalysisResult(ScreenshotEntity.TYPE_BANK, finalInfo)
            }
        }

        val rawText = visionText.text

        // 2. URL (Giữ nguyên)
        val urlMatcher = Patterns.WEB_URL.matcher(rawText)
        if (urlMatcher.find()) {
            return AnalysisResult(ScreenshotEntity.TYPE_URL, urlMatcher.group())
        }

        // 3. Phone (Giữ nguyên)
        val phoneMatch = PHONE_REGEX.find(rawText)
        if (phoneMatch != null) {
            return AnalysisResult(ScreenshotEntity.TYPE_PHONE, phoneMatch.value)
        }

        // 4. EVENT & MAP (SỬA LẠI LOGIC DUYỆT)
        // Duyệt qua từng "Vỏ hộp" (Annotation) để lấy vị trí
        for (annotation in annotations) {
            val entities = annotation.entities // Lấy danh sách "Ruột" bên trong

            for (entity in entities) {
                when (entity.type) {
                    // A. Phát hiện Ngày/Giờ -> TYPE_EVENT
                    Entity.TYPE_DATE_TIME -> {
                        val dateTimeEntity = entity.asDateTimeEntity()
                        val timestamp = dateTimeEntity?.timestampMillis

                        // Lấy chính xác đoạn text mô tả thời gian (VD: "tomorrow at 9pm")
                        // Lưu ý: Có thể dùng annotation.annotatedText hoặc cắt từ rawText
                        val eventText = rawText.substring(annotation.start, annotation.end)

                        return AnalysisResult(
                            type = ScreenshotEntity.TYPE_EVENT,
                            extractedContent = eventText, // Lưu đoạn text chứa ngày giờ
                            extraData = timestamp
                        )
                    }

                    // B. Phát hiện Địa chỉ -> TYPE_MAP
                    Entity.TYPE_ADDRESS -> {
                        // SỬA 2: Lấy start/end từ annotation (Vỏ hộp) -> Hết lỗi Unresolved reference
                        val addressText = rawText.substring(annotation.start, annotation.end)

                        return AnalysisResult(
                            type = ScreenshotEntity.TYPE_MAP,
                            extractedContent = addressText
                        )
                    }
                }
            }
        }

        // 5. Note (Giữ nguyên)
        if (rawText.length > 50) {
            return AnalysisResult(ScreenshotEntity.TYPE_NOTE, rawText)
        }

        return AnalysisResult(ScreenshotEntity.TYPE_OTHER, rawText.trim())
    }

    // Giữ nguyên hàm findNameFromOcr bên dưới...
    private fun findNameFromOcr(visionText: Text): String {
        // ... (Code cũ của bạn)
        val ignoredWords = listOf("VIETQR", "NAPAS", "BANK", "CHUYEN", "TIEN", "QR", "SCAN", "QUET", "MA")
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val text = line.text.trim()
                if (text.length > 4 &&
                    text == text.uppercase() &&
                    !text.any { it.isDigit() } &&
                    ignoredWords.none { text.contains(it, ignoreCase = true) }
                ) {
                    return text
                }
            }
        }
        return ""
    }
}