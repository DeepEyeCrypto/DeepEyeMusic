package com.deepeye.musicpro.aeos.control_plane

import com.deepeye.musicpro.aeos.data_plane.MusicDataPlane
import com.deepeye.musicpro.aeos.mesh.MusicIntent
import com.deepeye.musicpro.aeos.mesh.MusicState
import com.deepeye.musicpro.aeos.observability.EbpfRuntimeSecurity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AEOS Day-0 Vibe-Coding Genesis
 * Music Control Plane - Orchestrates intents from UI to Data Plane safely.
 */
class MusicControlPlane(private val dataPlane: MusicDataPlane = MusicDataPlane()) : ControlPlaneBoundary() {
    
    private val _uiState = MutableStateFlow(MusicState())
    val uiState: StateFlow<MusicState> = _uiState.asStateFlow()
    
    private val controlPlaneScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun dispatchIntent(intent: MusicIntent) {
        super.invokePolicy(intent.javaClass.simpleName)
        // Ensure all state emissions and intensive operations do not block the Main thread
        controlPlaneScope.launch {
            try {
                val newState = dataPlane.execute(intent)
                _uiState.value = newState
            } catch (e: Exception) {
                // Graceful Chaos Engineering Catch
                EbpfRuntimeSecurity.traceExecution("CONTROL_PLANE", "FAULT INJECTION CAUGHT: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isSystemFailure = true,
                    errorMessage = e.message ?: "Unknown AEOS Error"
                )
            }
        }
    }
}
