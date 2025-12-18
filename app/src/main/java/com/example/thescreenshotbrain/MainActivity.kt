package com.example.thescreenshotbrain

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.thescreenshotbrain.data.service.ScreenshotDetectionService
import com.example.thescreenshotbrain.presentation.screens.history.HistoryScreen
import com.example.thescreenshotbrain.presentation.screens.setting.SettingScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    // 1. Định nghĩa danh sách quyền cần xin (Tùy theo phiên bản Android)
    private val permissionsToRequest = mutableListOf<String>().apply {
        // Quyền đọc ảnh
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.POST_NOTIFICATIONS) // Quyền thông báo
        } else { // Android 12 trở xuống
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    // 2. Tạo Launcher để hứng kết quả xin quyền
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Kiểm tra xem user có đồng ý hết không
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Nếu đã cấp quyền Storage/Noti -> Tiếp tục kiểm tra quyền Overlay
            checkOverlayPermission()
        } else {
            Toast.makeText(this, "Bạn cần cấp quyền Thư viện và Thông báo để App hoạt động!", Toast.LENGTH_LONG).show()
            // Mở trang Cài đặt ứng dụng để user cấp tay nếu họ lỡ bấm "Từ chối vĩnh viễn"
            openAppSettings()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 3. Bắt đầu quy trình kiểm tra quyền ngay khi mở App
        checkAndRequestPermissions()

        // Khởi động Service nếu đã bật trong setting
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("service_enabled", true)) {
            startScreenshotService()
        }

        setContent {
            MaterialTheme {
                var currentScreen by remember { mutableStateOf("history") }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (currentScreen) {
                        "history" -> HistoryScreen(
                            onNavigateToSettings = { currentScreen = "settings" }
                        )
                        "settings" -> SettingScreen(
                            onBack = { currentScreen = "history" }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        // Lọc ra các quyền CHƯA được cấp
        val permissionsNeeded = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isNotEmpty()) {
            // Nếu thiếu quyền -> Hiện hộp thoại xin quyền hệ thống
            requestPermissionLauncher.launch(permissionsNeeded.toTypedArray())
        } else {
            // Nếu đã đủ quyền Storage/Noti -> Kiểm tra tiếp quyền Overlay
            checkOverlayPermission()
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Bước cuối: Cấp quyền 'Hiển thị trên ứng dụng khác'", Toast.LENGTH_LONG).show()

            // Mở màn hình cài đặt Overlay
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            try {
                startActivityForResult(intent, 101)
            } catch (e: Exception) {
                openAppSettings()
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }

    private fun startScreenshotService() {
        // Chỉ start service khi đã có đủ quyền Overlay
        if (Settings.canDrawOverlays(this)) {
            val intent = Intent(this, ScreenshotDetectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }
}