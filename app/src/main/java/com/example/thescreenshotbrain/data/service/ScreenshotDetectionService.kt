package com.example.thescreenshotbrain.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.thescreenshotbrain.R
import com.example.thescreenshotbrain.core.common.DataAnalyzer
import com.example.thescreenshotbrain.domain.repository.ScreenshotRepository
import com.example.thescreenshotbrain.domain.usecase.TextRecognitionHelper
import com.example.thescreenshotbrain.data.local.entity.ScreenshotEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScreenshotDetectionService: Service(){
    @Inject lateinit var repository: ScreenshotRepository

    //Coroutine scope for service, run on IO thread
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    //Listen the change of screenshot
    private lateinit var contentObserver: ContentObserver

    //OCR
    private lateinit var ocrHelper: TextRecognitionHelper

    override fun onCreate(){
        super.onCreate()
        startForegroundService()
        ocrHelper = TextRecognitionHelper(this)
        registerScreenshotObserver()
    }

    private fun registerScreenshotObserver() {
        TODO("Not yet implemented")
    }

    private fun startForegroundService() {
        val channel_ID = "screenshot_service_channel"
        val channel = NotificationChannel(
            channel_ID,
            "Screenshot Monitor",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, channel_ID)
            .setContentTitle("The Screenshot Brain")
            .setContentText("Đang lắng nghe chụp ảnh...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy(){
        super.onDestroy()
        contentResolver.unregisterContentObserver(contentObserver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

}