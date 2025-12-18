package com.example.thescreenshotbrain.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.thescreenshotbrain.data.local.dao.ScreenshotDao
import com.example.thescreenshotbrain.data.local.entity.ScreenshotEntity

@Database(entities = [ScreenshotEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun screenshotDao(): ScreenshotDao
}


