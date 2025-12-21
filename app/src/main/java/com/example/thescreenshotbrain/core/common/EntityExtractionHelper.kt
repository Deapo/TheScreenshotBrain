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

    private val entityExtractor: EntityExtractor = EntityExtraction.getClient(
        EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
    )

    suspend fun extractEntities(text: String): List<EntityAnnotation> {
        return try {
            entityExtractor.downloadModelIfNeeded().await()

            val params = EntityExtractionParams.Builder(text).build()

            entityExtractor.annotate(params).await()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}