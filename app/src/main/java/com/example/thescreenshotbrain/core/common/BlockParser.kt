package com.example.thescreenshotbrain.core.common

import android.util.Patterns
import com.example.thescreenshotbrain.data.local.entity.ScreenshotEntity

object BlockParser {
    
    data class TextBlock(
        val type: BlockType,
        val content: String,
        val metadata: Map<String, String> = emptyMap()
    )

    enum class BlockType {
        QR_CODE,
        MAP_LOCATION,
        PHONE_NUMBER,
        URL_LINK,
        TEXT,
        BANK_INFO
    }
    
    private val PHONE_REGEX = Regex("(03|05|07|08|09)[0-9]{8}\\b")
    private val URL_PATTERN = Patterns.WEB_URL

    fun parseBlocks(
        rawText: String,
        extractedText: String,
        type: String,
        qrContent: String? = null
    ): List<TextBlock> {
        val blocks = mutableListOf<TextBlock>()
        
        if (!qrContent.isNullOrBlank()) {
            blocks.add(
                TextBlock(
                    type = BlockType.QR_CODE,
                    content = qrContent,
                    metadata = mapOf("format" to "VIETQR")
                )
            )
        }
        
        if (type == ScreenshotEntity.TYPE_BANK && extractedText.isNotBlank()) {
            blocks.add(
                TextBlock(
                    type = BlockType.BANK_INFO,
                    content = extractedText,
                    metadata = extractBankMetadata(extractedText)
                )
            )
        }
        
        val urlMatcher = URL_PATTERN.matcher(rawText)
        while (urlMatcher.find()) {
            val url = urlMatcher.group()
            blocks.add(
                TextBlock(
                    type = BlockType.URL_LINK,
                    content = url,
                    metadata = mapOf("domain" to extractDomain(url))
                )
            )
        }
        
        PHONE_REGEX.findAll(rawText).forEach { match ->
            blocks.add(
                TextBlock(
                    type = BlockType.PHONE_NUMBER,
                    content = match.value,
                    metadata = emptyMap()
                )
            )
        }
        
        val addressPattern = Regex(
            "\\d+\\s+[A-ZÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ\\s,]+" +
            "(?:Đường|Phường|Quận|Huyện|Tỉnh|Thành phố|Việt Nam|VN)",
            RegexOption.IGNORE_CASE
        )
        
        val addressMatches = addressPattern.findAll(rawText)
        addressMatches.forEach { match ->
            val address = match.value.trim()
            if (address.length > 20 && 
                (address.contains("Đường", ignoreCase = true) ||
                 address.contains("Phường", ignoreCase = true) ||
                 address.contains("Quận", ignoreCase = true) ||
                 address.contains("Tỉnh", ignoreCase = true) ||
                 address.contains("Thành phố", ignoreCase = true) ||
                 address.contains("Việt Nam", ignoreCase = true))) {
                blocks.add(
                    TextBlock(
                        type = BlockType.MAP_LOCATION,
                        content = address,
                        metadata = emptyMap()
                    )
                )
            }
        }
        
        if (type == ScreenshotEntity.TYPE_MAP && extractedText.isNotBlank()) {
            val alreadyAdded = blocks.any { 
                it.type == BlockType.MAP_LOCATION && 
                it.content.contains(extractedText, ignoreCase = true)
            }
            if (!alreadyAdded) {
                blocks.add(
                    TextBlock(
                        type = BlockType.MAP_LOCATION,
                        content = extractedText,
                        metadata = emptyMap()
                    )
                )
            }
        }
        
        val usedContent = blocks.map { it.content }.joinToString("|")
        val remainingText = rawText
            .split(Regex("\\|"))
            .filter { it.isNotBlank() && !usedContent.contains(it.trim(), ignoreCase = true) }
            .joinToString("\n")
            .trim()
        
        if (remainingText.isNotBlank() && remainingText.length > 10) {
            blocks.add(
                TextBlock(
                    type = BlockType.TEXT,
                    content = remainingText,
                    metadata = emptyMap()
                )
            )
        }
        
        if (blocks.isEmpty() && extractedText.isNotBlank()) {
            blocks.add(
                TextBlock(
                    type = BlockType.TEXT,
                    content = extractedText,
                    metadata = emptyMap()
                )
            )
        }
        
        if (extractedText.isNotBlank()) {
            val extractedBlockIndex = blocks.indexOfFirst { 
                it.content.contains(extractedText, ignoreCase = true) || 
                extractedText.contains(it.content, ignoreCase = true)
            }
            
            if (extractedBlockIndex > 0) {
                val extractedBlock = blocks.removeAt(extractedBlockIndex)
                blocks.add(0, extractedBlock)
            } else if (extractedBlockIndex == -1) {
                val extractedBlockType = when (type) {
                    ScreenshotEntity.TYPE_MAP -> BlockType.MAP_LOCATION
                    ScreenshotEntity.TYPE_EVENT -> BlockType.TEXT
                    ScreenshotEntity.TYPE_BANK -> BlockType.BANK_INFO
                    ScreenshotEntity.TYPE_URL -> BlockType.URL_LINK
                    ScreenshotEntity.TYPE_PHONE -> BlockType.PHONE_NUMBER
                    else -> BlockType.TEXT
                }
                
                blocks.add(0, TextBlock(
                    type = extractedBlockType,
                    content = extractedText,
                    metadata = emptyMap()
                ))
            }
        }
        
        return blocks
    }
    
    private fun extractBankMetadata(content: String): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        
        val stkMatch = Regex("STK[\\s:]+(\\d+)", RegexOption.IGNORE_CASE).find(content)
        stkMatch?.let { metadata["accountNumber"] = it.groupValues[1] }
        
        val bankNames = listOf("Vietcombank", "BIDV", "Agribank", "Techcombank", "Vietinbank", "ACB", "TPBank", "MB", "VPBank")
        bankNames.forEach { bankName ->
            if (content.contains(bankName, ignoreCase = true)) {
                metadata["bankName"] = bankName
            }
        }
        
        val amountMatch = Regex("(\\d{1,3}(?:[.,]\\d{3})*)\\s*(VND|đ|dong)", RegexOption.IGNORE_CASE).find(content)
        amountMatch?.let { metadata["amount"] = it.groupValues[1] }
        
        return metadata
    }
    
    private fun extractDomain(url: String): String {
        return try {
            val urlStr = if (!url.startsWith("http")) "https://$url" else url
            java.net.URL(urlStr).host.replace("www.", "")
        } catch (e: Exception) {
            url.take(30)
        }
    }
}
