package com.example.thescreenshotbrain.presentation.screens.setting

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.thescreenshotbrain.data.service.ScreenshotDetectionService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    // Lưu trạng thái service vào SharedPreferences đơn giản
    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    var isServiceEnabled by remember {
        mutableStateOf(prefs.getBoolean("service_enabled", true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // Mục: Bật/Tắt Service
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tự động phát hiện", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Lắng nghe ảnh chụp màn hình và hiện bong bóng",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isServiceEnabled,
                    onCheckedChange = { enabled ->
                        isServiceEnabled = enabled
                        prefs.edit().putBoolean("service_enabled", enabled).apply()

                        // Kích hoạt/Tắt Service ngay lập tức
                        val intent = Intent(context, ScreenshotDetectionService::class.java)
                        if (enabled) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        } else {
                            context.stopService(intent)
                        }
                    }
                )
            }
        }
    }
}