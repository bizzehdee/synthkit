package com.bizzeh.synthkit.looper

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.ui.studio.BeatLights
import com.bizzeh.synthkit.ui.studio.LcdDisplay
import com.bizzeh.synthkit.ui.studio.LcdValue
import com.bizzeh.synthkit.ui.studio.RecTag
import com.bizzeh.synthkit.ui.studio.RoundPlayStopButton
import com.bizzeh.synthkit.ui.studio.RoundRecordButton

/** Record arms or starts a take; while a take runs it ends it. */
@Composable
fun RecordButton(recording: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    RoundRecordButton(
        live = recording,
        enabled = enabled,
        contentDescription = stringResource(if (recording) R.string.stop_recording else R.string.record),
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun PlayStopButton(playing: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    RoundPlayStopButton(
        playing = playing,
        enabled = enabled,
        contentDescription = stringResource(if (playing) R.string.stop else R.string.play),
        onClick = onClick,
        modifier = modifier,
    )
}

/**
 * The transport display: REC while recording, the beat lights, the bar and the
 * tempo. TalkBack reads the full status sentence instead of the short codes.
 */
@Composable
fun TransportLcd(state: LooperState, modifier: Modifier = Modifier, inTrack: Boolean = false, showTempo: Boolean = true) {
    val status = phaseText(state, inTrack)
    val bar = state.bar + 1
    val bars = state.project.loopBars
    val code = when {
        state.finishing -> stringResource(R.string.lcd_saving)
        state.phase == Phase.ARMED -> stringResource(R.string.lcd_ready)
        state.phase == Phase.RECORDING_FIRST -> stringResource(R.string.lcd_bar, bar)
        state.phase == Phase.PLAYING || state.phase == Phase.OVERDUBBING -> stringResource(R.string.lcd_bar_of, bar, bars)
        bars == 0 -> stringResource(R.string.lcd_no_loop)
        else -> stringResource(R.string.lcd_bar_of, 1, bars)
    }
    LcdDisplay(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = status
            liveRegion = LiveRegionMode.Polite
        },
    ) {
        if (state.phase == Phase.RECORDING_FIRST || state.phase == Phase.OVERDUBBING) RecTag(stringResource(R.string.lcd_rec))
        BeatLights(state.beat)
        LcdValue(code)
        if (showTempo) LcdValue(state.project.tempoBpm.toString(), stringResource(R.string.lcd_bpm))
    }
}

/** One sentence saying what the looper is doing and where it is. */
@Composable
fun phaseText(state: LooperState, inTrack: Boolean = false): String {
    val bar = state.bar + 1
    val bars = state.project.loopBars
    return when {
        state.finishing -> stringResource(R.string.phase_finishing)
        state.phase == Phase.ARMED -> stringResource(R.string.phase_armed)
        state.phase == Phase.RECORDING_FIRST -> stringResource(R.string.phase_recording_first, bar)
        state.phase == Phase.PLAYING -> stringResource(R.string.phase_playing, bar, bars)
        state.phase == Phase.OVERDUBBING -> stringResource(R.string.phase_overdubbing, bar, bars)
        bars == 0 -> stringResource(if (inTrack) R.string.phase_no_loop_track else R.string.phase_no_loop)
        else -> stringResource(R.string.phase_stopped)
    }
}

val LooperState.isRecording: Boolean
    get() = phase == Phase.ARMED || phase == Phase.RECORDING_FIRST || phase == Phase.OVERDUBBING

val LooperState.isPlaying: Boolean
    get() = phase != Phase.STOPPED
