package com.example.thescreenshotbrain.core.common

import android.content.Context
import com.google.mlkit.nl.entityextraction.EntityAnnotation
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractionParams
import com.google.mlkit.nl.entityextraction.EntityExtractor
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class EntityExtractionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Dùng ENGLISH để tối ưu nhận diện ngày giờ/số (Universal)
    private val entityExtractor: EntityExtractor = EntityExtraction.getClient(
        EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
    )

    // SỬA: Trả về List<EntityAnnotation> (Không Nullable)
    suspend fun extractEntities(text: String): List<EntityAnnotation> {
        return try {
            entityExtractor.downloadModelIfNeeded().await()

            val params = EntityExtractionParams.Builder(text).build()

            // Hàm annotate trả về List<EntityAnnotation>
            entityExtractor.annotate(params).await()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}