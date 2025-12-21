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
import com.example.thescreenshotbrain.presentation.navigation.NavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val permissionsToRequest = mutableListOf<String>().apply {
        //permission read image
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.POST_NOTIFICATIONS) // permisson noti
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    //create launch activity for result
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        //check all permisson granted
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            //if all granted -> check permission overlay
            checkOverlayPermission()
        } else {
            Toast.makeText(this, "Bạn cần cấp quyền Thư viện và Thông báo để App hoạt động!", Toast.LENGTH_LONG).show()
            // If not granted, open app settings
            openAppSettings()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //start to check permisson and request
        checkAndRequestPermissions()

        //start service
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("service_enabled", true)) {
            startScreenshotService()
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NavGraph()
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        //filter permission not granted
        val permissionsNeeded = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isNotEmpty()) {
            //ìf not granted -> request
            requestPermissionLauncher.launch(permissionsNeeded.toTypedArray())
        } else {
            //if granted -> check permission overlay
            checkOverlayPermission()
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Bước cuối: Cấp quyền 'Hiển thị trên ứng dụng khác'", Toast.LENGTH_LONG).show()

            //open overlay permission
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
        // only start service if permission granted
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