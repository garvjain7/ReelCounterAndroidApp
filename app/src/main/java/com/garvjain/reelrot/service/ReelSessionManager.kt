package com.garvjain.reelrot.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object ReelSessionManager {
    // Map of Platform to current session count
    private val _sessionCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val sessionCounts: StateFlow<Map<String, Int>> = _sessionCounts.asStateFlow()

    fun increment(platform: String) {
        _sessionCounts.update { current ->
            val count = current[platform] ?: 0
            current + (platform to count + 1)
        }
    }

    fun reset(): Map<String, Int> {
        val current = _sessionCounts.value
        _sessionCounts.value = emptyMap()
        return current
    }
}
