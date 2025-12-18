package com.example.thescreenshotbrain.presentation.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thescreenshotbrain.data.local.entity.ScreenshotEntity
import com.example.thescreenshotbrain.domain.repository.ScreenshotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ScreenshotRepository
) : ViewModel() {
    // var saved state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow<String?>(null)
    val filterType = _filterType.asStateFlow()


    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(type: String?) {
        // Nếu chọn lại cái đang chọn thì bỏ chọn (Toggle)
        if (_filterType.value == type) {
            _filterType.value = null
        } else {
            _filterType.value = type
        }
    }

    fun deleteScreenshot(screenshot: ScreenshotEntity) {
        viewModelScope.launch {
            repository.deleteScreenshot(screenshot)
        }
    }

    val screenshots: StateFlow<List<ScreenshotEntity>> = combine(
        _searchQuery,
        _filterType
    ) { query, type ->
        Pair(query, type)
    }.flatMapLatest { (query, type) ->
        // 1. Lấy dữ liệu thô từ DB
        val flow = if (query.isBlank()) repository.getAllScreenshots() else repository.searchScreenshots(query)

        // 2. Xử lý Logic lọc (QUAN TRỌNG)
        flow.map { list ->
            when (type) {
                // CASE A: Người dùng đang chọn một bộ lọc cụ thể
                // (Lúc này UI đã lo việc xác thực vân tay rồi, ViewModel chỉ việc trả về đúng loại đó)
                ScreenshotEntity.TYPE_BANK -> list.filter { it.type == ScreenshotEntity.TYPE_BANK }
                ScreenshotEntity.TYPE_NOTE -> list.filter { it.type == ScreenshotEntity.TYPE_NOTE }
                ScreenshotEntity.TYPE_URL -> list.filter { it.type == ScreenshotEntity.TYPE_URL }
                ScreenshotEntity.TYPE_PHONE -> list.filter { it.type == ScreenshotEntity.TYPE_PHONE }

                // CASE B: Người dùng KHÔNG chọn bộ lọc nào (Màn hình chính mặc định)
                // -> Phải ẨN các nội dung nhạy cảm (Bank, Note) đi
                null -> list.filter { item ->
                    item.type != ScreenshotEntity.TYPE_BANK
                }

                // Fallback (cho các trường hợp khác nếu có)
                else -> list.filter { it.type == type }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}
