package com.example.thescreenshotbrain.core.common

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

    fun analyze(visionText: Text, qrContent: String?, annotations: List<EntityAnnotation>): AnalysisResult {
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

        val cleanedText = TextFilter.cleanText(visionText.text)
        val rawText = visionText.text

        val urlMatcher = Patterns.WEB_URL.matcher(rawText)
        if (urlMatcher.find()) {
            return AnalysisResult(ScreenshotEntity.TYPE_URL, urlMatcher.group())
        }

        val phoneMatch = PHONE_REGEX.find(rawText)
        if (phoneMatch != null) {
            return AnalysisResult(ScreenshotEntity.TYPE_PHONE, phoneMatch.value)
        }
        
        val timePatterns = listOf(
            Regex("\\d{1,2}h\\s+\\d{1,2}/\\d{1,2}"),
            Regex("TH\\s*\\d+\\s+LÚC\\s+\\d{1,2}:\\d{2}", RegexOption.IGNORE_CASE),
            Regex("\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?\\s+\\d{1,2}:\\d{2}"),
            Regex("\\d{1,2}:\\d{2}\\s+\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?"),
        )
        
        for (pattern in timePatterns) {
            val timeMatch = pattern.find(rawText)
            if (timeMatch != null) {
                val timeText = timeMatch.value.trim()
                if (timeText.length > 5 && !PHONE_REGEX.containsMatchIn(timeText)) {
                    return AnalysisResult(
                        type = ScreenshotEntity.TYPE_EVENT,
                        extractedContent = timeText,
                        extraData = null
                    )
                }
            }
        }
        
        val addressPattern = Regex(
            "\\d+\\s+[A-ZÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ\\s,]+" +
            "(?:Đường|Phường|Quận|Huyện|Tỉnh|Thành phố|Việt Nam|VN)",
            RegexOption.IGNORE_CASE
        )
        val addressMatch = addressPattern.find(rawText)
        if (addressMatch != null) {
            val address = addressMatch.value.trim()
            if (address.length > 20 && 
                (address.contains("Đường", ignoreCase = true) ||
                 address.contains("Phường", ignoreCase = true) ||
                 address.contains("Quận", ignoreCase = true) ||
                 address.contains("Tỉnh", ignoreCase = true) ||
                 address.contains("Thành phố", ignoreCase = true) ||
                 address.contains("Việt Nam", ignoreCase = true))) {
                return AnalysisResult(ScreenshotEntity.TYPE_MAP, address)
            }
        }

        for (annotation in annotations) {
            val entities = annotation.entities

            for (entity in entities) {
                when (entity.type) {
                    Entity.TYPE_DATE_TIME -> {
                        val dateTimeEntity = entity.asDateTimeEntity()
                        val timestamp = dateTimeEntity?.timestampMillis

                        if (annotation.start < rawText.length && annotation.end <= rawText.length) {
                            val eventText = rawText.substring(annotation.start, annotation.end)

                            return AnalysisResult(
                                type = ScreenshotEntity.TYPE_EVENT,
                                extractedContent = eventText,
                                extraData = timestamp
                            )
                        }
                    }

                    Entity.TYPE_ADDRESS -> {
                        if (annotation.start < rawText.length && annotation.end <= rawText.length) {
                            val addressText = rawText.substring(annotation.start, annotation.end)

                            return AnalysisResult(
                                type = ScreenshotEntity.TYPE_MAP,
                                extractedContent = addressText
                            )
                        }
                    }
                }
            }
        }

        val determinedType = ScoringAlgorithm.determineType(cleanedText, qrContent)
        
        val finalContent = when (determinedType) {
            ScreenshotEntity.TYPE_MAP -> {
                addressMatch?.value?.trim() ?: cleanedText
            }
            ScreenshotEntity.TYPE_NOTE -> {
                cleanedText
            }
            else -> cleanedText
        }
        
        if (determinedType == ScreenshotEntity.TYPE_NOTE && cleanedText.length > 50) {
            return AnalysisResult(ScreenshotEntity.TYPE_NOTE, finalContent)
        }

        return AnalysisResult(determinedType, finalContent.trim())
    }


    private fun findNameFromOcr(visionText: Text): String {
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