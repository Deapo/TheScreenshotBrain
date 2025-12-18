package com.example.thescreenshotbrain.presentation.screens.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.thescreenshotbrain.data.local.entity.ScreenshotEntity
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

    // Scroll state cho hàng Filter (để cuộn ngang nếu nhiều nút)
    val scrollState = rememberScrollState()

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

                // Filter type (Thêm horizontalScroll để cuộn ngang)
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .horizontalScroll(scrollState), // <- Cho phép cuộn ngang
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. WEB
                    FilterChip(
                        selected = currentFilter == ScreenshotEntity.TYPE_URL,
                        onClick = { viewModel.onFilterSelected(ScreenshotEntity.TYPE_URL) },
                        label = { Text("Web") }
                    )

                    // 2. BANK (Đã gỡ bỏ bảo mật, bấm là chọn luôn)
                    FilterChip(
                        label = { Text("Bank") },
                        selected = currentFilter == ScreenshotEntity.TYPE_BANK,
                        onClick = { viewModel.onFilterSelected(ScreenshotEntity.TYPE_BANK) }
                    )

                    // 3. PHONE
                    FilterChip(
                        selected = currentFilter == ScreenshotEntity.TYPE_PHONE,
                        onClick = { viewModel.onFilterSelected(ScreenshotEntity.TYPE_PHONE) },
                        label = { Text("SĐT") }
                    )

                    // 4. CALENDAR (LỊCH)
                    FilterChip(
                        selected = currentFilter == ScreenshotEntity.TYPE_EVENT,
                        onClick = { viewModel.onFilterSelected(ScreenshotEntity.TYPE_EVENT) },
                        label = { Text("Lịch") }
                    )

                    // 5. MAP (BẢN ĐỒ)
                    FilterChip(
                        selected = currentFilter == ScreenshotEntity.TYPE_MAP,
                        onClick = { viewModel.onFilterSelected(ScreenshotEntity.TYPE_MAP) },
                        label = { Text("Map") }
                    )

                    // 6. NOTE
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