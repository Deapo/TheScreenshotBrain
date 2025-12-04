package com.example.thescreenshotbrain.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screenshot")
data class ScreenshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uri: String,
    val rawText: String,
    val type: String,
    val timestamp: Long
){
    companion object{
        const val TYPE_URL = "TYPE_URL"
        const val TYPE_PHONE = "TYPE_PHONE"
        const val TYPE_BANK = "TYPE_BANK"
        const val TYPE_OTHER = "TYPE_OTHER"
    }
}