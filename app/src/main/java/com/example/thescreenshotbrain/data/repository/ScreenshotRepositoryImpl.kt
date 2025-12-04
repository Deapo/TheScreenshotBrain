package com.example.thescreenshotbrain.data.repository

import com.example.thescreenshotbrain.data.local.dao.ScreenshotDao
import com.example.thescreenshotbrain.data.local.entity.ScreenshotEntity
import com.example.thescreenshotbrain.domain.repository.ScreenshotRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScreenshotRepositoryImpl @Inject constructor(
    private val screenshotDao: ScreenshotDao
) : ScreenshotRepository {

    override suspend fun saveScreenshot(screenshot: ScreenshotEntity){
        screenshotDao.insert(screenshot)
    }

    override fun getAllScreenshots(): Flow<List<ScreenshotEntity>>{
        return screenshotDao.getAllFlow()
    }

    override fun searchScreenshots(query: String): Flow<List<ScreenshotEntity>>{
        return screenshotDao.search(query)
    }
}
