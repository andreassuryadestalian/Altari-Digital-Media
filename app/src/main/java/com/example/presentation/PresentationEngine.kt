package com.example.presentation

import com.example.model.PresentationContent
import kotlinx.coroutines.flow.StateFlow

interface PresentationEngine {
    val state: StateFlow<PresentationState>

    fun go(content: PresentationContent)
    fun nextSlide()
    fun previousSlide()
    fun clear()
    fun black()
    fun selectDisplay(displayId: String)
}
