package com.example.thescreenshotbrain.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.thescreenshotbrain.data.local.entity.ScreenshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenshotDao {

    //Insert new screenshot
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(screenshot: ScreenshotEntity)

    //Get all screenshot
    @Query("SELECT * FROM screenshot ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshot WHERE id = :id")
    suspend fun getScreenshotById(id: Long): ScreenshotEntity?

    // Search
    @Query("SELECT * FROM screenshot WHERE extractedText LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<ScreenshotEntity>>

    @Delete
    suspend fun delete(screenshot: ScreenshotEntity)
}
