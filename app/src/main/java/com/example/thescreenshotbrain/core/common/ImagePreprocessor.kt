package com.example.thescreenshotbrain.core.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object ImagePreprocessor {
    suspend fun cropTopArea(
        uri: Uri,
        context: Context,
        topCropPx: Int = 150
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (originalBitmap == null) return@withContext null
            
            val width = originalBitmap.width
            val height = originalBitmap.height
            
            if (height <= topCropPx) {
                return@withContext originalBitmap
            }
            
            val maxDimension = 1024
            val workingBitmap = if (width > maxDimension || height > maxDimension) {
                val scale = maxDimension.toFloat() / maxOf(width, height)
                val scaledWidth = (width * scale).toInt()
                val scaledHeight = (height * scale).toInt()
                Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true).also {
                    originalBitmap.recycle()
                }
            } else {
                originalBitmap
            }
            
            val finalHeight = workingBitmap.height
            val finalWidth = workingBitmap.width
            val cropY = if (workingBitmap != originalBitmap) {
                (topCropPx * (finalHeight.toFloat() / height)).toInt()
            } else {
                topCropPx
            }
            
            val croppedBitmap = if (finalHeight > cropY) {
                Bitmap.createBitmap(
                    workingBitmap,
                    0,
                    cropY,
                    finalWidth,
                    finalHeight - cropY
                )
            } else {
                workingBitmap
            }
            
            if (workingBitmap != originalBitmap && croppedBitmap != workingBitmap) {
                workingBitmap.recycle()
            }
            
            croppedBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun saveProcessedImage(
        bitmap: Bitmap,
        context: Context,
        fileName: String = "processed_${System.currentTimeMillis()}.jpg",
        recycleAfterSave: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        try {
            val outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.close()
            val path = context.getFileStreamPath(fileName).absolutePath
            
            if (recycleAfterSave) {
                bitmap.recycle()
            }
            
            path
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

