package com.deepeye.musicpro.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.musicpro.data.cache.CacheManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel
@Inject
constructor(
    private val cacheManager: CacheManager,
) : ViewModel() {
    private val _cacheLoadComplete = MutableStateFlow(false)
    val cacheLoadComplete = _cacheLoadComplete.asStateFlow()

    init {
        viewModelScope.launch {
            // Check if there is anything in cache.
            // Even if there isn't, we stop the splash screen instantly so the app can start
            cacheManager.loadCachedRecommendations()
            _cacheLoadComplete.value = true
        }
    }
}
