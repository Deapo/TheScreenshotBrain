package com.example.thescreenshotbrain.di

import android.app.Application
import androidx.room.Room
import com.example.thescreenshotbrain.data.local.dao.ScreenshotDao
import com.example.thescreenshotbrain.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(application: Application): AppDatabase{
        return Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "screenshot_brain_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideScreenshotDao(database: AppDatabase): ScreenshotDao{
        return database.screenshotDao()
    }

}

