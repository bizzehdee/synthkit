package com.bizzeh.synthkit.looper

import androidx.lifecycle.ViewModel
import com.bizzeh.synthkit.audio.InstrumentPlayer
import com.bizzeh.synthkit.audio.Transport
import com.bizzeh.synthkit.project.Project
import java.util.UUID

/** Keeps one open project's session alive while its screens come and go. */
interface SessionHost {
    fun open(project: Project, transport: Transport, player: InstrumentPlayer, onChange: (Project) -> Unit): LooperSession

    fun close()
}

class SessionViewModel : ViewModel(), SessionHost {
    private var session: LooperSession? = null
    private var projectId: String? = null

    override fun open(
        project: Project,
        transport: Transport,
        player: InstrumentPlayer,
        onChange: (Project) -> Unit,
    ): LooperSession = session?.takeIf { projectId == project.id }
        ?: LooperSession(transport, player, project, onChange) { UUID.randomUUID().toString() }.also {
            session?.close()
            session = it
            projectId = project.id
        }

    override fun close() {
        session?.close()
        session = null
        projectId = null
    }

    // The engine's own view model may already have closed the engine: clearing
    // order is not defined. Closing the engine stops every sound, so the session
    // is only dropped here, never asked to stop.
    override fun onCleared() {
        session = null
        projectId = null
    }
}
