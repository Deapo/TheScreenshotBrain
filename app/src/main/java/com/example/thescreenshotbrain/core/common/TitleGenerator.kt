package com.example.thescreenshotbrain.core.common

import com.example.thescreenshotbrain.data.local.entity.ScreenshotEntity
import java.text.SimpleDateFormat
import java.util.*

object TitleGenerator {
    
    fun generateTitle(
        extractedContent: String,
        type: String,
        rawText: String = ""
    ): String {
        val content = extractedContent.trim()
        
        return when (type) {
            ScreenshotEntity.TYPE_BANK -> {
                val bankName = extractBankName(content)
                if (bankName.isNotEmpty()) {
                    "Ngân hàng $bankName"
                } else if (content.contains("CHỦ TÀI KHOẢN", ignoreCase = true)) {
                    val nameMatch = Regex("CHỦ TÀI KHOẢN[\\s:]+([A-ZÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ\\s]+)", RegexOption.IGNORE_CASE)
                        .find(content)
                    if (nameMatch != null) {
                        "Tài khoản ${nameMatch.groupValues[1].trim()}"
                    } else {
                        "Thông tin ngân hàng"
                    }
                } else {
                    "Thông tin ngân hàng"
                }
            }
            
            ScreenshotEntity.TYPE_URL -> {
                try {
                    val url = if (!content.startsWith("http")) "https://$content" else content
                    val domain = java.net.URL(url).host
                    domain.replace("www.", "").replaceFirstChar { it.uppercase() }
                } catch (e: Exception) {
                    if (content.length > 30) content.take(30) + "..." else content
                }
            }
            
            ScreenshotEntity.TYPE_PHONE -> {
                "Số điện thoại $content"
            }
            
            ScreenshotEntity.TYPE_EVENT -> {
                val dateTimeMatch = Regex("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{1,2}:\\d{2})").find(content)
                if (dateTimeMatch != null) {
                    "Sự kiện ${dateTimeMatch.value}"
                } else {
                    "Sự kiện"
                }
            }
            
            ScreenshotEntity.TYPE_MAP -> {
                val address = content.split(",").firstOrNull()?.trim() ?: content
                if (address.length > 40) {
                    address.take(40) + "..."
                } else {
                    address
                }
            }
            
            ScreenshotEntity.TYPE_NOTE -> {
                val firstLine = content.lines().firstOrNull()?.trim() ?: content
                if (firstLine.length > 50) {
                    firstLine.take(50) + "..."
                } else if (firstLine.isNotEmpty()) {
                    firstLine
                } else {
                    "Ghi chú"
                }
            }
            
            else -> {
                val firstLine = content.lines().firstOrNull()?.trim() ?: content
                if (firstLine.length > 30) {
                    firstLine.take(30) + "..."
                } else if (firstLine.isNotEmpty()) {
                    firstLine
                } else {
                    "Screenshot ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}"
                }
            }
        }
    }
    
    private fun extractBankName(content: String): String {
        val bankNames = listOf(
            "Vietcombank", "VCB",
            "BIDV",
            "Agribank",
            "Techcombank", "TCB",
            "Vietinbank", "CTG",
            "ACB",
            "TPBank", "TPB",
            "MB Bank", "MBBank", "MB",
            "VPBank", "VP"
        )
        
        val contentUpper = content.uppercase()
        bankNames.forEach { bankName ->
            if (contentUpper.contains(bankName.uppercase())) {
                return bankName
            }
        }
        
        return ""
    }
}

