package com.example.thescreenshotbrain.presentation.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thescreenshotbrain.data.local.entity.ScreenshotEntity
import com.example.thescreenshotbrain.core.common.BiometricHelper
import com.example.thescreenshotbrain.presentation.screens.history.components.ScreenshotItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit // Callback để mở màn hình Settings
) {
    val screenshots by viewModel.screenshots.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFilter by viewModel.filterType.collectAsState()

    val context = LocalContext.current
    val activity = context as? FragmentActivity

    Scaffold(
        topBar = {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)) {
                CenterAlignedTopAppBar(
                    title = { Text("The Screenshot Brain") },
                    actions = {
                        // Button Setting
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )

                // Search
                SearchBarComponent(
                    query = searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) }
                )

                // Filter type
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentFilter == ScreenshotEntity.TYPE_URL,
                        onClick = { viewModel.onFilterSelected(ScreenshotEntity.TYPE_URL) },
                        label = { Text("Web") }
                    )
                    FilterChip(
                        label = { Text("Bank") },
                        selected = currentFilter == ScreenshotEntity.TYPE_BANK,
                        onClick = {
                            // Logic: Nếu đang chưa chọn Bank -> Đòi vân tay
                            if (currentFilter != ScreenshotEntity.TYPE_BANK) {

                                // 1. Tìm Activity bằng hàm mới viết
                                val activity = context.findActivity()

                                // 2. Kiểm tra tính khả dụng
                                val isBioAvailable = BiometricHelper.isBiometricAvailable(context)

                                Log.d("BiometricTest", "Activity tìm thấy: $activity")
                                Log.d("BiometricTest", "Máy có vân tay không: $isBioAvailable")

                                if (activity != null && isBioAvailable) {
                                    // GỌI HỘP THOẠI
                                    BiometricHelper.showBiometricPrompt(
                                        activity = activity,
                                        onSuccess = {
                                            Log.d("BiometricTest", "Quét thành công -> Mở Filter")
                                            viewModel.onFilterSelected(ScreenshotEntity.TYPE_BANK)
                                        },
                                        onError = {
                                            Log.d("BiometricTest", "Lỗi hoặc Hủy")
                                        }
                                    )
                                } else {
                                    // Fallback: Nếu không tìm thấy Activity hoặc máy không có vân tay -> Mở luôn
                                    Log.w("BiometricTest", "Bỏ qua bảo mật (Do lỗi context hoặc máy không hỗ trợ)")
                                    viewModel.onFilterSelected(ScreenshotEntity.TYPE_BANK)
                                }
                            } else {
                                // Đang chọn rồi thì tắt đi (Toggle Off)
                                viewModel.onFilterSelected(ScreenshotEntity.TYPE_BANK)
                            }
                        }
                    )
                    FilterChip(
                        selected = currentFilter == ScreenshotEntity.TYPE_PHONE,
                        onClick = { viewModel.onFilterSelected(ScreenshotEntity.TYPE_PHONE) },
                        label = { Text("SĐT") }
                    )
                    FilterChip(
                        selected = currentFilter == ScreenshotEntity.TYPE_NOTE,
                        onClick = { viewModel.onFilterSelected(ScreenshotEntity.TYPE_NOTE) },
                        label = { Text("Note") }
                    )
                }
            }
        }
    ) { paddingValues ->
        if (screenshots.isEmpty()) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Không có dữ liệu", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)) {
                items(screenshots, key = { it.id }) { screenshot ->
                    ScreenshotItem(
                        item = screenshot,
                        onClick = {},
                        onDeleteClick = {
                            viewModel.deleteScreenshot(screenshot)
                        }
                    )
                }
            }
        }
    }
}


// Component search
@Composable
fun SearchBarComponent(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        placeholder = { Text("Tìm kiếm...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        shape = MaterialTheme.shapes.extraLarge,
        singleLine = true
    )
}

fun Context.findActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}