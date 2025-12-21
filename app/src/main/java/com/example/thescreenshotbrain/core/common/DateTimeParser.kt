package com.example.thescreenshotbrain.core.common

import java.util.Calendar

object DateTimeParser {
    
    fun parseDateTime(text: String): Calendar? {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH)
        val currentDay = now.get(Calendar.DAY_OF_MONTH)
        
        try {
            val pattern1 = Regex("(\\d{1,2})h(\\d{0,2})\\s+(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?")
            val match1 = pattern1.find(text)
            if (match1 != null) {
                val hour = match1.groupValues[1].toInt()
                val minute = match1.groupValues[2].toIntOrNull() ?: 0
                val day = match1.groupValues[3].toInt()
                val month = match1.groupValues[4].toInt() - 1
                val year = match1.groupValues[5].toIntOrNull() ?: currentYear
                
                val calendar = Calendar.getInstance()
                calendar.set(year, month, day, hour, minute, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                
                if (match1.groupValues[5].isEmpty()) {
                    val dateOnly = Calendar.getInstance().apply {
                        set(year, month, day, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val nowDateOnly = Calendar.getInstance().apply {
                        set(currentYear, currentMonth, currentDay, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    
                    if (dateOnly.before(nowDateOnly)) {
                        calendar.add(Calendar.YEAR, 1)
                    }
                }
                
                return calendar
            }
            
            val pattern2 = Regex("TH(Ứ)?\\s*\\d+\\s+LÚC\\s+(\\d{1,2}):(\\d{2})")
            val match2 = pattern2.find(text.uppercase())
            if (match2 != null) {
                val hour = match2.groupValues[2].toInt()
                val minute = match2.groupValues[3].toInt()
                
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                
                if (calendar.before(now)) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                
                return calendar
            }
            
            val pattern3 = Regex("(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?\\s+(\\d{1,2}):(\\d{2})")
            val match3 = pattern3.find(text)
            if (match3 != null) {
                val day = match3.groupValues[1].toInt()
                val month = match3.groupValues[2].toInt() - 1
                val year = match3.groupValues[3].toIntOrNull() ?: currentYear
                val hour = match3.groupValues[4].toInt()
                val minute = match3.groupValues[5].toInt()
                
                val calendar = Calendar.getInstance()
                calendar.set(year, month, day, hour, minute, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                if (calendar.before(now) && match3.groupValues[3].isEmpty()) {
                    calendar.add(Calendar.YEAR, 1)
                }
                
                return calendar
            }
            
            val pattern4 = Regex("(\\d{1,2}):(\\d{2})\\s+(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?")
            val match4 = pattern4.find(text)
            if (match4 != null) {
                val hour = match4.groupValues[1].toInt()
                val minute = match4.groupValues[2].toInt()
                val day = match4.groupValues[3].toInt()
                val month = match4.groupValues[4].toInt() - 1
                val year = match4.groupValues[5].toIntOrNull() ?: currentYear
                
                val calendar = Calendar.getInstance()
                calendar.set(year, month, day, hour, minute, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                if (calendar.before(now) && match4.groupValues[5].isEmpty()) {
                    calendar.add(Calendar.YEAR, 1)
                }
                
                return calendar
            }
            
            val pattern5 = Regex("(\\d{1,2})h(\\d{0,2})|(\\d{1,2}):(\\d{2})")
            val match5 = pattern5.find(text)
            if (match5 != null) {
                val hour: Int
                val minute: Int
                
                if (match5.groupValues[1].isNotEmpty()) {
                    hour = match5.groupValues[1].toInt()
                    minute = match5.groupValues[2].toIntOrNull() ?: 0
                } else {
                    hour = match5.groupValues[3].toInt()
                    minute = match5.groupValues[4].toInt()
                }
                
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                
                if (calendar.before(now)) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                
                return calendar
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return null
    }

    fun findAndParseDateTime(text: String): Calendar? {
        val priorityPattern = Regex("(\\d{1,2})h(\\d{0,2})\\s+(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?")
        val priorityMatch = priorityPattern.find(text)
        if (priorityMatch != null) {
            val matchedText = priorityMatch.value
            val calendar = parseDateTime(matchedText)
            if (calendar != null) {
                return calendar
            }
        }
        
        val lines = text.lines()
        
        for (line in lines) {
            val calendar = parseDateTime(line.trim())
            if (calendar != null) {
                return calendar
            }
        }
        
        return parseDateTime(text.trim())
    }
}

