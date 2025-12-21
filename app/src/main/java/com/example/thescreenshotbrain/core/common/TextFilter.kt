package com.example.thescreenshotbrain.core.common

object TextFilter {
    
    private val TIME_PATTERNS = listOf(
        Regex("\\d{1,2}:\\d{2}"),
        Regex("\\d{1,2}/\\d{1,2}/\\d{2,4}"),
        Regex("\\d{1,2}-\\d{1,2}-\\d{2,4}"),
    )
    
    private val BATTERY_PATTERNS = listOf(
        Regex("\\d{1,3}%"),
        Regex("Battery.*\\d+%", RegexOption.IGNORE_CASE),
    )
    
    private val SIGNAL_PATTERNS = listOf(
        Regex("\\d+\\s*(dBm|bars?)", RegexOption.IGNORE_CASE),
        Regex("Signal.*\\d+", RegexOption.IGNORE_CASE),
    )
    
    private val NOISE_KEYWORDS = listOf(
        "AM", "PM",
        "Wi-Fi", "Wifi", "WiFi",
        "Bluetooth",
        "GPS", "Location",
        "Do Not Disturb", "DND",
        "Airplane Mode",
        "Silent", "Vibrate",
        "Battery", "Charging",
        "Signal", "No Service",
        "Roaming",
        "VPN",
        "Hotspot",
    )
    
    fun filterNoise(text: String): String {
        var filtered = text
        
        val lines = filtered.lines()
        val importantLines = mutableSetOf<Int>()
        
        lines.forEachIndexed { index, line ->
            val upperLine = line.uppercase().trim()
            if (upperLine.contains("ĐƯỜNG") || upperLine.contains("PHƯỜNG") || 
                upperLine.contains("QUẬN") || upperLine.contains("HUYỆN") ||
                upperLine.contains("TỈNH") || upperLine.contains("THÀNH PHỐ") ||
                upperLine.contains("VIỆT NAM") || upperLine.contains(",") && 
                (upperLine.contains("PHAN THIẾT") || upperLine.contains("BÌNH THUẬN") ||
                 upperLine.contains("BÌNH HƯNG") || upperLine.contains("TUYÊN QUANG"))) {
                importantLines.add(index)
            }
            if ((upperLine.contains("LÚC") || upperLine.contains("AT") || upperLine.contains("VÀO")) &&
                TIME_PATTERNS.any { pattern -> pattern.find(line) != null }) {
                importantLines.add(index)
            }
            if (line.length in 3..30 && !line.any { it.isDigit() } &&
                !NOISE_KEYWORDS.any { upperLine.contains(it.uppercase()) }) {
                importantLines.add(index)
            }
        }
        
        val filteredLines = lines.mapIndexed { index, line ->
            if (importantLines.contains(index)) {
                line
            } else {
                var filteredLine = line
                
                if (!line.contains("LÚC", ignoreCase = true) && 
                    !line.contains("AT", ignoreCase = true) &&
                    !line.contains("VÀO", ignoreCase = true)) {
                    TIME_PATTERNS.forEach { pattern ->
                        filteredLine = filteredLine.replace(pattern, "")
                    }
                }
                
                BATTERY_PATTERNS.forEach { pattern ->
                    filteredLine = filteredLine.replace(pattern, "")
                }
                
                SIGNAL_PATTERNS.forEach { pattern ->
                    filteredLine = filteredLine.replace(pattern, "")
                }
                
                val upperLine = filteredLine.uppercase().trim()
                if (NOISE_KEYWORDS.any { keyword ->
                    upperLine == keyword.uppercase() || 
                    (upperLine.length < 20 && upperLine.contains(keyword.uppercase()))
                }) {
                    ""
                } else {
                    filteredLine
                }
            }
        }.filter { it.isNotBlank() }
        
        filtered = filteredLines.joinToString("\n")
        
        filtered = filtered
            .replace(Regex("[•●○◉◯]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        
        return filtered
    }
    
    fun cleanText(rawText: String): String {
        if (rawText.isBlank()) return ""
        
        var cleaned = filterNoise(rawText)
        
        val lines = cleaned.lines()
        cleaned = lines.filter { line ->
            val trimmed = line.trim()
            trimmed.length >= 3
        }.joinToString("\n")
        
        cleaned = cleaned.replace(Regex("(.)\\1{3,}"), "$1$1$1")
        
        return cleaned.trim()
    }
}

