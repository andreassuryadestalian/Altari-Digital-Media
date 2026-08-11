package com.example.features.remote

import com.example.model.LyricsContent
import com.example.presentation.PresentationServer
import com.example.presentation.PresentationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed class RemoteCommand {
    data object NextSlide : RemoteCommand()
    data object PreviousSlide : RemoteCommand()
    data object BlackOut : RemoteCommand()
    data object ClearDisplay : RemoteCommand()
    data class JumpToSlide(val slideIndex: Int) : RemoteCommand()
}

sealed class RemoteEvent {
    data class StateUpdated(val state: PresentationState) : RemoteEvent()
    data class Notification(val message: String) : RemoteEvent()
}

class RemoteServerEngine(private val presentationServer: PresentationServer) {
    private val _pairingPin = MutableStateFlow("4829")
    val pairingPin: StateFlow<String> = _pairingPin.asStateFlow()

    private val activeSessions = mutableSetOf<String>()

    private val _eventFlow = MutableSharedFlow<RemoteEvent>(replay = 1)
    val eventFlow: SharedFlow<RemoteEvent> = _eventFlow.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        // Synchronize PresentationServer state updates to WebSocket event stream
        scope.launch {
            presentationServer.state.collect { state ->
                _eventFlow.emit(RemoteEvent.StateUpdated(state))
            }
        }
    }

    fun generateNewPin(): String {
        val newPin = (1000..9999).random().toString()
        _pairingPin.value = newPin
        return newPin
    }

    fun authenticatePairing(pinInput: String): String? {
        return if (pinInput.trim() == _pairingPin.value) {
            val token = UUID.randomUUID().toString()
            activeSessions.add(token)
            scope.launch {
                _eventFlow.emit(RemoteEvent.Notification("New Remote Client Paired Successfully"))
            }
            token
        } else {
            null
        }
    }

    fun isSessionValid(token: String): Boolean {
        return activeSessions.contains(token)
    }

    fun sendCommand(sessionToken: String, command: RemoteCommand): Boolean {
        if (!isSessionValid(sessionToken)) return false

        when (command) {
            is RemoteCommand.NextSlide -> presentationServer.nextSlide()
            is RemoteCommand.PreviousSlide -> presentationServer.previousSlide()
            is RemoteCommand.BlackOut -> presentationServer.black()
            is RemoteCommand.ClearDisplay -> presentationServer.clear()
            is RemoteCommand.JumpToSlide -> presentationServer.setSlideIndex(command.slideIndex)
        }
        return true
    }
}
