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

    //Chen screenshot moi
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(screenshot: ScreenshotEntity)

    //Lay toan bo danh sach
    @Query("SELECT * FROM screenshot ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<ScreenshotEntity>>

    //Tim kiem noi dung cua Text
    @Query("SELECT * FROM screenshot " +
            "       WHERE rawText LIKE '%'|| :query || '%'" +
            "       ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<ScreenshotEntity>>

    @Delete
    suspend fun delete(screenshot: ScreenshotEntity)
}
