package com.example.features.display

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import com.example.presentation.PresentationState
import kotlinx.coroutines.flow.StateFlow

class DisplayRenderer(
    private val context: Context,
    private val presentationStateFlow: StateFlow<PresentationState>
) {
    private val activePresentations = mutableMapOf<Int, Presentation>()
    private val displayProfiles = mutableMapOf<Int, DisplayProfileType>()
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    private var onDisplayChangeListener: (() -> Unit)? = null

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            Log.d("DisplayRenderer", "Display Connected: $displayId")
            onDisplayChangeListener?.invoke()
            // Auto-recovery attempt for newly attached presentation display
            val display = displayManager.getDisplay(displayId)
            if (display != null && displayId != Display.DEFAULT_DISPLAY) {
                val profile = displayProfiles[displayId] ?: DisplayProfileType.MAIN_PROJECTOR
                connectDisplay(display, profile)
            }
        }

        override fun onDisplayRemoved(displayId: Int) {
            Log.d("DisplayRenderer", "Display Removed: $displayId")
            disconnectDisplay(displayId)
            onDisplayChangeListener?.invoke()
        }

        override fun onDisplayChanged(displayId: Int) {
            onDisplayChangeListener?.invoke()
        }
    }

    init {
        try {
            displayManager.registerDisplayListener(displayListener, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setOnDisplayChangeListener(listener: () -> Unit) {
        onDisplayChangeListener = listener
    }

    fun detectDisplays(): List<Display> {
        return displayManager.displays.filter { it.displayId != Display.DEFAULT_DISPLAY }
    }

    fun getAllDisplays(): List<Display> {
        return displayManager.displays.toList()
    }

    fun connectDisplay(display: Display, profileType: DisplayProfileType = DisplayProfileType.MAIN_PROJECTOR) {
        if (display.displayId == Display.DEFAULT_DISPLAY) {
            Log.w("DisplayRenderer", "Cannot launch Presentation on DEFAULT_DISPLAY")
            return
        }
        disconnectDisplay(display.displayId)
        displayProfiles[display.displayId] = profileType

        try {
            val presentation = object : Presentation(context, display) {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    try {
                        val composeView = ComposeView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setContent {
                                val state by presentationStateFlow.collectAsState()
                                PresentationFrameRenderer(
                                    state = state,
                                    profileType = profileType
                                )
                            }
                        }
                        setContentView(composeView)
                    } catch (e: Throwable) {
                        Log.e("DisplayRenderer", "Error inside Presentation onCreate", e)
                    }
                }
            }

            presentation.show()
            activePresentations[display.displayId] = presentation
        } catch (e: Throwable) {
            Log.e("DisplayRenderer", "Failed to launch presentation on display ${display.displayId}", e)
        }
    }

    fun disconnectDisplay(displayId: Int) {
        activePresentations[displayId]?.dismiss()
        activePresentations.remove(displayId)
        displayProfiles.remove(displayId)
    }

    fun disconnectAll() {
        activePresentations.values.forEach { it.dismiss() }
        activePresentations.clear()
        displayProfiles.clear()
    }

    fun isDisplayConnected(displayId: Int): Boolean {
        return activePresentations[displayId]?.isShowing == true
    }

    fun getActiveProfile(displayId: Int): DisplayProfileType {
        return displayProfiles[displayId] ?: DisplayProfileType.MAIN_PROJECTOR
    }

    fun destroy() {
        try {
            displayManager.unregisterDisplayListener(displayListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        disconnectAll()
    }
}
