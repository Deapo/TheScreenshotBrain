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
import com.example.thescreenshotbrain.core.common.BarcodeHelper
import com.example.thescreenshotbrain.core.common.DataAnalyzer
import com.example.thescreenshotbrain.core.common.EntityExtractionHelper
import com.example.thescreenshotbrain.data.local.entity.ScreenshotEntity
import com.example.thescreenshotbrain.domain.repository.ScreenshotRepository
import com.example.thescreenshotbrain.domain.usecase.TextRecognitionHelper
import com.example.thescreenshotbrain.presentation.overlay.FloatingBubbleManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class ScreenshotDetectionService : Service() {

    @Inject lateinit var repository: ScreenshotRepository
    @Inject lateinit var overlayManager: FloatingBubbleManager
    @Inject lateinit var barcodeHelper: BarcodeHelper
    @Inject lateinit var entityExtractionHelper: EntityExtractionHelper

    //Inject TextRecognitionHelper
    @Inject lateinit var textRecognitionHelper: TextRecognitionHelper

    // Coroutine scope for service, run on IO thread
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Listen the change of screenshot
    private lateinit var contentObserver: ContentObserver

    //Saved state of last processed screenshot
    private var lastProcessedId: Long = -1L
    private var lastProcessedTime: Long = 0L

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        registerScreenshotObserver()
    }

    private fun startForegroundService() {
        val channelId = "screenshot_service_channel"
        val channel = NotificationChannel(
            channelId,
            "Screenshot Monitor",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("The Screenshot Brain")
            .setContentText("Đang lắng nghe chụp ảnh...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    private fun registerScreenshotObserver() {
        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                handleNewImage()
            }
        }

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )
    }

    private fun handleNewImage() {
        // Take new image
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
        )

        // Sort by day
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                    val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))

                    // Filter
                    val currentTime = System.currentTimeMillis() / 1000

                    // If this id doesnt process less than 1.5s, skip
                    if (id == lastProcessedId && (currentTime - lastProcessedTime) < 1500) {
                        return // Exit
                    }

                    val unixTime = System.currentTimeMillis() / 1000
                    if (unixTime - dateAdded <= 10 && name.contains("Screenshot", ignoreCase = true)) {
                        lastProcessedId = id
                        lastProcessedTime = currentTime

                        val contentUri = Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        )

                        Log.d("ScreenshotService", "Phát hiện screenshot: $contentUri")
                        processScreenshot(contentUri, dateAdded * 1000)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Logic process screenshot
    private fun processScreenshot(uri: Uri, timestamp: Long) {
        serviceScope.launch {
            try {
                // read text and find qr code
                val ocrJob = async { textRecognitionHelper.processImageForText(uri) }
                val qrJob = async { barcodeHelper.extractQrCode(uri) }

                // Đợi cả 2 cùng xong (await)
                val mlText = ocrJob.await()
                val qrContent = qrJob.await()

                val entities = entityExtractionHelper.extractEntities(mlText.text)

                val result = DataAnalyzer.analyze(mlText, qrContent, entities)

                var finalUriString = uri.toString()

                if (result.type == ScreenshotEntity.TYPE_NOTE || result.type == ScreenshotEntity.TYPE_BANK) {
                    val internalPath = saveToInternalStorage(uri)
                    if (internalPath != null) {
                        finalUriString = internalPath
                        Log.d("ScreenshotService", "Đã lưu an toàn vào: $finalUriString")
                    }
                }


                if (result.extractedContent.isNotBlank() || mlText.text.isNotBlank()) {

                    val entity = ScreenshotEntity(
                        uri = finalUriString,
                        rawText = mlText.text,
                        extractedText = result.extractedContent,
                        type = result.type,
                        timestamp = timestamp
                    )

                    repository.saveScreenshot(entity)

                    // Hiện bong bóng
                    withContext(Dispatchers.Main) {
                        overlayManager.showFloatingBubble(result.extractedContent, result.type)
                    }
                }
            } catch (e: Exception) {
                Log.e("ScreenshotService", "Lỗi xử lý ảnh: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    //Saved screenshot into internal storage
    private fun saveToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = "note_${System.currentTimeMillis()}.jpg"

            val outputStream = openFileOutput(fileName, MODE_PRIVATE)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            getFileStreamPath(fileName).absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(contentObserver)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}