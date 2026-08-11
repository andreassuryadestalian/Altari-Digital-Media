package com.example.features.settings

import android.content.Context
import coil.Coil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SystemHealthState(
    val memoryUsageMb: Long = 0,
    val isNetworkConnected: Boolean = true,
    val activeDisplaysCount: Int = 1,
    val statusMessage: String = "System Healthy"
)

class SystemOptimizationEngine(private val context: Context) {
    private val _healthState = MutableStateFlow(SystemHealthState())
    val healthState: StateFlow<SystemHealthState> = _healthState.asStateFlow()

    fun runMemoryOptimization() {
        try {
            // Clear Coil Image Caches to free bitmap heap memory
            Coil.imageLoader(context).memoryCache?.clear()
            System.gc()

            val runtime = Runtime.getRuntime()
            val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)

            _healthState.value = _healthState.value.copy(
                memoryUsageMb = usedMemory,
                statusMessage = "Memory Cache Cleared & Garbage Collected"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun checkSystemHealth(displayCount: Int): SystemHealthState {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)

        val state = SystemHealthState(
            memoryUsageMb = usedMemory,
            isNetworkConnected = true,
            activeDisplaysCount = displayCount,
            statusMessage = if (usedMemory > 250) "High Memory Warning" else "System Optimal"
        )
        _healthState.value = state
        return state
    }
}
