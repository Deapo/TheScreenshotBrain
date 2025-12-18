package com.example.thescreenshotbrain.core.common

import android.content.Context
import com.google.mlkit.nl.entityextraction.Entity
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
    // Cấu hình cho Tiếng Anh (ML Kit hỗ trợ tốt nhất) và Tiếng Việt (nếu hỗ trợ tùy version)
    // Hiện tại ta dùng ENGLISH vì nó nhận diện số/giờ giấc (Universal) rất tốt
    private val entityExtractor: EntityExtractor = EntityExtraction.getClient(
        EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
    )

    suspend fun extractEntities(text: String): List<EntityAnnotation?>? {
        return try {
            // 1. Tải Model nếu chưa có (Chỉ tải lần đầu, các lần sau sẽ nhanh)
            entityExtractor.downloadModelIfNeeded().await()

            // 2. Thực hiện trích xuất
            val params = EntityExtractionParams.Builder(text).build()
            val result = entityExtractor.annotate(params).await()

            // 3. Trả về danh sách thực thể tìm thấy
            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}