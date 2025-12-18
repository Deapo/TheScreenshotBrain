package com.example.thescreenshotbrain.di

import com.example.thescreenshotbrain.data.repository.ScreenshotRepositoryImpl
import com.example.thescreenshotbrain.domain.repository.ScreenshotRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule{

    @Binds
    @Singleton
    abstract fun bindScreenshotRepository(
        screenshotRepositoryImpl: ScreenshotRepositoryImpl
    ): ScreenshotRepository

}