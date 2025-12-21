package com.example.thescreenshotbrain.presentation.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thescreenshotbrain.data.local.entity.ScreenshotEntity
import com.example.thescreenshotbrain.domain.repository.ScreenshotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: ScreenshotRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _screenshot = MutableStateFlow<ScreenshotEntity?>(null)
    val screenshot: StateFlow<ScreenshotEntity?> = _screenshot.asStateFlow()

    init {
        //get screenshot id from navigation
        val screenshotId = savedStateHandle.get<Long>("screenshotId")
        if (screenshotId != null) {
            getScreenshotDetail(screenshotId)
        }
    }

    private fun getScreenshotDetail(id: Long) {
        viewModelScope.launch {
            val item = repository.getScreenshotById(id)
            _screenshot.value = item
        }
    }
}