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
        //get data from db
        val flow = if (query.isBlank()) repository.getAllScreenshots() else repository.searchScreenshots(query)

        //logic
        flow.map { list ->
            when (type) {
                ScreenshotEntity.TYPE_BANK -> list.filter { it.type == ScreenshotEntity.TYPE_BANK }
                ScreenshotEntity.TYPE_NOTE -> list.filter { it.type == ScreenshotEntity.TYPE_NOTE }
                ScreenshotEntity.TYPE_URL -> list.filter { it.type == ScreenshotEntity.TYPE_URL }
                ScreenshotEntity.TYPE_PHONE -> list.filter { it.type == ScreenshotEntity.TYPE_PHONE }
                ScreenshotEntity.TYPE_EVENT -> list.filter { it.type == ScreenshotEntity.TYPE_EVENT }
                ScreenshotEntity.TYPE_MAP -> list.filter { it.type == ScreenshotEntity.TYPE_MAP }
                ScreenshotEntity.TYPE_OTHER -> list.filter { it.type == ScreenshotEntity.TYPE_OTHER }

                null -> list

                else -> list.filter { it.type == type }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}