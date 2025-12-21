package com.example.thescreenshotbrain.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TextRecognitionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun processImageForText(uri: Uri): Text {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image).await()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
    
    suspend fun processBitmapForText(bitmap: Bitmap): Text {
        return try {
            //Check bitmap
            if (bitmap.isRecycled) {
                throw IllegalStateException("Bitmap has been recycled")
            }
            
            //Convert bitmap to InputImage
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image).await()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun extractText(uri: Uri): String {
        return try {
            val result = processImageForText(uri)
            result.text
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
