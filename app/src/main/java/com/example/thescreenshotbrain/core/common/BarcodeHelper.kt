package com.example.thescreenshotbrain.core.common

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class BarcodeHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Cấu hình chỉ quét QR Code để tối ưu tốc độ
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    suspend fun extractQrCode(uri: Uri): String? {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val barcodes = scanner.process(image).await()

            // Lấy giá trị raw của mã QR đầu tiên tìm thấy
            val qrContent = barcodes.firstOrNull()?.rawValue

            if (qrContent != null) {
                Log.d("BarcodeHelper", "Tìm thấy QR: $qrContent")
            }
            return qrContent
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}