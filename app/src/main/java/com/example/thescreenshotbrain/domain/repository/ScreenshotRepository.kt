package com.example.thescreenshotbrain.domain.repository

import com.example.thescreenshotbrain.data.local.entity.ScreenshotEntity
import kotlinx.coroutines.flow.Flow

interface ScreenshotRepository{
    suspend fun saveScreenshot(screenshot: ScreenshotEntity)
    fun getAllScreenshots(): Flow<List<ScreenshotEntity>>
    fun searchScreenshots(query: String): Flow<List<ScreenshotEntity>>
}
